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
 * Concurrency Testing No. 8 (CT-08) — PULL katalog berjalan saat mutasi sedang dicatat.
 *
 * Skenario: siklus sinkronisasi menarik ("pull") versi server sebuah katalog TEPAT ketika mandor
 * sedang menyelesaikan input mutasi pada SKU yang sama. Karena Room memakai satu koneksi penulis,
 * kedua operasi terserialisasi; hanya ADA DUA interleaving yang mungkin, dan test ini menguji
 * KEDUANYA secara deterministik (bukan race flaky yang cuma pernah menyentuh satu urutan):
 *
 *  A. MUTASI commit lebih dulu → baris anak `log_mutasi` sudah ada. Pull memakai
 *     [MasterKatalogDao.upsert] = `@Insert(REPLACE)`, dan REPLACE menghapus-lalu-menyisip induk;
 *     DELETE induk ber-anak DITOLAK `ForeignKey.RESTRICT`. Jadi upsert pull GAGAL dan stok hasil
 *     checkout TIDAK tertimpa — FK RESTRICT justru melindungi konsistensi lokal.
 *
 *  B. PULL commit lebih dulu → katalog BELUM punya anak `log_mutasi`, sehingga REPLACE
 *     BENAR-BENAR tereksekusi (FK valid, tidak ditolak Room). Setelah itu mandor menuntaskan
 *     checkout: [LogMutasiDao.recordMutasiAtomic] membaca stok di dalam transaksinya sendiri,
 *     menghitung ulang dari stok server terbaru, lalu menyisipkan baris log. Sistem konvergen.
 *
 * Invariant yang harus SELALU benar di kedua interleaving:
 *  - Audit trail utuh: tepat satu baris checkout tercatat, tak pernah hilang.
 *  - Stok akhir konsisten = 10 − 2 = 8, tak pernah rusak/minus.
 *  - Tidak ada deadlock (operasi selesai tanpa menggantung).
 *
 * Catatan: bahwa pull REPLACE atas katalog ber-riwayat mutasi bisa gagal FK RESTRICT adalah
 * karakteristik produksi yang SUDAH DIKETAHUI (lihat PANDUAN skenario 8). Test ini membuktikan
 * perilaku itu apa adanya — TIDAK mengubah kode produksi.
 *
 * Memakai Room in-memory. Butuh perangkat/emulator (androidTest).
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

    /**
     * Interleaving B — PULL menang lebih dulu. Karena katalog BELUM punya anak `log_mutasi`,
     * upsert(REPLACE) pull BENAR-BENAR tereksekusi (bukan ditolak Room). Dibuktikan lewat
     * majunya `local_timestamp` ke nilai server. Checkout yang menyusul menghitung ulang stok
     * dari nilai terbaru sehingga sistem konvergen ke 8.
     */
    @Test
    fun pullTereksekusiLebihDulu_laluMutasi_stokKonvergenTanpaError() = runBlocking {
        val sku = "SKU-P-B"
        val t0 = 1_000_000L
        katalogDao.upsert(katalog(sku, 10, t0))   // seed lokal, belum ada baris log_mutasi

        // Pull menulis versi server (stok otoritatif 10, timestamp lebih baru). FK valid → upsert JALAN.
        val local = katalogDao.getKatalogBySku(sku)!!
        katalogDao.upsert(local.copy(local_timestamp = t0 + 500, stok_lokal = 10))

        val setelahPull = katalogDao.getKatalogBySku(sku)!!
        assertEquals("upsert pull benar-benar tereksekusi (timestamp maju)", t0 + 500, setelahPull.local_timestamp)
        assertEquals("stok server tertulis", 10, setelahPull.stok_lokal)

        // Mandor menuntaskan input: checkout 2 dihitung ulang dari stok terbaru (10 → 8) secara atomik.
        val ok = mutasiDao.recordMutasiAtomic(checkoutLog(sku, 2), -2)
        assertTrue("checkout harus sukses (stok cukup)", ok)

        assertEquals("stok akhir konvergen 10 - 2", 8, katalogDao.getKatalogBySku(sku)!!.stok_lokal)
        val logs = mutasiDao.getAllMutasi().first().filter { it.sku_id == sku }
        assertEquals("audit trail: tepat satu baris checkout", 1, logs.size)
    }

    /**
     * Interleaving A — MUTASI menang lebih dulu. Baris anak `log_mutasi` sudah ada, sehingga
     * upsert(REPLACE) pull DITOLAK FK RESTRICT. Stok hasil checkout TIDAK tertimpa oleh stok
     * server — proteksi FK menjaga konsistensi lokal.
     */
    @Test
    fun mutasiTereksekusiLebihDulu_pullDitolakFkRestrict_stokTerlindungi() = runBlocking {
        val sku = "SKU-P-A"
        val t0 = 2_000_000L
        katalogDao.upsert(katalog(sku, 10, t0))

        // Mutasi commit lebih dulu → menyisipkan baris anak log_mutasi.
        val ok = mutasiDao.recordMutasiAtomic(checkoutLog(sku, 2), -2)
        assertTrue("checkout harus sukses", ok)
        assertEquals("stok setelah checkout", 8, katalogDao.getKatalogBySku(sku)!!.stok_lokal)

        // Pull mencoba upsert(REPLACE) atas induk yang SUDAH punya anak → DELETE ditolak RESTRICT.
        val local = katalogDao.getKatalogBySku(sku)!!
        val pull = runCatching {
            katalogDao.upsert(local.copy(local_timestamp = t0 + 500, stok_lokal = 10))
        }
        assertTrue("upsert pull ditolak FK RESTRICT (tidak menimpa stok)", pull.isFailure)

        // Stok hasil checkout terlindungi: tidak di-clobber menjadi 10.
        assertEquals("stok tetap 8 (tidak tertimpa pull)", 8, katalogDao.getKatalogBySku(sku)!!.stok_lokal)
        val logs = mutasiDao.getAllMutasi().first().filter { it.sku_id == sku }
        assertEquals("audit trail tetap satu baris", 1, logs.size)
    }
}
