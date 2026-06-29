package com.ta.konstruksi.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ta.konstruksi.presentation.ui.theme.AmberBg
import com.ta.konstruksi.presentation.ui.theme.AmberBorder
import com.ta.konstruksi.presentation.ui.theme.AmberMain
import com.ta.konstruksi.presentation.ui.theme.AmberText
import com.ta.konstruksi.presentation.ui.theme.GreenBg
import com.ta.konstruksi.presentation.ui.theme.GreenBorder
import com.ta.konstruksi.presentation.ui.theme.GreenMain
import com.ta.konstruksi.presentation.ui.theme.GreenText
import com.ta.konstruksi.util.SyncStatus

@Composable
fun SyncStatusBadge(status: SyncStatus, modifier: Modifier = Modifier) {
    val (icon, label, bg, border, textColor, iconColor) = when (status) {
        SyncStatus.PENDING -> SyncBadgeStyle(
            Icons.Default.HourglassEmpty, "MENUNGGU SINKRONISASI",
            AmberBg, AmberBorder, AmberText, AmberMain
        )
        SyncStatus.IN_TRANSIT -> SyncBadgeStyle(
            Icons.Default.LocalShipping, "DALAM PERJALANAN",
            AmberBg, AmberBorder, AmberText, AmberMain
        )
        SyncStatus.SYNCED -> SyncBadgeStyle(
            Icons.Default.CheckCircle, "TERSINKRONISASI",
            GreenBg, GreenBorder, GreenText, GreenMain
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = iconColor)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = textColor)
    }
}

private data class SyncBadgeStyle(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val bg: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color,
    val iconColor: androidx.compose.ui.graphics.Color
)
