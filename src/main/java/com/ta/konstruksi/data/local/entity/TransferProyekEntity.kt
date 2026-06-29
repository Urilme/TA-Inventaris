package com.ta.konstruksi.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "transfer_proyek",
    foreignKeys = [ForeignKey(
        entity = MasterKatalogEntity::class,
        parentColumns = ["sku_id"],
        childColumns = ["sku_id"],
        onDelete = ForeignKey.RESTRICT
    )]
)
data class TransferProyekEntity(
    @PrimaryKey val id_transfer: String,
    val sku_id: String,
    val proyek_asal: String,
    val proyek_tujuan: String,
    val jumlah: Int,
    val status_transfer: String = "IN_TRANSIT",
    val local_timestamp: Long = System.currentTimeMillis(),
    val sync_status: String = "PENDING"
)
