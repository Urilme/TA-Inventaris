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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
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
import com.ta.konstruksi.presentation.viewmodel.RiwayatItem
import com.ta.konstruksi.presentation.viewmodel.RiwayatViewModel
import com.ta.konstruksi.util.JenisMutasi
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
    var searchQuery by remember { mutableStateOf("") }

    val filtered = riwayatList.filter {
        if (searchQuery.isBlank()) true
        else it.label.contains(searchQuery, true) || when (it) {
            is RiwayatItem.Mutasi -> it.data.sku_id.contains(searchQuery, true)
            is RiwayatItem.Transfer -> it.data.sku_id.contains(searchQuery, true)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Navy header with search
            Column(modifier = Modifier.fillMaxWidth().background(Navy).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
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
                        Text(if (isOnline) "DARING" else "LURING", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, tint = Color(0xFFd0d8de), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Belum ada riwayat", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFa7b2bc))
                        Text("Catat mutasi atau transfer untuk melihat riwayat.", fontSize = 13.sp, color = TextSubtle)
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
                    item { Spacer(Modifier.height(6.dp)) }
                }
            }

            // Bottom nav
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).background(Color.White)
                    .border(1.5.dp, BorderLight, RoundedCornerShape(0.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().clickable { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) } },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Home, null, tint = TextSubtle, modifier = Modifier.size(24.dp))
                    Text("Beranda", fontSize = 11.sp, color = TextSubtle)
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.History, null, tint = Navy, modifier = Modifier.size(24.dp))
                    Text("Riwayat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy)
                }
            }
        }
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.5.dp, BorderLight, RoundedCornerShape(14.dp)).padding(13.dp)
    ) {
        when (item) {
            is RiwayatItem.Mutasi -> {
                val m: LogMutasi = item.data
                val isOut = m.jenis_mutasi == JenisMutasi.CHECK_OUT
                val iconBg = if (isOut) RedBg else GreenBg
                val iconTint = if (isOut) RedMain else GreenMain
                val typeLabel = if (isOut) "Keluar" else "Masuk"
                val typeColor = if (isOut) RedMain else GreenMain

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                        Icon(
                            if (isOut) Icons.Default.HourglassEmpty else Icons.Default.CheckCircle,
                            null, tint = iconTint, modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
                        Row {
                            Text(typeLabel, fontSize = 12.sp, color = typeColor, fontWeight = FontWeight.Bold)
                            Text(" · ${m.jumlah} unit · ${timeLabel(m.local_timestamp)}", fontSize = 12.sp, color = TextSubtle)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SyncStatusBadge(status = m.sync_status)
                }
            }
            is RiwayatItem.Transfer -> {
                val t: TransferProyek = item.data
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFeef1f3)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalShipping, null, tint = Navy, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
                        Row {
                            Text("Transfer", fontSize = 12.sp, color = TextSubtle, fontWeight = FontWeight.Bold)
                            Text(" · ${t.jumlah} unit · ${timeLabel(t.local_timestamp)}", fontSize = 12.sp, color = TextSubtle)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SyncStatusBadge(status = t.sync_status)
                }
            }
        }
    }
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
