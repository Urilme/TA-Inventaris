  package com.ta.konstruksi.presentation.ui.dashboard

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.presentation.navigation.Screen
import com.ta.konstruksi.presentation.ui.component.NetworkBadge
import com.ta.konstruksi.presentation.ui.theme.AmberBg
import com.ta.konstruksi.presentation.ui.theme.AmberBorder
import com.ta.konstruksi.presentation.ui.theme.AmberMain
import com.ta.konstruksi.presentation.ui.theme.AmberText
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderLight
import com.ta.konstruksi.presentation.ui.theme.InfoBg
import com.ta.konstruksi.presentation.ui.theme.InfoBorder
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedBg
import com.ta.konstruksi.presentation.ui.theme.RedBorder
import com.ta.konstruksi.presentation.ui.theme.RedDark
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextSecondary
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.DashboardViewModel
import com.ta.konstruksi.presentation.viewmodel.PendingSyncItem

@Composable
fun DashboardScreen(
    navController: NavController,
    isOnline: Boolean = false,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Saat online dan masih ada backlog, dorong sinkronisasi otomatis (tanpa tombol manual)
    LaunchedEffect(isOnline, state.pendingCount) {
        if (isOnline && state.pendingCount > 0) viewModel.requestSync()
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Navy Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy)
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("Selamat datang,", fontSize = 13.sp, color = Color(0xFF9fb0bf), fontWeight = FontWeight.Medium)
                        Text(
                            state.session?.nama ?: "Mandor",
                            fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.2).sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0x4DFFFFFF), RoundedCornerShape(10.dp))
                            .clickable { viewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) } }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("KELUAR", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFF5B301), modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            state.session?.role?.replaceFirstChar { c -> c.uppercase() } ?: "Mandor",
                            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFc7d2db)
                        )
                    }
                    NetworkBadge(isOnline = isOnline, onDarkBg = true)
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
            ) {
                // Stats row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.SwapVert,
                        label = "MUTASI HARI INI",
                        value = state.todayMutasiCount.toString(),
                        subtitle = "transaksi tercatat",
                        isWarning = false
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.HourglassEmpty,
                        label = "MENUNGGU SINKRON",
                        value = state.pendingCount.toString(),
                        subtitle = "belum terkirim",
                        isWarning = true
                    )
                }

                // Daftar item yang belum tersinkron (auto-sync di latar belakang)
                if (state.pendingItems.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    PendingSyncSection(items = state.pendingItems, isOnline = isOnline)
                }

                Spacer(Modifier.height(22.dp))

                // Quick actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalShipping,
                        label = "Transfer Keluar",
                        onClick = { navController.navigate(Screen.Transfer.route) }
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory,
                        label = "Katalog",
                        onClick = { navController.navigate(Screen.Katalog.route) }
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Low stock section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stok Material Terendah", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Navy)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(RedBg)
                            .border(1.dp, RedBorder, RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text("TOP 3", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = RedDark)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Peringatan dini — segera ajukan pengadaan.", fontSize = 12.sp, color = TextSubtle)
                Spacer(Modifier.height(14.dp))

                if (state.lowStockItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.5.dp, BorderLight, RoundedCornerShape(14.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Semua stok aman", fontSize = 14.sp, color = TextSubtle)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.lowStockItems.forEachIndexed { idx, item ->
                            LowStockCard(item = item, isCritical = idx == 0)
                        }
                    }
                }
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 80.dp)
                .size(64.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Navy)
                .clickable { navController.navigate(Screen.Mutasi.route) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Catat Mutasi", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // Bottom Nav
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.White)
                .border(1.5.dp, BorderLight, RoundedCornerShape(0.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavItem(icon = Icons.Default.Home, label = "Beranda", selected = true, onClick = {}, modifier = Modifier.weight(1f))
            BottomNavItem(icon = Icons.Default.History, label = "Riwayat", selected = false,
                onClick = { navController.navigate(Screen.Riwayat.route) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PendingSyncSection(items: List<PendingSyncItem>, isOnline: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isOnline) InfoBg else AmberBg)
            .border(1.5.dp, if (isOnline) InfoBorder else AmberBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isOnline) Icons.Default.Sync else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (isOnline) Navy else AmberMain,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (isOnline) "Menyinkronkan ${items.size} item…"
                else "Mode Luring — ${items.size} item belum tersinkron",
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = if (isOnline) Navy else AmberText
            )
        }
        Spacer(Modifier.height(10.dp))
        items.take(5).forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = AmberMain, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(item.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.weight(1f))
                Text(item.detail, fontSize = 12.sp, color = TextSecondary)
            }
        }
        if (items.size > 5) {
            Text("+${items.size - 5} item lainnya", fontSize = 12.sp, color = TextSubtle, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (isOnline) "Sedang dikirim ke server…" else "Akan terkirim otomatis saat ada sinyal.",
            fontSize = 12.sp, color = TextSubtle
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subtitle: String,
    isWarning: Boolean
) {
    val bg = if (isWarning) AmberBg else Color.White
    val borderColor = if (isWarning) AmberBorder else BorderLight
    val iconTint = if (isWarning) AmberMain else Navy
    val labelColor = if (isWarning) AmberText else TextSecondary
    val valueColor = if (isWarning) AmberMain else Navy

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor, letterSpacing = 0.3.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 46.sp, fontWeight = FontWeight.Bold, color = valueColor, lineHeight = 48.sp)
        Text(subtitle, fontSize = 12.sp, color = if (isWarning) Color(0xFFb08742) else TextSubtle)
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.5.dp, BorderLight, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
    }
}

@Composable
private fun LowStockCard(item: MasterKatalog, isCritical: Boolean) {
    val borderColor = if (isCritical) RedBorder else AmberBorder
    val leftBorderColor = if (isCritical) RedMain else AmberMain
    val iconBg = if (isCritical) RedBg else AmberBg
    val iconTint = if (isCritical) RedMain else AmberMain
    val valueColor = if (isCritical) RedMain else AmberMain
    val badgeText = if (isCritical) "KRITIS" else "RENDAH"
    val badgeColor = if (isCritical) RedDark else AmberText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Inventory, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.nama_barang, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(item.kategori, fontSize = 12.sp, color = TextSubtle)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${item.stok_lokal} ${item.satuan}",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = valueColor, lineHeight = 26.sp
            )
            Text(badgeText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = badgeColor)
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Navy else TextSubtle, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Navy else TextSubtle)
    }
}
