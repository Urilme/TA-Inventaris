package com.ta.konstruksi.domain.repository

import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.util.StatusTransfer
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun getAllTransfer(): Flow<List<TransferProyek>>
    suspend fun createTransfer(transfer: TransferProyek)
    suspend fun updateStatusTransfer(idTransfer: String, status: StatusTransfer)
    suspend fun getPendingTransfer(): List<TransferProyek>
    suspend fun markSynced(idTransfer: String)
}
