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
