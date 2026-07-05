package com.ta.konstruksi.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.TextSubtle

/**
 * Indikator memuat bergaya aplikasi — spinner Navy dengan label opsional.
 * Dipakai saat halaman/daftar sedang dimuat dari Room.
 */
@Composable
fun AppLoading(
    modifier: Modifier = Modifier,
    label: String? = "Memuat…"
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Navy,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp)
        )
        if (!label.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(label, fontSize = 13.sp, color = TextSubtle)
        }
    }
}
