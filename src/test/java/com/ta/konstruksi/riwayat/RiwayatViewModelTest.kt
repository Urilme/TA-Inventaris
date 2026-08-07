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
}
