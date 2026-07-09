package com.ta.konstruksi.util

object Constants {
    const val DATABASE_NAME = "konstruksi_db"
    const val SYNC_WORK_TAG = "delta_sync_work"
    const val SYNC_INTERVAL_MINUTES = 15L

    // Domain sintetis untuk login berbasis username. Firebase Auth hanya punya provider
    // email/password, jadi username diubah jadi "<username>@skn.app" sebelum dikirim ke Firebase.
    // Mandor tidak punya email nyata (akun dibuat admin), domain ini disembunyikan dari user.
    // WAJIB identik dengan konvensi yang dipakai admin website saat membuat akun.
    const val AUTH_DOMAIN = "skn.app"

    // Fallback proyek bila sesi belum punya proyek (mis. dokumen users/{uid} belum
    // di-provision oleh admin/website). Proyek aktif yang sebenarnya disimpan di UserSession
    // (id_proyek/nama_proyek) dan mengikat katalog + mutasi ke proyek si mandor.
    const val DEFAULT_PROYEK = "PROYEK-UTAMA"
    const val DEFAULT_NAMA_PROYEK = "Proyek Belum Ditetapkan"

    // Firestore collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_KATALOG = "katalog"
    const val COLLECTION_MUTASI = "mutasi"
    const val COLLECTION_TRANSFER = "transfer"
    const val COLLECTION_PROYEK = "proyek"   // registry proyek (read-only, dikelola website admin)
}

/**
 * Tiga kategori barang tetap. Aturan bisnis (F3): hanya kategori [ALAT] yang
 * mewajibkan pengisian kondisi_barang; [BAHAN] & [SCAFFOLDING] selalu null.
 */
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
