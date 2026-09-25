// ============================================================
// File: ReservationDtos.kt
// Purpose: Wire shapes for the reservation + verify-qr endpoints
//          (architecture.md §3). Server-side validation only —
//          the client never computes business rules.
// Author: Dinil
// ============================================================
package com.smartmicrogrid.data.remote.dto

// ---------- verify-qr ----------
data class VerifyQrRequest(val qrToken: String)

data class VerifyQrResponse(
    val reservationId: String,
    val prosumerName: String,
    val stationName: String,
    val slotNumber: Int,
    val status: String
)

// ---------- generic API error body ----------
data class ApiErrorBody(val code: String?, val message: String?)