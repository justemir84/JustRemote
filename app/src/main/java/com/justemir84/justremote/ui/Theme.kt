package com.justemir84.justremote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF050505)
val Panel = Color(0xFF0C0C0C)
val Line = Color(0xFF232323)
val Fg = Color(0xFFF4F4F4)
val Muted = Color(0xFF8C8C8C)
val Accent = Color(0xFFFFFFFF)

private val MonoColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Fg,
    secondary = Muted,
    onSecondary = Color.Black,
    background = Ink,
    onBackground = Fg,
    surface = Panel,
    onSurface = Fg,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Color(0xFF1A1A1A),
    error = Color(0xFFFF7A7A),
    onError = Color.Black,
)

private val MonoType = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(letterSpacing = 8.sp, fontWeight = FontWeight.Light),
        headlineSmall = headlineSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Light),
        titleLarge = titleLarge.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium),
        labelSmall = labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
fun JustRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonoColors,
        typography = MonoType,
        content = content,
    )
}
