// ============================================================
// File: ApiService.kt
// Purpose: Retrofit interface. Dinil owns the verify-qr call
//          (operator scan); Shalon owns the dashboard/reservations-
//          search/nearby-stations calls below (merged in 2026-09-26
//          — previously lived in a separate JouleApi.kt Shalon built
//          before coordinating with Dinil on this file). Other owners
//          append their endpoints here rather than creating separate
//          interfaces. Response shapes are
//          ProsumerDashboardModels.kt / NearbyStationModels.kt, whose
//          field names already match the JSON exactly.
// Author: Dinil + Shalon (shared infra — coordinate before editing)
// ============================================================
package com.smartmicrogrid.data.remote

import com.smartmicrogrid.data.remote.dto.VerifyQrRequest
import com.smartmicrogrid.data.remote.dto.VerifyQrResponse
import com.smartmicrogrid.ui.dashboard.ProsumerDashboardSummary
import com.smartmicrogrid.ui.dashboard.ReservationListItem
import com.smartmicrogrid.ui.operator.NearbyStation
import com.smartmicrogrid.data.remote.dto.ProsumerLoginRequest
import com.smartmicrogrid.data.remote.dto.ProsumerLoginResponse
import com.smartmicrogrid.data.remote.dto.ProsumerProfileResponse
import com.smartmicrogrid.data.remote.dto.ProsumerRegistrationRequest
import com.smartmicrogrid.data.remote.dto.ProsumerUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PUT

interface ApiService {

    // Dinil — Grid Operator scans a Prosumer's QR, server flips Confirmed -> Completed.
    @POST("api/reservations/verify-qr")
    suspend fun verifyQr(@Body req: VerifyQrRequest): Response<VerifyQrResponse>

    // Shalon — GET /dashboard/prosumer/{nic}/summary: active/pending/approved-future counts +
    // recent history, for ProsumerDashboardScreen.kt's stat trays.
    @GET("api/dashboard/prosumer/{nic}/summary")
    suspend fun getProsumerDashboardSummary(@Path("nic") nic: String): ProsumerDashboardSummary

    // Shalon — GET /reservations?nic=&stationId=&status=&from=&to=: read-only search/filter for
    // ProsumerDashboardScreen.kt's booking lists. Pass null for any filter to omit that query
    // param entirely (the server treats a missing param as "no filter on this field"). `status`
    // must be a real ReservationStatus value ("Confirmed"/"Completed"/"Cancelled") or null — never
    // the UI's "All" sentinel, which the server would reject as an invalid status.
    @GET("api/reservations")
    suspend fun searchReservations(
        @Query("nic") nic: String?,
        @Query("stationId") stationId: String?,
        @Query("status") status: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): List<ReservationListItem>

    // Shalon — GET /stations/nearby?lat=&lng=&radiusKm=: active stations within radiusKm, closest
    // first, for MapScreen.kt's markers.
    @GET("api/stations/nearby")
    suspend fun getNearbyStations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double,
    ): List<NearbyStation>

    // ============================================================
    // PROSUMER AUTHENTICATION & PROFILE ENDPOINTS (Rukshan)
    // Note: ApiClient.authInterceptor automatically attaches Bearer token.
    // Author: Rukshan
    // ============================================================

    @POST("api/prosumers/register")
    suspend fun registerProsumer(@Body request: ProsumerRegistrationRequest): Response<Any>

    @POST("api/auth/prosumer/login")
    suspend fun loginProsumer(@Body request: ProsumerLoginRequest): Response<ProsumerLoginResponse>

    @GET("api/prosumers/{nic}")
    suspend fun getProsumerProfile(@Path("nic") nic: String): Response<ProsumerProfileResponse>

    @PUT("api/prosumers/{nic}")
    suspend fun updateProsumerProfile(
        @Path("nic") nic: String,
        @Body request: ProsumerUpdateRequest
    ): Response<Unit>

    @PUT("api/prosumers/{nic}/request-deactivation")
    suspend fun requestDeactivation(@Path("nic") nic: String): Response<Unit>
}
