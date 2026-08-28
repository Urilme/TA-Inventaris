package com.ta.konstruksi.sync

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.domain.repository.SyncRepository
import com.ta.konstruksi.domain.usecase.sync.TriggerSyncUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Uji orkestrasi sinkronisasi push→pull di [TriggerSyncUseCase.invoke] dengan repository di-mock.
 * Membuktikan urutan push (katalog→mutasi→transfer) mendahului pull (katalog→proyek), success
 * bila semua lancar, dan failure "Sync sebagian gagal" bila ada push yang gagal (pull tetap jalan).
 */
class TriggerSyncOrderTest {

    private val repo = mockk<SyncRepository>()
    private val useCase = TriggerSyncUseCase(repo)

    private fun stubSemuaSukses() {
        coEvery { repo.syncPendingKatalog() } returns Result.success(0)
        coEvery { repo.syncPendingMutasi() } returns Result.success(0)
        coEvery { repo.syncPendingTransfer() } returns Result.success(0)
        coEvery { repo.pullLatestKatalog() } returns Result.success(Unit)
        coEvery { repo.pullLatestMutasi() } returns Result.success(Unit)
        coEvery { repo.pullLatestTransfer() } returns Result.success(Unit)
        coEvery { repo.pullLatestProyek() } returns Result.success(Unit)
    }

    @Test
    fun `invoke menjalankan push lalu pull dalam urutan yang benar dan sukses`() = runTest {
        stubSemuaSukses()

        val hasil = useCase()

        assertThat(hasil.isSuccess).isTrue()
        coVerifyOrder {
            repo.syncPendingKatalog()
            repo.syncPendingMutasi()
            repo.syncPendingTransfer()
            repo.pullLatestKatalog()
            repo.pullLatestMutasi()
            repo.pullLatestTransfer()
            repo.pullLatestProyek()
        }
    }

    @Test
    fun `bila satu push gagal pull tetap dijalankan dan hasil akhir gagal`() = runTest {
        stubSemuaSukses()
        // Mutasi gagal push (mis. koneksi putus di tengah antrean).
        coEvery { repo.syncPendingMutasi() } returns Result.failure(Exception("network down"))

        val hasil = useCase()

        assertThat(hasil.isFailure).isTrue()
        assertThat(hasil.exceptionOrNull()?.message).isEqualTo("Sync sebagian gagal")
        // Pull tetap best-effort dijalankan meski ada push yang gagal.
        coVerify { repo.pullLatestKatalog() }
        coVerify { repo.pullLatestProyek() }
    }
}
