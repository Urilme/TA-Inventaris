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
 * Uji kegagalan parsial di tengah batch unggah. Lima mutasi diunggah; item ke-3 gagal.
 * Membuktikan perilaku dua-pass [SyncRepositoryImpl.syncPendingMutasi]: semua ditandai
 * IN_TRANSIT dulu, lalu diunggah satu per satu; item gagal di-rollback ke PENDING dan loop
 * berhenti, item sesudahnya tetap IN_TRANSIT (aman karena ikut retry berikutnya).
 */
class SyncPartialBatchTest {

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

    private fun mutasi(id: String) = LogMutasiEntity(
        id_transaksi = id, sku_id = "SKU-1", jenis_mutasi = "CHECK_OUT", jumlah = 2
    )

    @Test
    fun `item gagal di tengah batch rollback ke PENDING dan item sesudahnya tetap IN_TRANSIT`() = runTest {
        val batch = (1..5).map { mutasi("TX$it") }
        coEvery { userSessionDao.getSession() } returns null
        coEvery { mutasiDao.getPending() } returns batch

        // Unggah TX3 gagal; sisanya sukses. Stub spesifik untuk TX3 didaftarkan terakhir agar menang.
        coEvery { firestore.uploadMutasi(any(), any()) } just Runs
        coEvery { firestore.uploadMutasi(match { it.id_transaksi == "TX3" }, any()) } throws
            RuntimeException("network down")

        val result = repo().syncPendingMutasi()

        assertThat(result.isFailure).isTrue()

        // Pass 1: kelima item ditandai IN_TRANSIT.
        (1..5).forEach { coVerify { mutasiDao.updateSyncStatus("TX$it", "IN_TRANSIT") } }

        // Pass 2 berhenti di TX3.
        coVerify { firestore.uploadMutasi(match { it.id_transaksi == "TX1" }, any()) }
        coVerify { firestore.uploadMutasi(match { it.id_transaksi == "TX2" }, any()) }
        coVerify { firestore.uploadMutasi(match { it.id_transaksi == "TX3" }, any()) }
        coVerify(exactly = 0) { firestore.uploadMutasi(match { it.id_transaksi == "TX4" }, any()) }
        coVerify(exactly = 0) { firestore.uploadMutasi(match { it.id_transaksi == "TX5" }, any()) }

        // TX1 & TX2 → SYNCED.
        coVerify { mutasiDao.updateSyncStatus("TX1", "SYNCED") }
        coVerify { mutasiDao.updateSyncStatus("TX2", "SYNCED") }

        // TX3 → rollback PENDING (bukan SYNCED).
        coVerify { mutasiDao.updateSyncStatus("TX3", "PENDING") }
        coVerify(exactly = 0) { mutasiDao.updateSyncStatus("TX3", "SYNCED") }

        // TX4 & TX5 tertinggal IN_TRANSIT: tidak pernah jadi SYNCED maupun di-rollback PENDING.
        coVerify(exactly = 0) { mutasiDao.updateSyncStatus("TX4", "SYNCED") }
        coVerify(exactly = 0) { mutasiDao.updateSyncStatus("TX4", "PENDING") }
        coVerify(exactly = 0) { mutasiDao.updateSyncStatus("TX5", "SYNCED") }
        coVerify(exactly = 0) { mutasiDao.updateSyncStatus("TX5", "PENDING") }
    }
}
