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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test untuk operasi atomik stok + mutasi ([LogMutasiDao.recordMutasiAtomic]).
 *
 * Fokus:
 * - CT-01: dua+ CHECK_OUT bersamaan pada SKU yang sama TIDAK boleh membuat stok minus.
 * - Guard stok mencegah check-out melebihi stok (transaksi dibatalkan, tak ada log tersimpan).
 *
 * Memakai Room in-memory agar cepat & terisolasi. Butuh perangkat/emulator (androidTest).
 */
@RunWith(AndroidJUnit4::class)
class MutasiConcurrencyTest {

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

    private fun katalog(sku: String, stok: Int) = MasterKatalogEntity(
        sku_id = sku, kategori = KategoriBarang.BAHAN, nama_barang = "Semen",
        satuan = "sak", stok_lokal = stok, id_proyek = "PRJ-001"
    )

    private fun checkoutLog(sku: String, jumlah: Int) = LogMutasiEntity(
        id_transaksi = UuidGenerator.generateId(), sku_id = sku,
        jenis_mutasi = "CHECK_OUT", jumlah = jumlah
    )

    @Test
    fun checkin_menambahStok_danMenyimpanLog() = runBlocking {
        katalogDao.upsert(katalog("SKU-A", 10))
        val ok = mutasiDao.recordMutasiAtomic(
            LogMutasiEntity(
                id_transaksi = UuidGenerator.generateId(), sku_id = "SKU-A",
                jenis_mutasi = "CHECK_IN", jumlah = 5
            ),
            +5
        )
        assertTrue(ok)
        assertEquals(15, katalogDao.getKatalogBySku("SKU-A")!!.stok_lokal)
        assertEquals(1, mutasiDao.getPending().size)
    }

    @Test
    fun checkoutMelebihiStok_ditolak_tanpaMengubahApapun() = runBlocking {
        katalogDao.upsert(katalog("SKU-B", 10))
        val ok = mutasiDao.recordMutasiAtomic(checkoutLog("SKU-B", 15), -15)
        assertFalse(ok)
        assertEquals(10, katalogDao.getKatalogBySku("SKU-B")!!.stok_lokal) // stok utuh
        assertTrue(mutasiDao.getPending().isEmpty())                       // tak ada log tercatat
    }

    /**
     * CT-01 — Zero Data Loss di bawah konkurensi.
     * Stok awal 10, sepuluh CHECK_OUT bersamaan @2 unit (permintaan total 20).
     * Harapan: tepat 5 yang lolos (5×2=10), stok berakhir 0, tidak pernah minus,
     * dan jumlah log = jumlah yang lolos (tak ada penulisan tanpa pengurangan stok).
     */
    @Test
    fun duaCheckoutBersamaan_tidakMembuatStokMinus() = runBlocking {
        katalogDao.upsert(katalog("SKU-C", 10))

        val hasil = (1..10).map {
            async(Dispatchers.Default) {
                mutasiDao.recordMutasiAtomic(checkoutLog("SKU-C", 2), -2)
            }
        }.awaitAll()

        val sukses = hasil.count { it }
        val stokAkhir = katalogDao.getKatalogBySku("SKU-C")!!.stok_lokal

        assertEquals("Tepat 5 check-out yang boleh lolos", 5, sukses)
        assertEquals("Stok harus habis tepat di 0", 0, stokAkhir)
        assertTrue("Stok tidak boleh minus", stokAkhir >= 0)
        assertEquals("Jumlah log = jumlah check-out sukses", sukses, mutasiDao.getPending().size)
    }
}
