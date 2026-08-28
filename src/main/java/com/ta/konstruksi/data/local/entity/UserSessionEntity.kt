package com.ta.konstruksi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey val id_mandor: String,
    val nama: String,
    val username: String,
    val role: String,
    val auth_token: String,
    // Kursor delta per koleksi (server_timestamp terbesar yang telah dikonsumsi).
    // last_sync_timestamp dipakai untuk katalog sekaligus tampilan "terakhir sinkron".
    val last_sync_timestamp: Long = 0L,
    @ColumnInfo(defaultValue = "0") val last_sync_mutasi: Long = 0L,
    @ColumnInfo(defaultValue = "0") val last_sync_transfer: Long = 0L,
    // Proyek yang ditugaskan ke mandor ini (dari Firestore users/{uid}).
    @ColumnInfo(defaultValue = "") val id_proyek: String = "",
    @ColumnInfo(defaultValue = "") val nama_proyek: String = ""
)
