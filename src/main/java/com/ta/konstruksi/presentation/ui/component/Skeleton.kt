package com.ta.konstruksi.presentation.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.ta.konstruksi.presentation.ui.theme.BorderLight

/**
 * Placeholder skeleton dengan efek shimmer (kilau bergerak) — pengganti spinner saat
 * daftar/halaman dimuat dari Room. Bentuk skeleton meniru layout konten aslinya agar
 * transisi ke konten terasa mulus.
 */

private val SkeletonBase = Color(0xFFE3E8EC)
private val SkeletonHighlight = Color(0xFFF3F6F8)

/** Brush shimmer beranimasi tak-hingga; buat sekali per layar lalu bagikan ke semua kotak. */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -350f,
        targetValue = 350f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-x"
    )
    return Brush.linearGradient(
        colors = listOf(SkeletonBase, SkeletonHighlight, SkeletonBase),
        start = Offset(x, 0f),
        end = Offset(x + 260f, 0f)
    )
}

/** Satu blok skeleton. */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    brush: Brush,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(modifier = modifier.clip(shape).background(brush))
}

/** Skeleton konten Dashboard: dua kartu statistik, aksi cepat, dan daftar stok. */
@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.padding(16.dp)) {
        // Dua kartu statistik
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.weight(1f).height(150.dp), brush, RoundedCornerShape(16.dp))
            SkeletonBox(Modifier.weight(1f).height(150.dp), brush, RoundedCornerShape(16.dp))
        }
        Spacer(Modifier.height(22.dp))
        // Aksi cepat
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBox(Modifier.weight(1f).height(48.dp), brush, RoundedCornerShape(12.dp))
            SkeletonBox(Modifier.weight(1f).height(48.dp), brush, RoundedCornerShape(12.dp))
        }
        Spacer(Modifier.height(22.dp))
        // Judul bagian
        SkeletonBox(Modifier.width(180.dp).height(18.dp), brush)
        Spacer(Modifier.height(14.dp))
        // Kartu daftar
        repeat(3) {
            SkeletonBox(Modifier.fillMaxWidth().height(64.dp), brush, RoundedCornerShape(14.dp))
            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * Skeleton daftar (Katalog & Riwayat): beberapa baris kartu berisi ikon, dua garis teks,
 * dan blok nilai di kanan.
 */
@Composable
fun ListSkeleton(modifier: Modifier = Modifier, rows: Int = 6) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        repeat(rows) {
            ListRowSkeleton(brush)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ListRowSkeleton(brush: Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, BorderLight, RoundedCornerShape(14.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBox(Modifier.size(44.dp), brush, RoundedCornerShape(10.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(Modifier.width(150.dp).height(14.dp), brush)
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.width(90.dp).height(11.dp), brush)
        }
        Spacer(Modifier.width(12.dp))
        SkeletonBox(Modifier.width(60.dp).height(20.dp), brush, RoundedCornerShape(10.dp))
    }
}
