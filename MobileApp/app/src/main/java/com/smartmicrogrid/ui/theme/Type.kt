// ============================================================
// File: Type.kt
// Purpose: Material3 type scale mapped from docs/stripe.design.md's
//          {typography.*} tokens, scaled down to phone sizes (the doc
//          marks its px values as desktop anchors). Uses Inter (OFL,
//          bundled as a variable font in res/font) - the same family
//          WebApp's --font-sans stack leads with - instead of the
//          platform default, so both clients share one typeface.
//          Headlines are SemiBold with tight tracking; body is Regular
//          (Light read washed out on real phone screens). NumericStyle
//          uses tabular figures so stat numbers don't jitter.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.smartmicrogrid.R

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

val Typography = Typography(
    displaySmall = style(34, 40, FontWeight.SemiBold, -0.03),
    headlineLarge = style(30, 36, FontWeight.SemiBold, -0.025),
    headlineMedium = style(26, 32, FontWeight.SemiBold, -0.02),
    headlineSmall = style(22, 28, FontWeight.SemiBold, -0.015),
    titleLarge = style(19, 26, FontWeight.SemiBold, -0.01),
    titleMedium = style(16, 22, FontWeight.SemiBold, -0.005),
    titleSmall = style(14, 20, FontWeight.SemiBold, 0.0),
    bodyLarge = style(16, 24, FontWeight.Normal, 0.0),
    bodyMedium = style(14, 20, FontWeight.Normal, 0.0),
    bodySmall = style(13, 18, FontWeight.Normal, 0.0),
    labelLarge = style(15, 20, FontWeight.Medium, 0.0),
    labelMedium = style(13, 18, FontWeight.Medium, 0.005),
    labelSmall = style(11, 16, FontWeight.Medium, 0.02),
)

// Stat numbers: tabular figures ("tnum") so digits align and don't shift width while refreshing.
val NumericStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
    letterSpacing = (-0.02).em,
)
