// ============================================================
// File: ProsumerDashboardFixtures.kt
// Purpose: Hardcoded stand-in for GET /dashboard/prosumer/{nic}/summary
//          and GET /reservations, shaped exactly like the real JSON
//          per architecture.md §3, so ProsumerDashboardScreen.kt can
//          be built and demoed before data/remote/ApiClient.kt (the
//          shared Retrofit client — still unbuilt anywhere in this
//          repo) exists and this branch is merged to main.
//          TODO(swap-in-real-api): once both of those are true,
//          delete this file's usage from ProsumerDashboardScreen.kt
//          and replace it with real calls to
//          GET /dashboard/prosumer/{nic}/summary and
//          GET /reservations?nic={nic} — the response shapes already
//          match ProsumerDashboardModels.kt field-for-field, so no
//          model changes should be needed, only the data source.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.dashboard

import kotlinx.coroutines.delay

// The NIC this fixture data is for — stands in for "the logged-in prosumer's NIC" until a real
// session/login exists to read it from.
const val FIXTURE_PROSUMER_NIC = "200023456789"

private val FIXTURE_RESERVATIONS = listOf(
    ReservationListItem(
        id = "650000000000000000000101",
        prosumerNic = FIXTURE_PROSUMER_NIC,
        stationId = "650000000000000000000001",
        slotId = "650000000000000000000011",
        scheduledAt = "2026-10-02T09:00:00Z",
        status = "Confirmed",
        qrToken = "fixture-qr-token-101",
        qrTokenExpiresAt = "2026-10-02T10:00:00Z",
        createdAt = "2026-09-22T14:12:00Z",
        updatedAt = "2026-09-22T14:12:00Z",
        completedAt = null,
        completedByUserId = null,
        cancelReason = null,
    ),
    ReservationListItem(
        id = "650000000000000000000102",
        prosumerNic = FIXTURE_PROSUMER_NIC,
        stationId = "650000000000000000000002",
        slotId = "650000000000000000000012",
        scheduledAt = "2026-09-28T16:30:00Z",
        status = "Confirmed",
        qrToken = "fixture-qr-token-102",
        qrTokenExpiresAt = "2026-09-28T17:30:00Z",
        createdAt = "2026-09-21T08:40:00Z",
        updatedAt = "2026-09-21T08:40:00Z",
        completedAt = null,
        completedByUserId = null,
        cancelReason = null,
    ),
    ReservationListItem(
        id = "650000000000000000000103",
        prosumerNic = FIXTURE_PROSUMER_NIC,
        stationId = "650000000000000000000001",
        slotId = "650000000000000000000013",
        scheduledAt = "2026-09-20T11:00:00Z",
        status = "Confirmed",
        qrToken = "fixture-qr-token-103",
        qrTokenExpiresAt = "2026-09-20T12:00:00Z",
        createdAt = "2026-09-13T10:05:00Z",
        updatedAt = "2026-09-13T10:05:00Z",
        completedAt = null,
        completedByUserId = null,
        cancelReason = null,
    ),
    ReservationListItem(
        id = "650000000000000000000104",
        prosumerNic = FIXTURE_PROSUMER_NIC,
        stationId = "650000000000000000000003",
        slotId = "650000000000000000000014",
        scheduledAt = "2026-09-10T13:00:00Z",
        status = "Completed",
        qrToken = "fixture-qr-token-104",
        qrTokenExpiresAt = "2026-09-10T14:00:00Z",
        createdAt = "2026-09-03T09:00:00Z",
        updatedAt = "2026-09-10T13:22:00Z",
        completedAt = "2026-09-10T13:22:00Z",
        completedByUserId = "650000000000000000000901",
        cancelReason = null,
    ),
    ReservationListItem(
        id = "650000000000000000000105",
        prosumerNic = FIXTURE_PROSUMER_NIC,
        stationId = "650000000000000000000002",
        slotId = "650000000000000000000015",
        scheduledAt = "2026-09-05T08:00:00Z",
        status = "Cancelled",
        qrToken = "fixture-qr-token-105",
        qrTokenExpiresAt = "2026-09-05T09:00:00Z",
        createdAt = "2026-08-29T12:00:00Z",
        updatedAt = "2026-09-04T18:47:00Z",
        completedAt = null,
        completedByUserId = null,
        cancelReason = "Prosumer requested cancellation",
    ),
    ReservationListItem(
        id = "650000000000000000000106",
        prosumerNic = FIXTURE_PROSUMER_NIC,
        stationId = "650000000000000000000001",
        slotId = "650000000000000000000011",
        scheduledAt = "2026-08-28T15:00:00Z",
        status = "Completed",
        qrToken = "fixture-qr-token-106",
        qrTokenExpiresAt = "2026-08-28T16:00:00Z",
        createdAt = "2026-08-21T07:30:00Z",
        updatedAt = "2026-08-28T15:19:00Z",
        completedAt = "2026-08-28T15:19:00Z",
        completedByUserId = "650000000000000000000901",
        cancelReason = null,
    ),
).sortedByDescending { it.scheduledAt }

// activeCount/pendingCount/approvedFutureCount below are hand-derived from FIXTURE_RESERVATIONS
// using DashboardService.GetProsumerSummaryAsync's exact definitions (architecture.md §3):
// activeCount = all Confirmed (3 here); pendingCount = Confirmed with scheduledAt <= "now" (the
// 2026-09-20 entry, already past as of this fixture's 2026-09-24 authoring date); approvedFutureCount
// = Confirmed with scheduledAt > "now" (the two October/2026-09-28 entries).
private val FIXTURE_SUMMARY = ProsumerDashboardSummary(
    activeCount = 3,
    pendingCount = 1,
    approvedFutureCount = 2,
    recentHistory = FIXTURE_RESERVATIONS.map {
        ReservationHistoryItem(
            id = it.id,
            stationId = it.stationId,
            slotId = it.slotId,
            scheduledAt = it.scheduledAt,
            status = it.status,
        )
    },
)

object ProsumerDashboardFixtures {
    val reservations: List<ReservationListItem> = FIXTURE_RESERVATIONS

    // Simulates GET /dashboard/prosumer/{nic}/summary: a real network round trip, not an instant
    // return — so ProsumerDashboardScreen's "cache first, then refresh" flow has an actual delay
    // for the cached value to be visibly shown ahead of.
    suspend fun loadSummary(): ProsumerDashboardSummary {
        delay(600)
        return FIXTURE_SUMMARY
    }
}
