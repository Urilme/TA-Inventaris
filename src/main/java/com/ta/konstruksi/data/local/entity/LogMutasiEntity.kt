package com.ta.konstruksi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "log_mutasi",
    foreignKeys = [ForeignKey(
        entity = MasterKatalogEntity::class,
        parentColumns = ["sku_id"],
        childColumns = ["sku_id"],
        onDelete = ForeignKey.RESTRICT
    )]
)
data class LogMutasiEntity(
    @PrimaryKey val id_transaksi: String,
    val sku_id: String,
    // defaultValue wajib cocok dengan DEFAULT pada MIGRATION_1_2 agar validasi skema Room lolos
    @ColumnInfo(defaultValue = "''") val id_mandor: String = "",        // FK ke UserSession — pencatat
    @ColumnInfo(defaultValue = "''") val id_proyek: String = "",        // proyek tempat mutasi terjadi
    val jenis_mutasi: String,              // "CHECK_IN" | "CHECK_OUT"
    val jumlah: Int,
    @ColumnInfo(defaultValue = "'BAIK'") val kondisi_barang: String = "BAIK", // KondisiBarang enum name
    @ColumnInfo(defaultValue = "''") val lokasi_penggunaan: String = "",      // area spesifik dalam proyek
    @ColumnInfo(defaultValue = "''") val penanggungjawab: String = "",        // nama mandor pemegang aset
    val catatan: String? = null,
    val local_timestamp: Long = System.currentTimeMillis(),
    val sync_status: String = "PENDING"
)
