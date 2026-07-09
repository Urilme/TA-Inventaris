package com.ta.konstruksi.domain.repository

import com.ta.konstruksi.domain.model.MasterKatalog
import kotlinx.coroutines.flow.Flow

interface KatalogRepository {
    fun getAllKatalog(): Flow<List<MasterKatalog>>
    suspend fun getKatalogBySku(skuId: String): MasterKatalog?
    suspend fun findByProyekAndNama(idProyek: String, nama: String): MasterKatalog?
    suspend fun upsertKatalog(item: MasterKatalog)
    suspend fun deleteKatalog(skuId: String)
    suspend fun updateStokLokal(skuId: String, delta: Int)
}
