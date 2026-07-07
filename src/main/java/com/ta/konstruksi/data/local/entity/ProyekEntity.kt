package com.ta.konstruksi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache lokal registry proyek (cermin dari koleksi Firestore `proyek`).
 * Read-only: hanya di-refresh dari server saat sync, tidak pernah ditulis dari perangkat.
 * Tidak punya sync_status karena bukan data yang diunggah mandor.
 */
@Entity(tableName = "proyek")
data class ProyekEntity(
    @PrimaryKey val id_proyek: String,
    val nama_proyek: String
)
