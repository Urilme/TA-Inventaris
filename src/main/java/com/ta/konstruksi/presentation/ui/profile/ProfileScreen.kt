package com.ta.konstruksi.presentation.ui.profile

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderLight
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedBorder
import com.ta.konstruksi.presentation.ui.theme.RedDark
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextSecondary
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session = state.session

    val context = LocalContext.current
    val versi = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "-"
    }

    val nama = session?.nama?.takeIf { it.isNotBlank() } ?: "Mandor"
    val inisial = nama.trim().split(" ").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }.ifBlank { "M" }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Navy + identitas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy)
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                    Text("Profil", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(inisial, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Navy)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(nama, fontSize = 21.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            (session?.role ?: "mandor").replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Color.White
                        )
                    }
                }
            }

            // Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                InfoCard(title = "Proyek Aktif") {
                    InfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "Nama proyek",
                        value = session?.nama_proyek?.takeIf { it.isNotBlank() } ?: "Belum ditetapkan"
                    )
                    InfoRow(
                        icon = Icons.Default.Tag,
                        label = "ID proyek",
                        value = session?.id_proyek?.takeIf { it.isNotBlank() } ?: "-"
                    )
                    Text(
                        "Penugasan proyek dikelola oleh admin.",
                        fontSize = 11.sp, color = TextSubtle,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                InfoCard(title = "Akun") {
                    InfoRow(icon = Icons.Default.Email, label = "Email", value = session?.email ?: "-")
                    InfoRow(icon = Icons.Default.Badge, label = "ID mandor", value = session?.id_mandor ?: "-")
                }

                Spacer(Modifier.height(14.dp))

                InfoCard(title = "Sinkronisasi") {
                    InfoRow(
                        icon = Icons.Default.Sync,
                        label = "Sinkron terakhir",
                        value = formatWaktuSync(session?.last_sync_timestamp ?: 0L)
                    )
                    InfoRow(
                        icon = Icons.Default.HourglassEmpty,
                        label = "Menunggu sinkron",
                        value = "${state.pendingCount} item"
                    )
                }

                Spacer(Modifier.height(14.dp))

                InfoCard(title = "Aplikasi") {
                    InfoRow(icon = Icons.Default.Info, label = "Versi", value = versi)
                }

                Spacer(Modifier.height(24.dp))

                // Logout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.5.dp, RedBorder, RoundedCornerShape(14.dp))
                        .clickable { viewModel.logout(onLoggedOut) }
                        .padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = RedMain, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Keluar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RedDark)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, BorderLight, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, color = TextSubtle)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(120.dp))
        Text(
            value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Format epoch millis (server_timestamp) → tanggal ringkas berbahasa Indonesia; 0 → belum pernah. */
private fun formatWaktuSync(millis: Long): String =
    if (millis <= 0L) "Belum pernah"
    else SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(millis))
