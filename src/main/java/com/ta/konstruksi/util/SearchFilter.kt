package com.ta.konstruksi.util

/**
 * Predikat pencarian in-memory untuk daftar Katalog & Riwayat, diekstrak dari Composable
 * (KatalogScreen / RiwayatScreen) agar dapat diuji langsung.
 *
 * Penting untuk keamanan: penyaringan dilakukan atas daftar yang SUDAH dimuat ke memori memakai
 * [String.contains] (bukan kueri SQL `LIKE`), sehingga karakter spesial/SQL diperlakukan sebagai
 * teks literal — injeksi SQL secara struktural tidak mungkin (Black-Box No. 27). Menutup pula
 * skenario filter pencarian reaktif (No. 19) dan pencarian fiktif → hasil kosong (No. 26).
 */
object SearchFilter {

    /** Cocok bila query kosong, atau termuat pada nama barang / kategori (tanpa peka huruf). */
    fun matchesKatalog(namaBarang: String, kategori: String, query: String): Boolean =
        query.isBlank() ||
            namaBarang.contains(query, ignoreCase = true) ||
            kategori.contains(query, ignoreCase = true)

    /** Cocok bila query kosong, atau termuat pada label (nama barang) / sku (tanpa peka huruf). */
    fun matchesRiwayat(label: String, skuId: String, query: String): Boolean =
        query.isBlank() ||
            label.contains(query, ignoreCase = true) ||
            skuId.contains(query, ignoreCase = true)
}
