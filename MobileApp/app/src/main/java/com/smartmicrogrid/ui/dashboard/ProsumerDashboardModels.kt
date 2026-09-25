// ============================================================
// File: ProsumerDashboardModels.kt
// Purpose: Kotlin shapes mirroring the two endpoints the Prosumer
//          Dashboard screen calls, per architecture.md §3 (moved
//          from Migara to Shalon 2026-09-24 — see §4/§6/§7):
//          GET /dashboard/prosumer/{nic}/summary and GET /reservations.
//          Field names match the JSON response exactly so a future
//          Gson-backed Retrofit call can deserialize straight into
//          these classes with no remapping — see
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

// One row of GET /reservations's "[{ ...reservation }]" response — every field from
// architecture.md §2.4's Reservations schema. scheduledAt/createdAt/updatedAt/etc. are kept as
// ISO-8601 strings here rather than a parsed date type, since this is fixture/display data only
// for now; a real Retrofit client would add a Gson date adapter when it replaces the fixture.
data class ReservationListItem(
    val id: String,
    val prosumerNic: String,
    val stationId: String,
    val slotId: String,
    val scheduledAt: String,
    val status: String,
    val qrToken: String,
    val qrTokenExpiresAt: String,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String?,
    val completedByUserId: String?,
    val cancelReason: String?,
)
