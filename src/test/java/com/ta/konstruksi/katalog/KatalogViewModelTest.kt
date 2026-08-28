package com.ta.konstruksi.katalog

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.presentation.viewmodel.HapusKatalogState
import com.ta.konstruksi.presentation.viewmodel.KatalogViewModel
import com.ta.konstruksi.sync.SyncScheduler
import com.ta.konstruksi.testutil.FakeKatalogRepository
import com.ta.konstruksi.testutil.MainDispatcherRule
import com.ta.konstruksi.testutil.masterKatalog
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.SyncStatus
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Uji tambah barang: muncul di daftar dengan SKU auto-generate & status PENDING. */
class KatalogViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val katalog = FakeKatalogRepository()
    private val syncScheduler = mockk<SyncScheduler>(relaxed = true)

    // Dibuat di dalam test (setelah @Rule menyetel Dispatchers.Main), bukan sebagai field —
    // konstruksi ViewModel mengakses viewModelScope yang butuh Main sudah diset.
    private fun buatVm() = KatalogViewModel(katalog, syncScheduler)

    @Test
    fun `tambah katalog memunculkan barang dengan SKU auto dan status PENDING`() = runTest {
        val vm = buatVm()
        vm.katalogList.test {
            assertThat(awaitItem()).isEmpty()   // state awal

            vm.tambahKatalog(
                kategori = KategoriBarang.BAHAN,
                namaBarang = "Semen Tiga Roda",
                satuan = "sak",
                stokAwal = 40,
                stokMinimum = 10
            )

            val after = awaitItem()
            assertThat(after).hasSize(1)
            assertThat(after[0].nama_barang).isEqualTo("Semen Tiga Roda")
            assertThat(after[0].sku_id).startsWith("SKU-")
            assertThat(after[0].sync_status).isEqualTo(SyncStatus.PENDING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tambah katalog memicu sinkronisasi otomatis`() = runTest {
        val vm = buatVm()
        vm.tambahKatalog(KategoriBarang.ALAT, "Bor Bosch", "unit", 5)
        verify { syncScheduler.requestSync() }
    }

    @Test
    fun `SKU auto-generate berformat SKU- dan unik`() = runTest {
        val vm = buatVm()
        vm.tambahKatalog(KategoriBarang.BAHAN, "Pasir", "m3", 10)
        vm.tambahKatalog(KategoriBarang.BAHAN, "Kerikil", "m3", 10)
        vm.katalogList.test {
            val list = awaitItem()
            assertThat(list).hasSize(2)
            assertThat(list.map { it.sku_id }.toSet()).hasSize(2)   // tak ada SKU duplikat
            assertThat(list.all { it.sku_id.startsWith("SKU-") }).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hapus barang tanpa riwayat berhasil dan menghapus dari daftar`() = runTest {
        katalog.put(masterKatalog(sku = "SKU-1", nama = "Semen"))
        val vm = buatVm()

        vm.hapusKatalog("SKU-1")

        assertThat(vm.hapusState.value).isEqualTo(HapusKatalogState.Success)
        assertThat(katalog.current("SKU-1")).isNull()
    }

    @Test
    fun `hapus barang yang punya riwayat mutasi ditolak dan barang tetap ada`() = runTest {
        katalog.put(masterKatalog(sku = "SKU-1", nama = "Semen"))
        katalog.skuBerriwayat.add("SKU-1")   // barang sudah punya riwayat → hapus dilarang
        val vm = buatVm()

        vm.hapusKatalog("SKU-1")

        val state = vm.hapusState.value
        assertThat(state).isInstanceOf(HapusKatalogState.Error::class.java)
        assertThat((state as HapusKatalogState.Error).message).contains("riwayat mutasi")
        assertThat(katalog.current("SKU-1")).isNotNull()   // tidak terhapus
    }
}
