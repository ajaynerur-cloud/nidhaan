package com.nidhaan.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EEE9),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3C2),
    onSecondaryContainer = Color(0xFF2B1700),
    background = Color(0xFFF4FAF9),
    onBackground = Color(0xFF111D1C),
    surface = Color.White,
    onSurface = Color(0xFF111D1C),
    surfaceVariant = Color(0xFFE3EFED),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DB6A6),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFD4EEE9),
    secondary = Color(0xFFF5A33B),
    onSecondary = Color(0xFF2B1700),
    secondaryContainer = Color(0xFF5C3A00),
    onSecondaryContainer = Color(0xFFFFE3C2),
    background = Color(0xFF0F1716),
    onBackground = Color(0xFFDDE5E3),
    surface = Color(0xFF172120),
    onSurface = Color(0xFFDDE5E3),
    surfaceVariant = Color(0xFF263230),
    onSurfaceVariant = Color(0xFFBFC9C7),
    outline = Color(0xFF899391)
)

@Composable
fun NidhaanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
