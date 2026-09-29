// ============================================================
// File: HomeRoutes.kt
// Purpose: Single source of truth for the Home nav-graph's route
//          names and the Intent contract for launching HomeActivity.
//            builds login   launches HomeActivity with
//          EXTRA_ROLE via HomeActivity.intentFor(...) - nobody needs
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

    // Prosumer destinations. Dashboard   (ui/dashboard/*); Bookings and Profile
    //  (ui/prosumer/*) - architecture.md §6. Map added 2026-09-26: the assignment
    // spec bundles "Dashboard & Maps" as one prosumer-facing feature ("...nearby grid nodes via
    // Google Maps API"), but the nav only ever wired Map under Grid Operator - a prosumer had no
    // way to reach it at all. Reuses the same ui/operator/MapScreen.kt  built; no
    // new screen needed, just a second route to the existing one.
    const val PROSUMER_DASHBOARD = "home/prosumer/dashboard"
    const val PROSUMER_BOOKINGS = "home/prosumer/bookings"
    const val PROSUMER_CREATE_BOOKING = "home/prosumer/bookings/create"
    const val PROSUMER_MAP = "home/prosumer/map"
    const val PROSUMER_PROFILE = "home/prosumer/profile"

    // Grid operator destinations. Dashboard, Scan QR and Map    (ui/dashboard/*
    // and ui/operator/*) - architecture.md §6.
    const val OPERATOR_DASHBOARD = "home/operator/dashboard"
    const val OPERATOR_SCAN_QR = "home/operator/scan-qr"
    const val OPERATOR_MAP = "home/operator/map"
    const val OPERATOR_PROFILE = "home/operator/profile"
}
