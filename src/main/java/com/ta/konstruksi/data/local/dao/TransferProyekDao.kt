package com.ta.konstruksi.data.local.dao

import androidx.room.*
import com.ta.konstruksi.data.local.entity.TransferProyekEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferProyekDao {
    @Query("SELECT * FROM transfer_proyek ORDER BY local_timestamp DESC")
    fun getAllTransfer(): Flow<List<TransferProyekEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transfer: TransferProyekEntity)

    @Query("UPDATE transfer_proyek SET status_transfer = :status, sync_status = 'PENDING' WHERE id_transfer = :id")
    suspend fun updateStatus(id: String, status: String)

    // Sertakan IN_TRANSIT agar item yang tersinkron terputus dipulihkan pada siklus berikutnya
    @Query("SELECT * FROM transfer_proyek WHERE sync_status IN ('PENDING', 'IN_TRANSIT')")
    suspend fun getPending(): List<TransferProyekEntity>

    @Query("UPDATE transfer_proyek SET sync_status = :status WHERE id_transfer = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE transfer_proyek SET sync_status = 'SYNCED' WHERE id_transfer = :id")
    suspend fun markSynced(id: String)
}
