package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.MasterKatalogDao
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.domain.repository.KatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class KatalogRepositoryImpl @Inject constructor(
    private val katalogDao: MasterKatalogDao
) : KatalogRepository {

    override fun getAllKatalog(): Flow<List<MasterKatalog>> =
        katalogDao.getAllKatalog().map { list -> list.map { it.toDomain() } }

    override suspend fun getKatalogBySku(skuId: String): MasterKatalog? =
        katalogDao.getKatalogBySku(skuId)?.toDomain()

    override suspend fun upsertKatalog(item: MasterKatalog) =
        katalogDao.upsert(item.toEntity())

    override suspend fun deleteKatalog(skuId: String) =
        katalogDao.delete(skuId)

    override suspend fun updateStokLokal(skuId: String, delta: Int) =
        katalogDao.updateStok(skuId, delta)
}
