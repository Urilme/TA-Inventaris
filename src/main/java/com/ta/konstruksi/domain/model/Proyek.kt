package com.ta.konstruksi.domain.model

/**
 * Proyek dalam registry (dikelola website admin, read-only di aplikasi mobile).
 * Dipakai sebagai daftar pilihan proyek tujuan pada Transfer agar nama proyek
 * konsisten & bebas typo (tidak diketik manual oleh mandor).
 */
data class Proyek(
    val id_proyek: String,
    val nama_proyek: String
)
