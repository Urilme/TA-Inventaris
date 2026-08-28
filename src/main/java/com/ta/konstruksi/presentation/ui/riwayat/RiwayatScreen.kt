package com.ta.konstruksi.presentation.ui.riwayat

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ta.konstruksi.domain.model.LogMutasi
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.presentation.navigation.Screen
import com.ta.konstruksi.presentation.ui.component.AppBottomBar
import com.ta.konstruksi.presentation.ui.component.ListSkeleton
import com.ta.konstruksi.presentation.ui.component.SyncStatusBadge
import com.ta.konstruksi.presentation.ui.theme.AmberBg
import com.ta.konstruksi.presentation.ui.theme.AmberBorder
import com.ta.konstruksi.presentation.ui.theme.AmberMain
import com.ta.konstruksi.presentation.ui.theme.AmberText
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderLight
import com.ta.konstruksi.presentation.ui.theme.GreenBg
import com.ta.konstruksi.presentation.ui.theme.GreenBorder
import com.ta.konstruksi.presentation.ui.theme.GreenMain
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedBg
import com.ta.konstruksi.presentation.ui.theme.RedBorder
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.RiwayatItem
import com.ta.konstruksi.presentation.viewmodel.RiwayatViewModel
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.KondisiBarang
import com.ta.konstruksi.util.SearchFilter
import com.ta.konstruksi.util.StatusTransfer
import com.ta.konstruksi.util.SyncStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun RiwayatScreen(
    navController: NavController,
    isOnline: Boolean = false,
    viewModel: RiwayatViewModel = hiltViewModel()
) {
    val riwayatList by viewModel.riwayatList.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val jenisFilter by viewModel.jenisFilter.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val adaFilterAktif = filterState != null || jenisFilter != null

    val filtered = riwayatList.filter {
        val sku = when (it) {
            is RiwayatItem.Mutasi -> it.data.sku_id
            is RiwayatItem.Transfer -> it.data.sku_id
        }
        SearchFilter.matchesRiwayat(it.label, sku, searchQuery)
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Navy header with search
            Column(modifier = Modifier.fillMaxWidth().background(Navy).statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Riwayat Mutasi", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    // Network badge (inline DARING/LURING)
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp))
                            .background(if (isOnline) Color(0x295fe39a) else Color(0x1AFFFFFF))
                            .border(1.dp, if (isOnline) Color(0x595fe39a) else Color(0x2DFFFFFF), RoundedCornerShape(999.dp))
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.size(8.dp).clip(CircleShape).background(if (isOnline) GreenMain else AmberMain))
                        Spacer(Modifier.width(7.dp))
                        Text(if (isOnline) "ONLINE" else "OFFLINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Search bar
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
                            if (searchQuery.isEmpty()) Text("Cari barang…", fontSize = 15.sp, color = TextSubtle)
                            inner()
                        }
                    )
                    Icon(Icons.Default.Tune, null, tint = Navy, modifier = Modifier.size(22.dp))
                }
            }

            // Filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(AppBackground).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(label = "Semua", selected = filterState == null, onClick = { viewModel.setFilter(null) })
                }
                item {
                    FilterChipWithDot(
                        label = "Menunggu", dotColor = AmberMain, selected = filterState == SyncStatus.PENDING,
                        onClick = { viewModel.setFilter(SyncStatus.PENDING) }
                    )
                }
                item {
                    FilterChipWithDot(
                        label = "Sinkron", dotColor = GreenMain, selected = filterState == SyncStatus.SYNCED,
                        onClick = { viewModel.setFilter(SyncStatus.SYNCED) }
                    )
                }
                // Pemisah antara filter status sinkron dan filter jenis mutasi
                item { FilterDivider() }
                item {
                    FilterChipWithDot(
                        label = "Masuk", dotColor = GreenMain, selected = jenisFilter == JenisMutasi.CHECK_IN,
                        onClick = {
                            viewModel.setJenisFilter(if (jenisFilter == JenisMutasi.CHECK_IN) null else JenisMutasi.CHECK_IN)
                        }
                    )
                }
                item {
                    FilterChipWithDot(
                        label = "Keluar", dotColor = RedMain, selected = jenisFilter == JenisMutasi.CHECK_OUT,
                        onClick = {
                            viewModel.setJenisFilter(if (jenisFilter == JenisMutasi.CHECK_OUT) null else JenisMutasi.CHECK_OUT)
                        }
                    )
                }
            }

            if (isLoading) {
                ListSkeleton(modifier = Modifier.weight(1f).fillMaxWidth(), rows = 7)
            } else if (filtered.isEmpty()) {
                // Beri ruang navbar (118dp) agar konten benar-benar terpusat di area terlihat
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 118.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
                        Box(
                            modifier = Modifier.size(96.dp).clip(CircleShape).background(Color(0xFFeef1f3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, null, tint = Color(0xFFa7b2bc), modifier = Modifier.size(46.dp))
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            if (!adaFilterAktif) "Belum ada riwayat" else "Tidak ada riwayat pada filter ini",
                            fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (!adaFilterAktif) "Semua mutasi & transfer yang kamu catat akan muncul di sini."
                            else "Coba ubah filter untuk melihat riwayat lainnya.",
                            fontSize = 13.sp, color = TextSubtle,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 19.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { item ->
                        RiwayatCard(item = item)
                    }
                    item { Spacer(Modifier.height(118.dp)) }
                }
            }
        }

        // Navbar bawah bersama
        AppBottomBar(
            navController = navController,
            currentRoute = Screen.Riwayat.route,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun RiwayatCard(item: RiwayatItem) {
    val formatter = SimpleDateFormat("HH:mm", Locale("id"))
    val fullFormatter = SimpleDateFormat("dd MMM yyyy", Locale("id"))
    val nowCal = Calendar.getInstance()

    fun timeLabel(ts: Long): String {
        val itemCal = Calendar.getInstance().apply { timeInMillis = ts }
        val sameYear = itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
        val dayDiff = nowCal.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR)
        return when {
            sameYear && dayDiff == 0 -> formatter.format(Date(ts))
            sameYear && dayDiff == 1 -> "Kemarin ${formatter.format(Date(ts))}"
            else -> fullFormatter.format(Date(ts))
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .border(1.5.dp, BorderLight, RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        when (item) {
            is RiwayatItem.Mutasi -> {
                val m: LogMutasi = item.data
                val isOut = m.jenis_mutasi == JenisMutasi.CHECK_OUT
                val iconBg = if (isOut) RedBg else GreenBg
                val iconTint = if (isOut) RedMain else GreenMain
                val typeLabel = if (isOut) "Keluar" else "Masuk"
                val accent = if (isOut) RedMain else GreenMain

                // Header: ikon · nama + jenis/waktu · jumlah menonjol
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                        Icon(
                            if (isOut) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                            null, tint = iconTint, modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(typeLabel, fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
                            Text(" · ${timeLabel(m.local_timestamp)}", fontSize = 12.sp, color = TextSubtle)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${if (isOut) "−" else "+"}${m.jumlah}",
                            fontSize = 20.sp, fontWeight = FontWeight.Black, color = accent, lineHeight = 22.sp
                        )
                        Text("unit", fontSize = 11.sp, color = TextSubtle)
                    }
                }

                Spacer(Modifier.height(12.dp))
                CardDivider()
                Spacer(Modifier.height(12.dp))

                // Kondisi + status sinkron
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slot kiri selalu terisi (paralel dgn badge status transfer): kondisi untuk
                    // "alat", atau badge kategori (Bahan/Scaffolding) bila kondisi tidak relevan.
                    when {
                        m.kondisi_barang != null -> KondisiBadge(m.kondisi_barang)
                        item.kategori.isNotBlank() -> KategoriBadge(item.kategori)
                        else -> Spacer(Modifier.width(1.dp))
                    }
                    SyncStatusBadge(status = m.sync_status)
                }

                if (m.lokasi_penggunaan.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    MetaRow(Icons.Default.Place, "Lokasi", m.lokasi_penggunaan)
                }
                if (m.penanggungjawab.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    MetaRow(Icons.Default.Person, "Penanggung Jawab", m.penanggungjawab)
                }
            }
            is RiwayatItem.Transfer -> {
                val t: TransferProyek = item.data
                // Aksen ikon mengikuti status transfer (bukan navy statis) agar sejalan dgn
                // semantik warna mutasi (masuk hijau / keluar merah).
                val iconBg = when (t.status_transfer) {
                    StatusTransfer.IN_TRANSIT -> AmberBg
                    StatusTransfer.SELESAI -> GreenBg
                    StatusTransfer.DIBATALKAN -> RedBg
                }
                val iconTint = when (t.status_transfer) {
                    StatusTransfer.IN_TRANSIT -> AmberMain
                    StatusTransfer.SELESAI -> GreenMain
                    StatusTransfer.DIBATALKAN -> RedMain
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalShipping, null, tint = iconTint, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Transfer", fontSize = 12.sp, color = Navy, fontWeight = FontWeight.Bold)
                            Text(" · ${timeLabel(t.local_timestamp)}", fontSize = 12.sp, color = TextSubtle)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${t.jumlah}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy, lineHeight = 22.sp)
                        Text("unit", fontSize = 11.sp, color = TextSubtle)
                    }
                }

                Spacer(Modifier.height(12.dp))
                CardDivider()
                Spacer(Modifier.height(12.dp))

                // Baris badge sejajar dgn mutasi: status transfer (kiri) + status sinkron (kanan)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusTransferBadge(t.status_transfer)
                    SyncStatusBadge(status = t.sync_status)
                }

                // Rute dipecah jadi dua MetaRow penuh (Dari / Ke) — konsisten dgn Lokasi/PJ mutasi
                if (t.proyek_asal.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    MetaRow(Icons.Default.Home, "Dari", t.proyek_asal)
                }
                if (t.proyek_tujuan.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    MetaRow(Icons.Default.Place, "Ke", t.proyek_tujuan)
                }
            }
        }
    }
}

@Composable
private fun CardDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderLight))
}

@Composable
private fun MetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("$label: ", fontSize = 12.sp, color = TextSubtle)
        Text(value, fontSize = 12.sp, color = Navy, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun KondisiBadge(kondisi: KondisiBarang) {
    val (label, bg, border, fg) = when (kondisi) {
        KondisiBarang.BAIK -> Quad("Baik", GreenBg, GreenBorder, GreenMain)
        KondisiBarang.RUSAK_RINGAN -> Quad("Rusak Ringan", AmberBg, AmberBorder, AmberMain)
        KondisiBarang.RUSAK_BERAT -> Quad("Rusak Berat", RedBg, RedMain, RedMain)
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.size(7.dp).clip(CircleShape).background(fg))
        Spacer(Modifier.width(6.dp))
        Text("Kondisi: $label", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

private data class Quad(val label: String, val bg: Color, val border: Color, val fg: Color)

@Composable
private fun KategoriBadge(kategori: String) {
    // Netral (bukan hijau/kuning/merah) agar tidak menyiratkan status baik/buruk —
    // hanya menandai jenis barang untuk mengisi slot kiri kartu mutasi bahan/scaffolding.
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFFeef1f3))
            .border(1.dp, BorderLight, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.size(7.dp).clip(CircleShape).background(TextSubtle))
        Spacer(Modifier.width(6.dp))
        Text(KategoriBarang.label(kategori), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy)
    }
}

@Composable
private fun StatusTransferBadge(status: StatusTransfer) {
    // Warna, ikon & label mengikuti TransferDetailScreen agar konsisten lintas layar
    val (label, bg, border, fg) = when (status) {
        StatusTransfer.IN_TRANSIT -> Quad("Dalam Perjalanan", AmberBg, AmberBorder, AmberText)
        StatusTransfer.SELESAI -> Quad("Selesai", GreenBg, GreenBorder, GreenMain)
        StatusTransfer.DIBATALKAN -> Quad("Dibatalkan", RedBg, RedBorder, RedMain)
    }
    val icon = when (status) {
        StatusTransfer.IN_TRANSIT -> Icons.Default.HourglassEmpty
        StatusTransfer.SELESAI -> Icons.Default.CheckCircle
        StatusTransfer.DIBATALKAN -> Icons.Default.Info
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun FilterDivider() {
    Box(
        modifier = Modifier.padding(horizontal = 2.dp).size(width = 1.5.dp, height = 22.dp)
            .clip(RoundedCornerShape(999.dp)).background(BorderLight)
    )
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(999.dp))
            .background(if (selected) Navy else Color.White)
            .border(if (selected) 0.dp else 1.5.dp, if (selected) Color.Transparent else BorderLight, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else TextSubtle)
    }
}

@Composable
private fun FilterChipWithDot(label: String, dotColor: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp))
            .background(if (selected) Navy else Color.White)
            .border(if (selected) 0.dp else 1.5.dp, if (selected) Color.Transparent else BorderLight, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.size(7.dp).clip(CircleShape).background(if (selected) Color.White else dotColor))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else TextSubtle)
    }
}
