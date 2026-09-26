// ============================================================
// File: NearbyStationModels.kt
// Purpose: Kotlin shapes mirroring GET /stations/nearby's response,
//          per architecture.md §3 (moved from Migara to Shalon
//          2026-09-21/24 — see §4/§6/§7). Field names match the JSON
//          exactly, including capacityKWh/availableSlots which
//          Shalon added to the endpoint 2026-09-24 specifically for
//          this screen's info window — see StationDtos.cs's
//          NearbyStationResponse for the backend side.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.operator

// One row of GET /stations/nearby's response array.
data class NearbyStation(
    val id: String,
    val name: String,
    val location: NearbyStationLocation,
    val distanceKm: Double,
    val capacityKWh: Double,
    val availableSlots: Int,
)

// GeoJSON point as the API returns it: coordinates are [lng, lat], not [lat, lng].
data class NearbyStationLocation(
    val type: String,
    val coordinates: List<Double>,
) {
    val lng: Double get() = coordinates[0]
    val lat: Double get() = coordinates[1]
}
