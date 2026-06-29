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

    // Hanya mencatat log; pembaruan stok dilakukan di RecordMutasiUseCase agar tidak ganda
    override suspend fun recordMutasi(mutasi: LogMutasi) =
        mutasiDao.insert(mutasi.toEntity())

    override suspend fun getPendingMutasi(): List<LogMutasi> =
        mutasiDao.getPending().map { it.toDomain() }

    override suspend fun markSynced(idTransaksi: String) =
        mutasiDao.markSynced(idTransaksi)
}
