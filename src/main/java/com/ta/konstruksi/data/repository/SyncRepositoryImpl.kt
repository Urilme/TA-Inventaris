package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.LogMutasiDao
import com.ta.konstruksi.data.local.dao.MasterKatalogDao
import com.ta.konstruksi.data.local.dao.TransferProyekDao
import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.data.remote.FirestoreDataSource
import com.ta.konstruksi.domain.repository.SyncRepository
import com.ta.konstruksi.util.SyncStatus
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val mutasiDao: LogMutasiDao,
    private val transferDao: TransferProyekDao,
    private val katalogDao: MasterKatalogDao,
    private val userSessionDao: UserSessionDao,
    private val firestore: FirestoreDataSource
) : SyncRepository {

    /** id_mandor pengunggah — dicatat sebagai `updated_by` pada dokumen Firestore. */
    private suspend fun updatedBy(): String = userSessionDao.getSession()?.id_mandor.orEmpty()

    override suspend fun syncPendingKatalog(): Result<Int> = runCatching {
        val pending = katalogDao.getPendingSync()
        val by = updatedBy()
        // State machine: PENDING -> IN_TRANSIT (tandai dulu agar terlihat di UI)
        pending.forEach { katalogDao.updateSyncStatus(it.sku_id, SyncStatus.IN_TRANSIT.name) }
        pending.forEach { entity ->
            try {
                firestore.uploadKatalog(entity, by)
                katalogDao.updateSyncStatus(entity.sku_id, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                katalogDao.updateSyncStatus(entity.sku_id, SyncStatus.PENDING.name) // rollback
                throw e
            }
        }
        pending.size
    }

    override suspend fun syncPendingMutasi(): Result<Int> = runCatching {
        val pending = mutasiDao.getPending()
        val by = updatedBy()
        pending.forEach { mutasiDao.updateSyncStatus(it.id_transaksi, SyncStatus.IN_TRANSIT.name) }
        pending.forEach { entity ->
            try {
                firestore.uploadMutasi(entity, by)
                mutasiDao.updateSyncStatus(entity.id_transaksi, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                mutasiDao.updateSyncStatus(entity.id_transaksi, SyncStatus.PENDING.name) // rollback
                throw e
            }
        }
        pending.size
    }

    override suspend fun syncPendingTransfer(): Result<Int> = runCatching {
        val pending = transferDao.getPending()
        val by = updatedBy()
        pending.forEach { transferDao.updateSyncStatus(it.id_transfer, SyncStatus.IN_TRANSIT.name) }
        pending.forEach { entity ->
            try {
                firestore.uploadTransfer(entity, by)
                transferDao.updateSyncStatus(entity.id_transfer, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                transferDao.updateSyncStatus(entity.id_transfer, SyncStatus.PENDING.name) // rollback
                throw e
            }
        }
        pending.size
    }

    override suspend fun pullLatestKatalog(): Result<Unit> = runCatching {
        val since = getLastSyncTimestamp()
        val result = firestore.pullKatalog(since)

        // Last-Write-Wins: remote hanya menimpa lokal bila local_timestamp-nya lebih baru.
        result.items.forEach { remote ->
            val local = katalogDao.getKatalogBySku(remote.sku_id)
            if (local == null || remote.local_timestamp > local.local_timestamp) {
                katalogDao.upsert(remote)
            }
            // else: versi lokal lebih baru → dipertahankan (menang LWW)
        }

        // Majukan kursor delta ke server_timestamp terbesar yang telah dikonsumsi
        if (result.latestServerMillis > since) {
            updateLastSyncTimestamp(result.latestServerMillis)
        }
    }

    override suspend fun getLastSyncTimestamp(): Long =
        userSessionDao.getSession()?.last_sync_timestamp ?: 0L

    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        userSessionDao.updateLastSync(timestamp)
    }
}
