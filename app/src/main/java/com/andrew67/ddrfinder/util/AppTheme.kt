package com.andrew67.ddrfinder.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** From `colors.xml` */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF005FAF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E3FF),
    onPrimaryContainer = Color(0xFF001C3A),
    secondary = Color(0xFF9A25AE),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD6FE),
    onSecondaryContainer = Color(0xFF35003F),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceTint = Color(0xFF005FAF),
    error = Color(0xFFBA1A1A),
)

/** From `colors.xml` */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5C8FF),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF004786),
    onPrimaryContainer = Color(0xFFD4E3FF),
    secondary = Color(0xFFF9ABFF),
    onSecondary = Color(0xFF570066),
    secondaryContainer = Color(0xFF7B008F),
    onSecondaryContainer = Color(0xFFFFD6FE),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceTint = Color(0xFFA5C8FF),
    error = Color(0xFFFFB4AB),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
