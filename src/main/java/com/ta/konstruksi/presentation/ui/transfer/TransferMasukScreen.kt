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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.presentation.navigation.Screen
import com.ta.konstruksi.presentation.ui.component.NetworkBadge
import com.ta.konstruksi.presentation.ui.theme.AmberBg
import com.ta.konstruksi.presentation.ui.theme.AmberBorder
import com.ta.konstruksi.presentation.ui.theme.AmberMain
import com.ta.konstruksi.presentation.ui.theme.AmberText
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderLight
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.TransferViewModel
import com.ta.konstruksi.util.StatusTransfer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransferMasukScreen(
    navController: NavController,
    proyekTujuan: String = "Proyek Aktif",
    isOnline: Boolean = false,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val transferList by viewModel.transferList.collectAsStateWithLifecycle()
    val incomingTransfers = transferList.filter { it.status_transfer == StatusTransfer.IN_TRANSIT }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().background(Navy).statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Text("Transfer Masuk", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
                NetworkBadge(isOnline = isOnline)
            }

            // Info banner
            if (incomingTransfers.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(AmberBg)
                        .border(0.dp, Color.Transparent).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocalShipping, null, tint = AmberMain, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Aset berikut sedang dalam perjalanan menuju proyek Anda — menunggu konfirmasi penerimaan.",
                        fontSize = 12.sp, lineHeight = 18.sp, color = AmberText, modifier = Modifier.weight(1f)
                    )
                }
            }

            if (incomingTransfers.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.weight(1f).padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFFe9edf0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalShipping, null, tint = Color(0xFFa7b2bc), modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("Tidak ada transfer masuk saat ini", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Aset yang dikirim dari proyek lain akan muncul di sini untuk Anda konfirmasi penerimaannya.",
                        fontSize = 13.sp, lineHeight = 20.sp, color = TextSubtle,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(incomingTransfers) { transfer ->
                        TransferMasukCard(
                            transfer = transfer,
                            onClick = { navController.navigate(Screen.TransferDetail.createRoute(transfer.id_transfer)) }
                        )
                    }
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
                    modifier = Modifier.weight(1f).fillMaxSize().clickable { navController.navigate(Screen.Riwayat.route) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.History, null, tint = TextSubtle, modifier = Modifier.size(24.dp))
                    Text("Riwayat", fontSize = 11.sp, color = TextSubtle)
                }
            }
        }
    }
}

@Composable
private fun TransferMasukCard(transfer: TransferProyek, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("HH:mm", Locale("id")).format(Date(transfer.local_timestamp))

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.5.dp, BorderLight, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AmberBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocalShipping, null, tint = AmberMain, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(transfer.sku_id, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text("${transfer.jumlah} unit · Dari ${transfer.proyek_asal}", fontSize = 12.sp, color = TextSubtle)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(AmberBg)
                    .border(1.dp, AmberBorder, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.HourglassEmpty, null, tint = AmberMain, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("DALAM PERJALANAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = AmberText)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
            Text(dateStr, fontSize = 11.sp, color = TextSubtle)
            Spacer(Modifier.height(14.dp))
            Icon(Icons.Default.ChevronRight, null, tint = TextSubtle, modifier = Modifier.size(26.dp))
        }
    }
}
