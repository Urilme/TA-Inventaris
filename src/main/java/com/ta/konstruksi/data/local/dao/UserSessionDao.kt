package com.ta.konstruksi.data.local.dao

import androidx.room.*
import com.ta.konstruksi.data.local.entity.UserSessionEntity

@Dao
interface UserSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: UserSessionEntity)

    @Query("SELECT * FROM user_session LIMIT 1")
    suspend fun getSession(): UserSessionEntity?

    @Query("DELETE FROM user_session")
    suspend fun clearSession()

    @Query("UPDATE user_session SET last_sync_timestamp = :timestamp")
    suspend fun updateLastSync(timestamp: Long)

    @Query("UPDATE user_session SET last_sync_mutasi = :timestamp")
    suspend fun updateLastSyncMutasi(timestamp: Long)

    @Query("UPDATE user_session SET last_sync_transfer = :timestamp")
    suspend fun updateLastSyncTransfer(timestamp: Long)
}
