package com.ta.konstruksi.presentation.ui.mutasi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.presentation.ui.component.NetworkBadge
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderInput
import com.ta.konstruksi.presentation.ui.theme.GreenDark
import com.ta.konstruksi.presentation.ui.theme.GreenGlow
import com.ta.konstruksi.presentation.ui.theme.InfoBg
import com.ta.konstruksi.presentation.ui.theme.InfoBorder
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextPrimary
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.KatalogViewModel
import com.ta.konstruksi.presentation.viewmodel.MutasiUiState
import com.ta.konstruksi.presentation.viewmodel.MutasiViewModel
import com.ta.konstruksi.util.JenisMutasi
import kotlinx.coroutines.delay

@Composable
fun MutasiScreen(
    onBack: () -> Unit,
    isOnline: Boolean = false,
    mutasiViewModel: MutasiViewModel = hiltViewModel(),
    katalogViewModel: KatalogViewModel = hiltViewModel()
) {
    val uiState by mutasiViewModel.uiState.collectAsStateWithLifecycle()
    val katalogList by katalogViewModel.katalogList.collectAsStateWithLifecycle()

    var jenis by remember { mutableStateOf(JenisMutasi.CHECK_IN) }
    val kategoriList = remember(katalogList) { katalogList.map { it.kategori }.distinct().sorted() }
    var selectedKategori by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<MasterKatalog?>(null) }
    var jumlah by remember { mutableIntStateOf(1) }
    var catatan by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }

    val filteredItems = katalogList.filter { selectedKategori.isEmpty() || it.kategori == selectedKategori }

    LaunchedEffect(uiState) {
        if (uiState is MutasiUiState.Success) {
            showSnackbar = true
            delay(2000)
            showSnackbar = false
            mutasiViewModel.reset()
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Navy Header
            Row(
                modifier = Modifier.fillMaxWidth().background(Navy)
                    .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Text("Catat Mutasi", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
                NetworkBadge(isOnline = isOnline)
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp)
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
                        value = selectedKategori.ifEmpty { "Pilih Kategori" },
                        isPlaceholder = selectedKategori.isEmpty(),
                        onClick = { kategoriExpanded = true }
                    )
                    DropdownMenu(expanded = kategoriExpanded, onDismissRequest = { kategoriExpanded = false }) {
                        kategoriList.forEach { kat ->
                            DropdownMenuItem(text = { Text(kat) }, onClick = {
                                selectedKategori = kat; selectedItem = null; jumlah = 1; kategoriExpanded = false
                            })
                        }
                        if (kategoriList.isEmpty()) {
                            DropdownMenuItem(text = { Text("Belum ada kategori", color = TextSubtle) }, onClick = {})
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Barang
                FormLabel("Barang", subtitle = "(terfilter sesuai kategori)")
                Spacer(Modifier.height(8.dp))
                var barangExpanded by remember { mutableStateOf(false) }
                Box {
                    DropdownField(
                        value = selectedItem?.nama_barang ?: "Pilih Barang",
                        isPlaceholder = selectedItem == null,
                        isSelected = selectedItem != null,
                        onClick = { barangExpanded = true }
                    )
                    DropdownMenu(expanded = barangExpanded, onDismissRequest = { barangExpanded = false }) {
                        filteredItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.nama_barang} (${item.stok_lokal} ${item.satuan})") },
                                onClick = { selectedItem = item; jumlah = 1; barangExpanded = false }
                            )
                        }
                        if (filteredItems.isEmpty()) {
                            DropdownMenuItem(text = { Text("Belum ada barang", color = TextSubtle) }, onClick = {})
                        }
                    }
                }

                // Stok info
                selectedItem?.let { item ->
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(InfoBg).border(1.5.dp, InfoBorder, RoundedCornerShape(12.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory, null, tint = Navy, modifier = Modifier.size(26.dp))
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

                // Jumlah stepper
                FormLabel("Jumlah Barang")
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, BorderInput, RoundedCornerShape(12.dp)).background(Color.White)
                            .clickable { if (jumlah > 1) jumlah-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, null, tint = Navy, modifier = Modifier.size(26.dp))
                    }
                    Box(
                        modifier = Modifier.weight(1f).height(58.dp).padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(12.dp)).border(1.5.dp, Navy, RoundedCornerShape(12.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(jumlah.toString(), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Navy)
                    }
                    Box(
                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, BorderInput, RoundedCornerShape(12.dp)).background(Color.White)
                            .clickable {
                                val max = if (jenis == JenisMutasi.CHECK_OUT) selectedItem?.stok_lokal ?: 9999 else 9999
                                if (jumlah < max) jumlah++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Navy, modifier = Modifier.size(26.dp))
                    }
                }

                selectedItem?.let { item ->
                    if (jenis == JenisMutasi.CHECK_OUT) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = TextSubtle, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Maksimal ${item.stok_lokal} ${item.satuan} — tidak boleh melebihi stok lokal.", fontSize = 12.sp, color = TextSubtle)
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
                        Icon(Icons.Default.Info, null, tint = RedMain, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text((uiState as MutasiUiState.Error).message, fontSize = 13.sp, color = RedMain)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // SIMPAN button
                val isOverStock = jenis == JenisMutasi.CHECK_OUT &&
                    (selectedItem?.let { jumlah > it.stok_lokal } ?: false)
                val canSave = selectedItem != null && jumlah > 0 && !isOverStock &&
                    uiState !is MutasiUiState.Loading
                Box(
                    modifier = Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (canSave) Navy else Color(0xFFB0BEC5))
                        .clickable(enabled = canSave) {
                            selectedItem?.let { item ->
                                mutasiViewModel.recordMutasi(item.sku_id, jenis, jumlah, catatan.ifBlank { null })
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("SIMPAN DATA LOKAL", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color.White)
                    }
                }
            }
        }

        // Snackbar sukses
        AnimatedVisibility(
            visible = showSnackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            enter = fadeIn() + slideInVertically { it }, exit = fadeOut()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(GreenDark).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = GreenGlow, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Data Tersimpan Secara Luring", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Akan disinkronkan otomatis saat daring.", fontSize = 12.sp, color = Color(0xFFa9d8bd))
                }
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
        Icon(Icons.Default.ExpandMore, null, tint = TextSubtle, modifier = Modifier.size(24.dp))
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
        Icon(icon, null, tint = if (selected) Color.White else TextSubtle, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else TextSubtle)
    }
}
