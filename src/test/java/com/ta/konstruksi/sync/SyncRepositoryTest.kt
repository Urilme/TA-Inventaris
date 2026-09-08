package com.ta.konstruksi.sync

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.data.local.dao.LogMutasiDao
import com.ta.konstruksi.data.local.dao.ProyekDao
import com.ta.konstruksi.data.local.dao.TransferProyekDao
import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.data.local.dao.MasterKatalogDao
import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import com.ta.konstruksi.data.local.entity.ProyekEntity
import com.ta.konstruksi.data.local.entity.TransferProyekEntity
import com.ta.konstruksi.data.local.entity.UserSessionEntity
import com.ta.konstruksi.data.remote.FirestoreDataSource
import com.ta.konstruksi.data.repository.SyncRepositoryImpl
import com.ta.konstruksi.util.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Uji delta sync (state machine PENDING → IN_TRANSIT → SYNCED / rollback PENDING).
 * Firestore di-mock (bukan network nyata).
 */
class SyncRepositoryTest {

    private val mutasiDao = mockk<LogMutasiDao>(relaxed = true)
    private val transferDao = mockk<TransferProyekDao>(relaxed = true)
    private val firestore = mockk<FirestoreDataSource>(relaxed = true)
    private val userSessionDao = mockk<UserSessionDao>(relaxed = true)
    private val proyekDao = mockk<ProyekDao>(relaxed = true)
    private val katalogDao = mockk<MasterKatalogDao>(relaxed = true)

    private fun repo() = SyncRepositoryImpl(
        mutasiDao = mutasiDao,
        transferDao = transferDao,
        katalogDao = katalogDao,
        proyekDao = proyekDao,
        userSessionDao = userSessionDao,
        firestore = firestore
    )

    private fun session(
        idProyek: String = "PRJ-001",
        namaProyek: String = "Proyek A",
        lastSyncMutasi: Long = 0L,
        lastSyncTransfer: Long = 0L
    ) = UserSessionEntity(
        id_mandor = "M1", nama = "Budi", username = "budi", role = "mandor",
        auth_token = "tok", last_sync_timestamp = 0L,
        last_sync_mutasi = lastSyncMutasi, last_sync_transfer = lastSyncTransfer,
        id_proyek = idProyek, nama_proyek = namaProyek
     )

    private fun katalog(sku: String) = MasterKatalogEntity(
        sku_id = sku, kategori = "bahan", nama_barang = "Semen", satuan = "sak",
        stok_lokal = 10, id_proyek = "PRJ-001"
    )

    private fun remoteMutasi(id: String = "MX1", sku: String = "SKU-1", ts: Long = 100L) =
        LogMutasiEntity(
            id_transaksi = id, sku_id = sku, id_proyek = "PRJ-001",
            jenis_mutasi = "CHECK_OUT", jumlah = 2, local_timestamp = ts, sync_status = "SYNCED"
        )

    private fun remoteTransfer(id: String = "TRX1", ts: Long = 100L, status: String = "SELESAI") =
        TransferProyekEntity(
            id_transfer = id, sku_id = "SKU-1", nama_barang = "Semen",
            proyek_asal = "Proyek A", proyek_tujuan = "Proyek B", jumlah = 2,
            status_transfer = status, local_timestamp = ts, sync_status = "SYNCED"
        )

    private val entity = LogMutasiEntity(
        id_transaksi = "TX1", sku_id = "SKU-1", jenis_mutasi = "CHECK_OUT", jumlah = 2
    )

    @Test
    fun `sync mutasi PENDING sukses menandai SYNCED`() = runTest {
        coEvery { userSessionDao.getSession() } returns null
        coEvery { mutasiDao.getPendingBatch(Constants.SYNC_BATCH_SIZE) } returns listOf(entity)

        val result = repo().syncPendingMutasi()

        assertThat(result.getOrNull()).isEqualTo(1)
        coVerify { mutasiDao.updateSyncStatus("TX1", "IN_TRANSIT") } // tandai dikirim
        coVerify { mutasiDao.updateSyncStatus("TX1", "SYNCED") }     // sukses
    }

    @Test
    fun `sync mutasi gagal mengembalikan status ke PENDING`() = runTest {
        coEvery { userSessionDao.getSession() } returns null
        coEvery { mutasiDao.getPendingBatch(Constants.SYNC_BATCH_SIZE) } returns listOf(entity)
        coEvery { firestore.uploadMutasi(any(), any()) } throws RuntimeException("network down")

        val result = repo().syncPendingMutasi()

        assertThat(result.isFailure).isTrue()
        coVerify { mutasiDao.updateSyncStatus("TX1", "PENDING") }    // rollback
    }

    // ---- B1: batch unggah terbatas + deteksi sisa antrean ----

    @Test
    fun `sync mutasi hanya mengunggah satu batch (SYNC_BATCH_SIZE) per siklus`() = runTest {
        val batch = (1..Constants.SYNC_BATCH_SIZE).map { entity.copy(id_transaksi = "TX$it") }
        coEvery { userSessionDao.getSession() } returns null
        coEvery { mutasiDao.getPendingBatch(Constants.SYNC_BATCH_SIZE) } returns batch

        val result = repo().syncPendingMutasi()

        assertThat(result.getOrNull()).isEqualTo(Constants.SYNC_BATCH_SIZE)
        // Ambil TERBATAS lewat batch, bukan getPending() tak terbatas.
        coVerify { mutasiDao.getPendingBatch(Constants.SYNC_BATCH_SIZE) }
        coVerify(exactly = 0) { mutasiDao.getPending() }
        coVerify(exactly = Constants.SYNC_BATCH_SIZE) { firestore.uploadMutasi(any(), any()) }
    }

    @Test
    fun `hasPendingSync true bila salah satu koleksi masih menyisakan antrean`() = runTest {
        coEvery { katalogDao.countPendingSync() } returns 0
        coEvery { mutasiDao.countPending() } returns 3   // sisa > 0 → perlu siklus lanjutan
        coEvery { transferDao.countPending() } returns 0

        assertThat(repo().hasPendingSync()).isTrue()
    }

    @Test
    fun `hasPendingSync false bila semua koleksi tuntas`() = runTest {
        coEvery { katalogDao.countPendingSync() } returns 0
        coEvery { mutasiDao.countPending() } returns 0
        coEvery { transferDao.countPending() } returns 0

        assertThat(repo().hasPendingSync()).isFalse()
    }

    @Test
    fun `pull registry proyek mengganti cache lokal bila server mengembalikan data`() = runTest {
        val remote = listOf(ProyekEntity(id_proyek = "PRJ-001", nama_proyek = "Proyek A"))
        coEvery { firestore.pullProyek() } returns remote

        val result = repo().pullLatestProyek()

        assertThat(result.isSuccess).isTrue()
        coVerify { proyekDao.replaceAll(remote) }
    }

    @Test
    fun `pull registry proyek tidak menghapus cache saat server mengembalikan daftar kosong`() = runTest {
        coEvery { firestore.pullProyek() } returns emptyList()

        val result = repo().pullLatestProyek()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) { proyekDao.replaceAll(any()) }
    }

    // ---- Kursor delta memakai server_timestamp, bukan jam perangkat ----
    // pullLatestKatalog() memajukan kursor delta memakai result.latestServerMillis dari server,
    // bukan System.currentTimeMillis() perangkat, sehingga jam device tidak berpengaruh.
    @Test
    fun `kursor delta sync dimajukan memakai server_timestamp bukan jam perangkat`() = runTest {
        coEvery { userSessionDao.getSession() } returns com.ta.konstruksi.data.local.entity.UserSessionEntity(
            id_mandor = "M1", nama = "Budi", username = "budi", role = "mandor",
            auth_token = "tok", last_sync_timestamp = 1_000L, id_proyek = "PRJ-001"
        )
        val serverMillisTerbaru = 9_999_999L // nilai otoritatif dari Firestore, independen dari jam lokal
        coEvery { firestore.pullKatalog(sinceServerMillis = 1_000L, idProyek = "PRJ-001") } returns
            com.ta.konstruksi.data.remote.FirestoreDataSource.PullResult(
                items = emptyList(), latestServerMillis = serverMillisTerbaru
            )

        val result = repo().pullLatestKatalog()

        assertThat(result.isSuccess).isTrue()
        coVerify { userSessionDao.updateLastSync(serverMillisTerbaru) }
    }

    // ---- Pull mutasi (delta + FK guard + LWW + kursor terpisah) ----

    @Test
    fun `pull mutasi menyimpan entri baru dan memajukan kursor last_sync_mutasi`() = runTest {
        coEvery { userSessionDao.getSession() } returns session(lastSyncMutasi = 0L)
        coEvery { firestore.pullMutasi(0L, "PRJ-001") } returns
            FirestoreDataSource.MutasiPullResult(items = listOf(remoteMutasi(ts = 100L)), latestServerMillis = 500L)
        coEvery { katalogDao.getKatalogBySku("SKU-1") } returns katalog("SKU-1")
        coEvery { mutasiDao.getById("MX1") } returns null

        val result = repo().pullLatestMutasi()

        assertThat(result.isSuccess).isTrue()
        coVerify { mutasiDao.insert(match { it.id_transaksi == "MX1" }) }
        coVerify { userSessionDao.updateLastSyncMutasi(500L) }        // kursor mutasi maju sendiri
        coVerify(exactly = 0) { userSessionDao.updateLastSync(any()) } // bukan kursor katalog
    }

    @Test
    fun `pull mutasi melewati entri yang katalognya belum ada (FK guard)`() = runTest {
        coEvery { userSessionDao.getSession() } returns session()
        coEvery { firestore.pullMutasi(any(), any()) } returns
            FirestoreDataSource.MutasiPullResult(items = listOf(remoteMutasi()), latestServerMillis = 500L)
        coEvery { katalogDao.getKatalogBySku("SKU-1") } returns null   // induk tak ada

        val result = repo().pullLatestMutasi()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) { mutasiDao.insert(any()) }              // tidak crash, dilewati
    }

    @Test
    fun `pull mutasi tidak menimpa entri lokal yang masih PENDING`() = runTest {
        coEvery { userSessionDao.getSession() } returns session()
        coEvery { firestore.pullMutasi(any(), any()) } returns
            FirestoreDataSource.MutasiPullResult(items = listOf(remoteMutasi(ts = 999L)), latestServerMillis = 500L)
        coEvery { katalogDao.getKatalogBySku("SKU-1") } returns katalog("SKU-1")
        coEvery { mutasiDao.getById("MX1") } returns
            remoteMutasi(ts = 100L).copy(sync_status = "PENDING")      // perubahan lokal belum ter-push

        val result = repo().pullLatestMutasi()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) { mutasiDao.insert(any()) }              // lokal PENDING dilindungi
    }

    // ---- Pull transfer (delta + LWW + kursor terpisah) ----

    @Test
    fun `pull transfer menyimpan entri baru dan memajukan kursor last_sync_transfer`() = runTest {
        coEvery { userSessionDao.getSession() } returns session(namaProyek = "Proyek B", lastSyncTransfer = 0L)
        coEvery { firestore.pullTransfer(0L, "Proyek B") } returns
            FirestoreDataSource.TransferPullResult(items = listOf(remoteTransfer(ts = 100L)), latestServerMillis = 700L)
        coEvery { transferDao.getById("TRX1") } returns null

        val result = repo().pullLatestTransfer()

        assertThat(result.isSuccess).isTrue()
        coVerify { transferDao.insert(match { it.id_transfer == "TRX1" }) }
        coVerify { userSessionDao.updateLastSyncTransfer(700L) }
    }

    @Test
    fun `pull transfer LWW - versi server lebih lama tidak menimpa lokal SYNCED`() = runTest {
        coEvery { userSessionDao.getSession() } returns session(namaProyek = "Proyek B")
        coEvery { firestore.pullTransfer(any(), any()) } returns
            FirestoreDataSource.TransferPullResult(items = listOf(remoteTransfer(ts = 500L)), latestServerMillis = 700L)
        coEvery { transferDao.getById("TRX1") } returns
            remoteTransfer(ts = 1000L).copy(sync_status = "SYNCED")    // lokal lebih baru

        val result = repo().pullLatestTransfer()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) { transferDao.insert(any()) }            // lokal menang LWW
    }
}
