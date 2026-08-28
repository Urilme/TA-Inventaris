package com.ta.konstruksi.util

object Constants {
    const val DATABASE_NAME = "konstruksi_db"
    const val SYNC_WORK_TAG = "delta_sync_work"
    const val SYNC_INTERVAL_MINUTES = 15L

    // Domain sintetis untuk login berbasis username: "<username>@skn.app".
    const val AUTH_DOMAIN = "skn.app"

    // Fallback proyek bila sesi belum punya proyek.
    const val DEFAULT_PROYEK = "PROYEK-UTAMA"
    const val DEFAULT_NAMA_PROYEK = "Proyek Belum Ditetapkan"

    // Firestore collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_KATALOG = "katalog"
    const val COLLECTION_MUTASI = "mutasi"
    const val COLLECTION_TRANSFER = "transfer"
    const val COLLECTION_PROYEK = "proyek"   // registry proyek (read-only)
}

/** Ubah username jadi email sintetis untuk Firebase Auth. Normalisasi: trim + lowercase. */
fun usernameToEmail(username: String): String = "${username.trim().lowercase()}@${Constants.AUTH_DOMAIN}"

object KategoriBarang {
    const val ALAT = "alat"
    const val BAHAN = "bahan"
    const val SCAFFOLDING = "scaffolding"

    val ALL = listOf(ALAT, BAHAN, SCAFFOLDING)

    /** Label tampilan untuk nilai kategori kanonik. */
    fun label(value: String): String = when (value) {
        ALAT -> "Alat"
        BAHAN -> "Bahan"
        SCAFFOLDING -> "Scaffolding"
        else -> value.replaceFirstChar { it.uppercase() }
    }
}
