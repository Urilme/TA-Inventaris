package com.ta.konstruksi.domain.model

data class UserSession(
    val id_mandor: String,
    val nama: String,
    val email: String,
    val role: String,
    val auth_token: String,
    val last_sync_timestamp: Long = 0L,
    val id_proyek: String = "",       // proyek aktif si mandor (dari provisioning admin/website)
    val nama_proyek: String = ""      // nama tampilan proyek (mis. "Proyek Tol Trans-Sumatera")
)
