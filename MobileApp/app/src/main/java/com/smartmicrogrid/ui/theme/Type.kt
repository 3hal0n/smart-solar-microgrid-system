// ============================================================
// File: Type.kt
// Purpose: Material3 type scale mapped from docs/stripe.design.md's
//          {typography.*} tokens. The doc's own px sizes (hero=68px,
//          headline-lg=48px, etc.) are explicitly marked as desktop
//          anchors that "should step down quickly on mobile", so
//          these are scaled-down phone-appropriate sizes that keep
//          the doc's weight/tracking/role relationships rather than
//          copying its pixel values 1:1. `sohne-var` isn't bundled as
//          a font asset in this app, so FontFamily.Default (the
//          platform sans-serif) stands in, matching the doc's own
//          fallback chain ("...Helvetica Neue, Arial, sans-serif").
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val Typography = Typography(
    // headline-lg (48px, w400, tracking -0.03em) → mobile section headers
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.02).em,
    ),
    // title-lg (32px, w400) → screen titles ("Dashboard", splash wordmark)
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    ),
    // title-md (24px, w400) → card group headings, stat tile numbers
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.01).em,
    ),
    // title-sm (18px, w500) → card titles
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.em,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.em,
    ),
    // body (17px, w300, generous line-height) → paragraphs/descriptions
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.em,
    ),
    // caption (13px, w300) → helper text, small table copy
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.015.em,
    ),
    // button (15px, w500) → CTAs, segmented controls
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.em,
    ),
    // label (13px, w500) → nav, labels, badges, metadata
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.015.em,
    ),
    // legal (12px, w300) → footnotes, fine print
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.em,
    ),
)
