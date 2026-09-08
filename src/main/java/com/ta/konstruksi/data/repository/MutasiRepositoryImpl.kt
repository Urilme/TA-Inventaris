package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.LogMutasiDao
import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.domain.model.LogMutasi
import com.ta.konstruksi.domain.repository.MutasiRepository
import com.ta.konstruksi.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class MutasiRepositoryImpl @Inject constructor(
    private val mutasiDao: LogMutasiDao,
    private val userSessionDao: UserSessionDao
) : MutasiRepository {

    /** Proyek aktif si mandor; jatuh ke fallback bila sesi belum di-provision. */
    private suspend fun proyekAktif(): String =
        userSessionDao.getSession()?.id_proyek?.takeIf { it.isNotBlank() } ?: Constants.DEFAULT_PROYEK

    override fun getAllMutasi(): Flow<List<LogMutasi>> =
        mutasiDao.getAllMutasi().map { list -> list.map { it.toDomain() } }

    override fun getRiwayatMutasi(limit: Int): Flow<List<LogMutasi>> =
        flow { emit(proyekAktif()) }.flatMapLatest { proyek ->
            mutasiDao.getRiwayatMutasi(proyek, limit).map { list -> list.map { it.toDomain() } }
        }

    // Insert log + update stok dalam satu transaksi atomik (mencegah race condition stok minus)
    override suspend fun recordMutasiWithStock(mutasi: LogMutasi, delta: Int): Boolean =
        mutasiDao.recordMutasiAtomic(mutasi.toEntity(), delta)

    override suspend fun getPendingMutasi(): List<LogMutasi> =
        mutasiDao.getPending().map { it.toDomain() }

    override suspend fun markSynced(idTransaksi: String) =
        mutasiDao.markSynced(idTransaksi)
}
