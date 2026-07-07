package com.ta.konstruksi.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ta.konstruksi.presentation.ui.theme.AmberBg
import com.ta.konstruksi.presentation.ui.theme.AmberBorder
import com.ta.konstruksi.presentation.ui.theme.AmberMain
import com.ta.konstruksi.presentation.ui.theme.AmberText
import com.ta.konstruksi.presentation.ui.theme.GreenBg
import com.ta.konstruksi.presentation.ui.theme.GreenBorder
import com.ta.konstruksi.presentation.ui.theme.GreenBright
import com.ta.konstruksi.presentation.ui.theme.GreenText

@Composable
fun NetworkBadge(
    isOnline: Boolean,
    onDarkBg: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isOnline) GreenBright else AmberMain
    val bgColor = if (onDarkBg) {
        if (isOnline) Color(0x28FFFFFF) else Color(0x1AFFFFFF)
    } else {
        if (isOnline) GreenBg else AmberBg
    }
    val borderColor = if (onDarkBg) {
        if (isOnline) Color(0x595fe39a) else Color(0x2DFFFFFF)
    } else {
        if (isOnline) GreenBorder else AmberBorder
    }
    val textColor = if (onDarkBg) Color.White else if (isOnline) GreenText else AmberText
    val label = if (isOnline) "ONLINE" else "OFFLINE"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = textColor
        )
    }
}
