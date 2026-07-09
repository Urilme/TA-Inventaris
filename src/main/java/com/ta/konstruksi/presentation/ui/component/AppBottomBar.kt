package com.ta.konstruksi.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ta.konstruksi.presentation.navigation.Screen
import com.ta.konstruksi.presentation.ui.theme.BorderLight
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.IncomingTransferViewModel

/** Tinggi total area navbar (termasuk tonjolan tombol tengah). Dipakai layar untuk padding bawah. */
val AppBottomBarHeight = 76.dp

/**
 * Navbar bawah bersama untuk seluruh fungsi utama aplikasi:
 * Beranda · Katalog · [Mutasi] · Transfer · Riwayat.
 * Mutasi ditempatkan sebagai tombol aksi menonjol di tengah (aksi harian tersering).
 *
 * @param currentRoute route layar aktif — untuk menyorot tab yang sedang dibuka.
 */
@Composable
fun AppBottomBar(
    navController: NavController,
    currentRoute: String,
    modifier: Modifier = Modifier,
    incomingViewModel: IncomingTransferViewModel = hiltViewModel()
) {
    val incomingCount by incomingViewModel.count.collectAsStateWithLifecycle()

    Box(modifier = modifier.navigationBarsPadding().fillMaxWidth().height(AppBottomBarHeight)) {

        // Bilah putih dengan 4 tab + celah tengah untuk tombol Mutasi
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(62.dp)
                .background(Color.White)
                .border(1.5.dp, BorderLight, RoundedCornerShape(0.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarTab(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Home, label = "Beranda",
                selected = currentRoute == Screen.Dashboard.route,
                onClick = { navController.switchTab(Screen.Dashboard.route) }
            )
            BarTab(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Inventory, label = "Katalog",
                selected = currentRoute == Screen.Katalog.route,
                onClick = { navController.switchTab(Screen.Katalog.route) }
            )
            Spacer(Modifier.weight(1f)) // ruang untuk tombol tengah
            BarTab(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalShipping, label = "Transfer",
                selected = currentRoute == Screen.Transfer.route,
                badgeCount = incomingCount,
                onClick = { navController.switchTab(Screen.Transfer.route) }
            )
            BarTab(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.History, label = "Riwayat",
                selected = currentRoute == Screen.Riwayat.route,
                onClick = { navController.switchTab(Screen.Riwayat.route) }
            )
        }

        // Tombol aksi Mutasi — menonjol di tengah
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(16.dp))
                .clickable { navController.openMutasi() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Navy),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = "Catat Mutasi", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(3.dp))
            Text(
                "Mutasi", fontSize = 11.sp,
                fontWeight = if (currentRoute == Screen.Mutasi.route) FontWeight.Bold else FontWeight.Medium,
                color = Navy
            )
        }
    }
}

@Composable
private fun BarTab(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ikon + badge jumlah transfer masuk yang menunggu konfirmasi (bila ada)
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = if (selected) Navy else TextSubtle, modifier = Modifier.size(24.dp))
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 11.dp, y = (-7).dp)
                        .size(if (badgeCount > 9) 18.dp else 16.dp)
                        .clip(CircleShape)
                        .background(RedMain)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (badgeCount > 9) "9+" else badgeCount.toString(),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label, fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Navy else TextSubtle
        )
    }
}

/** Pindah tab dengan perilaku tab yang benar: satu instance, simpan & pulihkan state. */
private fun NavController.switchTab(route: String) {
    if (currentDestination?.route == route) return

    // Beranda = start destination. Menuju ke sini lewat pola navigate() biasa menabrakkan kunci
    // saveState/restoreState (target popUpTo == tujuan navigate), sehingga tab yang barusan
    // disimpan justru dipulihkan kembali → seolah tombol tak berfungsi. Cukup pop ke Beranda
    // yang selalu berada di akar back stack.
    if (route == Screen.Dashboard.route) {
        popBackStack(Screen.Dashboard.route, inclusive = false)
        return
    }

    navigate(route) {
        popUpTo(Screen.Dashboard.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Buka layar Mutasi sebagai aksi terfokus (bukan tab yang disimpan state-nya). */
private fun NavController.openMutasi() {
    if (currentDestination?.route == Screen.Mutasi.route) return
    navigate(Screen.Mutasi.createRoute()) { launchSingleTop = true }
}
