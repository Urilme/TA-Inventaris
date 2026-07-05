package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.LogMutasiDao
import com.ta.konstruksi.domain.model.LogMutasi
import com.ta.konstruksi.domain.repository.MutasiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MutasiRepositoryImpl @Inject constructor(
    private val mutasiDao: LogMutasiDao
) : MutasiRepository {

    override fun getAllMutasi(): Flow<List<LogMutasi>> =
        mutasiDao.getAllMutasi().map { list -> list.map { it.toDomain() } }

    // Insert log + update stok dalam satu transaksi atomik (mencegah race condition stok minus)
    override suspend fun recordMutasiWithStock(mutasi: LogMutasi, delta: Int): Boolean =
        mutasiDao.recordMutasiAtomic(mutasi.toEntity(), delta)

    override suspend fun getPendingMutasi(): List<LogMutasi> =
        mutasiDao.getPending().map { it.toDomain() }

    override suspend fun markSynced(idTransaksi: String) =
        mutasiDao.markSynced(idTransaksi)
}
