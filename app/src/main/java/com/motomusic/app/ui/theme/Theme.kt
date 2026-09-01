package com.motomusic.app.ui.theme

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.motomusic.app.domain.model.ThemeMode

private val LightScheme = lightColorScheme(
    primary = MotoColors.LightPrimary,
    onPrimary = MotoColors.LightOnPrimary,
    primaryContainer = MotoColors.LightPrimaryContainer,
    onPrimaryContainer = MotoColors.LightOnPrimaryContainer,
    secondary = MotoColors.LightSecondary,
    onSecondary = MotoColors.LightOnSecondary,
    secondaryContainer = MotoColors.LightSecondaryContainer,
    onSecondaryContainer = MotoColors.LightOnSecondaryContainer,
    tertiary = MotoColors.LightTertiary,
    onTertiary = MotoColors.LightOnTertiary,
    tertiaryContainer = MotoColors.LightTertiaryContainer,
    onTertiaryContainer = MotoColors.LightOnTertiaryContainer,
    error = MotoColors.LightError,
    onError = MotoColors.LightOnError,
    errorContainer = MotoColors.LightErrorContainer,
    onErrorContainer = MotoColors.LightOnErrorContainer,
    background = MotoColors.LightBackground,
    onBackground = MotoColors.LightOnBackground,
    surface = MotoColors.LightSurface,
    onSurface = MotoColors.LightOnSurface,
    surfaceVariant = MotoColors.LightSurfaceVariant,
    onSurfaceVariant = MotoColors.LightOnSurfaceVariant,
    outline = MotoColors.LightOutline,
    outlineVariant = MotoColors.LightOutlineVariant,
    inverseSurface = MotoColors.LightInverseSurface,
    inverseOnSurface = MotoColors.LightInverseOnSurface,
    inversePrimary = MotoColors.LightInversePrimary,
)

private val DarkScheme = darkColorScheme(
    primary = MotoColors.DarkPrimary,
    onPrimary = MotoColors.DarkOnPrimary,
    primaryContainer = MotoColors.DarkPrimaryContainer,
    onPrimaryContainer = MotoColors.DarkOnPrimaryContainer,
    secondary = MotoColors.DarkSecondary,
    onSecondary = MotoColors.DarkOnSecondary,
    secondaryContainer = MotoColors.DarkSecondaryContainer,
    onSecondaryContainer = MotoColors.DarkOnSecondaryContainer,
    tertiary = MotoColors.DarkTertiary,
    onTertiary = MotoColors.DarkOnTertiary,
    tertiaryContainer = MotoColors.DarkTertiaryContainer,
    onTertiaryContainer = MotoColors.DarkOnTertiaryContainer,
    error = MotoColors.DarkError,
    onError = MotoColors.DarkOnError,
    errorContainer = MotoColors.DarkErrorContainer,
    onErrorContainer = MotoColors.DarkOnErrorContainer,
    background = MotoColors.DarkBackground,
    onBackground = MotoColors.DarkOnBackground,
    surface = MotoColors.DarkSurface,
    onSurface = MotoColors.DarkOnSurface,
    surfaceVariant = MotoColors.DarkSurfaceVariant,
    onSurfaceVariant = MotoColors.DarkOnSurfaceVariant,
    outline = MotoColors.DarkOutline,
    outlineVariant = MotoColors.DarkOutlineVariant,
    inverseSurface = MotoColors.DarkInverseSurface,
    inverseOnSurface = MotoColors.DarkInverseOnSurface,
    inversePrimary = MotoColors.DarkInversePrimary,
)

/** True when dynamic colour is available on this device. */
val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun MotoMusicTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme: ColorScheme = when {
        useDynamicColor && supportsDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    // The activity draws edge to edge, so the status and navigation bars show the app's own
    // background. Their icons have to be told which way to flip, or a light theme leaves white
    // icons on a white bar.
    val view = LocalView.current
    val activity = LocalActivity.current
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MotoTypography,
        content = content,
    )
}
