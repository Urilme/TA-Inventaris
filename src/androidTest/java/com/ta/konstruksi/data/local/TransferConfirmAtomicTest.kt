package com.ta.konstruksi.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ta.konstruksi.data.local.dao.MasterKatalogDao
import com.ta.konstruksi.data.local.dao.TransferProyekDao
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import com.ta.konstruksi.data.local.entity.TransferProyekEntity
import com.ta.konstruksi.util.KategoriBarang
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Membuktikan atomicity konfirmasi/pembatalan transfer di DAO: guard status IN_TRANSIT + perubahan
 * stok + perubahan status berada dalam satu @Transaction, sehingga konfirmasi ulang / double-submit
 * atas transfer yang sama tidak menambah stok dua kali. Room in-memory (androidTest).
 */
@RunWith(AndroidJUnit4::class)
class TransferConfirmAtomicTest {

    private lateinit var db: KonstruksiDatabase
    private lateinit var katalogDao: MasterKatalogDao
    private lateinit var transferDao: TransferProyekDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KonstruksiDatabase::class.java).build()
        katalogDao = db.masterKatalogDao()
        transferDao = db.transferProyekDao()
    }

    @After
    fun tearDown() { db.close() }

    private fun transfer(id: String, status: String = "IN_TRANSIT") = TransferProyekEntity(
        id_transfer = id, sku_id = "SKU-ASAL", nama_barang = "Bor",
        kategori = KategoriBarang.ALAT, satuan = "unit",
        proyek_asal = "Proyek A", proyek_tujuan = "Proyek B", jumlah = 2, status_transfer = status
    )

    @Test
    fun duaKonfirmasiBeruntun_hanyaMenambahStokSekali() = runBlocking {
        transferDao.insert(transfer("TR-1"))

        val pertama = transferDao.konfirmasiTerimaAtomik("TR-1", "PRJ-002", 1000L, "SKU-BARU-1")
        val kedua = transferDao.konfirmasiTerimaAtomik("TR-1", "PRJ-002", 2000L, "SKU-BARU-2")

        assertTrue("Konfirmasi pertama harus berhasil", pertama)
        assertFalse("Konfirmasi kedua harus ditolak (sudah SELESAI)", kedua)

        val masuk = katalogDao.findByProyekAndNama("PRJ-002", "Bor")
        assertNotNull(masuk)
        assertEquals("Stok hanya bertambah sekali", 2, masuk!!.stok_lokal)
        assertEquals("SELESAI", transferDao.getById("TR-1")!!.status_transfer)
    }

    @Test
    fun konfirmasiTransferYangSudahSelesai_ditolak() = runBlocking {
        transferDao.insert(transfer("TR-2", status = "SELESAI"))

        val hasil = transferDao.konfirmasiTerimaAtomik("TR-2", "PRJ-002", 1000L, "SKU-BARU")

        assertFalse(hasil)
        assertEquals(null, katalogDao.findByProyekAndNama("PRJ-002", "Bor"))   // stok tak dibuat
    }

    @Test
    fun duaPembatalanBeruntun_hanyaMengembalikanStokSekali() = runBlocking {
        katalogDao.upsert(
            MasterKatalogEntity(
                sku_id = "SKU-ASAL", kategori = KategoriBarang.ALAT, nama_barang = "Bor",
                satuan = "unit", stok_lokal = 3, id_proyek = "PRJ-001"
            )
        )
        transferDao.insert(transfer("TR-3"))

        val pertama = transferDao.batalkanAtomik("TR-3", 1000L)
        val kedua = transferDao.batalkanAtomik("TR-3", 2000L)

        assertTrue(pertama)
        assertFalse(kedua)
        assertEquals("Stok kembali sekali (3 + 2)", 5, katalogDao.getKatalogBySku("SKU-ASAL")!!.stok_lokal)
        assertEquals("DIBATALKAN", transferDao.getById("TR-3")!!.status_transfer)
    }
}
