// ============================================================
// File: ProfileScreen.kt
// Purpose: Profile destination for the Home nav-graph shell (view
//          /edit own profile, request deactivation), per
//          architecture.md §6 "Android — Prosumer Register/Login
//          /Profile | ...ProfileActivity | ui/prosumer/* | Dinil".
// Author: Shalon (stub only — see TODO below)
// ============================================================
package com.smartmicrogrid.ui.prosumer

import androidx.compose.runtime.Composable
import com.smartmicrogrid.ui.home.PlaceholderScreen

// TODO(Dinil): replace this placeholder with the real profile screen — view/edit own details
// (PUT /prosumers/{nic}) and request deactivation. HomeActivity's nav graph already routes here
// by name; nothing in ui/home needs to change when you do this.
@Composable
fun ProfileScreen() {
    PlaceholderScreen(title = "Profile", owner = "Dinil")
}
