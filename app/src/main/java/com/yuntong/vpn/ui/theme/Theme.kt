package com.yuntong.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = darkColorScheme(
    primary = Color(0xFF42A5F5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A237E),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF64FFDA),
    onSecondary = Color(0xFF00352E),
    secondaryContainer = Color(0xFF004D43),
    onSecondaryContainer = Color(0xFF64FFDA),
    tertiary = Color(0xFFBB86FC),
    background = Color(0xFF0D1117),
    onBackground = Color.White,
    surface = Color(0xFF161B22),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFF8B949E),
    error = Color(0xFFFF5252),
    onError = Color.White,
)

@Composable
fun YunTongVPNTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content
    )
}
