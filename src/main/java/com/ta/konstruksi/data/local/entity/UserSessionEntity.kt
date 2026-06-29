package com.ta.konstruksi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey val id_mandor: String,
    val nama: String,
    val email: String,
    val role: String,
    val auth_token: String,
    val last_sync_timestamp: Long = 0L
)
