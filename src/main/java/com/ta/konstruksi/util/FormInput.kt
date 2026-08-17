package com.ta.konstruksi.util

/**
 * Logika validasi & sanitasi input formulir Mutasi (F3) yang murni (tanpa efek samping),
 * diekstrak dari [com.ta.konstruksi.presentation.ui.mutasi.MutasiScreen] agar dapat diuji
 * langsung lewat Compose UI test / unit test tanpa merender seluruh layar berbasis Hilt.
 *
 * Menutup skenario Black-Box:
 * - No. 8  : kondisi barang hanya relevan untuk kategori "alat" (cascading dropdown).
 * - No. 13 : validasi input (non-digit ditolak, jumlah 0 menonaktifkan simpan).
 * - No. 29 : batas masukan angka maksimal [MAX_JUMLAH_DIGITS] digit.
 * - No. 30 : pencegahan double-submit (tombol nonaktif saat sedang menyimpan).
 */
object FormInput {

    /** Batas maksimum digit untuk kolom jumlah barang (mencegah overflow & salah ketik). */
    const val MAX_JUMLAH_DIGITS = 5

    /**
     * Menyaring masukan kolom jumlah: buang karakter non-digit dan potong ke
     * [MAX_JUMLAH_DIGITS] angka. Dipakai pada `onValueChange` kolom jumlah.
     */
    fun sanitizeJumlah(raw: String): String =
        raw.filter { it.isDigit() }.take(MAX_JUMLAH_DIGITS)

    /**
     * Kondisi barang WAJIB (dan dropdown-nya ditampilkan) hanya untuk kategori "alat".
     * Untuk "bahan"/"scaffolding" kondisi tidak relevan → null. Sesuai aturan F3.
     */
    fun isKondisiRequired(kategori: String?): Boolean = kategori == KategoriBarang.ALAT

    /**
     * Menentukan boleh-tidaknya tombol SIMPAN ditekan. Saat `isLoading` bernilai true
     * (mutasi sedang disimpan) tombol nonaktif — inilah guard pencegah double-submit (No.30).
     */
    fun canSaveMutasi(
        hasItem: Boolean,
        jumlah: Int,
        isOverStock: Boolean,
        lokasiValid: Boolean,
        penanggungjawabBlank: Boolean,
        isLoading: Boolean
    ): Boolean =
        hasItem && jumlah > 0 && !isOverStock && lokasiValid && !penanggungjawabBlank && !isLoading
}
