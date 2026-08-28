package com.ta.konstruksi.util

/**
 * Predikat pencarian in-memory untuk daftar Katalog & Riwayat, dipisah dari Composable
 * agar mudah diuji. Menyaring atas daftar yang sudah dimuat memakai [String.contains]
 * (bukan kueri SQL), sehingga karakter spesial diperlakukan sebagai teks literal.
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
