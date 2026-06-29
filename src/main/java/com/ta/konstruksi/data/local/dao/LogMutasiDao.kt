package com.ta.konstruksi.data.local.dao

import androidx.room.*
import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogMutasiDao {
    @Query("SELECT * FROM log_mutasi ORDER BY local_timestamp DESC")
    fun getAllMutasi(): Flow<List<LogMutasiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mutasi: LogMutasiEntity)

    @Query("SELECT * FROM log_mutasi WHERE sync_status = 'PENDING'")
    suspend fun getPending(): List<LogMutasiEntity>

    @Query("UPDATE log_mutasi SET sync_status = 'SYNCED' WHERE id_transaksi = :id")
    suspend fun markSynced(id: String)
}
