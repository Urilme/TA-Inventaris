package com.ta.konstruksi.riwayat

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.presentation.viewmodel.RiwayatItem
import com.ta.konstruksi.presentation.viewmodel.RiwayatViewModel
import com.ta.konstruksi.testutil.FakeKatalogRepository
import com.ta.konstruksi.testutil.FakeMutasiRepository
import com.ta.konstruksi.testutil.FakeTransferRepository
import com.ta.konstruksi.testutil.MainDispatcherRule
import com.ta.konstruksi.testutil.logMutasi
import com.ta.konstruksi.testutil.masterKatalog
import com.ta.konstruksi.testutil.transferProyek
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Uji riwayat: gabungan mutasi+transfer terurut terbaru, pelabelan nama, dan filter status. */
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

    @Test
    fun `filter jenis CHECK_IN hanya menampilkan mutasi masuk dan menyembunyikan transfer`() = runTest {
        val katalog = FakeKatalogRepository().apply { put(masterKatalog(sku = "SKU-1", nama = "Semen")) }
        val mutasi = FakeMutasiRepository().apply {
            emit(
                listOf(
                    logMutasi(id = "TX-IN", sku = "SKU-1", jenis = JenisMutasi.CHECK_IN, ts = 100),
                    logMutasi(id = "TX-OUT", sku = "SKU-1", jenis = JenisMutasi.CHECK_OUT, ts = 150)
                )
            )
        }
        val transfer = FakeTransferRepository().apply { seed(transferProyek(id = "TR-1", sku = "SKU-1", ts = 200)) }
        val vm = RiwayatViewModel(mutasi, transfer, katalog)

        vm.setJenisFilter(JenisMutasi.CHECK_IN)

        vm.riwayatList.test {
            var list = awaitItem()
            while (list.size != 1) list = awaitItem()
            assertThat(list).hasSize(1)                                  // transfer & CHECK_OUT tersembunyi
            val item = list[0]
            assertThat(item).isInstanceOf(RiwayatItem.Mutasi::class.java)
            assertThat((item as RiwayatItem.Mutasi).data.jenis_mutasi).isEqualTo(JenisMutasi.CHECK_IN)
            assertThat(list.none { it is RiwayatItem.Transfer }).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter jenis CHECK_OUT hanya menampilkan mutasi keluar`() = runTest {
        val katalog = FakeKatalogRepository().apply { put(masterKatalog(sku = "SKU-1", nama = "Semen")) }
        val mutasi = FakeMutasiRepository().apply {
            emit(
                listOf(
                    logMutasi(id = "TX-IN", sku = "SKU-1", jenis = JenisMutasi.CHECK_IN, ts = 100),
                    logMutasi(id = "TX-OUT", sku = "SKU-1", jenis = JenisMutasi.CHECK_OUT, ts = 150)
                )
            )
        }
        val transfer = FakeTransferRepository().apply { seed(transferProyek(id = "TR-1", sku = "SKU-1", ts = 200)) }
        val vm = RiwayatViewModel(mutasi, transfer, katalog)

        vm.setJenisFilter(JenisMutasi.CHECK_OUT)

        vm.riwayatList.test {
            var list = awaitItem()
            while (list.size != 1) list = awaitItem()
            assertThat((list[0] as RiwayatItem.Mutasi).data.jenis_mutasi).isEqualTo(JenisMutasi.CHECK_OUT)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Integritas urutan saat data lampau disinkron belakangan. Mutasi ber-timestamp 6 jam
     * lampau ditaruh lebih dulu di daftar sumber; pengurutan memakai local_timestamp mentah,
     * item terbaru tetap di atas.
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
