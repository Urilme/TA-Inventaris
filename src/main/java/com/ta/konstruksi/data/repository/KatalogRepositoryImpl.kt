package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.MasterKatalogDao
import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class KatalogRepositoryImpl @Inject constructor(
    private val katalogDao: MasterKatalogDao,
    private val userSessionDao: UserSessionDao
) : KatalogRepository {

    /** Proyek aktif si mandor; jatuh ke fallback bila sesi belum di-provision. */
    private suspend fun proyekAktif(): String =
        userSessionDao.getSession()?.id_proyek?.takeIf { it.isNotBlank() } ?: Constants.DEFAULT_PROYEK

    // Katalog disaring ke proyek aktif — mandor hanya melihat barang di proyeknya sendiri.
    override fun getAllKatalog(): Flow<List<MasterKatalog>> =
        flow { emit(proyekAktif()) }
            .flatMapLatest { proyek ->
                katalogDao.getAllKatalog(proyek).map { list -> list.map { it.toDomain() } }
            }

    override suspend fun getKatalogBySku(skuId: String): MasterKatalog? =
        katalogDao.getKatalogBySku(skuId)?.toDomain()

    override suspend fun findByProyekAndNama(idProyek: String, nama: String): MasterKatalog? =
        katalogDao.findByProyekAndNama(idProyek, nama)?.toDomain()

    // Barang baru distempel proyek aktif agar muncul di katalog proyek ini.
    override suspend fun upsertKatalog(item: MasterKatalog) {
        val stamped = if (item.id_proyek.isBlank()) item.copy(id_proyek = proyekAktif()) else item
        katalogDao.upsert(stamped.toEntity())
    }

    override suspend fun deleteKatalog(skuId: String) =
        katalogDao.delete(skuId)

    override suspend fun updateStokLokal(skuId: String, delta: Int) =
        katalogDao.updateStok(skuId, delta)

    override suspend fun decrementStokLokalJikaCukup(skuId: String, jumlah: Int): Boolean =
        katalogDao.decrementStokJikaCukup(skuId, jumlah)
}
