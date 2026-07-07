package com.ta.konstruksi.presentation.ui.katalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ta.konstruksi.presentation.ui.component.AppDropdownItem
import com.ta.konstruksi.presentation.ui.component.AppDropdownMenu
import com.ta.konstruksi.presentation.ui.component.NetworkBadge
import com.ta.konstruksi.presentation.ui.theme.AmberBg
import com.ta.konstruksi.presentation.ui.theme.AmberBorder
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderInput
import com.ta.konstruksi.presentation.ui.theme.InfoBg
import com.ta.konstruksi.presentation.ui.theme.InfoBorder
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedBg
import com.ta.konstruksi.presentation.ui.theme.RedBorder
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextPrimary
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.KatalogUiState
import com.ta.konstruksi.presentation.viewmodel.KatalogViewModel
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.UuidGenerator

// Tiga kategori kanonik (alat/bahan/scaffolding). Nilai disimpan kanonik, ditampilkan sbg label.
private val kategoriOptions = KategoriBarang.ALL
private val satuanOptions = listOf("Sak", "Batang", "Pcs", "Dus", "Liter", "Meter", "M²", "M³", "Buah", "Kg", "Ton")

@Composable
fun TambahKatalogScreen(
    onBack: () -> Unit,
    isOnline: Boolean = false,
    viewModel: KatalogViewModel = hiltViewModel()
) {
    var kategori by remember { mutableStateOf("") }
    var namaBarang by remember { mutableStateOf("") }
    var satuan by remember { mutableStateOf("") }
    var stokMinimumText by remember { mutableStateOf("") }
    val stokMinimum = stokMinimumText.toIntOrNull() ?: 0
    val generatedSku = remember { UuidGenerator.generateSkuId() }
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Tunggu hasil simpan: kembali hanya jika sukses, tampilkan error bila gagal
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is KatalogUiState.Success -> {
                viewModel.resetUiState()
                onBack()
            }
            is KatalogUiState.Error -> snackbarHostState.showSnackbar(s.message)
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().background(Navy).statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Text("Tambah ke Katalog", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
                NetworkBadge(isOnline = isOnline)
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp)) {
                // Warning banner
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(RedBg).border(1.5.dp, RedBorder, RoundedCornerShape(14.dp)).padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Warning, null, tint = RedMain, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Perhatian. Hanya gunakan form ini jika barang belum ada di katalog. Cek riwayat terlebih dahulu untuk hindari duplikat.",
                        fontSize = 13.sp, lineHeight = 20.sp, color = Color(0xFFA02519)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Kategori
                Text("Kategori", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                var kategoriExpanded by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                            .border(1.5.dp, if (kategori.isNotEmpty()) Navy else BorderInput, RoundedCornerShape(12.dp))
                            .clickable { kategoriExpanded = true }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (kategori.isEmpty()) "Pilih Kategori" else KategoriBarang.label(kategori),
                            fontSize = 16.sp, fontWeight = FontWeight.Medium,
                            color = if (kategori.isEmpty()) TextSubtle else Navy, modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ExpandMore, null, tint = TextSubtle, modifier = Modifier.size(24.dp))
                    }
                    AppDropdownMenu(expanded = kategoriExpanded, onDismissRequest = { kategoriExpanded = false }) {
                        kategoriOptions.forEach { opt ->
                            AppDropdownItem(text = KategoriBarang.label(opt), selected = opt == kategori, onClick = { kategori = opt; kategoriExpanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Nama Barang
                Text("Nama Barang Baru", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = namaBarang, onValueChange = { namaBarang = it },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy, unfocusedBorderColor = BorderInput,
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                    ),
                    placeholder = { Text("Contoh: Semen Tiga Roda 40kg", color = TextSubtle) },
                    singleLine = true
                )

                Spacer(Modifier.height(18.dp))

                // Satuan
                Text("Satuan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                var satuanExpanded by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                            .border(1.5.dp, if (satuan.isNotEmpty()) Navy else BorderInput, RoundedCornerShape(12.dp))
                            .clickable { satuanExpanded = true }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            satuan.ifEmpty { "Pilih Satuan" },
                            fontSize = 16.sp, fontWeight = FontWeight.Medium,
                            color = if (satuan.isEmpty()) TextSubtle else Navy, modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ExpandMore, null, tint = TextSubtle, modifier = Modifier.size(24.dp))
                    }
                    AppDropdownMenu(expanded = satuanExpanded, onDismissRequest = { satuanExpanded = false }) {
                        satuanOptions.forEach { opt ->
                            AppDropdownItem(text = opt, selected = opt == satuan, onClick = { satuan = opt; satuanExpanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Stok Minimum (ambang peringatan Dashboard)
                Text("Stok Minimum", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Dashboard menampilkan peringatan saat stok menyentuh/di bawah angka ini. Kosongkan bila tidak perlu.",
                    fontSize = 12.sp, color = TextSubtle
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = stokMinimumText,
                    onValueChange = { raw -> stokMinimumText = raw.filter { it.isDigit() }.take(6) },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy, unfocusedBorderColor = BorderInput,
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                    ),
                    placeholder = { Text("mis. 20 (opsional)", color = TextSubtle) },
                    singleLine = true
                )

                Spacer(Modifier.height(18.dp))

                // Info auto
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(InfoBg).border(1.5.dp, InfoBorder, RoundedCornerShape(12.dp)).padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory, null, tint = Navy, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Stok awal diinisialisasi ", fontSize = 13.sp, color = TextPrimary)
                        Text("0 ${satuan.ifEmpty { "unit" }}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
                    }
                }

                Spacer(Modifier.height(26.dp))

                // TAMBAH button
                val isSaving = uiState is KatalogUiState.Loading
                val canSave = kategori.isNotEmpty() && namaBarang.isNotBlank() && satuan.isNotEmpty() && !isSaving
                Box(
                    modifier = Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (canSave || isSaving) Navy else Color(0xFFB0BEC5))
                        .clickable(enabled = canSave) {
                            viewModel.tambahKatalog(kategori, namaBarang, satuan, 0, stokMinimum, generatedSku)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddBox, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("TAMBAH KE KATALOG", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}
