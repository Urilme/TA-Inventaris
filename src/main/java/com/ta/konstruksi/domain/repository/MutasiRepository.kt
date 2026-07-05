package com.ta.konstruksi.domain.repository

import com.ta.konstruksi.domain.model.LogMutasi
import kotlinx.coroutines.flow.Flow

interface MutasiRepository {
    fun getAllMutasi(): Flow<List<LogMutasi>>

    /**
     * Catat mutasi + perbarui stok secara atomik (satu transaksi Room).
     * @return true bila tersimpan; false bila stok tidak mencukupi.
     */
    suspend fun recordMutasiWithStock(mutasi: LogMutasi, delta: Int): Boolean
    suspend fun getPendingMutasi(): List<LogMutasi>
    suspend fun markSynced(idTransaksi: String)
}
