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
    @SerializedName("reservationId") val reservationId: String,
    @SerializedName("prosumerName") val prosumerName: String,
    @SerializedName("stationName") val stationName: String,
    @SerializedName("slotNumber") val slotNumber: Int,
    @SerializedName("status") val status: String
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

// ---------- ADDED: Reservation Response (Matches C# Backend) ----------
data class ReservationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("prosumerNic") val prosumerNic: String,
    @SerializedName("stationId") val stationId: String,
    @SerializedName("slotId") val slotId: String,
    @SerializedName("scheduledAt") val scheduledAt: String,
    @SerializedName("status") val status: String,
    @SerializedName("qrToken") val qrToken: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("completedAt") val completedAt: String?,
    @SerializedName("cancelReason") val cancelReason: String?
)

// ---------- generic API error body ----------
data class ApiErrorBody(val code: String?, val message: String?)