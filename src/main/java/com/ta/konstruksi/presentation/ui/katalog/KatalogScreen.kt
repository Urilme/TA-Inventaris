package com.ta.konstruksi.presentation.ui.katalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.presentation.navigation.Screen
import com.ta.konstruksi.presentation.ui.component.AppBottomBar
import com.ta.konstruksi.presentation.ui.component.ListSkeleton
import com.ta.konstruksi.presentation.ui.component.StokHealth
import com.ta.konstruksi.presentation.ui.component.kategoriColor
import com.ta.konstruksi.presentation.ui.component.kategoriIcon
import com.ta.konstruksi.presentation.ui.component.stokHealth
import com.ta.konstruksi.presentation.ui.theme.AmberBg
import com.ta.konstruksi.presentation.ui.theme.AmberBorder
import com.ta.konstruksi.presentation.ui.theme.AmberMain
import com.ta.konstruksi.presentation.ui.theme.AmberText
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderLight
import com.ta.konstruksi.presentation.ui.theme.GreenBg
import com.ta.konstruksi.presentation.ui.theme.GreenMain
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedDark
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.KatalogViewModel
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.SyncStatus

@Composable
fun KatalogScreen(
    navController: NavController,
    viewModel: KatalogViewModel = hiltViewModel()
) {
    val katalogList by viewModel.katalogList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedKategori by remember { mutableStateOf<String?>(null) } // null = Semua

    val filtered = katalogList.filter {
        (selectedKategori == null || it.kategori == selectedKategori) &&
            (searchQuery.isBlank() || it.nama_barang.contains(searchQuery, true) ||
                it.kategori.contains(searchQuery, true))
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.fillMaxWidth().background(Navy).statusBarsPadding().padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                    Text("Katalog Aset", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                // Search
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color.White).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = TextSubtle, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Navy),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("Cari nama atau kategori…", fontSize = 15.sp, color = TextSubtle)
                            inner()
                        }
                    )
                }

                // Filter kategori
                Spacer(Modifier.height(11.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CategoryChip("Semua", active = selectedKategori == null, dotColor = null) { selectedKategori = null }
                    KategoriBarang.ALL.forEach { kat ->
                        CategoryChip(
                            label = KategoriBarang.label(kat),
                            active = selectedKategori == kat,
                            dotColor = kategoriColor(kat)
                        ) { selectedKategori = if (selectedKategori == kat) null else kat }
                    }
                }

                // Ringkasan jumlah aset per kategori
                if (!isLoading && katalogList.isNotEmpty()) {
                    val nAlat = katalogList.count { it.kategori == KategoriBarang.ALAT }
                    val nBahan = katalogList.count { it.kategori == KategoriBarang.BAHAN }
                    val nScaff = katalogList.count { it.kategori == KategoriBarang.SCAFFOLDING }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "${katalogList.size} aset · $nAlat Alat · $nBahan Bahan · $nScaff Scaffolding",
                        fontSize = 11.sp, color = Color(0x99FFFFFF), fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isLoading) {
                ListSkeleton(modifier = Modifier.weight(1f).fillMaxWidth())
            } else if (filtered.isEmpty()) {
                val isFiltering = searchQuery.isNotBlank() || selectedKategori != null
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
                        Icon(Icons.Default.Inventory, null, tint = Color(0xFFd0d8de), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (isFiltering) "Tidak ada hasil" else "Katalog kosong",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFa7b2bc)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (isFiltering) "Tidak ada barang yang cocok dengan filter saat ini."
                            else "Tekan tombol + untuk menambahkan barang baru ke katalog.",
                            fontSize = 13.sp, color = TextSubtle, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { item ->
                        KatalogItemCard(
                            item = item,
                            onClick = { navController.navigate(Screen.DetailBarang.createRoute(item.sku_id)) }
                        )
                    }
                    item { Spacer(Modifier.height(118.dp)) }
                }
            }
        }

        // FAB tambah barang — diangkat di atas navbar
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 18.dp, bottom = 88.dp).size(64.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                .background(Navy).clickable { navController.navigate(Screen.TambahKatalog.route) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah barang", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // Navbar bawah bersama
        AppBottomBar(
            navController = navController,
            currentRoute = Screen.Katalog.route,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun KatalogItemCard(item: MasterKatalog, onClick: () -> Unit) {
    val catColor = kategoriColor(item.kategori)
    val health = stokHealth(item)
    val stokColor = when (health) {
        StokHealth.KRITIS -> RedMain
        StokHealth.RENDAH -> AmberMain
        StokHealth.AMAN -> Navy
    }
    val healthLabel = when (health) {
        StokHealth.KRITIS -> "KRITIS"
        StokHealth.RENDAH -> "RENDAH"
        StokHealth.AMAN -> null
    }
    val labelColor = if (health == StokHealth.KRITIS) RedDark else AmberText

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.5.dp, BorderLight, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Aksen kategori di tepi kiri
        Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(catColor))

        Row(
            modifier = Modifier.weight(1f).padding(start = 11.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(catColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(kategoriIcon(item.kategori), contentDescription = null, tint = catColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.nama_barang, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text(
                    KategoriBarang.label(item.kategori),
                    fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = catColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${item.stok_lokal} ${item.satuan}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = stokColor
                )
                if (healthLabel != null) {
                    Text(
                        healthLabel, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp,
                        color = labelColor, modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.height(5.dp))
                KatalogSyncIndicator(item.sync_status)
            }
        }
    }
}

/** Indikator sinkron ringkas — SYNCED cukup centang mungil; hanya yang perlu aksi yang menonjol. */
@Composable
private fun KatalogSyncIndicator(status: SyncStatus) {
    when (status) {
        SyncStatus.SYNCED -> Box(
            modifier = Modifier.size(18.dp).clip(CircleShape).background(GreenBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = "Tersinkronisasi", tint = GreenMain, modifier = Modifier.size(12.dp))
        }
        SyncStatus.PENDING -> SyncMiniPill(Icons.Default.HourglassEmpty, "Menunggu")
        SyncStatus.IN_TRANSIT -> SyncMiniPill(Icons.Default.LocalShipping, "Dikirim")
    }
}

@Composable
private fun SyncMiniPill(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(AmberBg)
            .border(1.dp, AmberBorder, RoundedCornerShape(999.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AmberMain, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AmberText)
    }
}

@Composable
private fun CategoryChip(label: String, active: Boolean, dotColor: Color?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp))
            .background(if (active) Color.White else Color(0x24FFFFFF))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dotColor != null) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (active) Navy else Color.White)
    }
}

