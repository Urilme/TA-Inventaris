package com.ta.konstruksi.domain.model

import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.SyncStatus

data class LogMutasi(
    val id_transaksi: String,
    val sku_id: String,
    val jenis_mutasi: JenisMutasi,
    val jumlah: Int,
    val catatan: String? = null,
    val local_timestamp: Long = System.currentTimeMillis(),
    val sync_status: SyncStatus = SyncStatus.PENDING
)
