package com.allen.restocker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Always light, same palette as the original PWA (page #f4f6f9, white cards, blue #2563eb).
private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFF16A34A),
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF1F4F9),
    onSurfaceVariant = Color(0xFF374151),
    surfaceTint = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F4F9),
    surfaceContainerHighest = Color(0xFFE5E7EB),
    outline = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFB91C1C)
)

@Composable
fun RestockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
