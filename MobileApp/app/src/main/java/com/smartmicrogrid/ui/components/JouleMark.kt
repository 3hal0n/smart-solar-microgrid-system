// ============================================================
// File: JouleMark.kt
// Purpose: The Joule brand mark (four rotated bars) drawn natively
//          in Compose, matching WebApp's JouleMark.jsx geometry
//          (220x180 viewBox, bars rotated -30deg) and its violet ->
//          cyan gradient, so web and mobile show the same logo
//          without shipping an image asset.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale

private const val VIEW_WIDTH = 220f
private const val VIEW_HEIGHT = 180f

// x, y, width of each bar in the 220x180 source viewBox; every bar is 30 tall with a 4 radius.
private val BARS = listOf(
    Triple(18f, 50f, 83f),
    Triple(115f, 50f, 87f),
    Triple(18f, 100f, 109f),
    Triple(141f, 100f, 61f),
)

private val GradientStart = Color(0xFF533AFD)
private val GradientEnd = Color(0xFF11EFE3)

// Draws the mark at whatever width the modifier gives it (height follows the 220:180 ratio).
// Pass `monochrome` to fill it with a single color instead of the brand gradient.
@Composable
fun JouleMark(modifier: Modifier = Modifier, monochrome: Color? = null) {
    Canvas(modifier = modifier.aspectRatio(VIEW_WIDTH / VIEW_HEIGHT)) {
        val factor = size.width / VIEW_WIDTH
        scale(scale = factor, pivot = Offset.Zero) {
            val brush = monochrome?.let { SolidColor(it) } ?: Brush.linearGradient(
                colors = listOf(GradientStart, GradientEnd),
                start = Offset(18f, 90f),
                end = Offset(202f, 90f),
            )
            rotate(degrees = -30f, pivot = Offset(110f, 90f)) {
                BARS.forEach { (x, y, width) ->
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x, y),
                        size = Size(width, 30f),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                }
            }
        }
    }
}
