package com.ta.konstruksi.transfer

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.presentation.viewmodel.TransferUiState
import com.ta.konstruksi.presentation.viewmodel.TransferViewModel
import com.ta.konstruksi.sync.SyncScheduler
import com.ta.konstruksi.testutil.FakeAuthRepository
import com.ta.konstruksi.testutil.FakeKatalogRepository
import com.ta.konstruksi.testutil.FakeProyekRepository
import com.ta.konstruksi.testutil.FakeTransferRepository
import com.ta.konstruksi.testutil.MainDispatcherRule
import com.ta.konstruksi.testutil.masterKatalog
import com.ta.konstruksi.testutil.transferProyek
import com.ta.konstruksi.testutil.userSession
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.StatusTransfer
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * F4 — Transfer Aset. Logika create/confirm/cancel ada di TransferViewModel
 * (guide §9 menyebut Create/ConfirmTransferUseCase yang tidak ada di kode — diuji di tempat
 * logikanya benar-benar berada, sesuai aturan "jangan mengarang API").
 */
class TransferViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val syncScheduler = mockk<SyncScheduler>(relaxed = true)

    private fun viewModel(
        katalog: FakeKatalogRepository,
        transfer: FakeTransferRepository,
        sesiProyek: String = "PRJ-001"
    ) = TransferViewModel(
        transfer, katalog, FakeProyekRepository(),
        FakeAuthRepository(session = userSession(idProyek = sesiProyek)),
        syncScheduler
    )

    @Test
    fun `buat transfer berstatus IN_TRANSIT dan mengurangi stok proyek asal`() = runTest {
        val katalog = FakeKatalogRepository().apply {
            put(masterKatalog(sku = "SKU-X", nama = "Bor", kategori = KategoriBarang.ALAT, satuan = "unit", stok = 10, idProyek = "PRJ-001"))
        }
        val transfer = FakeTransferRepository()
        val vm = viewModel(katalog, transfer, sesiProyek = "PRJ-001")

        vm.createTransfer(katalog.current("SKU-X")!!, proyekTujuan = "Proyek B", jumlah = 3, proyekAsal = "Proyek A")
        advanceUntilIdle()

        assertThat(transfer.transfers).hasSize(1)
        val t = transfer.transfers.values.first()
        assertThat(t.status_transfer).isEqualTo(StatusTransfer.IN_TRANSIT)
        assertThat(t.jumlah).isEqualTo(3)
        assertThat(t.nama_barang).isEqualTo("Bor")     // snapshot self-describing
        assertThat(katalog.current("SKU-X")!!.stok_lokal).isEqualTo(7)
        assertThat(vm.uiState.value).isEqualTo(TransferUiState.Success)
    }

    @Test
    fun `transfer ditolak bila proyek asal sama dengan tujuan`() = runTest {
        val katalog = FakeKatalogRepository().apply { put(masterKatalog(sku = "SKU-X", stok = 10)) }
        val transfer = FakeTransferRepository()
        val vm = viewModel(katalog, transfer)

        vm.createTransfer(katalog.current("SKU-X")!!, proyekTujuan = "Proyek A", jumlah = 1, proyekAsal = "Proyek A")
        advanceUntilIdle()

        assertThat(vm.uiState.value).isInstanceOf(TransferUiState.Error::class.java)
        assertThat(transfer.transfers).isEmpty()
        assertThat(katalog.current("SKU-X")!!.stok_lokal).isEqualTo(10)   // stok utuh
    }

    @Test
    fun `konfirmasi transfer memindahkan barang ke katalog penerima dan set SELESAI`() = runTest {
        val katalog = FakeKatalogRepository()   // katalog PRJ-002 masih kosong
        val transfer = FakeTransferRepository()
        val t = transferProyek(
            id = "TR-1", sku = "SKU-X", nama = "Bor", kategori = KategoriBarang.ALAT, satuan = "unit",
            jumlah = 2, status = StatusTransfer.IN_TRANSIT
        )
        transfer.seed(t)
        val vm = viewModel(katalog, transfer, sesiProyek = "PRJ-002")

        vm.confirmTransfer(t)
        advanceUntilIdle()

        assertThat(transfer.transfers["TR-1"]!!.status_transfer).isEqualTo(StatusTransfer.SELESAI)
        val masuk = katalog.findByProyekAndNama("PRJ-002", "Bor")
        assertThat(masuk).isNotNull()
        assertThat(masuk!!.stok_lokal).isEqualTo(2)
        assertThat(masuk.id_proyek).isEqualTo("PRJ-002")
    }

    @Test
    fun `konfirmasi menambah stok bila barang sejenis sudah ada di proyek penerima`() = runTest {
        val katalog = FakeKatalogRepository().apply {
            put(masterKatalog(sku = "SKU-EX", nama = "Bor", kategori = KategoriBarang.ALAT, satuan = "unit", stok = 5, idProyek = "PRJ-002"))
        }
        val transfer = FakeTransferRepository()
        val t = transferProyek(id = "TR-2", sku = "SKU-X", nama = "Bor", jumlah = 2, status = StatusTransfer.IN_TRANSIT)
        transfer.seed(t)
        val vm = viewModel(katalog, transfer, sesiProyek = "PRJ-002")

        vm.confirmTransfer(t)
        advanceUntilIdle()

        assertThat(katalog.current("SKU-EX")!!.stok_lokal).isEqualTo(7)   // 5 + 2, tanpa duplikat
    }

    @Test
    fun `batalkan transfer mengembalikan stok ke proyek asal dan set DIBATALKAN`() = runTest {
        // Stok asal 7 = kondisi setelah createTransfer mengurangi 3 dari 10.
        val katalog = FakeKatalogRepository().apply {
            put(masterKatalog(sku = "SKU-X", stok = 7, idProyek = "PRJ-001"))
        }
        val transfer = FakeTransferRepository()
        val t = transferProyek(id = "TR-9", sku = "SKU-X", jumlah = 3, status = StatusTransfer.IN_TRANSIT)
        transfer.seed(t)
        val vm = viewModel(katalog, transfer, sesiProyek = "PRJ-002")

        vm.cancelTransfer(t)
        advanceUntilIdle()

        assertThat(transfer.transfers["TR-9"]!!.status_transfer).isEqualTo(StatusTransfer.DIBATALKAN)
        assertThat(katalog.current("SKU-X")!!.stok_lokal).isEqualTo(10)   // stok kembali
    }

    @Test
    fun `batalkan ditolak bila transfer sudah SELESAI`() = runTest {
        val katalog = FakeKatalogRepository().apply { put(masterKatalog(sku = "SKU-X", stok = 7)) }
        val transfer = FakeTransferRepository()
        val t = transferProyek(id = "TR-3", sku = "SKU-X", jumlah = 3, status = StatusTransfer.SELESAI)
        transfer.seed(t)
        val vm = viewModel(katalog, transfer)

        vm.cancelTransfer(t)
        advanceUntilIdle()

        assertThat(vm.uiState.value).isInstanceOf(TransferUiState.Error::class.java)
        assertThat(transfer.transfers["TR-3"]!!.status_transfer).isEqualTo(StatusTransfer.SELESAI) // tak berubah
        assertThat(katalog.current("SKU-X")!!.stok_lokal).isEqualTo(7)                             // stok utuh
    }
}
