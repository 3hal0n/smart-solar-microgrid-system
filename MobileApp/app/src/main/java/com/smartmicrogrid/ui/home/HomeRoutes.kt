// ============================================================
// File: HomeRoutes.kt
// Purpose: Single source of truth for the Home nav-graph's route
//          names and the Intent contract for launching HomeActivity.
//          Whoever builds login (Dinil) launches HomeActivity with
//          EXTRA_ROLE via HomeActivity.intentFor(...) — nobody needs
//          to edit HomeActivity.kt itself to plug in. Route→package
//          ownership below is exactly architecture.md §6's table.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.home

object HomeRoutes {
    // Intent extra key a caller (e.g. a future LoginActivity) sets to one of UserRole's
    // claimValue strings ("Prosumer" / "GridOperator") before starting HomeActivity. Prefer
    // HomeActivity.intentFor(context, role) over setting this directly.
    const val EXTRA_ROLE = "com.smartmicrogrid.extra.ROLE"

    // Prosumer destinations. Dashboard belongs to Migara (ui/dashboard/*); Bookings and Profile
    // belong to Dinil (ui/prosumer/*) — architecture.md §6.
    const val PROSUMER_DASHBOARD = "home/prosumer/dashboard"
    const val PROSUMER_BOOKINGS = "home/prosumer/bookings"
    const val PROSUMER_PROFILE = "home/prosumer/profile"

    // Grid operator destinations. Dashboard, Scan QR and Map all belong to Migara (ui/dashboard/*
    // and ui/operator/*) — architecture.md §6.
    const val OPERATOR_DASHBOARD = "home/operator/dashboard"
    const val OPERATOR_SCAN_QR = "home/operator/scan-qr"
    const val OPERATOR_MAP = "home/operator/map"
}
