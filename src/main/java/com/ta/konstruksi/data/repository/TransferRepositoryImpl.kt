package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.TransferProyekDao
import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.domain.repository.HasilBuatTransfer
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.util.StatusTransfer
import com.ta.konstruksi.util.UuidGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class TransferRepositoryImpl @Inject constructor(
    private val transferDao: TransferProyekDao,
    private val userSessionDao: UserSessionDao
) : TransferRepository {

    /** Nama proyek aktif — dipakai memfilter transfer (dokumen menyimpan NAMA, bukan id). */
    private suspend fun namaProyekAktif(): String =
        userSessionDao.getSession()?.nama_proyek.orEmpty()

    override fun getAllTransfer(): Flow<List<TransferProyek>> =
        transferDao.getAllTransfer().map { list -> list.map { it.toDomain() } }

    override fun getRiwayatTransfer(limit: Int): Flow<List<TransferProyek>> =
        flow { emit(namaProyekAktif()) }.flatMapLatest { nama ->
            transferDao.getRiwayatTransfer(nama, limit).map { list -> list.map { it.toDomain() } }
        }

    override suspend fun createTransfer(transfer: TransferProyek): HasilBuatTransfer =
        transferDao.buatTransferAtomik(transfer.toEntity())

    override suspend fun updateStatusTransfer(idTransfer: String, status: StatusTransfer) =
        transferDao.updateStatus(idTransfer, status.name, System.currentTimeMillis())

    override suspend fun confirmReceiveAtomic(idTransfer: String, idProyekPenerima: String): Boolean =
        transferDao.konfirmasiTerimaAtomik(
            idTransfer, idProyekPenerima, System.currentTimeMillis(), UuidGenerator.generateSkuId()
        )

    override suspend fun cancelAtomic(idTransfer: String): Boolean =
        transferDao.batalkanAtomik(idTransfer, System.currentTimeMillis())

    override suspend fun getPendingTransfer(): List<TransferProyek> =
        transferDao.getPending().map { it.toDomain() }

    override suspend fun markSynced(idTransfer: String) =
        transferDao.markSynced(idTransfer)

    override suspend fun getIncomingInTransit(namaProyek: String): List<TransferProyek> =
        transferDao.getIncomingInTransit(namaProyek).map { it.toDomain() }
}
