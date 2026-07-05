package com.ta.konstruksi.util

object Constants {
    const val DATABASE_NAME = "konstruksi_db"
    const val SYNC_WORK_TAG = "delta_sync_work"
    const val SYNC_INTERVAL_MINUTES = 15L

    // Proyek default untuk sesi mandor (auto-fill id_proyek pada LogMutasi).
    // Sesi login saat ini belum menyimpan proyek aktif, jadi mutasi diikat ke proyek ini.
    const val DEFAULT_PROYEK = "PROYEK-UTAMA"

    // Firestore collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_KATALOG = "katalog"
    const val COLLECTION_MUTASI = "mutasi"
    const val COLLECTION_TRANSFER = "transfer"
}

/**
 * Tiga kategori barang tetap. Aturan bisnis (F3): hanya kategori [ALAT] yang
 * mewajibkan pengisian kondisi_barang; [BAHAN] & [SCAFFOLDING] selalu null.
 */
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
