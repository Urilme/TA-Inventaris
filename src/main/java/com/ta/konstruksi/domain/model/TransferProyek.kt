package com.ta.konstruksi.domain.model

import com.ta.konstruksi.util.StatusTransfer
import com.ta.konstruksi.util.SyncStatus

data class TransferProyek(
    val id_transfer: String,
    val sku_id: String,
    // Snapshot identitas barang saat transfer dibuat — agar penerima bisa menampilkan nama &
    // menambahkannya ke katalog proyeknya sendiri tanpa perlu katalog proyek asal.
    val nama_barang: String = "",
    val kategori: String = "",
    val satuan: String = "",
    val proyek_asal: String,
    val proyek_tujuan: String,
    val jumlah: Int,
    val status_transfer: StatusTransfer = StatusTransfer.IN_TRANSIT,
    val local_timestamp: Long = System.currentTimeMillis(),
    val sync_status: SyncStatus = SyncStatus.PENDING
)
