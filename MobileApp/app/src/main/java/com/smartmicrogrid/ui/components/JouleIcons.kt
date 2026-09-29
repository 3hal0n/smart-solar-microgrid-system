// ============================================================
// File: JouleIcons.kt
// Purpose: Small shared stroke-icon set for the mobile app, built
//          as ImageVectors from SVG path strings on the same 20x20
//          grid / 1.6 stroke as WebApp's Icon.jsx, so both clients
//          share one icon language. Avoids pulling in Material Icons
//          Extended for a handful of glyphs. Tint via Icon(tint=...).
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Builds a 20x20 stroke-only icon from one or more SVG path-data strings.
private fun strokeIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f,
    ).apply {
        paths.forEach { data ->
            addPath(
                pathData = addPathNodes(data),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

object JouleIcons {
    val Hubs: ImageVector = strokeIcon(
        "hubs",
        "M12.2 10a2.2 2.2 0 1 1-4.4 0 2.2 2.2 0 0 1 4.4 0Z",
        "M5.6 4.5a1.6 1.6 0 1 1-3.2 0 1.6 1.6 0 0 1 3.2 0Z",
        "M17.6 4.5a1.6 1.6 0 1 1-3.2 0 1.6 1.6 0 0 1 3.2 0Z",
        "M11.6 16.5a1.6 1.6 0 1 1-3.2 0 1.6 1.6 0 0 1 3.2 0Z",
        "M5.2 5.6 8.4 8.6M14.8 5.6l-3.2 3M10 12.2v2.7",
    )
    val Bolt: ImageVector = strokeIcon("bolt", "M11 2.5 4.5 11h5l-1 6.5L15.5 9h-5l.5-6.5Z")
    val Pulse: ImageVector = strokeIcon("pulse", "M2.5 10h3.5l2-5 4 10 2-5h3.5")
    val Battery: ImageVector = strokeIcon(
        "battery",
        "M4 6h10a1.5 1.5 0 0 1 1.5 1.5v5A1.5 1.5 0 0 1 14 14H4a1.5 1.5 0 0 1-1.5-1.5v-5A1.5 1.5 0 0 1 4 6Z",
        "M17.5 8.5v3M5.5 9v2M8.5 9v2",
    )
    val Grid: ImageVector = strokeIcon(
        "grid",
        "M3 3h5.5v5.5H3ZM11.5 3H17v5.5h-5.5ZM3 11.5h5.5V17H3ZM11.5 11.5H17V17h-5.5Z",
    )
    val Calendar: ImageVector = strokeIcon(
        "calendar",
        "M5 5h10a1.5 1.5 0 0 1 1.5 1.5v8.5A1.5 1.5 0 0 1 15 16.5H5A1.5 1.5 0 0 1 3.5 15V6.5A1.5 1.5 0 0 1 5 5Z",
        "M3.5 8.5h13M7 3.5v3M13 3.5v3",
    )
    val User: ImageVector = strokeIcon(
        "user",
        "M13 7a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
        "M4 16.5c.9-2.7 3.2-4 6-4s5.1 1.3 6 4",
    )
    val Scan: ImageVector = strokeIcon(
        "scan",
        "M3.5 7V4.5a1 1 0 0 1 1-1H7M13 3.5h2.5a1 1 0 0 1 1 1V7M16.5 13v2.5a1 1 0 0 1-1 1H13M7 16.5H4.5a1 1 0 0 1-1-1V13",
        "M6.5 10h7",
    )
    val MapPin: ImageVector = strokeIcon(
        "map-pin",
        "M10 17s-5.5-4.8-5.5-9a5.5 5.5 0 0 1 11 0c0 4.2-5.5 9-5.5 9Z",
        "M11.8 8a1.8 1.8 0 1 1-3.6 0 1.8 1.8 0 0 1 3.6 0Z",
    )
    val Refresh: ImageVector = strokeIcon(
        "refresh",
        "M16 10a6 6 0 1 1-1.8-4.3",
        "M16 3.5V7h-3.5",
    )
    val Check: ImageVector = strokeIcon("check", "M4 10.5 8 14l8-8")
    val Back: ImageVector = strokeIcon("back", "M12.5 4.5 6 10l6.5 5.5")
    val Logout: ImageVector = strokeIcon(
        "logout",
        "M7.5 17.5H4.5a1.5 1.5 0 0 1-1.5-1.5V4a1.5 1.5 0 0 1 1.5-1.5h3",
        "M13 13.5l3.5-3.5L13 6.5",
        "M16.5 10H7.5",
    )
    val Eye: ImageVector = strokeIcon(
        "eye",
        "M1.5 10s3.5-5.5 8.5-5.5 8.5 5.5 8.5 5.5-3.5 5.5-8.5 5.5S1.5 10 1.5 10Z",
        "M12.5 10a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0Z",
    )
    val EyeOff: ImageVector = strokeIcon(
        "eye-off",
        "M2 2l16 16",
        "M8.5 8.6a2.5 2.5 0 0 0 3.5 3.5",
        "M6.8 5.6C7.8 5.2 8.9 5 10 5c5 0 8.5 5 8.5 5a13.4 13.4 0 0 1-3.2 3.8",
        "M3.3 7.8A13.4 13.4 0 0 0 1.5 10s3.5 5 8.5 5c1.4 0 2.6-.3 3.7-.8",
    )
    val Camera: ImageVector = strokeIcon(
        "camera",
        "M3 7a2 2 0 0 1 2-2h2l1.5-2h3L13 5h2a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z",
        "M13 11a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
    )
    val Filter: ImageVector = strokeIcon(
        "filter",
        "M3 4.5h14M5.5 9.5h9M8.5 14.5h3",
    )
    val Search: ImageVector = strokeIcon(
        "search",
        "M9 14.5a5.5 5.5 0 1 0 0-11 5.5 5.5 0 0 0 0 11Z",
        "M13 13l4 4",
    )
    val ChevronRight: ImageVector = strokeIcon("chevron-right", "M8 4.5 13.5 10 8 15.5")
    val Sliders: ImageVector = strokeIcon(
        "sliders",
        "M3.5 6h8M15 6h1.5M3.5 14h2M9 14h7.5",
        "M13.2 6a1.7 1.7 0 1 1-3.4 0 1.7 1.7 0 0 1 3.4 0ZM8.7 14a1.7 1.7 0 1 1-3.4 0 1.7 1.7 0 0 1 3.4 0Z",
    )
    val Plus: ImageVector = strokeIcon("plus", "M10 4v12M4 10h12")
    val Clock: ImageVector = strokeIcon("clock", "M16.5 10a6.5 6.5 0 1 1-13 0 6.5 6.5 0 0 1 13 0Z", "M10 6.5V10l2.5 1.5")
    val Mail: ImageVector = strokeIcon(
        "mail",
        "M4 5h12a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z",
        "M3.5 6 10 11l6.5-5",
    )
    val Phone: ImageVector = strokeIcon(
        "phone",
        "M6.5 3.5h-2a1 1 0 0 0-1 1C3.5 11 9 16.5 15.5 16.5a1 1 0 0 0 1-1v-2l-3-1.2-1.5 1.5a8 8 0 0 1-4.8-4.8L8.7 7.5 7.5 4.5Z",
    )
    val Home: ImageVector = strokeIcon("home", "M3.5 9 10 3.5 16.5 9v7a.5.5 0 0 1-.5.5h-4v-4.5h-4V16.5H4a.5.5 0 0 1-.5-.5Z")
    val IdCard: ImageVector = strokeIcon(
        "id-card",
        "M4 5h12a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z",
        "M9 9a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0ZM5.5 13c.4-1 1.2-1.5 2-1.5s1.6.5 2 1.5M11.5 8.5h3M11.5 11.5h3",
    )
    val Lock: ImageVector = strokeIcon(
        "lock",
        "M5.5 9h9a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1h-9a1 1 0 0 1-1-1v-6a1 1 0 0 1 1-1Z",
        "M7 9V6.5a3 3 0 0 1 6 0V9",
    )
    val Edit: ImageVector = strokeIcon("edit", "M13.5 3.5l3 3L8 15l-4 1 1-4Z")
    val Close: ImageVector = strokeIcon("close", "M5 5l10 10M15 5 5 15")
    val Qr: ImageVector = strokeIcon(
        "qr",
        "M3.5 3.5h5v5h-5ZM11.5 3.5h5v5h-5ZM3.5 11.5h5v5h-5Z",
        "M11.5 11.5h2v2h-2ZM14.5 14.5h2v2h-2ZM14.5 11.5h2M11.5 16.5h2",
    )
    val Shield: ImageVector = strokeIcon("shield", "M10 2.8 16 5v4.6c0 3.8-2.6 6.4-6 7.6-3.4-1.2-6-3.8-6-7.6V5Z")
}
