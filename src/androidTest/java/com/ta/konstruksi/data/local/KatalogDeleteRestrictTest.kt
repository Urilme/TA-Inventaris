package com.ta.konstruksi.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ta.konstruksi.data.local.dao.LogMutasiDao
import com.ta.konstruksi.data.local.dao.MasterKatalogDao
import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.UuidGenerator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Membuktikan FK RESTRICT: barang yang masih punya riwayat mutasi tidak bisa dihapus dari katalog
 * (SQLite menolak), sekaligus memverifikasi countBySku sebagai basis pra-cek di aplikasi.
 * Room in-memory (androidTest) — foreign_keys aktif secara default.
 */
@RunWith(AndroidJUnit4::class)
class KatalogDeleteRestrictTest {

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

    private fun katalog(sku: String) = MasterKatalogEntity(
        sku_id = sku, kategori = KategoriBarang.BAHAN, nama_barang = "Semen",
        satuan = "sak", stok_lokal = 10, id_proyek = "PRJ-001"
    )

    private fun mutasi(sku: String) = LogMutasiEntity(
        id_transaksi = UuidGenerator.generateId(), sku_id = sku,
        jenis_mutasi = "CHECK_OUT", jumlah = 2
    )

    @Test
    fun hapusBarangYangPunyaRiwayat_ditolakConstraint() = runBlocking {
        katalogDao.upsert(katalog("SKU-A"))
        mutasiDao.insert(mutasi("SKU-A"))

        assertEquals(1, mutasiDao.countBySku("SKU-A"))

        var ditolak = false
        try {
            katalogDao.delete("SKU-A")
        } catch (e: SQLiteConstraintException) {
            ditolak = true
        }

        assertTrue("Hapus barang bermutasi harus ditolak FK RESTRICT", ditolak)
        assertNotNull("Barang tetap ada setelah penolakan", katalogDao.getKatalogBySku("SKU-A"))
    }

    @Test
    fun hapusBarangTanpaRiwayat_berhasil() = runBlocking {
        katalogDao.upsert(katalog("SKU-B"))

        assertEquals(0, mutasiDao.countBySku("SKU-B"))
        katalogDao.delete("SKU-B")

        assertEquals(null, katalogDao.getKatalogBySku("SKU-B"))
    }
}
