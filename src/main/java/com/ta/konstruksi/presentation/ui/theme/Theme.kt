package com.ta.konstruksi.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Navy,
    onPrimaryContainer = Color.White,
    secondary = AmberMain,
    onSecondary = Color.White,
    secondaryContainer = AmberBg,
    onSecondaryContainer = AmberText,
    tertiary = GreenMain,
    onTertiary = Color.White,
    background = AppBackground,
    onBackground = Navy,
    surface = AppSurface,
    onSurface = Navy,
    surfaceVariant = InfoBg,
    onSurfaceVariant = TextSecondary,
    error = RedMain,
    onError = Color.White,
    errorContainer = RedBg,
    onErrorContainer = RedDark,
    outline = BorderInput,
    outlineVariant = BorderLight,
)

@Composable
fun KonstruksiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
