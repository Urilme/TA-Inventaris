package com.ta.konstruksi.mutasi

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.domain.usecase.mutasi.RecordMutasiUseCase
import com.ta.konstruksi.testutil.FakeKatalogRepository
import com.ta.konstruksi.testutil.FakeMutasiRepository
import com.ta.konstruksi.testutil.masterKatalog
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.KondisiBarang
import com.ta.konstruksi.util.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * F3 — Pencatatan Mutasi. Aturan bisnis luring deterministik (§8):
 * stok tidak minus, kondisi_barang wajib utk alat & null utk bahan/scaffolding,
 * field wajib (penanggungjawab / lokasi_penggunaan), status awal PENDING.
 */
class RecordMutasiUseCaseTest {

    private lateinit var katalog: FakeKatalogRepository
    private lateinit var mutasi: FakeMutasiRepository
    private lateinit var useCase: RecordMutasiUseCase

    @Before
    fun setUp() {
        katalog = FakeKatalogRepository()
        mutasi = FakeMutasiRepository(katalog)
        useCase = RecordMutasiUseCase(mutasi, katalog)
    }

    private suspend fun record(
        sku: String = "SKU-1",
        jenis: JenisMutasi = JenisMutasi.CHECK_OUT,
        jumlah: Int = 2,
        kondisi: KondisiBarang? = null,
        lokasi: String = "Lantai 2",
        pj: String = "Budi"
    ) = useCase(
        skuId = sku, idMandor = "M1", idProyek = "PRJ-001",
        jenisMutasi = jenis, jumlah = jumlah, kondisiBarang = kondisi,
        lokasiPenggunaan = lokasi, penanggungjawab = pj
    )

    @Test
    fun `check-in menambah stok dan menyimpan mutasi berstatus PENDING`() = runTest {
        katalog.put(masterKatalog(stok = 10))
        val r = record(jenis = JenisMutasi.CHECK_IN, jumlah = 5, lokasi = "")
        assertThat(r.isSuccess).isTrue()
        assertThat(katalog.current("SKU-1")!!.stok_lokal).isEqualTo(15)
        assertThat(mutasi.recorded).hasSize(1)
        assertThat(mutasi.recorded.last().sync_status).isEqualTo(SyncStatus.PENDING)
    }

    @Test
    fun `check-out valid mengurangi stok`() = runTest {
        katalog.put(masterKatalog(stok = 10))
        val r = record(jenis = JenisMutasi.CHECK_OUT, jumlah = 4)
        assertThat(r.isSuccess).isTrue()
        assertThat(katalog.current("SKU-1")!!.stok_lokal).isEqualTo(6)
    }

    @Test
    fun `check-out melebihi stok lokal ditolak dan stok tidak berubah`() = runTest {
        katalog.put(masterKatalog(stok = 3))
        val r = record(jenis = JenisMutasi.CHECK_OUT, jumlah = 5)
        assertThat(r.isFailure).isTrue()
        assertThat(r.exceptionOrNull()!!.message).contains("Stok tidak mencukupi")
        assertThat(katalog.current("SKU-1")!!.stok_lokal).isEqualTo(3)
        assertThat(mutasi.recorded).isEmpty()
    }

    @Test
    fun `kondisi barang wajib diisi untuk kategori alat`() = runTest {
        katalog.put(masterKatalog(kategori = KategoriBarang.ALAT, stok = 10))
        val r = record(jenis = JenisMutasi.CHECK_OUT, jumlah = 1, kondisi = null)
        assertThat(r.isFailure).isTrue()
        assertThat(r.exceptionOrNull()!!.message).contains("Kondisi barang wajib")
    }

    @Test
    fun `kondisi barang tersimpan untuk kategori alat`() = runTest {
        katalog.put(masterKatalog(kategori = KategoriBarang.ALAT, stok = 10))
        val r = record(jenis = JenisMutasi.CHECK_OUT, jumlah = 1, kondisi = KondisiBarang.RUSAK_RINGAN)
        assertThat(r.isSuccess).isTrue()
        assertThat(mutasi.recorded.last().kondisi_barang).isEqualTo(KondisiBarang.RUSAK_RINGAN)
    }

    @Test
    fun `kondisi barang dipaksa null untuk kategori bahan`() = runTest {
        katalog.put(masterKatalog(kategori = KategoriBarang.BAHAN, stok = 10))
        val r = record(jenis = JenisMutasi.CHECK_OUT, jumlah = 1, kondisi = KondisiBarang.BAIK)
        assertThat(r.isSuccess).isTrue()
        assertThat(mutasi.recorded.last().kondisi_barang).isNull()
    }

    @Test
    fun `kondisi barang dipaksa null untuk kategori scaffolding`() = runTest {
        katalog.put(masterKatalog(kategori = KategoriBarang.SCAFFOLDING, stok = 10))
        val r = record(jenis = JenisMutasi.CHECK_OUT, jumlah = 1, kondisi = KondisiBarang.BAIK)
        assertThat(r.isSuccess).isTrue()
        assertThat(mutasi.recorded.last().kondisi_barang).isNull()
    }

    @Test
    fun `penanggungjawab wajib diisi`() = runTest {
        katalog.put(masterKatalog(stok = 10))
        val r = record(pj = "   ")
        assertThat(r.isFailure).isTrue()
        assertThat(r.exceptionOrNull()!!.message).contains("Penanggung jawab wajib")
    }

    @Test
    fun `lokasi penggunaan wajib untuk check-out`() = runTest {
        katalog.put(masterKatalog(stok = 10))
        val r = record(jenis = JenisMutasi.CHECK_OUT, lokasi = "")
        assertThat(r.isFailure).isTrue()
        assertThat(r.exceptionOrNull()!!.message).contains("Lokasi penggunaan wajib")
    }

    @Test
    fun `mutasi barang tidak dikenal ditolak`() = runTest {
        val r = record(sku = "TIDAK-ADA")
        assertThat(r.isFailure).isTrue()
        assertThat(r.exceptionOrNull()!!.message).contains("Barang tidak ditemukan")
    }
}
