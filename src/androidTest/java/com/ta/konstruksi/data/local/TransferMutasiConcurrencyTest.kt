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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uji transfer + mutasi bersamaan pada SKU yang sama: stok lokal tidak boleh minus.
 * Jalur pengurangan stok identik dengan produksi — transfer via
 * [MasterKatalogDao.decrementStokJikaCukup] dan mutasi via [LogMutasiDao.recordMutasiAtomic],
 * keduanya atomik baca-cek-tulis. Memakai Room in-memory (androidTest).
 */
@RunWith(AndroidJUnit4::class)
class TransferMutasiConcurrencyTest {

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
        sku_id = sku, kategori = KategoriBarang.BAHAN, nama_barang = "Besi Hollow",
        satuan = "batang", stok_lokal = stok, id_proyek = "PRJ-001"
    )

    private fun checkoutLog(sku: String, jumlah: Int) = LogMutasiEntity(
        id_transaksi = UuidGenerator.generateId(), sku_id = sku,
        jenis_mutasi = "CHECK_OUT", jumlah = jumlah
    )

    /**
     * Stok awal 10. Tiga transfer keluar @2 unit dan tiga check-out @2 unit ditembakkan
     * BERSAMAAN (total permintaan 12 > 10). Diulang beberapa trial agar berbagai interleaving
     * terjadi. Harapan: pada setiap trial stok berakhir tidak pernah negatif, dan total
     * pengurangan tidak melebihi stok awal.
     */
    @Test
    fun transferDanCheckoutBersamaan_tidakMembuatStokMinus() = runBlocking {
        repeat(25) { trial ->
            // SKU unik per trial: hindari REPLACE atas baris yang sudah punya anak (log_mutasi
            // FK sku_id) dari trial sebelumnya — fokus uji adalah invariant stok, bukan upsert.
            val sku = "SKU-T$trial"
            katalogDao.upsert(katalog(sku, 10))

            val transferOps = (1..3).map {
                async(Dispatchers.Default) { katalogDao.decrementStokJikaCukup(sku, 2) }
            }
            val mutasiOps = (1..3).map {
                async(Dispatchers.Default) { mutasiDao.recordMutasiAtomic(checkoutLog(sku, 2), -2) }
            }
            val hasil = (transferOps + mutasiOps).awaitAll()

            val stok = katalogDao.getKatalogBySku(sku)!!.stok_lokal
            val sukses = hasil.count { it }
            assertTrue("Trial $trial: stok tidak boleh minus (stok=$stok)", stok >= 0)
            assertTrue(
                "Trial $trial: total pengurangan (${sukses * 2}) tak boleh melebihi stok awal 10",
                sukses * 2 <= 10
            )
            assertTrue("Trial $trial: stok harus = 10 - 2*sukses", stok == 10 - sukses * 2)
        }
    }
}
