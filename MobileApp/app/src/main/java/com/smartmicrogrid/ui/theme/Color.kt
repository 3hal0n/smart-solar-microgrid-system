// ============================================================
// File: Color.kt
// Purpose: Color tokens ported from docs/stripe.design.md (the same
//          design system already applied to WebApp) so the mobile app
//          reads as the same product, not a separate Material default
//          theme. Names mirror the doc's {colors.*} tokens directly.
//          Light-mode only per 2026-09-28 direction (Theme.kt always
//          builds LightColorScheme) - no Dark* tokens here anymore.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.theme

import androidx.compose.ui.graphics.Color

// --- Stripe design tokens (docs/stripe.design.md `colors:`) ---
val StripePrimary = Color(0xFF533AFD)
val StripeAccent = Color(0xFF635BFF)
val StripeAccentHover = Color(0xFF4032C8)
val StripeAccentPressed = Color(0xFF2E2B8C)
val StripeInk = Color(0xFF0A2540)
val StripeBody = Color(0xFF425466)
val StripeMuted = Color(0xFF727F96)
val StripeCanvas = Color(0xFFF6F9FC)
val StripeSurface = Color(0xFFFFFFFF)
val StripeSurfaceAlt = Color(0xFFFAFBFD)
val StripeBorder = Color(0xFFE7ECF1)
val StripeBorderStrong = Color(0xFFC1C9D2)
val StripeSuccess = Color(0xFF09825D)
val StripeWarning = Color(0xFFBB5504)
val StripeError = Color(0xFFCD3D64)
val StripeBrandVioletSoft = Color(0xFFE8E9FF)
val StripeSuperNavy = Color(0xFF061B31)
val StripeFooterBg = Color(0xFF0A2540)
val StripeOnPrimary = Color(0xFFFFFFFF)

// The cyan stop from the JouleMark brand gradient, reused as a standalone accent for a third
// stat-tray color (2026-09-26 "more colorful, not full enterprise" pass) - Active/Pending already
// read naturally as primary/warning, this gives Upcoming its own identity instead of repeating one.
val StripeCyan = Color(0xFF0EA5A0)
val StripeCyanContainer = Color(0xFFD9F5F3)
val StripeOnCyanContainer = Color(0xFF063D3A)

// Tray tint for stat/feature trays - a step darker than {colors.canvas} so a tray reads as a frame
// on the canvas background (the doc's surfaceAlt #fafbfd is lighter than canvas, which washes out).
// Same value WebApp uses for its surface-alt trays.
val StripeTray = Color(0xFFF1F5F9)

// Soft container tints for semantic colors - not literal doc tokens (the doc only gives a single
// hex per semantic role), derived by lightening each one for container/on-container pairs the way
// Material3's color roles require.
val StripeSuccessContainer = Color(0xFFDCF3E9)
val StripeOnSuccessContainer = Color(0xFF07422F)
val StripeWarningContainer = Color(0xFFFBE7D6)
val StripeOnWarningContainer = Color(0xFF5C2B02)
val StripeErrorContainer = Color(0xFFFBE4EA)
val StripeOnErrorContainer = Color(0xFF611026)
