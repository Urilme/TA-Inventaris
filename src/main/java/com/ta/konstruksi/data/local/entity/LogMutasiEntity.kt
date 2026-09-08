package com.ta.konstruksi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "log_mutasi",
    foreignKeys = [
        ForeignKey(
            entity = MasterKatalogEntity::class,
            parentColumns = ["sku_id"],
            childColumns = ["sku_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        // FK ke Proyek (v10): riwayat mutasi tidak boleh "yatim" dari proyek tempatnya terjadi.
        // Lihat ProyekDao.replaceAll — proyek disegarkan via upsert, bukan hapus-semua, supaya
        // RESTRICT ini tidak menggagalkan sinkronisasi registry proyek.
        ForeignKey(
            entity = ProyekEntity::class,
            parentColumns = ["id_proyek"],
            childColumns = ["id_proyek"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("id_proyek")]
)
data class LogMutasiEntity(
    @PrimaryKey val id_transaksi: String,
    val sku_id: String,
    @ColumnInfo(defaultValue = "''") val id_mandor: String = "",        // pencatat
    @ColumnInfo(defaultValue = "''") val id_proyek: String = "",        // proyek tempat mutasi terjadi
    val jenis_mutasi: String,              // "CHECK_IN" | "CHECK_OUT"
    val jumlah: Int,
    // Hanya diisi untuk kategori "alat"; bahan & scaffolding = null.
    val kondisi_barang: String? = null,    // KondisiBarang enum name atau null
    @ColumnInfo(defaultValue = "''") val lokasi_penggunaan: String = "",      // area spesifik dalam proyek
    @ColumnInfo(defaultValue = "''") val penanggungjawab: String = "",        // nama mandor pemegang aset
    val catatan: String? = null,
    val local_timestamp: Long = System.currentTimeMillis(),
    val sync_status: String = "PENDING"
)
