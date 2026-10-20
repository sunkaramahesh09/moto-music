package com.motomusic.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The "Sonic Glass" palette: neutral charcoal and parchment grounds that let album art supply
 * the colour, with a single Action Blue accent for anything interactive.
 *
 * Used unless the user opts into colours from their wallpaper (Android 12+). Every surface is
 * achromatic on purpose; nothing here is a gradient.
 */
internal object MotoColors {
    // Light: museum-gallery parchment with white cards.
    val LightPrimary = Color(0xFF0066CC)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFEBF4FF)
    val LightOnPrimaryContainer = Color(0xFF00315C)
    val LightSecondary = Color(0xFF0071E3)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightSecondaryContainer = Color(0xFFDCEBFF)
    val LightOnSecondaryContainer = Color(0xFF002F66)
    val LightTertiary = Color(0xFFE0254A)
    val LightOnTertiary = Color(0xFFFFFFFF)
    val LightTertiaryContainer = Color(0xFFFFE4E9)
    val LightOnTertiaryContainer = Color(0xFF5C0016)
    val LightError = Color(0xFFD70015)
    val LightOnError = Color(0xFFFFFFFF)
    val LightErrorContainer = Color(0xFFFFDAD6)
    val LightOnErrorContainer = Color(0xFF410002)
    val LightBackground = Color(0xFFF5F5F7)
    val LightOnBackground = Color(0xFF1D1D1F)
    val LightSurface = Color(0xFFF5F5F7)
    val LightOnSurface = Color(0xFF1D1D1F)
    val LightSurfaceVariant = Color(0xFFE5E5EA)
    val LightOnSurfaceVariant = Color(0xFF6E6E73)
    val LightOutline = Color(0xFFA1A1A6)
    val LightOutlineVariant = Color(0xFFE0E0E5)
    val LightInverseSurface = Color(0xFF1D1D1F)
    val LightInverseOnSurface = Color(0xFFF5F5F7)
    val LightInversePrimary = Color(0xFF2997FF)
    val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
    val LightSurfaceContainerLow = Color(0xFFFFFFFF)
    val LightSurfaceContainer = Color(0xFFFFFFFF)
    val LightSurfaceContainerHigh = Color(0xFFEBEBF0)
    val LightSurfaceContainerHighest = Color(0xFFE5E5EA)

    // Dark: deep charcoal rather than pure black, so cards still read as raised.
    val DarkPrimary = Color(0xFF2997FF)
    val DarkOnPrimary = Color(0xFFFFFFFF)
    val DarkPrimaryContainer = Color(0xFF0B3A66)
    val DarkOnPrimaryContainer = Color(0xFFD3E3FF)
    val DarkSecondary = Color(0xFF64B0FF)
    val DarkOnSecondary = Color(0xFF002F66)
    val DarkSecondaryContainer = Color(0xFF14304F)
    val DarkOnSecondaryContainer = Color(0xFFD3E3FF)
    val DarkTertiary = Color(0xFFFF2D55)
    val DarkOnTertiary = Color(0xFFFFFFFF)
    val DarkTertiaryContainer = Color(0xFF4A1020)
    val DarkOnTertiaryContainer = Color(0xFFFFD9DF)
    val DarkError = Color(0xFFFF6961)
    val DarkOnError = Color(0xFF690005)
    val DarkErrorContainer = Color(0xFF93000A)
    val DarkOnErrorContainer = Color(0xFFFFDAD6)
    val DarkBackground = Color(0xFF121214)
    val DarkOnBackground = Color(0xFFF5F5F7)
    val DarkSurface = Color(0xFF121214)
    val DarkOnSurface = Color(0xFFF5F5F7)
    val DarkSurfaceVariant = Color(0xFF2C2C2E)
    val DarkOnSurfaceVariant = Color(0xFF9898A0)
    val DarkOutline = Color(0xFF636366)
    val DarkOutlineVariant = Color(0xFF2A2A2D)
    val DarkInverseSurface = Color(0xFFF5F5F7)
    val DarkInverseOnSurface = Color(0xFF1D1D1F)
    val DarkInversePrimary = Color(0xFF0066CC)
    val DarkSurfaceContainerLowest = Color(0xFF0E0E10)
    val DarkSurfaceContainerLow = Color(0xFF161618)
    val DarkSurfaceContainer = Color(0xFF1B1B1D)
    val DarkSurfaceContainerHigh = Color(0xFF252528)
    val DarkSurfaceContainerHighest = Color(0xFF2C2C2E)

    /** Reserved for favourites: hearts and "loved" badges, in both themes. */
    val Heart = Color(0xFFFF2D55)
}
