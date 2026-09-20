// ============================================================
// File: OperatorDashboardScreen.kt
// Purpose: Grid operator dashboard destination for the Home
//          nav-graph shell (confirmed/completed-today counts,
//          pending-by-station), per architecture.md §6 "Android —
//          Prosumer & Operator Dashboards | ui/dashboard/* | Migara".
// Author: Shalon (stub only — see TODO below)
// ============================================================
package com.smartmicrogrid.ui.dashboard

import androidx.compose.runtime.Composable
import com.smartmicrogrid.ui.home.PlaceholderScreen

// TODO(Migara): replace this placeholder with the real operator dashboard — confirmed/completed
// today counts and pending-by-station breakdown. HomeActivity's nav graph already routes here by
// name; nothing in ui/home needs to change when you do this.
@Composable
fun OperatorDashboardScreen() {
    PlaceholderScreen(title = "Operator dashboard", owner = "Migara")
}
