package com.ta.konstruksi.presentation.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.presentation.ui.theme.KatAlat
import com.ta.konstruksi.presentation.ui.theme.KatBahan
import com.ta.konstruksi.presentation.ui.theme.KatScaffolding
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.util.KategoriBarang

/**
 * Bahasa visual kategori & kesehatan stok — dipakai konsisten di Katalog dan Dashboard
 * agar sebuah barang tampil dengan warna, ikon, dan status stok yang sama di mana pun.
 */

enum class StokHealth { KRITIS, RENDAH, AMAN }

/**
 * Kesehatan stok berbasis `stok_minimum`:
 * - tak ada ambang (`<= 0`) → AMAN
 * - stok menyentuh/di bawah ambang → KRITIS
 * - stok masih dalam 1.5× ambang → RENDAH (peringatan dini)
 * - selebihnya → AMAN
 */
fun stokHealth(item: MasterKatalog): StokHealth = when {
    item.stok_minimum <= 0 -> StokHealth.AMAN
    item.stok_lokal <= item.stok_minimum -> StokHealth.KRITIS
    item.stok_lokal <= item.stok_minimum * 1.5 -> StokHealth.RENDAH
    else -> StokHealth.AMAN
}

fun kategoriColor(kategori: String): Color = when (kategori) {
    KategoriBarang.ALAT -> KatAlat
    KategoriBarang.BAHAN -> KatBahan
    KategoriBarang.SCAFFOLDING -> KatScaffolding
    else -> Navy
}

fun kategoriIcon(kategori: String): ImageVector = when (kategori) {
    KategoriBarang.ALAT -> Icons.Default.Build
    KategoriBarang.BAHAN -> Icons.Default.Grain
    KategoriBarang.SCAFFOLDING -> Icons.Default.GridView
    else -> Icons.Default.Inventory
}
