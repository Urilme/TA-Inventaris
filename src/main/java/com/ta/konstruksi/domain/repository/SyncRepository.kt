package com.ta.konstruksi.domain.repository

interface SyncRepository {
    suspend fun syncPendingKatalog(): Result<Int>   // returns count synced
    suspend fun syncPendingMutasi(): Result<Int>   // returns count synced
    suspend fun syncPendingTransfer(): Result<Int>
    suspend fun pullLatestKatalog(): Result<Unit>
    suspend fun pullLatestMutasi(): Result<Unit>
    suspend fun pullLatestTransfer(): Result<Unit>
    suspend fun pullLatestProyek(): Result<Unit>
    suspend fun getLastSyncTimestamp(): Long
    suspend fun updateLastSyncTimestamp(timestamp: Long)

    /** true bila masih ada baris PENDING/IN_TRANSIT tersisa (antrean melebihi satu batch). */
    suspend fun hasPendingSync(): Boolean
}
