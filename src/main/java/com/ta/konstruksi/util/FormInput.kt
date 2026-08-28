package com.ta.konstruksi.util

/**
 * Logika validasi & sanitasi input formulir mutasi (murni, tanpa efek samping),
 * dipisah dari layar agar mudah diuji.
 */
object FormInput {

    /** Batas maksimum digit untuk kolom jumlah barang. */
    const val MAX_JUMLAH_DIGITS = 5

    /** Buang karakter non-digit dan potong ke [MAX_JUMLAH_DIGITS] angka. */
    fun sanitizeJumlah(raw: String): String =
        raw.filter { it.isDigit() }.take(MAX_JUMLAH_DIGITS)

    /** Kondisi barang hanya wajib untuk kategori "alat". */
    fun isKondisiRequired(kategori: String?): Boolean = kategori == KategoriBarang.ALAT

    /** Boleh-tidaknya tombol simpan ditekan; nonaktif saat sedang menyimpan. */
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
