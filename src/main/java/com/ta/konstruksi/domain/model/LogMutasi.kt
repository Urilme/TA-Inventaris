package com.ta.konstruksi.domain.model

import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.KondisiBarang
import com.ta.konstruksi.util.SyncStatus

data class LogMutasi(
    val id_transaksi: String,
    val sku_id: String,
    val id_mandor: String,                 // FK ke UserSession — siapa yang mencatat
    val id_proyek: String,                 // proyek tempat mutasi terjadi
    val jenis_mutasi: JenisMutasi,
    val jumlah: Int,
    val kondisi_barang: KondisiBarang = KondisiBarang.BAIK, // kondisi fisik aset saat transaksi
    val lokasi_penggunaan: String,         // area spesifik dalam proyek
    val penanggungjawab: String,           // nama mandor pemegang aset
    val catatan: String? = null,
    val local_timestamp: Long = System.currentTimeMillis(),
    val sync_status: SyncStatus = SyncStatus.PENDING
)
