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
 * Concurrency Testing No. 1 (CT-01) — Orkestrasi sinkronisasi push→pull saat transisi
 * \textit{offline} → \textit{online} dengan antrean operasi lokal menumpuk.
 *
 * Menguji [TriggerSyncUseCase.invoke] end-to-end dengan [SyncRepository] di-mock. Membuktikan:
 * - Urutan push (katalog → mutasi → transfer) mendahului pull (katalog → proyek), sesuai desain
 *   di mana master data diunggah dulu sebelum menarik versi server (kursor delta dimajukan di
 *   dalam pullLatestKatalog).
 * - Bila seluruh push \& pull katalog sukses → invoke() mengembalikan Result.success.
 * - Bila salah satu push gagal, pull tetap dijalankan (best-effort) namun hasil akhir
 *   Result.failure berpesan "Sync sebagian gagal" — kegagalan tidak disembunyikan.
 *
 * Catatan desain (temuan analisis): PULL dua arah hanya berlaku untuk KATALOG \& PROYEK.
 * Mutasi/transfer bersifat push-only dari perangkat (website tidak menulis balik ke koleksi
 * tersebut), sehingga "eventual consistency dua arah" pada skenario ini adalah konsistensi
 * katalog, bukan mutasi.
 */
class TriggerSyncOrderTest {

    private val repo = mockk<SyncRepository>()
    private val useCase = TriggerSyncUseCase(repo)

    private fun stubSemuaSukses() {
        coEvery { repo.syncPendingKatalog() } returns Result.success(0)
        coEvery { repo.syncPendingMutasi() } returns Result.success(0)
        coEvery { repo.syncPendingTransfer() } returns Result.success(0)
        coEvery { repo.pullLatestKatalog() } returns Result.success(Unit)
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
