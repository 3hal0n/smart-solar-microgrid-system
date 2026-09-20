// ============================================================
// File: MapScreen.kt
// Purpose: Map destination for the Home nav-graph shell (nearby grid
//          nodes via Google Maps API, station details on selection),
//          per architecture.md §6 "Android — Nearby Stations Map |
//          ui/operator/MapActivity | Migara".
// Author: Shalon (stub only — see TODO below)
// ============================================================
package com.smartmicrogrid.ui.operator

import androidx.compose.runtime.Composable
import com.smartmicrogrid.ui.home.PlaceholderScreen

// TODO(Migara): replace this placeholder with the real nearby-stations map (Google Maps SDK),
// backed by GET /stations/nearby. HomeActivity's nav graph already routes here by name; nothing
// in ui/home needs to change when you do this.
@Composable
fun MapScreen() {
    PlaceholderScreen(title = "Nearby stations", owner = "Migara")
}
