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
 * Concurrency Testing No. 6 (CT-06) — Konflik agregat stok pada dua "perangkat" (LWW).
 *
 * Dua perangkat men-checkout SKU yang sama secara paralel (A: 3 unit, B: 2 unit) terhadap stok
 * awal 10. Konflik LWW yang sesungguhnya BUKAN di dokumen mutasi (tiap mutasi = dokumen unik
 * ber-UUID, tak pernah bentrok), melainkan di AGREGAT `stok_lokal`. Test ini memvalidasi lapisan
 * Room lokal:
 *  - AUDIT TRAIL: [LogMutasiDao.recordMutasiAtomic] bersifat @Transaction, sehingga KEDUA baris
 *    mutasi selalu tersimpan tanpa ada yang hilang (invariant utama yang harus selalu benar).
 *  - STOK: karena kedua transaksi atomik terserialisasi pada satu koneksi Room, hasil akhir
 *    deterministik = 10 − 3 − 2 = 5, tak pernah minus/rusak, apa pun urutan commit-nya.
 *
 * Divergensi `stok_lokal` ANTAR PERANGKAT via Firestore (uploadKatalog memakai `.set()` tanpa
 * cek versi di sisi push; LWW hanya di sisi pull) adalah trade-off arsitektur yang SUDAH DIKETAHUI
 * dan TIDAK diklaim "diperbaiki" oleh test ini — yang dibuktikan hanyalah log mutasi tak pernah
 * hilang dan stok lokal tak pernah rusak.
 *
 * Memakai Room in-memory. Butuh perangkat/emulator (androidTest).
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
            // SKU unik per trial agar tidak menimpa baris ber-anak (log_mutasi FK sku_id) trial lalu.
            val sku = "SKU-R$trial"
            katalogDao.upsert(katalog(sku, 10))

            val deviceA = async(Dispatchers.Default) { mutasiDao.recordMutasiAtomic(checkoutLog(sku, 3), -3) }
            val deviceB = async(Dispatchers.Default) { mutasiDao.recordMutasiAtomic(checkoutLog(sku, 2), -2) }
            val hasil = awaitAll(deviceA, deviceB)

            // Audit trail: kedua operasi sukses (stok cukup) & kedua baris tercatat, tak ada yang hilang.
            assertEquals("Trial $trial: kedua checkout harus sukses", 2, hasil.count { it })
            val logs = mutasiDao.getAllMutasi().first().filter { it.sku_id == sku }
            assertEquals("Trial $trial: log_mutasi memuat kedua baris mutasi", 2, logs.size)
            if (logs.size == 2) trialAuditUtuh++

            // Stok deterministik apa pun urutan commit; tidak pernah minus/rusak.
            val stok = katalogDao.getKatalogBySku(sku)!!.stok_lokal
            assertEquals("Trial $trial: stok akhir = 10 - 3 - 2", 5, stok)
            assertTrue("Trial $trial: stok tidak boleh minus", stok >= 0)
            if (stok == 5) trialStokKonsisten++
        }

        // Ringkasan kuantitatif utk dokumentasi (dibaca lewat Logcat filter package:com.ta.konstruksi) —
        // menggantikan Database Inspector, yang tidak sempat menyambung ke DB in-memory berumur pendek ini.
        println("KatalogPushRaceTest — audit trail utuh: $trialAuditUtuh/25 trial (log_mutasi selalu 2 baris, tak pernah hilang)")
        println("KatalogPushRaceTest — stok akhir konsisten: $trialStokKonsisten/25 trial (selalu 10-3-2=5, tak pernah minus/rusak)")
    }
}
