package com.michel.deutschmacht.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green800 = Color(0xFF1B4D3E)
val Green600 = Color(0xFF2E7D5B)
val Green100 = Color(0xFFD8EFE3)
val Amber = Color(0xFFF2A94C)
val Cream = Color(0xFFFAF6EE)
val Ink = Color(0xFF1F2421)

private val LightColors = lightColorScheme(
    primary = Green600,
    onPrimary = Color.White,
    primaryContainer = Green100,
    onPrimaryContainer = Green800,
    secondary = Amber,
    onSecondary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Green100,
    onSurfaceVariant = Color(0xFF44514B),
)

private val DarkColors = darkColorScheme(
    primary = Green100,
    onPrimary = Green800,
    primaryContainer = Green800,
    onPrimaryContainer = Green100,
    secondary = Amber,
    background = Color(0xFF101412),
    onBackground = Color(0xFFE6E2D8),
    surface = Color(0xFF181D1A),
    onSurface = Color(0xFFE6E2D8),
    surfaceVariant = Color(0xFF24302A),
    onSurfaceVariant = Color(0xFFB7C4BD),
)

@Composable
fun DeutschMachtTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
