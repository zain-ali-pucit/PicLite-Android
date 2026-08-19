package com.axain.photocompressor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E6FF),
    onPrimaryContainer = VioletDeep,
    secondary = Orchid,
    onSecondary = Color.White,
    tertiary = Mint,
    onTertiary = Color.White,
    background = Canvas,
    onBackground = Ink,
    surface = CanvasElevated,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFEDF7),
    onSurfaceVariant = InkSoft,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Rose,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E2A55),
    onPrimaryContainer = Color(0xFFCFC9FF),
    secondary = Orchid,
    onSecondary = Color.White,
    tertiary = Mint,
    onTertiary = Color(0xFF06231C),
    background = NightCanvas,
    onBackground = NightInk,
    surface = NightElevated,
    onSurface = NightInk,
    surfaceVariant = Color(0xFF23223A),
    onSurfaceVariant = NightInkSoft,
    outline = NightHairline,
    outlineVariant = NightHairline,
    error = Rose,
    onError = Color.White
)

/** Ambient gradients used across the app, adapted to the current theme. */
data class BrandGradients(
    val hero: Brush,
    val heroSoft: Brush,
    val accentA: Brush,
    val accentB: Brush,
    val accentC: Brush,
    val accentD: Brush
)

@Composable
fun rememberBrandGradients(dark: Boolean): BrandGradients = BrandGradients(
    hero = Brush.linearGradient(listOf(Violet, Orchid)),
    heroSoft = Brush.verticalGradient(
        if (dark) listOf(Color(0xFF1C1836), NightCanvas)
        else listOf(Color(0xFFEDE9FF), Canvas)
    ),
    accentA = Brush.linearGradient(listOf(Violet, Sky)),
    accentB = Brush.linearGradient(listOf(Orchid, Rose)),
    accentC = Brush.linearGradient(listOf(Mint, Sky)),
    accentD = Brush.linearGradient(listOf(Amber, Rose))
)

@Composable
fun PhotoCompressorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
