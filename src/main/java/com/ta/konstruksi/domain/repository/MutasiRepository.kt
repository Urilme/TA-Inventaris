package com.ta.konstruksi.domain.repository

import com.ta.konstruksi.domain.model.LogMutasi
import kotlinx.coroutines.flow.Flow

interface MutasiRepository {
    fun getAllMutasi(): Flow<List<LogMutasi>>
    suspend fun recordMutasi(mutasi: LogMutasi)
    suspend fun getPendingMutasi(): List<LogMutasi>
    suspend fun markSynced(idTransaksi: String)
}
