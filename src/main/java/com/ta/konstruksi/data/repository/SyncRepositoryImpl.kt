package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.LogMutasiDao
import com.ta.konstruksi.data.local.dao.MasterKatalogDao
import com.ta.konstruksi.data.local.dao.TransferProyekDao
import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.data.remote.FirestoreDataSource
import com.ta.konstruksi.domain.repository.SyncRepository
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val mutasiDao: LogMutasiDao,
    private val transferDao: TransferProyekDao,
    private val katalogDao: MasterKatalogDao,
    private val userSessionDao: UserSessionDao,
    private val firestore: FirestoreDataSource
) : SyncRepository {

    override suspend fun syncPendingKatalog(): Result<Int> = runCatching {
        val pending = katalogDao.getPendingSync()
        pending.forEach { entity ->
            firestore.uploadKatalog(entity)
            katalogDao.markSynced(entity.sku_id)
        }
        pending.size
    }

    override suspend fun syncPendingMutasi(): Result<Int> = runCatching {
        val pending = mutasiDao.getPending()
        pending.forEach { entity ->
            firestore.uploadMutasi(entity)
            mutasiDao.markSynced(entity.id_transaksi)
        }
        pending.size
    }

    override suspend fun syncPendingTransfer(): Result<Int> = runCatching {
        val pending = transferDao.getPending()
        pending.forEach { entity ->
            firestore.uploadTransfer(entity)
            transferDao.markSynced(entity.id_transfer)
        }
        pending.size
    }

    override suspend fun pullLatestKatalog(): Result<Unit> = runCatching {
        val since = getLastSyncTimestamp()
        firestore.pullKatalog(since).forEach { katalogDao.upsert(it) }
    }

    override suspend fun getLastSyncTimestamp(): Long =
        userSessionDao.getSession()?.last_sync_timestamp ?: 0L

    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        userSessionDao.updateLastSync(timestamp)
    }
}
