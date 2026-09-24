// ============================================================
// File: NearbyStationFixtures.kt
// Purpose: Hardcoded stand-in for GET /stations/nearby, shaped
//          exactly like the real JSON per architecture.md §3, so
//          MapScreen.kt can be built and demoed before
//          data/remote/ApiClient.kt (the shared Retrofit client —
//          still unbuilt anywhere in this repo) exists and this
//          branch is merged to main.
//          TODO(swap-in-real-api): once both of those are true,
//          replace findNearby()'s body with a real call to
//          GET /stations/nearby?lat={lat}&lng={lng}&radiusKm={radiusKm}
//          and delete this file's fixture data. The function
//          signature (suspend, same three params, same return type)
//          is deliberately shaped to match that call already, so the
//          swap in MapScreen.kt should be a one-line change to
//          the data source, not a rewrite of the screen.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.operator

import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private val ALL_STATIONS = listOf(
    NearbyStation(
        id = "650000000000000000000001",
        name = "Colombo Central Hub",
        location = NearbyStationLocation("Point", listOf(79.8612, 6.9271)),
        distanceKm = 0.0,
        capacityKWh = 240.0,
        availableSlots = 5,
    ),
    NearbyStation(
        id = "650000000000000000000002",
        name = "Kollupitiya Hub",
        location = NearbyStationLocation("Point", listOf(79.8489, 6.9147)),
        distanceKm = 0.0,
        capacityKWh = 150.0,
        availableSlots = 0,
    ),
    NearbyStation(
        id = "650000000000000000000003",
        name = "Dehiwala Hub",
        location = NearbyStationLocation("Point", listOf(79.8700, 6.8500)),
        distanceKm = 0.0,
        capacityKWh = 180.0,
        availableSlots = 3,
    ),
    NearbyStation(
        id = "650000000000000000000004",
        name = "Kandy Ridge Hub",
        location = NearbyStationLocation("Point", listOf(80.6337, 7.2906)),
        distanceKm = 0.0,
        capacityKWh = 200.0,
        availableSlots = 4,
    ),
    NearbyStation(
        id = "650000000000000000000005",
        name = "Galle Coastal Hub",
        location = NearbyStationLocation("Point", listOf(80.2210, 6.0535)),
        distanceKm = 0.0,
        capacityKWh = 120.0,
        availableSlots = 2,
    ),
)

object NearbyStationFixtures {

    // Simulates GET /stations/nearby: computes every fixture station's real distance from
    // (lat, lng) via the haversine formula, keeps only those within radiusKm, and returns them
    // sorted closest-first — mirroring $geoNear's maxDistance + sort behavior exactly, so
    // whichever station set appears here is the same set the real endpoint would return for the
    // same inputs. The delay stands in for real network latency so the screen's loading state is
    // actually exercised, not just present in code.
    suspend fun findNearby(lat: Double, lng: Double, radiusKm: Double): List<NearbyStation> {
        delay(600)
        return ALL_STATIONS
            .map { station -> station.copy(distanceKm = haversineKm(lat, lng, station.location.lat, station.location.lng)) }
            .filter { it.distanceKm <= radiusKm }
            .sortedBy { it.distanceKm }
    }

    // Great-circle distance between two lat/lng points in kilometers.
    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
