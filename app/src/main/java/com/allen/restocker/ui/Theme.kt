package com.allen.restocker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

// High-contrast light theme (every text/background pair is at least 7:1, i.e. WCAG AAA;
// borders and outlines are at least 4.5:1). Same page/card layout as the original PWA.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1E40AF),
    onPrimary = Color.White,
    secondary = Color(0xFF166534),
    onSecondary = Color.White,
    // Selected chips use this: dark blue with white text instead of a pale tint.
    secondaryContainer = Color(0xFF1E40AF),
    onSecondaryContainer = Color.White,
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF0B0F19),
    surface = Color.White,
    onSurface = Color(0xFF0B0F19),
    surfaceVariant = Color(0xFFE5EAF2),
    onSurfaceVariant = Color(0xFF1F2937),
    surfaceTint = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEDF0F6),
    surfaceContainerHighest = Color(0xFFE9EDF4),
    outline = Color(0xFF4B5563),
    outlineVariant = Color(0xFF6B7280),
    error = Color(0xFF991B1B),
    onError = Color.White
)

private val base = Typography()

// Bigger and heavier than the Material defaults.
private val BigTypography = Typography(
    bodyLarge = base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 17.sp, lineHeight = 24.sp),
    bodySmall = base.bodySmall.copy(fontSize = 15.sp, lineHeight = 21.sp),
    titleLarge = base.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    labelLarge = base.labelLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun RestockerTheme(textScale: Float = 1f, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        // Multiplies on top of the phone's own font-size setting.
        LocalDensity provides Density(density.density, density.fontScale * textScale)
    ) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = BigTypography,
            content = content
        )
    }
}
