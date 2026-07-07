package com.ta.konstruksi.presentation.ui.transfer

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.navigation.NavController
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.presentation.navigation.Screen
import com.ta.konstruksi.presentation.ui.component.AppBottomBar
import com.ta.konstruksi.presentation.ui.component.AppDropdownItem
import com.ta.konstruksi.presentation.ui.component.AppDropdownMenu
import com.ta.konstruksi.presentation.ui.component.NetworkBadge
import com.ta.konstruksi.presentation.ui.theme.AmberBg
import com.ta.konstruksi.presentation.ui.theme.AmberBorder
import com.ta.konstruksi.presentation.ui.theme.AmberMain
import com.ta.konstruksi.presentation.ui.theme.AmberText
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderInput
import com.ta.konstruksi.presentation.ui.theme.InfoBg
import com.ta.konstruksi.presentation.ui.theme.InfoBorder
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.TextPrimary
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.TransferUiState
import com.ta.konstruksi.presentation.viewmodel.TransferViewModel

@Composable
fun TransferScreen(
    onBack: () -> Unit,
    navController: NavController,
    isOnline: Boolean = false,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val katalogList by viewModel.katalogList.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val proyekOptions by viewModel.proyekList.collectAsStateWithLifecycle()
    // Proyek asal = proyek aktif mandor dari sesi (bukan placeholder statis)
    val proyekAsal = session?.nama_proyek?.takeIf { it.isNotBlank() } ?: "Proyek Anda"
    val snackbarHostState = remember { SnackbarHostState() }

    val kategoriList = remember(katalogList) { katalogList.map { it.kategori }.distinct().sorted() }
    var selectedKategori by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<MasterKatalog?>(null) }
    var proyekTujuan by remember { mutableStateOf("") }
    var jumlah by remember { mutableIntStateOf(1) }
    val filteredItems = katalogList.filter { selectedKategori.isEmpty() || it.kategori == selectedKategori }

    LaunchedEffect(uiState) {
        if (uiState is TransferUiState.Success) {
            snackbarHostState.showSnackbar("Transfer berhasil dibuat — IN_TRANSIT")
            viewModel.reset()
            onBack()
        }
        if (uiState is TransferUiState.Error) {
            snackbarHostState.showSnackbar((uiState as TransferUiState.Error).message)
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
                Text("Transfer Aset", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
                NetworkBadge(isOnline = isOnline)
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 118.dp)) {
                // Kategori
                FormLabel("Kategori")
                Spacer(Modifier.height(8.dp))
                var kategoriExpanded by remember { mutableStateOf(false) }
                Box {
                    DropdownField(value = selectedKategori.ifEmpty { "Pilih Kategori" }, isPlaceholder = selectedKategori.isEmpty(), onClick = { kategoriExpanded = true })
                    AppDropdownMenu(expanded = kategoriExpanded, onDismissRequest = { kategoriExpanded = false }) {
                        kategoriList.forEach { kat ->
                            AppDropdownItem(text = kat, selected = kat == selectedKategori, onClick = {
                                selectedKategori = kat; selectedItem = null; kategoriExpanded = false
                            })
                        }
                        if (kategoriList.isEmpty()) {
                            AppDropdownItem(text = "Belum ada kategori", enabled = false, onClick = {})
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Nama Barang
                FormLabel("Nama Barang")
                Spacer(Modifier.height(8.dp))
                var barangExpanded by remember { mutableStateOf(false) }
                Box {
                    DropdownField(
                        value = selectedItem?.nama_barang ?: "Pilih Barang",
                        isPlaceholder = selectedItem == null,
                        isSelected = selectedItem != null,
                        onClick = { barangExpanded = true }
                    )
                    AppDropdownMenu(expanded = barangExpanded, onDismissRequest = { barangExpanded = false }) {
                        filteredItems.forEach { item ->
                            AppDropdownItem(
                                text = "${item.nama_barang} (${item.stok_lokal} ${item.satuan})",
                                selected = selectedItem?.sku_id == item.sku_id,
                                onClick = { selectedItem = item; jumlah = 1; barangExpanded = false }
                            )
                        }
                        if (filteredItems.isEmpty()) {
                            AppDropdownItem(text = "Belum ada barang", enabled = false, onClick = {})
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
                            Text("Stok di Proyek Ini", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                            Text(proyekAsal, fontSize = 11.sp, color = TextSubtle)
                        }
                        Text("${item.stok_lokal} ${item.satuan}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Navy)
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Proyek Tujuan — DIPILIH dari registry proyek (bukan diketik) agar bebas typo.
                // Daftar berasal dari koleksi Firestore `proyek` (dikelola website admin), di-cache lokal.
                FormLabel("Proyek Tujuan")
                Spacer(Modifier.height(8.dp))
                val opsiTujuan = proyekOptions.filter { it.id_proyek != session?.id_proyek }
                var proyekExpanded by remember { mutableStateOf(false) }
                Box {
                    DropdownField(
                        value = proyekTujuan.ifEmpty { "Pilih Proyek Tujuan" },
                        isPlaceholder = proyekTujuan.isEmpty(),
                        isSelected = proyekTujuan.isNotEmpty(),
                        onClick = { proyekExpanded = true }
                    )
                    AppDropdownMenu(expanded = proyekExpanded, onDismissRequest = { proyekExpanded = false }) {
                        opsiTujuan.forEach { proyek ->
                            AppDropdownItem(
                                text = proyek.nama_proyek,
                                selected = proyek.nama_proyek == proyekTujuan,
                                onClick = { proyekTujuan = proyek.nama_proyek; proyekExpanded = false }
                            )
                        }
                        if (opsiTujuan.isEmpty()) {
                            AppDropdownItem(text = "Belum ada proyek lain — sinkronkan saat online", enabled = false, onClick = {})
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Jumlah Dikirim
                FormLabel("Jumlah Dikirim")
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
                                val max = selectedItem?.stok_lokal ?: 9999
                                if (jumlah < max) jumlah++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Navy, modifier = Modifier.size(26.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // IN_TRANSIT info
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(AmberBg).border(1.5.dp, AmberBorder, RoundedCornerShape(12.dp)).padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocalShipping, null, tint = AmberMain, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Aset akan ditandai DALAM PERJALANAN (IN_TRANSIT) hingga dikonfirmasi diterima proyek tujuan.",
                        fontSize = 12.sp, lineHeight = 18.sp, color = AmberText
                    )
                }

                Spacer(Modifier.height(24.dp))

                // KIRIM button
                val withinStock = selectedItem?.let { jumlah in 1..it.stok_lokal } ?: false
                val tujuanValid = proyekTujuan.isNotBlank() &&
                    !proyekTujuan.trim().equals(proyekAsal, ignoreCase = true)
                val canSend = selectedItem != null && tujuanValid && withinStock &&
                    uiState !is TransferUiState.Loading
                Column(
                    modifier = Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (canSend) Navy else Color(0xFFB0BEC5))
                        .clickable(enabled = canSend) {
                            selectedItem?.let { item ->
                                viewModel.createTransfer(item.sku_id, proyekTujuan.trim(), jumlah, proyekAsal)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("KIRIM ASET", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color.White)
                    }
                    Text("DALAM PERJALANAN", fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, color = Color(0xFF9fb0bf))
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 88.dp))

        // Navbar bawah bersama
        AppBottomBar(
            navController = navController,
            currentRoute = Screen.Transfer.route,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FormLabel(label: String) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
