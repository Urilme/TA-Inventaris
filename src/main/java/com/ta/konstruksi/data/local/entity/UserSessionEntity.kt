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
    val last_sync_timestamp: Long = 0L,
    // Proyek yang ditugaskan ke mandor ini (di-provision dari Firestore users/{uid} oleh
    // admin/website). Mengikat katalog & mutasi ke proyek. defaultValue harus cocok MIGRATION_3_4.
    @ColumnInfo(defaultValue = "") val id_proyek: String = "",
    @ColumnInfo(defaultValue = "") val nama_proyek: String = ""
)
