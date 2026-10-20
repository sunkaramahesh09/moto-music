package com.motomusic.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.motomusic.app.R

/**
 * Inter, bundled as one variable font (SIL Open Font License, see assets/licenses). It is the
 * closest open match for the San Francisco look the design is built around.
 */
private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.em,
)

/**
 * Headlines are set tight, the way editorial media apps set them; small labels are opened up
 * so they stay legible at caption size. Sizes stay in `sp` so the system font-size setting
 * still scales the whole app.
 */
val MotoTypography = Typography(
    displayLarge = style(48, 56, FontWeight.Bold, -0.025),
    displayMedium = style(40, 48, FontWeight.Bold, -0.025),
    displaySmall = style(34, 41, FontWeight.Bold, -0.025),
    headlineLarge = style(32, 38, FontWeight.Bold, -0.0125),
    headlineMedium = style(28, 34, FontWeight.Bold, -0.02),
    headlineSmall = style(24, 30, FontWeight.Bold, -0.02),
    titleLarge = style(20, 25, FontWeight.SemiBold, -0.01),
    titleMedium = style(17, 22, FontWeight.SemiBold, -0.005),
    titleSmall = style(15, 20, FontWeight.SemiBold, -0.005),
    bodyLarge = style(16, 21, FontWeight.Normal, -0.01),
    bodyMedium = style(14, 19, FontWeight.Normal, -0.01),
    bodySmall = style(13, 18, FontWeight.Normal, 0.0),
    labelLarge = style(14, 18, FontWeight.SemiBold, 0.0),
    labelMedium = style(13, 18, FontWeight.Medium, 0.01),
    labelSmall = style(11, 14, FontWeight.SemiBold, 0.04),
)

/** Figures that keep the same width, so a ticking clock never makes its neighbours jump. */
val TabularFigures: TextStyle = TextStyle(fontFeatureSettings = "tnum")
