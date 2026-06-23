package com.gps.dashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Accent,
    background = Background,
    surface = Surface,
    onPrimary = Background,
    onSecondary = Background,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun GpsDashboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
