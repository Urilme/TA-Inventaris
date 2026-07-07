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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.presentation.ui.component.NetworkBadge
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
import com.ta.konstruksi.presentation.ui.theme.TextPrimary
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.TransferUiState
import com.ta.konstruksi.presentation.viewmodel.TransferViewModel
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.StatusTransfer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransferDetailScreen(
    idTransfer: String,
    onBack: () -> Unit,
    isOnline: Boolean = false,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val transferList by viewModel.transferList.collectAsStateWithLifecycle()
    val katalogList by viewModel.katalogList.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transfer = transferList.find { it.id_transfer == idTransfer }
    val katalogItem = transfer?.let { t -> katalogList.find { it.sku_id == t.sku_id } }

    // Kembali hanya setelah konfirmasi benar-benar tersimpan
    LaunchedEffect(uiState) {
        if (uiState is TransferUiState.Success) {
            viewModel.reset()
            onBack()
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
                Text("Detail Transfer", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
                NetworkBadge(isOnline = isOnline)
            }

            if (transfer == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Transfer tidak ditemukan", color = TextSubtle)
                }
            } else {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp)) {
                    // Item header
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(AmberBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Inventory, null, tint = AmberMain, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                katalogItem?.nama_barang ?: "Barang tidak dikenal",
                                fontSize = 18.sp, fontWeight = FontWeight.Black, color = Navy
                            )
                            Text(katalogItem?.kategori?.let { KategoriBarang.label(it) } ?: "", fontSize = 12.sp, color = TextSubtle)
                        }
                    }

                    // Detail card
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                            .border(1.5.dp, BorderLight, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp)
                    ) {
                        DetailRow("Nama Barang", katalogItem?.nama_barang ?: "Barang tidak dikenal")
                        DetailRow("Kategori", katalogItem?.kategori?.let { KategoriBarang.label(it) } ?: "-")
                        DetailRow("Jumlah", "${transfer.jumlah} ${katalogItem?.satuan ?: "unit"}")
                        DetailRow("Dari Proyek", transfer.proyek_asal)
                        DetailRow("Ke Proyek", transfer.proyek_tujuan, isLast = false, isHighlight = true)
                        DetailRow("Waktu Kirim", SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date(transfer.local_timestamp)))
                        // Status row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status", fontSize = 13.sp, color = TextSubtle, fontWeight = FontWeight.Medium)
                            val badgeBg = when (transfer.status_transfer) {
                                StatusTransfer.IN_TRANSIT -> AmberBg
                                StatusTransfer.SELESAI -> GreenBg
                                StatusTransfer.DIBATALKAN -> RedBg
                            }
                            val badgeBorder = when (transfer.status_transfer) {
                                StatusTransfer.IN_TRANSIT -> AmberBorder
                                StatusTransfer.SELESAI -> GreenBorder
                                StatusTransfer.DIBATALKAN -> RedBorder
                            }
                            val badgeTint = when (transfer.status_transfer) {
                                StatusTransfer.IN_TRANSIT -> AmberMain
                                StatusTransfer.SELESAI -> GreenMain
                                StatusTransfer.DIBATALKAN -> RedMain
                            }
                            val badgeTextColor = when (transfer.status_transfer) {
                                StatusTransfer.IN_TRANSIT -> AmberText
                                StatusTransfer.SELESAI -> GreenMain
                                StatusTransfer.DIBATALKAN -> RedMain
                            }
                            val badgeIcon = when (transfer.status_transfer) {
                                StatusTransfer.IN_TRANSIT -> Icons.Default.HourglassEmpty
                                StatusTransfer.SELESAI -> Icons.Default.CheckCircle
                                StatusTransfer.DIBATALKAN -> Icons.Default.Info
                            }
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(999.dp))
                                    .background(badgeBg)
                                    .border(1.dp, badgeBorder, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(badgeIcon, null, tint = badgeTint, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(transfer.status_transfer.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = badgeTextColor)
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Info
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = TextSubtle, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Pastikan aset sudah diterima secara fisik sebelum mengkonfirmasi penerimaan.",
                            fontSize = 12.sp, lineHeight = 18.sp, color = TextSubtle
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // KONFIRMASI button
                    Box(
                        modifier = Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (transfer.status_transfer == StatusTransfer.IN_TRANSIT) Navy else Color(0xFFB0BEC5))
                            .clickable(
                                enabled = transfer.status_transfer == StatusTransfer.IN_TRANSIT &&
                                    uiState !is TransferUiState.Loading
                            ) {
                                viewModel.confirmTransfer(transfer.id_transfer, transfer.sku_id, transfer.jumlah)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("KONFIRMASI PENERIMAAN", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Laporkan Masalah button
                    Box(
                        modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, BorderLight, RoundedCornerShape(14.dp)).background(Color.Transparent)
                            .clickable {},
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Flag, null, tint = RedMain, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Laporkan Masalah", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isLast: Boolean = false, isHighlight: Boolean = false) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp, color = TextSubtle, fontWeight = FontWeight.Medium)
            Text(
                value, fontSize = 14.sp, color = if (isHighlight) GreenMain else Navy,
                fontWeight = FontWeight.Bold
            )
        }
        if (!isLast) HorizontalDivider(color = Color(0xFFeef1f3), thickness = 1.dp)
    }
}
