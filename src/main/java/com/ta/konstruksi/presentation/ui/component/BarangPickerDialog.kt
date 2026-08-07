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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderInput
import com.ta.konstruksi.presentation.ui.theme.BorderLight
import com.ta.konstruksi.presentation.ui.theme.InfoBg
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.util.KategoriBarang

/**
 * Pemilih barang bersama: pengguna dapat SEKALIGUS mengetik pencarian (nama/kategori) dan
 * menggulir (scroll) daftar hasil. Dipakai di form Mutasi maupun Transfer agar pengalaman
 * mencari barang konsisten. Daftar [items] biasanya sudah difilter per kategori oleh pemanggil.
 */
@Composable
fun BarangPickerDialog(
    items: List<MasterKatalog>,
    onDismiss: () -> Unit,
    onSelect: (MasterKatalog) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val shown = items.filter {
        query.isBlank() || it.nama_barang.contains(query, true) ||
            it.kategori.contains(query, true)
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(InfoBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, tint = Navy, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pilih Barang", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy)
                    Text("${items.size} barang tersedia", fontSize = 12.sp, color = TextSubtle)
                }
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(AppBackground)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSubtle, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Search — bergaya app (putih, sudut membulat, border, ikon cari)
            Row(
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp))
                    .background(AppBackground).border(1.5.dp, BorderInput, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.weight(1f), singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = Navy),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Cari nama atau kategori…", fontSize = 15.sp, color = TextSubtle)
                        inner()
                    }
                )
            }

            Spacer(Modifier.height(14.dp))

            if (shown.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (items.isEmpty()) "Belum ada barang pada kategori ini." else "Tidak ada hasil untuk \"$query\".",
                        fontSize = 13.sp, color = TextSubtle, textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color.White).border(1.5.dp, BorderLight, RoundedCornerShape(12.dp))
                                .clickable { onSelect(item) }.padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFeef1f3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.nama_barang, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                                Text(KategoriBarang.label(item.kategori), fontSize = 11.sp, color = TextSubtle)
                            }
                            Text(
                                "${item.stok_lokal} ${item.satuan}",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = if (item.stok_lokal <= 5) RedMain else Navy
                            )
                        }
                    }
                }
            }
        }
    }
}
