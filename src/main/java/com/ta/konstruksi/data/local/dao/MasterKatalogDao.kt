package com.ta.konstruksi.data.local.dao

import androidx.room.*
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MasterKatalogDao {
    // Katalog difilter per proyek: mandor hanya melihat barang di proyeknya sendiri.
    @Query("SELECT * FROM master_katalog WHERE id_proyek = :idProyek ORDER BY nama_barang ASC")
    abstract fun getAllKatalog(idProyek: String): Flow<List<MasterKatalogEntity>>

    @Query("SELECT * FROM master_katalog WHERE sku_id = :skuId")
    abstract suspend fun getKatalogBySku(skuId: String): MasterKatalogEntity?

    // Cari barang sejenis (nama sama) di suatu proyek — dipakai saat konfirmasi transfer masuk
    // untuk menambah stok ke item yang sudah ada, bukan membuat duplikat.
    @Query("SELECT * FROM master_katalog WHERE id_proyek = :idProyek AND nama_barang = :nama LIMIT 1")
    abstract suspend fun findByProyekAndNama(idProyek: String, nama: String): MasterKatalogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(item: MasterKatalogEntity)

    @Query("DELETE FROM master_katalog WHERE sku_id = :skuId")
    abstract suspend fun delete(skuId: String)

    @Query("SELECT stok_lokal FROM master_katalog WHERE sku_id = :skuId")
    abstract suspend fun getStok(skuId: String): Int?

    @Query("UPDATE master_katalog SET stok_lokal = stok_lokal + :delta, sync_status = 'PENDING' WHERE sku_id = :skuId")
    abstract suspend fun updateStok(skuId: String, delta: Int)

    /**
     * Kurangi stok dalam satu transaksi atomik, hanya bila mencukupi. Guard baca-cek-tulis
     * mencegah stok minus saat akses bersamaan.
     *
     * @return true bila stok dikurangi; false bila tidak mencukupi.
     */
    @Transaction
    open suspend fun decrementStokJikaCukup(skuId: String, jumlah: Int): Boolean {
        val current = getStok(skuId) ?: return false
        if (current - jumlah < 0) return false   // stok tidak mencukupi → batalkan
        updateStok(skuId, -jumlah)
        return true
    }

    // Sertakan IN_TRANSIT agar item yang tersinkron terputus dipulihkan pada siklus berikutnya
    @Query("SELECT * FROM master_katalog WHERE sync_status IN ('PENDING', 'IN_TRANSIT')")
    abstract suspend fun getPendingSync(): List<MasterKatalogEntity>

    @Query("UPDATE master_katalog SET sync_status = :status WHERE sku_id = :skuId")
    abstract suspend fun updateSyncStatus(skuId: String, status: String)

    @Query("UPDATE master_katalog SET sync_status = 'SYNCED' WHERE sku_id = :skuId")
    abstract suspend fun markSynced(skuId: String)
}
