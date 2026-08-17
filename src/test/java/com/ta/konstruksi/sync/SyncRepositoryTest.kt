package com.ta.konstruksi.sync

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.data.local.dao.LogMutasiDao
import com.ta.konstruksi.data.local.dao.ProyekDao
import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import com.ta.konstruksi.data.local.entity.ProyekEntity
import com.ta.konstruksi.data.remote.FirestoreDataSource
import com.ta.konstruksi.data.repository.SyncRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * F6 — Delta Sync (state machine PENDING → IN_TRANSIT → SYNCED / rollback PENDING).
 * Firestore di-mock (bukan network nyata); pull & concurrency multi-perangkat diuji manual.
 */
class SyncRepositoryTest {

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

    @Test
    fun `sync mutasi PENDING sukses menandai SYNCED`() = runTest {
        coEvery { userSessionDao.getSession() } returns null
        coEvery { mutasiDao.getPending() } returns listOf(entity)

        val result = repo().syncPendingMutasi()

        assertThat(result.getOrNull()).isEqualTo(1)
        coVerify { mutasiDao.updateSyncStatus("TX1", "IN_TRANSIT") } // tandai dikirim
        coVerify { mutasiDao.updateSyncStatus("TX1", "SYNCED") }     // sukses
    }

    @Test
    fun `sync mutasi gagal mengembalikan status ke PENDING`() = runTest {
        coEvery { userSessionDao.getSession() } returns null
        coEvery { mutasiDao.getPending() } returns listOf(entity)
        coEvery { firestore.uploadMutasi(any(), any()) } throws RuntimeException("network down")

        val result = repo().syncPendingMutasi()

        assertThat(result.isFailure).isTrue()
        coVerify { mutasiDao.updateSyncStatus("TX1", "PENDING") }    // rollback
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
}
