package com.ta.konstruksi.presentation.ui.katalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ta.konstruksi.domain.model.LogMutasi
import com.ta.konstruksi.presentation.ui.component.NetworkBadge
import com.ta.konstruksi.presentation.ui.component.SyncStatusBadge
import com.ta.konstruksi.presentation.ui.theme.AmberBg
import com.ta.konstruksi.presentation.ui.theme.AmberBorder
import com.ta.konstruksi.presentation.ui.theme.AmberMain
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderLight
import com.ta.konstruksi.presentation.ui.theme.GreenBg
import com.ta.konstruksi.presentation.ui.theme.GreenBorder
import com.ta.konstruksi.presentation.ui.theme.GreenMain
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedBg
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.KatalogViewModel
import com.ta.konstruksi.presentation.viewmodel.RiwayatItem
import com.ta.konstruksi.presentation.viewmodel.RiwayatViewModel
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.KondisiBarang
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailBarangScreen(
    skuId: String,
    onBack: () -> Unit,
    isOnline: Boolean = false,
    katalogViewModel: KatalogViewModel = hiltViewModel(),
    riwayatViewModel: RiwayatViewModel = hiltViewModel()
) {
    val katalogList by katalogViewModel.katalogList.collectAsStateWithLifecycle()
    val riwayatList by riwayatViewModel.riwayatList.collectAsStateWithLifecycle()

    val item = katalogList.firstOrNull { it.sku_id == skuId }
    val mutasiBarang = riwayatList
        .filterIsInstance<RiwayatItem.Mutasi>()
        .map { it.data }
        .filter { it.sku_id == skuId }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().background(Navy).statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Text("Detail Barang", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
                NetworkBadge(isOnline = isOnline)
            }

            if (item == null) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Barang tidak ditemukan.", fontSize = 14.sp, color = TextSubtle)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Kartu info barang
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                                .border(1.5.dp, BorderLight, RoundedCornerShape(16.dp)).padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFeef1f3)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Inventory, contentDescription = null, tint = Navy, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.nama_barang, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy)
                                    Text(KategoriBarang.label(item.kategori), fontSize = 12.sp, color = TextSubtle)
                                }
                                SyncStatusBadge(status = item.sync_status)
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(item.sku_id, fontSize = 12.sp, color = TextSubtle, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("STOK LOKAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSubtle, letterSpacing = 0.5.sp)
                                    Text(
                                        "${item.stok_lokal} ${item.satuan}",
                                        fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                        color = if (item.stok_lokal <= 5) RedMain else Navy
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("SATUAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSubtle, letterSpacing = 0.5.sp)
                                    Text(item.satuan, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                                }
                            }
                        }
                    }

                    // Judul riwayat
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Riwayat Mutasi Barang Ini", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
                        }
                    }

                    if (mutasiBarang.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                                    .border(1.5.dp, BorderLight, RoundedCornerShape(14.dp)).padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada mutasi untuk barang ini.", fontSize = 13.sp, color = TextSubtle)
                            }
                        }
                    } else {
                        items(mutasiBarang) { m -> MutasiRingkasCard(m) }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MutasiRingkasCard(m: LogMutasi) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("id")) }
    val isOut = m.jenis_mutasi == JenisMutasi.CHECK_OUT
    val iconBg = if (isOut) RedBg else GreenBg
    val iconTint = if (isOut) RedMain else GreenMain
    val typeLabel = if (isOut) "Keluar" else "Masuk"
    val typeColor = if (isOut) RedMain else GreenMain

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.5.dp, BorderLight, RoundedCornerShape(14.dp)).padding(13.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(
                    if (isOut) Icons.Default.HourglassEmpty else Icons.Default.CheckCircle,
                    contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(typeLabel, fontSize = 14.sp, color = typeColor, fontWeight = FontWeight.Bold)
                    Text(" · ${m.jumlah} unit", fontSize = 14.sp, color = Navy, fontWeight = FontWeight.Bold)
                }
                Text(formatter.format(Date(m.local_timestamp)), fontSize = 12.sp, color = TextSubtle)
            }
            SyncStatusBadge(status = m.sync_status)
        }
        Spacer(Modifier.height(10.dp))
        m.kondisi_barang?.let { KondisiBadge(it) }   // kondisi hanya ada untuk kategori "alat"
        if (m.lokasi_penggunaan.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("Lokasi: ${m.lokasi_penggunaan}", fontSize = 12.sp, color = TextSubtle)
        }
        if (m.penanggungjawab.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text("PJ: ${m.penanggungjawab}", fontSize = 12.sp, color = TextSubtle)
        }
        m.catatan?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(2.dp))
            Text("Catatan: $it", fontSize = 12.sp, color = TextSubtle)
        }
    }
}

@Composable
private fun KondisiBadge(kondisi: KondisiBarang) {
    data class Style(val label: String, val bg: Color, val border: Color, val fg: Color)
    val s = when (kondisi) {
        KondisiBarang.BAIK -> Style("Baik", GreenBg, GreenBorder, GreenMain)
        KondisiBarang.RUSAK_RINGAN -> Style("Rusak Ringan", AmberBg, AmberBorder, AmberMain)
        KondisiBarang.RUSAK_BERAT -> Style("Rusak Berat", RedBg, RedMain, RedMain)
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(s.bg)
            .border(1.dp, s.border, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.size(7.dp).clip(CircleShape).background(s.fg))
        Spacer(Modifier.width(6.dp))
        Text("Kondisi: ${s.label}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = s.fg)
    }
}
