package com.example.chess_app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary         = AccentGold,
    onPrimary       = Color(0xFF1A1A1A),
    primaryContainer = SurfaceVariant,
    secondary       = AccentGoldLight,
    background      = BackgroundDark,
    surface         = SurfaceDark,
    onBackground    = OnSurface,
    onSurface       = OnSurface,
    error           = ErrorRed,
    outline         = OnSurfaceVariant
)

@Composable
fun ChessAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}