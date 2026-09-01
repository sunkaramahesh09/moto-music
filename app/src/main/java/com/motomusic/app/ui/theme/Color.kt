package com.motomusic.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Fallback palette used when the device cannot supply a dynamic colour scheme
 * (Android 11 and older, or when the user turns dynamic colour off).
 *
 * Contrast ratios were chosen to stay readable in both themes; nothing here is a gradient.
 */
internal object MotoColors {
    // Light
    val LightPrimary = Color(0xFF4B57C8)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFE0E0FF)
    val LightOnPrimaryContainer = Color(0xFF000E5E)
    val LightSecondary = Color(0xFF5B5D72)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightSecondaryContainer = Color(0xFFE0E0F9)
    val LightOnSecondaryContainer = Color(0xFF181A2C)
    val LightTertiary = Color(0xFF77536D)
    val LightOnTertiary = Color(0xFFFFFFFF)
    val LightTertiaryContainer = Color(0xFFFFD7F0)
    val LightOnTertiaryContainer = Color(0xFF2D1228)
    val LightError = Color(0xFFBA1A1A)
    val LightOnError = Color(0xFFFFFFFF)
    val LightErrorContainer = Color(0xFFFFDAD6)
    val LightOnErrorContainer = Color(0xFF410002)
    val LightBackground = Color(0xFFFBF8FF)
    val LightOnBackground = Color(0xFF1B1B21)
    val LightSurface = Color(0xFFFBF8FF)
    val LightOnSurface = Color(0xFF1B1B21)
    val LightSurfaceVariant = Color(0xFFE3E1EC)
    val LightOnSurfaceVariant = Color(0xFF46464F)
    val LightOutline = Color(0xFF777680)
    val LightOutlineVariant = Color(0xFFC7C5D0)
    val LightInverseSurface = Color(0xFF303036)
    val LightInverseOnSurface = Color(0xFFF2EFF7)
    val LightInversePrimary = Color(0xFFBFC2FF)

    // Dark
    val DarkPrimary = Color(0xFFBFC2FF)
    val DarkOnPrimary = Color(0xFF142297)
    val DarkPrimaryContainer = Color(0xFF313DAF)
    val DarkOnPrimaryContainer = Color(0xFFE0E0FF)
    val DarkSecondary = Color(0xFFC4C4DD)
    val DarkOnSecondary = Color(0xFF2D2F42)
    val DarkSecondaryContainer = Color(0xFF434559)
    val DarkOnSecondaryContainer = Color(0xFFE0E0F9)
    val DarkTertiary = Color(0xFFE6BAD7)
    val DarkOnTertiary = Color(0xFF45263D)
    val DarkTertiaryContainer = Color(0xFF5E3B55)
    val DarkOnTertiaryContainer = Color(0xFFFFD7F0)
    val DarkError = Color(0xFFFFB4AB)
    val DarkOnError = Color(0xFF690005)
    val DarkErrorContainer = Color(0xFF93000A)
    val DarkOnErrorContainer = Color(0xFFFFDAD6)
    val DarkBackground = Color(0xFF131318)
    val DarkOnBackground = Color(0xFFE4E1E9)
    val DarkSurface = Color(0xFF131318)
    val DarkOnSurface = Color(0xFFE4E1E9)
    val DarkSurfaceVariant = Color(0xFF46464F)
    val DarkOnSurfaceVariant = Color(0xFFC7C5D0)
    val DarkOutline = Color(0xFF918F9A)
    val DarkOutlineVariant = Color(0xFF46464F)
    val DarkInverseSurface = Color(0xFFE4E1E9)
    val DarkInverseOnSurface = Color(0xFF303036)
    val DarkInversePrimary = Color(0xFF4B57C8)
}
