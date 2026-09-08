package com.ta.konstruksi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "master_katalog",
    // FK ke Proyek (v10): katalog yang masih ada tidak boleh "yatim" dari proyek pemiliknya.
    // Proyek adalah cache read-only yang disegarkan via upsert (ProyekDao.replaceAll), bukan
    // hapus-semua-lalu-isi-ulang, supaya RESTRICT ini tidak menggagalkan sinkronisasi.
    foreignKeys = [ForeignKey(
        entity = ProyekEntity::class,
        parentColumns = ["id_proyek"],
        childColumns = ["id_proyek"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("id_proyek")]
)
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
