package com.ta.konstruksi.domain.model

import com.ta.konstruksi.util.SyncStatus

data class MasterKatalog(
    val sku_id: String,
    val kategori: String,
    val nama_barang: String,
    val satuan: String,
    val stok_lokal: Int,
    val sync_status: SyncStatus = SyncStatus.PENDING,
    val local_timestamp: Long = System.currentTimeMillis()
)
