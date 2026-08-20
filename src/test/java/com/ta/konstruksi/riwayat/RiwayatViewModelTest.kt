package com.ta.konstruksi.riwayat

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.presentation.viewmodel.RiwayatViewModel
import com.ta.konstruksi.testutil.FakeKatalogRepository
import com.ta.konstruksi.testutil.FakeMutasiRepository
import com.ta.konstruksi.testutil.FakeTransferRepository
import com.ta.konstruksi.testutil.MainDispatcherRule
import com.ta.konstruksi.testutil.logMutasi
import com.ta.konstruksi.testutil.masterKatalog
import com.ta.konstruksi.testutil.transferProyek
import com.ta.konstruksi.util.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * F5 — Riwayat. Gabungan mutasi+transfer terurut terbaru, pelabelan nama via katalog,
 * dan filter berdasarkan status sinkronisasi.
 */
class RiwayatViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `riwayat menggabungkan mutasi dan transfer terurut paling baru dulu`() = runTest {
        val katalog = FakeKatalogRepository().apply { put(masterKatalog(sku = "SKU-1", nama = "Semen")) }
        val mutasi = FakeMutasiRepository().apply { emit(listOf(logMutasi(id = "TX-1", sku = "SKU-1", ts = 100))) }
        val transfer = FakeTransferRepository().apply { seed(transferProyek(id = "TR-1", sku = "SKU-1", ts = 200)) }
        val vm = RiwayatViewModel(mutasi, transfer, katalog)

        vm.riwayatList.test {
            var list = awaitItem()
            while (list.size < 2) list = awaitItem()   // tunggu combine selesai
            assertThat(list).hasSize(2)
            assertThat(list[0].timestamp).isEqualTo(200)   // transfer lebih baru → di atas
            assertThat(list[1].timestamp).isEqualTo(100)
            assertThat(list[1].label).isEqualTo("Semen")   // nama dari katalog, bukan UUID
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter PENDING hanya menampilkan item berstatus PENDING`() = runTest {
        val katalog = FakeKatalogRepository().apply { put(masterKatalog(sku = "SKU-1", nama = "Semen")) }
        val mutasi = FakeMutasiRepository().apply {
            emit(listOf(logMutasi(id = "TX-1", sku = "SKU-1", ts = 100, sync = SyncStatus.PENDING)))
        }
        val transfer = FakeTransferRepository().apply {
            seed(transferProyek(id = "TR-1", sku = "SKU-1", ts = 200, sync = SyncStatus.SYNCED))
        }
        val vm = RiwayatViewModel(mutasi, transfer, katalog)

        vm.setFilter(SyncStatus.PENDING)

        vm.riwayatList.test {
            var list = awaitItem()
            while (list.size != 1) list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list.all { it.syncStatus == SyncStatus.PENDING }).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Concurrency Testing No. 4 (CT-04) — Integritas urutan saat data lampau tersinkron belakangan.
     *
     * Satu mutasi ber-`local_timestamp` 6 jam lampau (perangkat luring, baru sinkron kemudian)
     * sengaja ditempatkan LEBIH DULU dalam daftar sumber, ditumpuk dengan mutasi "baru saja"
     * ber-timestamp sekarang. Pengurutan riwayat murni memakai `local_timestamp` mentah
     * (`sortedByDescending`) TANPA koreksi clock-skew — sesuai keputusan desain. Test ini
     * MEMBUKTIKAN integritas urutan (item terbaru tetap di atas, item lampau tetap di bawah, tak
     * ada distorsi), bukan mencari "koreksi jam" yang memang tidak ada di sistem.
     */
    @Test
    fun `urutan riwayat tetap oleh local_timestamp meski data lampau disinkron belakangan`() = runTest {
        val now = System.currentTimeMillis()
        val enamJamLalu = now - 6 * 60 * 60 * 1000L
        val katalog = FakeKatalogRepository().apply { put(masterKatalog(sku = "SKU-1", nama = "Semen")) }
        val mutasi = FakeMutasiRepository().apply {
            emit(
                listOf(
                    logMutasi(id = "TX-LAMPAU", sku = "SKU-1", ts = enamJamLalu), // sengaja di depan daftar
                    logMutasi(id = "TX-BARU", sku = "SKU-1", ts = now)
                )
            )
        }
        val transfer = FakeTransferRepository()
        val vm = RiwayatViewModel(mutasi, transfer, katalog)

        vm.riwayatList.test {
            var list = awaitItem()
            while (list.size < 2) list = awaitItem()
            assertThat(list).hasSize(2)
            assertThat(list[0].timestamp).isEqualTo(now)          // transaksi baru → di atas
            assertThat(list[1].timestamp).isEqualTo(enamJamLalu)  // data lampau → di bawah
            cancelAndIgnoreRemainingEvents()
        }
    }
}
