package com.ta.konstruksi.domain.repository

import com.ta.konstruksi.domain.model.MasterKatalog
import kotlinx.coroutines.flow.Flow

interface KatalogRepository {
    fun getAllKatalog(): Flow<List<MasterKatalog>>
    suspend fun getKatalogBySku(skuId: String): MasterKatalog?
    suspend fun findByProyekAndNama(idProyek: String, nama: String): MasterKatalog?
    suspend fun upsertKatalog(item: MasterKatalog)
    suspend fun deleteKatalog(skuId: String)

    /** true bila barang sudah punya riwayat mutasi — hapus katalog dilarang (audit trail). */
    suspend fun hasRiwayatMutasi(skuId: String): Boolean
    suspend fun updateStokLokal(skuId: String, delta: Int)

    /** Pengurangan stok terjaga (atomik) — false bila stok tidak mencukupi. Dipakai jalur transfer. */
    suspend fun decrementStokLokalJikaCukup(skuId: String, jumlah: Int): Boolean
}
