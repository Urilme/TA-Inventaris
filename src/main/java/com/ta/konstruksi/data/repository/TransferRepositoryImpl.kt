package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.TransferProyekDao
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.util.StatusTransfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransferRepositoryImpl @Inject constructor(
    private val transferDao: TransferProyekDao
) : TransferRepository {

    override fun getAllTransfer(): Flow<List<TransferProyek>> =
        transferDao.getAllTransfer().map { list -> list.map { it.toDomain() } }

    override suspend fun createTransfer(transfer: TransferProyek) =
        transferDao.insert(transfer.toEntity())

    override suspend fun updateStatusTransfer(idTransfer: String, status: StatusTransfer) =
        transferDao.updateStatus(idTransfer, status.name, System.currentTimeMillis())

    override suspend fun getPendingTransfer(): List<TransferProyek> =
        transferDao.getPending().map { it.toDomain() }

    override suspend fun markSynced(idTransfer: String) =
        transferDao.markSynced(idTransfer)
}
