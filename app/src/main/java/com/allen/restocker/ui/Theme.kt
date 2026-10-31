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

// White and blue only. Page and cards are pure white, accents are blue, text is deep navy.
// Every text/background pair is at least 7:1 (WCAG AAA); borders are at least 3:1.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1E40AF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FF),        // light blue tint for "has quantity" cards
    onPrimaryContainer = Color(0xFF0B1F5C),
    inversePrimary = Color(0xFF93C5FD),
    secondary = Color(0xFF1E40AF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E40AF),       // selected chips: solid blue, white text
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF1D4ED8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F0FF),
    onTertiaryContainer = Color(0xFF0B1F5C),
    background = Color.White,
    onBackground = Color(0xFF0A1A44),
    surface = Color.White,
    onSurface = Color(0xFF0A1A44),
    surfaceVariant = Color(0xFFEAF1FF),
    onSurfaceVariant = Color(0xFF3B4A6B),         // muted text
    surfaceTint = Color.White,
    inverseSurface = Color(0xFF0A1A44),
    inverseOnSurface = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEAF1FF),
    surfaceContainerHighest = Color(0xFFEEF4FF),
    outline = Color(0xFF2F4A8F),                   // input and button borders
    outlineVariant = Color(0xFF6F8FD8),            // card borders and dividers
    error = Color(0xFF991B1B),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val base = Typography()

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
