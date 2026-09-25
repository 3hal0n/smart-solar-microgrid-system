// ============================================================
// File: Theme.kt
// Purpose: Builds the app's Material3 ColorScheme/Shapes from the
//          Stripe design tokens in Color.kt, per docs/stripe.design.md
//          — the same design system WebApp uses. Two changes from the
//          stock Compose template that both matter for brand
//          consistency: dynamicColor now defaults to false (Material
//          You dynamic color on Android 12+ would otherwise silently
//          replace this palette with one derived from the device
//          wallpaper, defeating the design system entirely), and
//          Shapes.medium (Card's default shape) is set to 8dp per
//          {rounded.lg} instead of M3's default 12dp.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = StripePrimary,
    onPrimary = StripeOnPrimary,
    primaryContainer = StripeAccentPressed,
    onPrimaryContainer = StripeBrandVioletSoft,
    secondary = StripeAccent,
    onSecondary = StripeOnPrimary,
    tertiary = StripeSuccess,
    onTertiary = StripeOnPrimary,
    tertiaryContainer = Color(0xFF0F3D2C),
    onTertiaryContainer = StripeSuccessContainer,
    background = StripeDarkBackground,
    onBackground = StripeDarkOnSurface,
    surface = StripeDarkSurface,
    onSurface = StripeDarkOnSurface,
    surfaceVariant = StripeDarkSurfaceAlt,
    onSurfaceVariant = StripeDarkOnSurfaceVariant,
    surfaceContainerLowest = StripeDarkSurface,
    surfaceContainerLow = StripeDarkSurface,
    surfaceContainer = StripeDarkSurface,
    surfaceContainerHigh = StripeDarkSurface,
    surfaceContainerHighest = StripeDarkSurfaceAlt,
    surfaceBright = StripeDarkSurface,
    surfaceDim = StripeDarkBackground,
    outline = StripeDarkBorder,
    outlineVariant = StripeDarkBorder,
    error = StripeError,
    onError = StripeOnPrimary,
    errorContainer = Color(0xFF4A0F20),
    onErrorContainer = StripeErrorContainer,
)

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
    // Material3's default surfaceContainer* roles are baseline lavender tints — they'd leak into the
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

// {rounded.sm}=4dp, {rounded.md}=6dp, {rounded.lg}=8dp — capped at {rounded.lg} for cards per the
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: Material You dynamic color would otherwise override this palette with one
    // derived from the device wallpaper on Android 12+, which means the app would never actually
    // show the Stripe design system on most real/modern test devices.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StripeShapes,
        content = content
    )
}
