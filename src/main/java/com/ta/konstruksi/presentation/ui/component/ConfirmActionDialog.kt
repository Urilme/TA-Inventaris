package com.ta.konstruksi.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ta.konstruksi.presentation.ui.theme.BorderInput
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.TextSubtle

/**
 * Dialog konfirmasi (are-you-sure) yang dipakai bersama sebelum aksi berdampak:
 * kirim/terima transfer, keluar akun, tambah katalog. Gayanya mengikuti
 * `MutasiConfirmDialog` agar seragam: kartu putih sudut 22dp, ikon bulat beraksen,
 * tombol Batal (outline) + tombol konfirmasi (terisi warna).
 *
 * @param content slot opsional untuk detail kaya (mis. kartu barang + perubahan stok),
 *                dirender di antara header dan pesan.
 * @param isProcessing saat true tombol dinonaktifkan dan tombol konfirmasi menampilkan spinner
 *                     — mencegah ketukan ganda selagi aksi diproses.
 */
@Composable
fun ConfirmActionDialog(
    icon: ImageVector,
    accent: Color,
    accentBg: Color,
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    subtitle: String? = null,
    confirmColor: Color = Navy,
    confirmIcon: ImageVector = Icons.Default.Check,
    isProcessing: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(22.dp)
        ) {
            // Header: ikon bulat beraksen + judul (+ subjudul beraksen)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy)
                    if (subtitle != null) {
                        Text(subtitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent)
                    }
                }
            }

            if (content != null) {
                Spacer(Modifier.height(18.dp))
                content()
            }

            Spacer(Modifier.height(16.dp))

            Text(message, fontSize = 13.sp, lineHeight = 20.sp, color = TextSubtle)

            Spacer(Modifier.height(20.dp))

            // Tombol: Batal (outline) + konfirmasi (terisi)
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, BorderInput, RoundedCornerShape(14.dp))
                        .clickable(enabled = !isProcessing, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Batal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (isProcessing) confirmColor.copy(alpha = 0.6f) else confirmColor)
                        .clickable(enabled = !isProcessing, onClick = onConfirm),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(confirmIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(confirmLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Baris ringkas untuk isi kartu detail di dalam [ConfirmActionDialog].
 * Bertumpuk (label di atas, nilai di bawah, rata kiri) agar nilai panjang — nama barang
 * atau nama proyek — membungkus rapi tanpa berdesakan di ruang dialog yang sempit.
 */
@Composable
fun ConfirmInfoRow(label: String, value: String, valueColor: Color = Navy) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, fontSize = 12.sp, color = TextSubtle)
        Spacer(Modifier.height(3.dp))
        Text(
            value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor,
            lineHeight = 20.sp
        )
    }
}
