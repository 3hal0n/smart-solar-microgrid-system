// ============================================================
// File: BookingsScreen.kt
// Purpose: Bookings destination for the Home nav-graph shell
//          (create/modify/cancel workflow, summary, QR display), per
//          architecture.md §6 "Android — Booking workflow | ui/prosumer/* | Dinil".
// Author: Shalon (stub only — see TODO below)
// ============================================================
package com.smartmicrogrid.ui.prosumer

import androidx.compose.runtime.Composable
import com.smartmicrogrid.ui.home.PlaceholderScreen

// TODO(Dinil): replace this placeholder with the real booking workflow — create/modify/cancel a
// reservation, the 12-hour-notice rule, and QR display after confirmation. HomeActivity's nav
// graph already routes here by name; nothing in ui/home needs to change when you do this.
@Composable
fun BookingsScreen() {
    PlaceholderScreen(title = "Bookings", owner = "Dinil")
}
