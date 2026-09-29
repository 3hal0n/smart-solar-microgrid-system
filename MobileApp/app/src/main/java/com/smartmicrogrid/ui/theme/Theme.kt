// ============================================================
// File: Theme.kt
// Purpose: Builds the app's Material3 ColorScheme/Shapes from the
//          Stripe design tokens in Color.kt, per docs/stripe.design.md
//          - the same design system WebApp uses. Light-only: an ERP-
//          style tool doesn't get a dark background, so this always
//          builds LightColorScheme regardless of the system theme
//          (2026-09-28, explicit user direction - see the removed
//          DarkColorScheme's former call site for history). Shapes
//          .medium (Card's default shape) is set to 8dp per
//          {rounded.lg} instead of M3's default 12dp.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = StripePrimary,
    onPrimary = StripeOnPrimary,
    primaryContainer = StripeBrandVioletSoft,
    onPrimaryContainer = StripeInk,
    secondary = StripeAccent,
    onSecondary = StripeOnPrimary,
    tertiary = StripeSuccess,
    onTertiary = StripeOnPrimary,
    tertiaryContainer = StripeSuccessContainer,
    onTertiaryContainer = StripeOnSuccessContainer,
    background = StripeCanvas,
    onBackground = StripeInk,
    surface = StripeSurface,
    onSurface = StripeInk,
    surfaceVariant = StripeTray,
    onSurfaceVariant = StripeBody,
    // Material3's default surfaceContainer* roles are baseline lavender tints - they'd leak into the
    // NavigationBar, Cards and dialogs. Pinned to neutral Stripe surfaces instead.
    surfaceContainerLowest = StripeSurface,
    surfaceContainerLow = StripeSurface,
    surfaceContainer = StripeSurface,
    surfaceContainerHigh = StripeSurface,
    surfaceContainerHighest = StripeTray,
    surfaceBright = StripeSurface,
    surfaceDim = StripeCanvas,
    outline = StripeBorder,
    outlineVariant = StripeBorderStrong,
    error = StripeError,
    onError = StripeOnPrimary,
    errorContainer = StripeErrorContainer,
    onErrorContainer = StripeOnErrorContainer,
)

// {rounded.sm}=4dp, {rounded.md}=6dp, {rounded.lg}=8dp - capped at {rounded.lg} for cards per the
// doc's "Do not use oversized rounded cards beyond {rounded.lg}". Buttons stay pill-shaped via
// Material3's own default button shape, not this Shapes object.
private val StripeShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun SmartMicrogridTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = StripeShapes,
        content = content
    )
}
