package com.ta.konstruksi.presentation.ui.mutasi

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.presentation.ui.component.AppDropdownItem
import com.ta.konstruksi.presentation.ui.component.AppDropdownMenu
import com.ta.konstruksi.presentation.ui.component.BarangPickerDialog
import com.ta.konstruksi.presentation.ui.component.NetworkBadge
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderInput
import com.ta.konstruksi.presentation.ui.theme.GreenBg
import com.ta.konstruksi.presentation.ui.theme.GreenMain
import com.ta.konstruksi.presentation.ui.theme.InfoBg
import com.ta.konstruksi.presentation.ui.theme.InfoBorder
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedBg
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextPrimary
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.KatalogViewModel
import com.ta.konstruksi.presentation.viewmodel.MutasiUiState
import com.ta.konstruksi.presentation.viewmodel.MutasiViewModel
import com.ta.konstruksi.util.FormInput
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.KondisiBarang

@Composable
fun MutasiScreen(
    onBack: () -> Unit,
    isOnline: Boolean = false,
    initialJenis: JenisMutasi = JenisMutasi.CHECK_IN,
    mutasiViewModel: MutasiViewModel = hiltViewModel(),
    katalogViewModel: KatalogViewModel = hiltViewModel()
) {
    val uiState by mutasiViewModel.uiState.collectAsStateWithLifecycle()
    val katalogList by katalogViewModel.katalogList.collectAsStateWithLifecycle()
    val session by mutasiViewModel.session.collectAsStateWithLifecycle()

    var jenis by remember { mutableStateOf(initialJenis) }
    val kategoriList = remember(katalogList) { katalogList.map { it.kategori }.distinct().sorted() }
    var selectedKategori by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<MasterKatalog?>(null) }
    // Jumlah disimpan sebagai teks agar bisa diketik langsung (bukan hanya stepper)
    var jumlahText by remember { mutableStateOf("1") }
    val jumlah = jumlahText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    var kondisi by remember { mutableStateOf(KondisiBarang.BAIK) }
    var lokasiPenggunaan by remember { mutableStateOf("") }
    var penanggungjawab by remember { mutableStateOf("") }
    var penanggungjawabTouched by remember { mutableStateOf(false) }
    var pjEditable by remember { mutableStateOf(false) }
    var catatan by remember { mutableStateOf("") }

    var showBarangPicker by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    // Pre-fill penanggung jawab dengan nama mandor dari sesi (bila belum diubah user)
    LaunchedEffect(session) {
        if (!penanggungjawabTouched && penanggungjawab.isBlank()) {
            session?.nama?.let { penanggungjawab = it }
        }
    }

    val filteredItems = katalogList.filter { selectedKategori.isEmpty() || it.kategori == selectedKategori }
    // Kondisi barang hanya relevan (dan wajib) untuk kategori "alat" (aturan F3)
    val isAlat = FormInput.isKondisiRequired(selectedItem?.kategori)

    LaunchedEffect(uiState) {
        if (uiState is MutasiUiState.Success) showSuccess = true
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Navy Header
            Row(
                modifier = Modifier.fillMaxWidth().background(Navy).statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Text("Catat Mutasi", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
                NetworkBadge(isOnline = isOnline)
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .navigationBarsPadding().imePadding().padding(18.dp)
            ) {
                // Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFe4e8eb)).padding(5.dp)
                ) {
                    JenisToggle(
                        modifier = Modifier.weight(1f), label = "Barang Masuk",
                        icon = Icons.AutoMirrored.Filled.Login,
                        selected = jenis == JenisMutasi.CHECK_IN,
                        onClick = { jenis = JenisMutasi.CHECK_IN }
                    )
                    JenisToggle(
                        modifier = Modifier.weight(1f), label = "Barang Keluar",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        selected = jenis == JenisMutasi.CHECK_OUT,
                        onClick = { jenis = JenisMutasi.CHECK_OUT }
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Kategori
                FormLabel("Kategori")
                Spacer(Modifier.height(8.dp))
                var kategoriExpanded by remember { mutableStateOf(false) }
                Box {
                    DropdownField(
                        value = if (selectedKategori.isEmpty()) "Pilih Kategori" else KategoriBarang.label(selectedKategori),
                        isPlaceholder = selectedKategori.isEmpty(),
                        onClick = { kategoriExpanded = true }
                    )
                    AppDropdownMenu(expanded = kategoriExpanded, onDismissRequest = { kategoriExpanded = false }) {
                        kategoriList.forEach { kat ->
                            AppDropdownItem(text = KategoriBarang.label(kat), selected = kat == selectedKategori, onClick = {
                                selectedKategori = kat; selectedItem = null; jumlahText = "1"; kategoriExpanded = false
                            })
                        }
                        if (kategoriList.isEmpty()) {
                            AppDropdownItem(text = "Belum ada kategori", enabled = false, onClick = {})
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Barang — pemilih dengan pencarian
                FormLabel("Barang", subtitle = "(cari & pilih)")
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    value = selectedItem?.nama_barang ?: "Pilih Barang",
                    isPlaceholder = selectedItem == null,
                    isSelected = selectedItem != null,
                    onClick = { showBarangPicker = true }
                )

                // Stok info
                selectedItem?.let { item ->
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(InfoBg).border(1.5.dp, InfoBorder, RoundedCornerShape(12.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, tint = Navy, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Sisa Stok Lokal Saat Ini", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                        }
                        Text(
                            "${item.stok_lokal} ${item.satuan}", fontSize = 30.sp, fontWeight = FontWeight.Bold,
                            color = if (item.stok_lokal <= 5) RedMain else Navy
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Jumlah — bisa diketik langsung + stepper
                FormLabel("Jumlah Barang", subtitle = "(ketik atau gunakan tombol)")
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, BorderInput, RoundedCornerShape(12.dp)).background(Color.White)
                            .clickable { if (jumlah > 1) jumlahText = (jumlah - 1).toString() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurangi jumlah", tint = Navy, modifier = Modifier.size(26.dp))
                    }
                    val jumlahOverStock = jenis == JenisMutasi.CHECK_OUT &&
                        (selectedItem?.let { jumlah > it.stok_lokal } ?: false)
                    Box(
                        modifier = Modifier.weight(1f).height(58.dp).padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, if (jumlahOverStock) RedMain else Navy, RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = jumlahText,
                            onValueChange = { raw -> jumlahText = FormInput.sanitizeJumlah(raw) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Navy, textAlign = TextAlign.Center
                            ),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                                    if (jumlahText.isEmpty()) Text("0", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                                    inner()
                                }
                            }
                        )
                    }
                    Box(
                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, BorderInput, RoundedCornerShape(12.dp)).background(Color.White)
                            .clickable {
                                val max = if (jenis == JenisMutasi.CHECK_OUT) selectedItem?.stok_lokal ?: 99999 else 99999
                                if (jumlah < max) jumlahText = (jumlah + 1).toString()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah jumlah", tint = Navy, modifier = Modifier.size(26.dp))
                    }
                }

                selectedItem?.let { item ->
                    if (jenis == JenisMutasi.CHECK_OUT) {
                        Spacer(Modifier.height(8.dp))
                        val isOver = jumlah > item.stok_lokal
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isOver) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isOver) RedMain else TextSubtle,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isOver)
                                    "Jumlah melebihi stok — maksimal ${item.stok_lokal} ${item.satuan}."
                                else
                                    "Maksimal ${item.stok_lokal} ${item.satuan} — tidak boleh melebihi stok lokal.",
                                fontSize = 12.sp,
                                fontWeight = if (isOver) FontWeight.Bold else FontWeight.Normal,
                                color = if (isOver) RedMain else TextSubtle
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Kondisi Barang — HANYA untuk kategori "alat" (bahan & scaffolding tidak relevan)
                if (isAlat) {
                    FormLabel("Kondisi Barang", subtitle = "(wajib untuk alat)")
                    Spacer(Modifier.height(8.dp))
                    var kondisiExpanded by remember { mutableStateOf(false) }
                    Box {
                        DropdownField(
                            value = kondisi.label(),
                            isPlaceholder = false,
                            isSelected = true,
                            onClick = { kondisiExpanded = true }
                        )
                        AppDropdownMenu(expanded = kondisiExpanded, onDismissRequest = { kondisiExpanded = false }) {
                            KondisiBarang.entries.forEach { k ->
                                AppDropdownItem(text = k.label(), selected = k == kondisi, onClick = {
                                    kondisi = k; kondisiExpanded = false
                                })
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                }

                // Lokasi Penggunaan — hanya relevan untuk Barang Keluar (CHECK_OUT).
                // Barang Masuk masuk ke stok gudang proyek, belum dipakai di area mana pun,
                // sehingga lokasi penggunaan tidak berlaku dan field disembunyikan.
                if (jenis == JenisMutasi.CHECK_OUT) {
                    FormLabel("Lokasi Penggunaan")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lokasiPenggunaan, onValueChange = { lokasiPenggunaan = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Navy, unfocusedBorderColor = BorderInput,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                        ),
                        placeholder = { Text("mis. Lantai 3 - Area Cor", color = TextSubtle) }
                    )

                    Spacer(Modifier.height(18.dp))
                }

                // Penanggung Jawab (wajib) — read-only default, bisa diubah
                FormLabel("Penanggung Jawab")
                Spacer(Modifier.height(8.dp))
                if (pjEditable) {
                    OutlinedTextField(
                        value = penanggungjawab,
                        onValueChange = { penanggungjawab = it; penanggungjawabTouched = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Navy, unfocusedBorderColor = BorderInput,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                        ),
                        placeholder = { Text("Nama mandor pemegang aset", color = TextSubtle) }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color.White).border(1.5.dp, BorderInput, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            penanggungjawab.ifBlank { "Belum diisi" },
                            fontSize = 16.sp, fontWeight = FontWeight.Medium,
                            color = if (penanggungjawab.isBlank()) TextSubtle else Navy,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable { pjEditable = true }.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Ubah penanggung jawab", tint = Navy, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ubah", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Catatan
                FormLabel("Catatan", subtitle = "(opsional)")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = catatan, onValueChange = { catatan = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy, unfocusedBorderColor = BorderInput,
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                    ),
                    placeholder = { Text("Tambahkan keterangan...", color = TextSubtle) },
                    minLines = 2, maxLines = 3
                )

                Spacer(Modifier.height(26.dp))

                // Error
                if (uiState is MutasiUiState.Error) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFBE9E7)).border(1.dp, Color(0xFFf0c4bd), RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = RedMain, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text((uiState as MutasiUiState.Error).message, fontSize = 13.sp, color = RedMain)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // SIMPAN button
                val isOverStock = jenis == JenisMutasi.CHECK_OUT &&
                    (selectedItem?.let { jumlah > it.stok_lokal } ?: false)
                // Lokasi hanya wajib untuk Barang Keluar; Barang Masuk tidak memerlukannya.
                val lokasiValid = jenis == JenisMutasi.CHECK_IN || lokasiPenggunaan.isNotBlank()
                val canSave = FormInput.canSaveMutasi(
                    hasItem = selectedItem != null,
                    jumlah = jumlah,
                    isOverStock = isOverStock,
                    lokasiValid = lokasiValid,
                    penanggungjawabBlank = penanggungjawab.isBlank(),
                    isLoading = uiState is MutasiUiState.Loading
                )
                val isSaving = uiState is MutasiUiState.Loading
                Box(
                    modifier = Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (canSave || isSaving) Navy else Color(0xFFB0BEC5))
                        .clickable(enabled = canSave) { showConfirm = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("SIMPAN DATA LOKAL", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Dialog pemilih barang dengan pencarian
    if (showBarangPicker) {
        BarangPickerDialog(
            items = filteredItems,
            onDismiss = { showBarangPicker = false },
            onSelect = { item ->
                selectedItem = item
                jumlahText = "1"
                showBarangPicker = false
            }
        )
    }

    // Dialog konfirmasi sebelum menyimpan
    val itemToConfirm = selectedItem
    if (showConfirm && itemToConfirm != null) {
        MutasiConfirmDialog(
            jenis = jenis,
            item = itemToConfirm,
            jumlah = jumlah,
            kondisi = if (isAlat) kondisi else null,
            lokasi = if (jenis == JenisMutasi.CHECK_OUT) lokasiPenggunaan else "",
            penanggungjawab = penanggungjawab,
            onDismiss = { showConfirm = false },
            onConfirm = {
                showConfirm = false
                mutasiViewModel.recordMutasi(
                    skuId = itemToConfirm.sku_id,
                    jenis = jenis,
                    jumlah = jumlah,
                    kondisi = if (isAlat) kondisi else null,
                    lokasiPenggunaan = if (jenis == JenisMutasi.CHECK_OUT) lokasiPenggunaan else "",
                    penanggungjawab = penanggungjawab,
                    catatan = catatan.ifBlank { null }
                )
            }
        )
    }

    // Dialog sukses — animasi centang, hanya opsi Selesai
    if (showSuccess) {
        MutasiSuccessDialog(
            onDone = {
                showSuccess = false
                mutasiViewModel.reset()
                onBack()
            }
        )
    }
}

private fun KondisiBarang.label(): String = when (this) {
    KondisiBarang.BAIK -> "Baik"
    KondisiBarang.RUSAK_RINGAN -> "Rusak Ringan"
    KondisiBarang.RUSAK_BERAT -> "Rusak Berat"
}

/**
 * Dialog konfirmasi mutasi yang interaktif: ikon jenis, kartu barang + jumlah,
 * baris detail berikon, dan perubahan stok (sebelum → sesudah).
 */
@Composable
private fun MutasiConfirmDialog(
    jenis: JenisMutasi,
    item: MasterKatalog,
    jumlah: Int,
    kondisi: KondisiBarang?,
    lokasi: String,
    penanggungjawab: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isOut = jenis == JenisMutasi.CHECK_OUT
    val accent = if (isOut) RedMain else GreenMain
    val accentBg = if (isOut) RedBg else GreenBg
    val sisa = if (isOut) item.stok_lokal - jumlah else item.stok_lokal + jumlah

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(22.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isOut) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                        contentDescription = null, tint = accent, modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Konfirmasi Mutasi", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy)
                    Text(
                        if (isOut) "Barang Keluar" else "Barang Masuk",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Kartu barang + jumlah
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(InfoBg).border(1.5.dp, InfoBorder, RoundedCornerShape(14.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, tint = Navy, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.nama_barang, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text(KategoriBarang.label(item.kategori), fontSize = 12.sp, color = TextSubtle)
                }
                Text(
                    "${if (isOut) "−" else "+"}$jumlah",
                    fontSize = 26.sp, fontWeight = FontWeight.Black, color = accent
                )
                Text(" ${item.satuan}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent)
            }

            Spacer(Modifier.height(14.dp))

            // Detail berikon
            if (kondisi != null) ConfirmDetailRow(Icons.Default.Check, "Kondisi", kondisi.label())
            if (lokasi.isNotBlank()) ConfirmDetailRow(Icons.Default.Place, "Lokasi", lokasi)
            ConfirmDetailRow(Icons.Default.Person, "Penanggung Jawab", penanggungjawab)

            Spacer(Modifier.height(14.dp))

            // Perubahan stok
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(accentBg).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sisa stok", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${item.stok_lokal}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                    Text("  →  ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                    Text("$sisa ${item.satuan}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = accent)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Tombol
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, BorderInput, RoundedCornerShape(14.dp)).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Batal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp))
                        .background(Navy).clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ya, Simpan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = TextSubtle, modifier = Modifier.width(140.dp))
        Text(
            value.ifBlank { "-" },
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Dialog sukses dengan animasi centang yang memantul (spring) — hanya opsi Selesai.
 */
@Composable
private fun MutasiSuccessDialog(onDone: () -> Unit) {
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animate = true }
    val scale by animateFloatAsState(
        targetValue = if (animate) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "checkScale"
    )

    Dialog(onDismissRequest = onDone) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(96.dp).scale(scale).clip(CircleShape).background(GreenBg),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(GreenMain),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Berhasil", tint = Color.White, modifier = Modifier.size(38.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Data Tersimpan", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Navy)
            Spacer(Modifier.height(8.dp))
            Text(
                "Mutasi tersimpan secara offline dan akan disinkronkan otomatis saat online.",
                fontSize = 13.sp, color = TextSubtle, textAlign = TextAlign.Center, lineHeight = 19.sp
            )
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp))
                    .background(Navy).clickable(onClick = onDone),
                contentAlignment = Alignment.Center
            ) {
                Text("SELESAI", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun FormLabel(label: String, subtitle: String = "") {
    Row {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Text(subtitle, fontSize = 13.sp, color = TextSubtle)
        }
    }
}

@Composable
private fun DropdownField(value: String, isPlaceholder: Boolean, isSelected: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(1.5.dp, if (isSelected) Navy else BorderInput, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            value, fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isPlaceholder) TextSubtle else Navy,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun JenisToggle(
    modifier: Modifier, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean, onClick: () -> Unit
) {
    Row(
        modifier = modifier.height(46.dp).clip(RoundedCornerShape(10.dp))
            .background(if (selected) Navy else Color.Transparent).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color.White else TextSubtle, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else TextSubtle)
    }
}
