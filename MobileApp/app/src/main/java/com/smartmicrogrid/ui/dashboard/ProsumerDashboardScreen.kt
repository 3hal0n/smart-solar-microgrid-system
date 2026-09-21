// ============================================================
// File: ProsumerDashboardScreen.kt
// Purpose: Prosumer dashboard destination for the Home nav-graph
//          shell (active/pending reservation counts, recent
//          bookings), per architecture.md §6 "Android — Prosumer &
//          Operator Dashboards | ui/dashboard/* | Migara".
// Author: Shalon (stub only — see TODO below)
// ============================================================
package com.smartmicrogrid.ui.dashboard

import androidx.compose.runtime.Composable
import com.smartmicrogrid.ui.home.PlaceholderScreen

// TODO(Migara): replace this placeholder with the real prosumer dashboard — active/pending
// reservation counts, recent bookings, nearby grid node summary. HomeActivity's nav graph
// already routes here by name; nothing in ui/home needs to change when you do this.
@Composable
fun ProsumerDashboardScreen() {
    PlaceholderScreen(title = "Prosumer dashboard", owner = "Migara")
}
