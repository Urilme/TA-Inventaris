package com.ta.konstruksi.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ta.konstruksi.util.FormInput
import com.ta.konstruksi.util.KategoriBarang
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Compose UI test untuk logika formulir Mutasi (F3) yang hidup di lapisan Composable —
 * TIDAK terjangkau unit test JVM biasa. Widget Compose asli dirender lalu diinteraksi
 * (ketik/klik) melalui kerangka pengujian Compose; logika transformasi/validasi memakai
 * fungsi PRODUKSI [FormInput] yang sama persis dipakai
 * [com.ta.konstruksi.presentation.ui.mutasi.MutasiScreen].
 *
 * Dijalankan di JVM lewat Robolectric (runtime Android tersimulasi, SDK 34) — bukan Espresso —
 * karena satu-satunya emulator yang tersedia (API 37, image pratinjau) belum kompatibel dengan
 * Espresso 3.6.1 (InputManager.getInstance dihapus di API baru). Kode uji identik dengan versi
 * instrumented dan dapat pula dijalankan pada perangkat/emulator API stabil.
 *
 * Menutup skenario Black-Box:
 * - No. 8  : Cascading Dropdown  — kondisi barang hanya muncul untuk kategori "alat".
 * - No. 13 : Validasi Input      — non-digit ditolak; jumlah 0 menonaktifkan SIMPAN.
 * - No. 29 : Batas Masukan Angka — kolom jumlah dibatasi 5 digit.
 * - No. 30 : Cegah Double-Submit — tombol SIMPAN nonaktif saat sedang menyimpan.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MutasiFormUiTest {

    @get:Rule
    val compose = createComposeRule()

    /** No. 29 + No. 13 (non-digit) — kolom jumlah hanya menerima digit, maksimal 5 angka. */
    @Test
    fun kolomJumlah_hanyaDigit_danMaksimalLimaAngka() {
        compose.setContent {
            var teks by remember { mutableStateOf("1") }
            BasicTextField(
                value = teks,
                onValueChange = { teks = FormInput.sanitizeJumlah(it) }, // fungsi produksi
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.testTag("jumlah")
            )
        }

        compose.onNodeWithTag("jumlah").performTextClearance()
        // Ketik campuran huruf + 9 digit — huruf harus dibuang, sisanya dipotong ke 5 digit.
        compose.onNodeWithTag("jumlah").performTextInput("12ab3456789")
        compose.onNodeWithTag("jumlah").assertTextEquals("12345")
    }

    /** No. 13 — tombol SIMPAN nonaktif saat jumlah 0, dan aktif kembali saat jumlah valid. */
    @Test
    fun tombolSimpan_nonaktifSaatJumlahNol() {
        compose.setContent {
            var jumlah by remember { mutableStateOf(0) }
            val canSave = FormInput.canSaveMutasi(
                hasItem = true, jumlah = jumlah, isOverStock = false,
                lokasiValid = true, penanggungjawabBlank = false, isLoading = false
            )
            Column {
                Box(Modifier.testTag("naikkan").clickable { jumlah = 3 }) { Text("+") }
                Box(Modifier.testTag("simpan").clickable(enabled = canSave) {}) { Text("SIMPAN") }
            }
        }

        compose.onNodeWithTag("simpan").assertIsNotEnabled() // jumlah 0 → tak bisa disimpan
        compose.onNodeWithTag("naikkan").performClick()       // jumlah jadi 3
        compose.onNodeWithTag("simpan").assertIsEnabled()     // valid → aktif
    }

    /** No. 30 — klik ganda pada SIMPAN saat menyimpan hanya memicu satu penyimpanan. */
    @Test
    fun tombolSimpan_cegahDoubleSubmit() {
        var submitCount = 0
        compose.setContent {
            var loading by remember { mutableStateOf(false) }
            val canSave = FormInput.canSaveMutasi(
                hasItem = true, jumlah = 3, isOverStock = false,
                lokasiValid = true, penanggungjawabBlank = false, isLoading = loading
            )
            Box(
                Modifier.testTag("simpan").clickable(enabled = canSave) {
                    submitCount++
                    loading = true // meniru transisi ke MutasiUiState.Loading di produksi
                }
            ) { Text(if (loading) "MENYIMPAN..." else "SIMPAN") }
        }

        val simpan = compose.onNodeWithTag("simpan")
        simpan.performClick() // klik pertama → menyimpan, tombol jadi nonaktif
        simpan.performClick() // klik kedua → diabaikan karena enabled=false
        compose.runOnIdle { assertEquals(1, submitCount) }
    }

    /** No. 8 — dropdown Kondisi Barang hanya muncul untuk kategori "alat". */
    @Test
    fun cascadingDropdown_kondisiHanyaUntukAlat() {
        compose.setContent {
            var kategori by remember { mutableStateOf(KategoriBarang.BAHAN) }
            Column {
                Box(Modifier.testTag("pilihAlat").clickable { kategori = KategoriBarang.ALAT }) {
                    Text("pilih alat")
                }
                if (FormInput.isKondisiRequired(kategori)) {
                    Text("Kondisi Barang", modifier = Modifier.testTag("kondisi"))
                }
            }
        }

        compose.onNodeWithTag("kondisi").assertDoesNotExist() // bahan → tidak ada
        compose.onNodeWithTag("pilihAlat").performClick()      // ganti ke alat
        compose.onNodeWithTag("kondisi").assertExists()        // alat → muncul
    }
}
