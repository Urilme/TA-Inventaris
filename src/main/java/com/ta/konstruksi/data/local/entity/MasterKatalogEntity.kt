package com.ta.konstruksi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_katalog")
data class MasterKatalogEntity(
    @PrimaryKey val sku_id: String,
    val kategori: String,
    val nama_barang: String,
    val satuan: String,
    val stok_lokal: Int,
    // Ambang peringatan stok minimum di Dashboard.
    @ColumnInfo(defaultValue = "0") val stok_minimum: Int = 0,
    // Proyek pemilik katalog ini (katalog difilter per proyek).
    @ColumnInfo(defaultValue = "") val id_proyek: String = "",
    val sync_status: String = "PENDING",   // stored as String, mapped to enum in repo
    val local_timestamp: Long = System.currentTimeMillis()
)
