// ============================================================
// File: UserRole.kt
// Purpose: The two roles the mobile app ever shows a Home shell for
//          (Backoffice is web-only - architecture.md's project
//          scenario has only Prosumers and Grid Operators using the
//          Android app). Maps to/from the exact JWT `role` claim
//          values architecture.md §3 defines, so callers never
//          invent their own string constants for this.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.home

enum class UserRole(val claimValue: String) {
    Prosumer("Prosumer"),
    GridOperator("GridOperator");

    companion object {
        // Falls back to Prosumer for a missing/unrecognized value - see HomeRoutes.EXTRA_ROLE.
        fun fromClaimValue(value: String?): UserRole =
            entries.firstOrNull { it.claimValue == value } ?: Prosumer
    }
}
