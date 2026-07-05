package com.ta.konstruksi.data.local.dao

import androidx.room.*
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterKatalogDao {
    @Query("SELECT * FROM master_katalog ORDER BY nama_barang ASC")
    fun getAllKatalog(): Flow<List<MasterKatalogEntity>>

    @Query("SELECT * FROM master_katalog WHERE sku_id = :skuId")
    suspend fun getKatalogBySku(skuId: String): MasterKatalogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MasterKatalogEntity)

    @Query("DELETE FROM master_katalog WHERE sku_id = :skuId")
    suspend fun delete(skuId: String)

    @Query("UPDATE master_katalog SET stok_lokal = stok_lokal + :delta, sync_status = 'PENDING' WHERE sku_id = :skuId")
    suspend fun updateStok(skuId: String, delta: Int)

    // Sertakan IN_TRANSIT agar item yang tersinkron terputus dipulihkan pada siklus berikutnya
    @Query("SELECT * FROM master_katalog WHERE sync_status IN ('PENDING', 'IN_TRANSIT')")
    suspend fun getPendingSync(): List<MasterKatalogEntity>

    @Query("UPDATE master_katalog SET sync_status = :status WHERE sku_id = :skuId")
    suspend fun updateSyncStatus(skuId: String, status: String)

    @Query("UPDATE master_katalog SET sync_status = 'SYNCED' WHERE sku_id = :skuId")
    suspend fun markSynced(skuId: String)
}
