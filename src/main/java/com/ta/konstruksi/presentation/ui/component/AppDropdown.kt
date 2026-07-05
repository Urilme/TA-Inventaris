package com.ta.konstruksi.presentation.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ta.konstruksi.presentation.ui.theme.BorderLight
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.TextPrimary

/**
 * Menu dropdown bergaya aplikasi (bukan tampilan Material bawaan):
 * latar putih, sudut membulat 14dp, border tipis, dan bayangan halus.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(14.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.5.dp, BorderLight),
        modifier = modifier,
        content = content
    )
}

/**
 * Item dropdown bergaya aplikasi: teks Navy, item terpilih ditebalkan + ikon centang.
 */
@Composable
fun AppDropdownItem(
    text: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Navy else TextPrimary
            )
        },
        trailingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp)) }
        } else null,
        enabled = enabled,
        onClick = onClick,
        colors = MenuDefaults.itemColors(
            textColor = Navy,
            trailingIconColor = Navy
        )
    )
}
