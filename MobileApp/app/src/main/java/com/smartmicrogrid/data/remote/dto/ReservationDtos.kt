// ============================================================
// File: ReservationDtos.kt
// Purpose: Wire shapes for the reservation + verify-qr endpoints
//          (architecture.md §3). Server-side validation only —
//          the client never computes business rules.
// Author: Dinil + Migara (Added Prosumer booking DTOs)
// ============================================================
package com.smartmicrogrid.data.remote.dto

import com.google.gson.annotations.SerializedName

// ---------- verify-qr (Dinil) ----------
data class VerifyQrRequest(val qrToken: String)

data class VerifyQrResponse(
    val reservationId: String,
    val prosumerName: String,
    val stationName: String,
    val slotNumber: Int,
    val status: String
)

// ---------- Prosumer Booking Actions (Migara) ----------
data class CreateReservationRequest(
    @SerializedName("stationId") val stationId: String,
    @SerializedName("slotId") val slotId: String,
    @SerializedName("scheduledAt") val scheduledAt: String // ISO 8601 format
)

data class CreateReservationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("qrToken") val qrToken: String,
    @SerializedName("qrTokenExpiresAt") val qrTokenExpiresAt: String
)

data class UpdateReservationRequest(
    @SerializedName("slotId") val slotId: String? = null,
    @SerializedName("scheduledAt") val scheduledAt: String? = null
)

data class CancelReservationRequest(
    @SerializedName("reason") val reason: String? = null
)

// ---------- generic API error body ----------
data class ApiErrorBody(val code: String?, val message: String?)