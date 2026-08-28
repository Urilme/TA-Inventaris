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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uji dua checkout paralel pada SKU yang sama (3 dan 2 unit dari stok 10).
 *
 * Karena recordMutasiAtomic bersifat @Transaction dan terserialisasi, kedua baris mutasi
 * selalu tersimpan dan stok akhir deterministik = 5, tak pernah minus. Memakai Room
 * in-memory (androidTest).
 */
@RunWith(AndroidJUnit4::class)
class KatalogPushRaceTest {

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
    fun duaDeviceCheckoutParalel_logMutasiUtuh_danStokDeterministik() = runBlocking {
        var trialAuditUtuh = 0
        var trialStokKonsisten = 0

        repeat(25) { trial ->
            // SKU unik per trial.
            val sku = "SKU-R$trial"
            katalogDao.upsert(katalog(sku, 10))

            val deviceA = async(Dispatchers.Default) { mutasiDao.recordMutasiAtomic(checkoutLog(sku, 3), -3) }
            val deviceB = async(Dispatchers.Default) { mutasiDao.recordMutasiAtomic(checkoutLog(sku, 2), -2) }
            val hasil = awaitAll(deviceA, deviceB)

            // Kedua operasi sukses dan kedua baris tercatat.
            assertEquals("Trial $trial: kedua checkout harus sukses", 2, hasil.count { it })
            val logs = mutasiDao.getAllMutasi().first().filter { it.sku_id == sku }
            assertEquals("Trial $trial: log_mutasi memuat kedua baris mutasi", 2, logs.size)
            if (logs.size == 2) trialAuditUtuh++

            // Stok akhir deterministik, tidak minus.
            val stok = katalogDao.getKatalogBySku(sku)!!.stok_lokal
            assertEquals("Trial $trial: stok akhir = 10 - 3 - 2", 5, stok)
            assertTrue("Trial $trial: stok tidak boleh minus", stok >= 0)
            if (stok == 5) trialStokKonsisten++
        }

        // Ringkasan hasil (dibaca lewat Logcat).
        println("KatalogPushRaceTest — audit trail utuh: $trialAuditUtuh/25 trial")
        println("KatalogPushRaceTest — stok akhir konsisten: $trialStokKonsisten/25 trial")
    }
}
