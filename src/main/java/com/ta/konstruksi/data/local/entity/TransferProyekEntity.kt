package com.ta.konstruksi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Tanpa FK ke master_katalog: transfer bersifat self-describing (bawa snapshot barang) sehingga
// bisa ditarik & ditampilkan di perangkat penerima yang tidak punya katalog sku asal.
@Entity(tableName = "transfer_proyek")
data class TransferProyekEntity(
    @PrimaryKey val id_transfer: String,
    val sku_id: String,
    // Snapshot barang agar detail transfer tetap tampil di sisi penerima.
    @ColumnInfo(defaultValue = "") val nama_barang: String = "",
    @ColumnInfo(defaultValue = "") val kategori: String = "",
    @ColumnInfo(defaultValue = "") val satuan: String = "",
    val proyek_asal: String,
    val proyek_tujuan: String,
    val jumlah: Int,
    val status_transfer: String = "IN_TRANSIT",
    val local_timestamp: Long = System.currentTimeMillis(),
    val sync_status: String = "PENDING"
)
