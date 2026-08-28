package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.LogMutasiDao
import com.ta.konstruksi.data.local.dao.MasterKatalogDao
import com.ta.konstruksi.data.local.dao.ProyekDao
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
    private val proyekDao: ProyekDao,
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
        // Hanya tarik katalog milik proyek si mandor (scoping per proyek).
        val proyek = userSessionDao.getSession()?.id_proyek.orEmpty()
        val result = firestore.pullKatalog(since, proyek)

        result.items.forEach { remote ->
            val local = katalogDao.getKatalogBySku(remote.sku_id)
            if (bolehTimpa(local?.sync_status, local?.local_timestamp, remote.local_timestamp)) {
                katalogDao.upsert(remote)
            }
        }

        // Majukan kursor delta ke server_timestamp terbesar yang telah dikonsumsi
        if (result.latestServerMillis > since) {
            updateLastSyncTimestamp(result.latestServerMillis)
        }
    }

    override suspend fun pullLatestMutasi(): Result<Unit> = runCatching {
        val session = userSessionDao.getSession()
        val since = session?.last_sync_mutasi ?: 0L
        val proyek = session?.id_proyek.orEmpty()
        val result = firestore.pullMutasi(since, proyek)

        result.items.forEach { remote ->
            // FK log_mutasi → master_katalog (RESTRICT): lewati mutasi yang barang induknya belum
            // ada di katalog lokal agar tak menggagalkan seluruh sync. Normalnya katalog proyek
            // yang sama sudah ditarik lebih dulu sehingga induknya tersedia.
            if (katalogDao.getKatalogBySku(remote.sku_id) == null) return@forEach
            val local = mutasiDao.getById(remote.id_transaksi)
            if (bolehTimpa(local?.sync_status, local?.local_timestamp, remote.local_timestamp)) {
                mutasiDao.insert(remote)
            }
        }

        if (result.latestServerMillis > since) {
            userSessionDao.updateLastSyncMutasi(result.latestServerMillis)
        }
    }

    override suspend fun pullLatestTransfer(): Result<Unit> = runCatching {
        val session = userSessionDao.getSession()
        val since = session?.last_sync_transfer ?: 0L
        // Filter transfer berbasis NAMA proyek (dokumen menyimpan nama, bukan id).
        val namaProyek = session?.nama_proyek.orEmpty()
        val result = firestore.pullTransfer(since, namaProyek)

        result.items.forEach { remote ->
            val local = transferDao.getById(remote.id_transfer)
            if (bolehTimpa(local?.sync_status, local?.local_timestamp, remote.local_timestamp)) {
                transferDao.insert(remote)
            }
        }

        if (result.latestServerMillis > since) {
            userSessionDao.updateLastSyncTransfer(result.latestServerMillis)
        }
    }

    /**
     * Aturan penerapan versi server ke lokal:
     * - baris belum ada di lokal → ambil.
     * - baris lokal masih PENDING/IN_TRANSIT (perubahan belum ter-push) → JANGAN timpa; perubahan
     *   lokal otoritatif sampai terkirim, lalu menang LWW di server.
     * - baris lokal SYNCED → Last-Write-Wins: remote menang hanya bila local_timestamp-nya lebih baru.
     */
    private fun bolehTimpa(localStatus: String?, localTimestamp: Long?, remoteTimestamp: Long): Boolean {
        if (localStatus == null) return true
        if (localStatus != SyncStatus.SYNCED.name) return false
        return remoteTimestamp > (localTimestamp ?: 0L)
    }

    override suspend fun pullLatestProyek(): Result<Unit> = runCatching {
        // Registry proyek ditarik penuh (koleksi kecil, read-only). Hanya ganti cache bila
        // server mengembalikan data — agar daftar proyek tak terhapus saat kegagalan/aturan kosong.
        val proyek = firestore.pullProyek()
        if (proyek.isNotEmpty()) proyekDao.replaceAll(proyek)
    }

    override suspend fun getLastSyncTimestamp(): Long =
        userSessionDao.getSession()?.last_sync_timestamp ?: 0L

    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        userSessionDao.updateLastSync(timestamp)
    }
}
