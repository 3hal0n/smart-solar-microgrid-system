// ============================================================
// File: StationDtos.kt
// Purpose: DTOs for Station and Battery Slot retrieval.
// ============================================================
package com.smartmicrogrid.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StationSummaryResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("capacityKWh") val capacityKWh: Double,
    @SerializedName("totalBatterySlots") val totalBatterySlots: Int,
    @SerializedName("status") val status: String
)

data class StationDetailResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("capacityKWh") val capacityKWh: Double,
    @SerializedName("totalBatterySlots") val totalBatterySlots: Int,
    @SerializedName("status") val status: String,
    @SerializedName("slots") val slots: List<SlotResponse> = emptyList()
)

data class SlotResponse(
    @SerializedName("id") val id: String,
    @SerializedName("stationId") val stationId: String,
    @SerializedName("slotNumber") val slotNumber: Int,
    @SerializedName("type") val type: String,
    @SerializedName("capacityKWh") val capacityKWh: Double,
    @SerializedName("status") val status: String
)
