  package com.ta.konstruksi.presentation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoveToInbox
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
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.presentation.navigation.Screen
import com.ta.konstruksi.presentation.ui.component.AppBottomBar
import com.ta.konstruksi.presentation.ui.component.DashboardSkeleton
import com.ta.konstruksi.presentation.ui.component.NetworkBadge
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
import com.ta.konstruksi.presentation.ui.theme.GreenBorder
import com.ta.konstruksi.presentation.ui.theme.GreenMain
import com.ta.konstruksi.presentation.ui.theme.GreenText
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
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

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
                    .statusBarsPadding()
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
                    // Avatar → buka Profil (logout kini ada di dalam Profil)
                    val dashNama = state.session?.nama?.takeIf { it.isNotBlank() } ?: "Mandor"
                    val dashInisial = dashNama.trim().split(" ").filter { it.isNotBlank() }
                        .take(2).joinToString("") { it.first().uppercase() }.ifBlank { "M" }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.5.dp, Color(0x4DFFFFFF), CircleShape)
                            .clickable { navController.navigate(Screen.Profil.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(dashInisial, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFF5B301), modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            state.session?.nama_proyek?.takeIf { it.isNotBlank() } ?: "Proyek belum ditetapkan",
                            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFc7d2db),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    NetworkBadge(isOnline = isOnline, onDarkBg = true)
                }
            }

            // Content
            if (isLoading) {
                DashboardSkeleton(modifier = Modifier.weight(1f).fillMaxWidth())
            } else
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 118.dp)
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
                    val adaBacklog = state.pendingCount > 0
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = if (adaBacklog) Icons.Default.HourglassEmpty else Icons.Default.CheckCircle,
                        label = "MENUNGGU SINKRON",
                        value = state.pendingCount.toString(),
                        subtitle = if (adaBacklog) "belum terkirim" else "semua tersinkron",
                        isWarning = adaBacklog,
                        isPositive = !adaBacklog
                    )
                }

                // Peringatan transfer masuk — aset dari proyek lain menunggu konfirmasi penerimaan.
                // Ditaruh menonjol di atas agar tidak terlewat (kesadaran, bukan perlu dicari).
                if (state.incomingTransferCount > 0) {
                    Spacer(Modifier.height(14.dp))
                    IncomingTransferCard(
                        count = state.incomingTransferCount,
                        proyek = state.session?.nama_proyek?.takeIf { it.isNotBlank() } ?: "proyek Anda",
                        onClick = { navController.navigate(Screen.TransferMasuk.route) }
                    )
                }

                // Daftar item yang belum tersinkron (auto-sync di latar belakang)
                if (state.pendingItems.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    PendingSyncSection(items = state.pendingItems, isOnline = isOnline)
                }

                Spacer(Modifier.height(22.dp))

                // Aksi cepat — pintasan ke form Mutasi dengan jenis sudah terpilih.
                // (Bukan duplikat navbar: navbar menuju halaman, ini langsung ke aksi harian.)
                Text("Catat Cepat", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Navy)
                Spacer(Modifier.height(4.dp))
                Text("Langsung ke form dengan jenis mutasi terpilih.", fontSize = 12.sp, color = TextSubtle)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Login,
                        label = "Barang Masuk",
                        accent = GreenMain,
                        onClick = { navController.navigate(Screen.Mutasi.createRoute(JenisMutasi.CHECK_IN)) }
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Logout,
                        label = "Barang Keluar",
                        accent = RedMain,
                        onClick = { navController.navigate(Screen.Mutasi.createRoute(JenisMutasi.CHECK_OUT)) }
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
                        state.lowStockItems.forEach { item ->
                            LowStockCard(item = item)
                        }
                    }
                }
            }
        }

        // Navbar bawah bersama (semua fungsi utama)
        AppBottomBar(
            navController = navController,
            currentRoute = Screen.Dashboard.route,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun IncomingTransferCard(count: Int, proyek: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AmberBg)
            .border(1.5.dp, AmberBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MoveToInbox, contentDescription = null, tint = AmberMain, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "$count aset menunggu konfirmasi",
                fontSize = 15.sp, fontWeight = FontWeight.Black, color = AmberText
            )
            Text(
                "Dikirim dari proyek lain ke $proyek — ketuk untuk konfirmasi.",
                fontSize = 12.sp, lineHeight = 17.sp, color = Color(0xFFb08742)
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AmberMain, modifier = Modifier.size(24.dp))
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
                else "Mode Offline — ${items.size} item belum tersinkron",
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
    isWarning: Boolean,
    isPositive: Boolean = false
) {
    val bg = when { isWarning -> AmberBg; isPositive -> GreenBg; else -> Color.White }
    val borderColor = when { isWarning -> AmberBorder; isPositive -> GreenBorder; else -> BorderLight }
    val iconTint = when { isWarning -> AmberMain; isPositive -> GreenMain; else -> Navy }
    val labelColor = when { isWarning -> AmberText; isPositive -> GreenText; else -> TextSecondary }
    val valueColor = when { isWarning -> AmberMain; isPositive -> GreenMain; else -> Navy }
    val subtitleColor = when { isWarning -> Color(0xFFb08742); isPositive -> GreenText; else -> TextSubtle }

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
        Text(subtitle, fontSize = 12.sp, color = subtitleColor)
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color = Navy,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.5.dp, BorderLight, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
    }
}

@Composable
private fun LowStockCard(item: MasterKatalog) {
    // Kritis/rendah ditentukan aturan stokHealth() (konsisten dgn Katalog), bukan posisi urutan.
    val isCritical = stokHealth(item) == StokHealth.KRITIS
    val catColor = kategoriColor(item.kategori)
    val borderColor = if (isCritical) RedBorder else AmberBorder
    val valueColor = if (isCritical) RedMain else AmberMain
    val badgeText = if (isCritical) "KRITIS" else "RENDAH"
    val badgeColor = if (isCritical) RedDark else AmberText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Aksen kategori di tepi kiri — bahasa visual sama dengan Katalog
        Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(catColor))

        Row(
            modifier = Modifier.weight(1f).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(catColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(kategoriIcon(item.kategori), contentDescription = null, tint = catColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.nama_barang, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text(
                    KategoriBarang.label(item.kategori),
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, color = catColor
                )
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
}

