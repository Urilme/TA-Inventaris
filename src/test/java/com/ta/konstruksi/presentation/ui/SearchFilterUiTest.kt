package com.ta.konstruksi.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ta.konstruksi.util.SearchFilter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Compose UI test untuk pencarian katalog/riwayat pada lapisan antarmuka. Kolom pencarian
 * Compose asli dirender lalu diketik; penyaringan memakai predikat PRODUKSI
 * [SearchFilter.matchesKatalog] yang sama dipakai KatalogScreen/RiwayatScreen. Dijalankan di
 * JVM lewat Robolectric (SDK 34).
 *
 * Menutup skenario Black-Box:
 * - No. 19 : Filter Pencarian        — daftar tersaring reaktif sesuai kata kunci.
 * - No. 26 : Pencarian Katalog Fiktif — kata kunci tak dikenal → daftar kosong.
 * - No. 27 : Injeksi Karakter Pencarian — payload SQL diperlakukan literal (in-memory
 *            String.contains, BUKAN kueri SQL) sehingga aman, tanpa force close.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SearchFilterUiTest {

    @get:Rule
    val compose = createComposeRule()

    private data class Item(val nama: String, val kategori: String)

    private val katalog = listOf(
        Item("Semen Gresik", "bahan"),
        Item("Paku Beton", "bahan"),
        Item("Bor Listrik", "alat")
    )

    @Composable
    private fun LayarPencarian() {
        var query by remember { mutableStateOf("") }
        val filtered = katalog.filter { SearchFilter.matchesKatalog(it.nama, it.kategori, query) }
        Column {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.testTag("cari")
            )
            if (filtered.isEmpty()) {
                Text("Tidak ada barang yang cocok", modifier = Modifier.testTag("kosong"))
            }
            filtered.forEach { Text(it.nama, modifier = Modifier.testTag("item_${it.nama}")) }
        }
    }

    /** No. 19 — mengetik kata kunci menyaring daftar secara reaktif. */
    @Test
    fun no19_filterPencarian_menyaringDaftarSecaraReaktif() {
        compose.setContent { LayarPencarian() }

        compose.onNodeWithTag("cari").performTextInput("semen")
        compose.onNodeWithTag("item_Semen Gresik").assertExists()
        compose.onNodeWithTag("item_Paku Beton").assertDoesNotExist()
        compose.onNodeWithTag("item_Bor Listrik").assertDoesNotExist()
    }

    /** No. 26 — kata kunci yang tak ada di katalog menghasilkan daftar kosong. */
    @Test
    fun no26_pencarianFiktif_menghasilkanDaftarKosong() {
        compose.setContent { LayarPencarian() }

        compose.onNodeWithTag("cari").performTextInput("barangtidakada123")
        compose.onNodeWithTag("kosong").assertExists()
        compose.onNodeWithTag("item_Semen Gresik").assertDoesNotExist()
    }

    /** No. 27 — payload SQL diperlakukan sebagai teks literal (in-memory), aman tanpa force close. */
    @Test
    fun no27_injeksiKarakterPencarian_ditanganiAmanTanpaCrash() {
        compose.setContent { LayarPencarian() }

        compose.onNodeWithTag("cari").performTextInput("'; DROP TABLE master_katalog;--")
        // Tidak ada baris yang memuat string literal tersebut → daftar kosong, aplikasi tetap hidup.
        compose.onNodeWithTag("kosong").assertExists()
        compose.onNodeWithTag("item_Semen Gresik").assertDoesNotExist()
    }
}
