package com.ta.konstruksi.domain.model

data class UserSession(
    val id_mandor: String,
    val nama: String,
    val email: String,
    val role: String,
    val auth_token: String,
    val last_sync_timestamp: Long = 0L
)
