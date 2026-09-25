// ============================================================
// File: JouleApi.kt
// Purpose: Shared Retrofit endpoint interface — one interface for
//          the whole app, same merge-conflict-avoidance shape as
//          AppDbHelper.kt: add your own @GET/@POST method for your
//          own endpoint here, don't edit anyone else's method. Built
//          via ApiClient.create(JouleApi::class.java).
//          Endpoints below are the three Shalon's screens call
//          (architecture.md §3): GET /dashboard/prosumer/{nic}/summary,
//          GET /reservations, GET /stations/nearby. Response shapes
//          are ProsumerDashboardModels.kt / NearbyStationModels.kt,
//          whose field names already match the JSON exactly.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.data.remote

import com.smartmicrogrid.ui.dashboard.ProsumerDashboardSummary
import com.smartmicrogrid.ui.dashboard.ReservationListItem
import com.smartmicrogrid.ui.operator.NearbyStation
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JouleApi {

    // GET /dashboard/prosumer/{nic}/summary — active/pending/approved-future counts + recent history.
    @GET("dashboard/prosumer/{nic}/summary")
    suspend fun getProsumerDashboardSummary(@Path("nic") nic: String): ProsumerDashboardSummary

    // GET /reservations?nic=&stationId=&status=&from=&to= — read-only search/filter. Pass null for
    // any filter to omit that query param entirely (the server treats a missing param as "no
    // filter on this field" — see DashboardService.SearchAsync). `status` must be a real
    // ReservationStatus value ("Confirmed"/"Completed"/"Cancelled") or null — never the UI's "All"
    // sentinel, which the server would reject as an invalid status.
    @GET("reservations")
    suspend fun searchReservations(
        @Query("nic") nic: String?,
        @Query("stationId") stationId: String?,
        @Query("status") status: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): List<ReservationListItem>

    // GET /stations/nearby?lat=&lng=&radiusKm= — active stations within radiusKm, closest first.
    @GET("stations/nearby")
    suspend fun getNearbyStations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double,
    ): List<NearbyStation>

    // TODO(Rukshan): add your own @GET/@POST method here for your own endpoint(s) — your own
    // function, don't edit anyone else's above.
    // TODO(Dinil): same — add yours here.
    // TODO(Migara): same — add yours here.
}
