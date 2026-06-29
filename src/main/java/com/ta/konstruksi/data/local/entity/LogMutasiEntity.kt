package com.ta.konstruksi.data.local.entity

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
    val jenis_mutasi: String,              // "CHECK_IN" | "CHECK_OUT"
    val jumlah: Int,
    val catatan: String? = null,
    val local_timestamp: Long = System.currentTimeMillis(),
    val sync_status: String = "PENDING"
)
