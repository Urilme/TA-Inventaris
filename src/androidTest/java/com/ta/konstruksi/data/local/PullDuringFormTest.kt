package com.ta.konstruksi.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ta.konstruksi.data.local.dao.LogMutasiDao
import com.ta.konstruksi.data.local.dao.MasterKatalogDao
import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.UuidGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uji pull katalog yang berjalan bersamaan dengan pencatatan mutasi.
 *
 * Menguji dua urutan yang mungkin: mutasi lebih dulu (upsert pull ditolak FK RESTRICT),
 * dan pull lebih dulu (upsert berjalan). Di kedua kasus: audit trail utuh, stok akhir 8,
 * tidak deadlock. Memakai Room in-memory (androidTest).
 */
@RunWith(AndroidJUnit4::class)
class PullDuringFormTest {

    private lateinit var db: KonstruksiDatabase
    private lateinit var katalogDao: MasterKatalogDao
    private lateinit var mutasiDao: LogMutasiDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KonstruksiDatabase::class.java).build()
        katalogDao = db.masterKatalogDao()
        mutasiDao = db.logMutasiDao()
    }

    @After
    fun tearDown() { db.close() }

    private fun katalog(sku: String, stok: Int, ts: Long) = MasterKatalogEntity(
        sku_id = sku, kategori = KategoriBarang.BAHAN, nama_barang = "Semen",
        satuan = "sak", stok_lokal = stok, id_proyek = "PRJ-001", local_timestamp = ts
    )

    private fun checkoutLog(sku: String, jumlah: Int) = LogMutasiEntity(
        id_transaksi = UuidGenerator.generateId(), sku_id = sku,
        jenis_mutasi = "CHECK_OUT", jumlah = jumlah
    )

    // Pull lebih dulu: upsert berjalan, lalu checkout menghitung ulang stok jadi 8.
    @Test
    fun pullTereksekusiLebihDulu_laluMutasi_stokKonvergenTanpaError() = runBlocking {
        val sku = "SKU-P-B"
        val t0 = 1_000_000L
        katalogDao.upsert(katalog(sku, 10, t0))

        val local = katalogDao.getKatalogBySku(sku)!!
        katalogDao.upsert(local.copy(local_timestamp = t0 + 500, stok_lokal = 10))

        val setelahPull = katalogDao.getKatalogBySku(sku)!!
        assertEquals("upsert pull tereksekusi (timestamp maju)", t0 + 500, setelahPull.local_timestamp)
        assertEquals("stok server tertulis", 10, setelahPull.stok_lokal)

        val ok = mutasiDao.recordMutasiAtomic(checkoutLog(sku, 2), -2)
        assertTrue("checkout harus sukses", ok)

        assertEquals("stok akhir 10 - 2", 8, katalogDao.getKatalogBySku(sku)!!.stok_lokal)
        val logs = mutasiDao.getAllMutasi().first().filter { it.sku_id == sku }
        assertEquals("satu baris checkout", 1, logs.size)
    }

    // Mutasi lebih dulu: upsert pull ditolak FK RESTRICT, stok checkout tidak tertimpa.
    @Test
    fun mutasiTereksekusiLebihDulu_pullDitolakFkRestrict_stokTerlindungi() = runBlocking {
        val sku = "SKU-P-A"
        val t0 = 2_000_000L
        katalogDao.upsert(katalog(sku, 10, t0))

        val ok = mutasiDao.recordMutasiAtomic(checkoutLog(sku, 2), -2)
        assertTrue("checkout harus sukses", ok)
        assertEquals("stok setelah checkout", 8, katalogDao.getKatalogBySku(sku)!!.stok_lokal)

        val local = katalogDao.getKatalogBySku(sku)!!
        val pull = runCatching {
            katalogDao.upsert(local.copy(local_timestamp = t0 + 500, stok_lokal = 10))
        }
        assertTrue("upsert pull ditolak", pull.isFailure)

        assertEquals("stok tetap 8", 8, katalogDao.getKatalogBySku(sku)!!.stok_lokal)
        val logs = mutasiDao.getAllMutasi().first().filter { it.sku_id == sku }
        assertEquals("satu baris checkout", 1, logs.size)
    }
}
