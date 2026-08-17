package com.ta.konstruksi.sync

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.data.local.dao.LogMutasiDao
import com.ta.konstruksi.data.local.dao.ProyekDao
import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import com.ta.konstruksi.data.remote.FirestoreDataSource
import com.ta.konstruksi.data.repository.SyncRepositoryImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Concurrency Testing No. 9 (CT-09) — Idempotensi \& Retry WorkManager.
 *
 * Membuktikan bahwa mengulang siklus sinkronisasi (mis. WorkManager retry setelah kegagalan
 * parsial, atau pemulihan item yang tertinggal berstatus IN_TRANSIT karena koneksi terputus di
 * tengah unggah) TIDAK menghasilkan dokumen ganda di Firestore. Jaminan idempotensi berasal dari
 * penulisan ke ID dokumen deterministik (`document(id_transaksi).set(...)` pada
 * [FirestoreDataSource.uploadMutasi]) sehingga penulisan ulang menimpa dokumen yang sama.
 *
 * Firestore di-mock (bukan network nyata) agar deterministik. Perhitungan dokumen fisik di server
 * juga divalidasi terpisah oleh stress test CT-05 (scripts/stress_test_sync.py, Zero Lost-Write).
 */
class SyncIdempotencyTest {

    private val mutasiDao = mockk<LogMutasiDao>(relaxed = true)
    private val firestore = mockk<FirestoreDataSource>(relaxed = true)
    private val userSessionDao = mockk<UserSessionDao>(relaxed = true)
    private val proyekDao = mockk<ProyekDao>(relaxed = true)

    private fun repo() = SyncRepositoryImpl(
        mutasiDao = mutasiDao,
        transferDao = mockk(relaxed = true),
        katalogDao = mockk(relaxed = true),
        proyekDao = proyekDao,
        userSessionDao = userSessionDao,
        firestore = firestore
    )

    private val entity = LogMutasiEntity(
        id_transaksi = "TX1", sku_id = "SKU-1", jenis_mutasi = "CHECK_OUT", jumlah = 2
    )

    /**
     * Pemulihan item yang tertinggal IN_TRANSIT (unggah terputus di siklus sebelumnya):
     * getPending() menyertakan status IN_TRANSIT, sehingga item diproses ulang dan diunggah
     * TEPAT SEKALI ke ID dokumen deterministik — bukan sebagai dokumen baru.
     */
    @Test
    fun `retry memproses ulang item IN_TRANSIT dan mengunggah tepat sekali ke id deterministik`() = runTest {
        coEvery { userSessionDao.getSession() } returns null
        coEvery { mutasiDao.getPending() } returns listOf(entity)

        val result = repo().syncPendingMutasi()

        assertThat(result.getOrNull()).isEqualTo(1)
        coVerify(exactly = 1) { firestore.uploadMutasi(match { it.id_transaksi == "TX1" }, any()) }
        coVerify { mutasiDao.updateSyncStatus("TX1", "SYNCED") }
    }

    /**
     * Dua siklus sinkronisasi pada transaksi yang sama (siklus-1 gagal → rollback PENDING,
     * siklus-2 sukses via retry). Kedua percobaan menulis ke ID dokumen yang SAMA (id_transaksi),
     * sehingga operasi set() menimpa dokumen yang sama dan TIDAK pernah membuat dokumen kedua.
     */
    @Test
    fun `sync diulang setelah gagal menulis ke id dokumen sama tanpa menduplikasi`() = runTest {
        coEvery { userSessionDao.getSession() } returns null
        coEvery { mutasiDao.getPending() } returns listOf(entity)

        // Siklus 1 — unggah gagal (jaringan putus) → repository melakukan rollback ke PENDING.
        coEvery { firestore.uploadMutasi(any(), any()) } throws RuntimeException("network down")
        val siklus1 = repo().syncPendingMutasi()
        assertThat(siklus1.isFailure).isTrue()
        coVerify { mutasiDao.updateSyncStatus("TX1", "PENDING") } // rollback

        // Siklus 2 — retry WorkManager, unggah sukses.
        coEvery { firestore.uploadMutasi(any(), any()) } just Runs
        val siklus2 = repo().syncPendingMutasi()
        assertThat(siklus2.getOrNull()).isEqualTo(1)

        // Kunci idempotensi: KEDUA percobaan menargetkan id_transaksi yang sama → set() menimpa.
        coVerify(exactly = 2) { firestore.uploadMutasi(match { it.id_transaksi == "TX1" }, any()) }
        coVerify { mutasiDao.updateSyncStatus("TX1", "SYNCED") }
    }
}
