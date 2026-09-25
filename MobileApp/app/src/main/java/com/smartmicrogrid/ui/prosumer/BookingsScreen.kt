// ============================================================
// File: BookingsScreen.kt
// Purpose: Bookings destination for the Home nav-graph shell
//          (create/modify/cancel workflow, summary, QR display), per
//          architecture.md §6 "Android — Booking workflow | ui/prosumer/* | Migara"
//          (moved from Dinil 2026-09-24 — see architecture.md §0/§7).
//          Note: the workflow itself still calls Dinil's
//          POST/PUT /reservations* endpoints (§3/§4) — only the
//          mobile screen moved, not the backend.
// Author: Shalon (stub only — see TODO below)
// ============================================================
package com.smartmicrogrid.ui.prosumer

import androidx.compose.runtime.Composable
import com.smartmicrogrid.ui.home.PlaceholderScreen

// TODO(Migara): replace this placeholder with the real booking workflow — create/modify/cancel a
// reservation (calling Dinil's POST/PUT/DELETE /reservations endpoints), the 12-hour-notice rule
// enforced server-side, and QR display after confirmation. HomeActivity's nav graph already
// routes here by name; nothing in ui/home needs to change when you do this.
@Composable
fun BookingsScreen() {
    PlaceholderScreen(title = "Bookings", owner = "Migara")
}
