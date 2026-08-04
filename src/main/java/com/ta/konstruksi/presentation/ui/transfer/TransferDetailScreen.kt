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
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.presentation.ui.component.ConfirmActionDialog
import com.ta.konstruksi.presentation.ui.component.ConfirmInfoRow
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
import com.ta.konstruksi.presentation.ui.theme.InfoBg
import com.ta.konstruksi.presentation.ui.theme.InfoBorder
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedBg
import com.ta.konstruksi.presentation.ui.theme.RedBorder
import com.ta.konstruksi.presentation.ui.theme.RedMain
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
    var showConfirm by remember { mutableStateOf(false) }
    var showBatal by remember { mutableStateOf(false) }

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
                // Utamakan snapshot pada transfer (self-describing); fallback ke katalog lokal bila kosong.
                val namaBarang = transfer.nama_barang.ifBlank { katalogItem?.nama_barang ?: "Barang tidak dikenal" }
                val kategoriRaw = transfer.kategori.ifBlank { katalogItem?.kategori ?: "" }
                val satuan = transfer.satuan.ifBlank { katalogItem?.satuan ?: "unit" }
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
                                namaBarang,
                                fontSize = 18.sp, fontWeight = FontWeight.Black, color = Navy
                            )
                            Text(if (kategoriRaw.isBlank()) "" else KategoriBarang.label(kategoriRaw), fontSize = 12.sp, color = TextSubtle)
                        }
                    }

                    // Detail card
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                            .border(1.5.dp, BorderLight, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp)
                    ) {
                        DetailRow("Nama Barang", namaBarang)
                        DetailRow("Kategori", if (kategoriRaw.isBlank()) "-" else KategoriBarang.label(kategoriRaw))
                        DetailRow("Jumlah", "${transfer.jumlah} $satuan")
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
                            // Label manusiawi — hindari istilah teknis IN_TRANSIT di UI
                            val badgeLabel = when (transfer.status_transfer) {
                                StatusTransfer.IN_TRANSIT -> "DALAM PERJALANAN"
                                StatusTransfer.SELESAI -> "SELESAI"
                                StatusTransfer.DIBATALKAN -> "DIBATALKAN"
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
                                Text(badgeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = badgeTextColor)
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
                                showConfirm = true
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

                    // Batalkan transfer — hanya relevan selagi masih IN_TRANSIT (belum diterima)
                    if (transfer.status_transfer == StatusTransfer.IN_TRANSIT) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, RedBorder, RoundedCornerShape(14.dp)).background(RedBg)
                                .clickable(enabled = uiState !is TransferUiState.Loading) { showBatal = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Flag, null, tint = RedMain, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Batalkan Transfer", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = RedMain)
                            }
                        }
                    }
                }

                // Guard konfirmasi sebelum stok berpindah ke proyek penerima (status → SELESAI)
                if (showConfirm) {
                    ConfirmActionDialog(
                        icon = Icons.Default.CheckCircle,
                        accent = GreenMain,
                        accentBg = GreenBg,
                        title = "Konfirmasi Penerimaan",
                        subtitle = "Aset diterima",
                        message = "Stok akan ditambahkan ke katalog proyek Anda dan status transfer menjadi SELESAI. Pastikan aset sudah diterima secara fisik.",
                        confirmLabel = "Ya, Terima",
                        confirmColor = GreenMain,
                        confirmIcon = Icons.Default.CheckCircle,
                        isProcessing = uiState is TransferUiState.Loading,
                        onDismiss = { showConfirm = false },
                        onConfirm = { viewModel.confirmTransfer(transfer) },
                        content = {
                            Column(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(InfoBg).border(1.5.dp, InfoBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                ConfirmInfoRow("Barang", namaBarang)
                                ConfirmInfoRow("Jumlah", "${transfer.jumlah} $satuan")
                                ConfirmInfoRow("Dari Proyek", transfer.proyek_asal)
                            }
                        }
                    )
                }

                // Guard pembatalan — stok dikembalikan ke proyek asal (status → DIBATALKAN)
                if (showBatal) {
                    ConfirmActionDialog(
                        icon = Icons.Default.Flag,
                        accent = RedMain,
                        accentBg = RedBg,
                        title = "Batalkan Transfer",
                        subtitle = "Aset dikembalikan",
                        message = "Stok akan dikembalikan ke proyek asal (${transfer.proyek_asal}) dan status transfer menjadi DIBATALKAN. Tindakan ini tidak bisa diurungkan.",
                        confirmLabel = "Ya, Batalkan",
                        confirmColor = RedMain,
                        confirmIcon = Icons.Default.Flag,
                        isProcessing = uiState is TransferUiState.Loading,
                        onDismiss = { showBatal = false },
                        onConfirm = { viewModel.cancelTransfer(transfer) },
                        content = {
                            Column(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(InfoBg).border(1.5.dp, InfoBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                ConfirmInfoRow("Barang", namaBarang)
                                ConfirmInfoRow("Jumlah", "${transfer.jumlah} $satuan")
                                ConfirmInfoRow("Kembali ke", transfer.proyek_asal)
                            }
                        }
                    )
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
