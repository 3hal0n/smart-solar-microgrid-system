// ============================================================
// File: ProsumerDashboardFixtures.kt
// Purpose: FIXTURE_PROSUMER_NIC — stands in for "the logged-in
//          prosumer's NIC" until a real session/login exists to read
//          it from. ProsumerDashboardScreen.kt now calls the real
//          GET /dashboard/prosumer/{nic}/summary and GET /reservations
//          endpoints (via data/remote/ApiClient.kt, wired 2026-09-26)
//          with this NIC — the fixture reservation/summary data that
//          used to live in this file was deleted once the real calls
//          replaced it.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.dashboard

const val FIXTURE_PROSUMER_NIC = "200023456789"
