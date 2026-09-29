// ============================================================
// File: DashboardDtos.kt
// Purpose: Wire shapes for the operator dashboard summary
//          endpoint. Read-only — the client renders these
//          numbers; it never computes them (FAT service pattern).
// Author: Dinil
// ============================================================
package com.smartmicrogrid.data.remote.dto

data class OperatorSummaryResponse(
    val confirmedTodayCount: Int,
    val completedTodayCount: Int,
    val pendingByStation: List<PendingByStation>
)

data class PendingByStation(
    val stationId: String,
    val stationName: String,
    val pendingCount: Int
)