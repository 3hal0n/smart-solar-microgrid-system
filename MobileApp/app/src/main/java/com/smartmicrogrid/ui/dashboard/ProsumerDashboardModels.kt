// ============================================================
// File: ProsumerDashboardModels.kt
// Purpose: Kotlin shapes mirroring the two endpoints the Prosumer
//          Dashboard screen calls, per architecture.md §3 (moved
//          from Migara to Shalon 2026-09-24 - see §4/§6/§7):
//          GET /dashboard/prosumer/{nic}/summary and GET /reservations.
//          Field names match the JSON response exactly so a future
//          Gson-backed Retrofit call can deserialize straight into
//          these classes with no remapping - see
//          ProsumerDashboardFixtures.kt for the fixture data using
//          them today, and ProsumerDashboardScreen.kt's TODO for
//          where the real call plugs in.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.dashboard

// GET /dashboard/prosumer/{nic}/summary response body.
data class ProsumerDashboardSummary(
    val activeCount: Int,
    val pendingCount: Int,
    val approvedFutureCount: Int,
    val recentHistory: List<ReservationHistoryItem>,
)

// One entry in ProsumerDashboardSummary.recentHistory.
data class ReservationHistoryItem(
    val id: String,
    val stationId: String,
    val slotId: String,
    val scheduledAt: String,
    val status: String,
)

// GET /reservations's row shape now lives as ReservationResponse in
// data/remote/dto/ReservationDtos.kt (added 2026-09-27 for Migara's booking screens) - it mirrors
// ReservationsController.MapToResponse on the backend exactly, so ProsumerDashboardScreen.kt uses
// that same class rather than a second, slightly-stale duplicate of the same wire shape.
