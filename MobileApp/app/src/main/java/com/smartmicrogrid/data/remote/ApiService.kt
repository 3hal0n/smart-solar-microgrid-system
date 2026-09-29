// ============================================================
// File: ApiService.kt
// Purpose: Retrofit interface. Dinil owns the verify-qr call
//          (operator scan); Shalon owns the dashboard/reservations-
//          search/nearby-stations calls. Migara added the
//          Prosumer booking lifecycle endpoints (create/update/cancel).
//          Rukshan added the Prosumer authentication/profile endpoints
//          (merged from feature/prosumer-management-mobile-app).
// Author: Dinil + Shalon + Migara + Rukshan (shared infra)
// ============================================================
package com.smartmicrogrid.data.remote

import com.smartmicrogrid.data.remote.dto.*
import com.smartmicrogrid.ui.dashboard.ProsumerDashboardSummary
import com.smartmicrogrid.ui.operator.NearbyStation
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Dinil - Grid Operator scans a Prosumer's QR, server flips Confirmed -> Completed.
    @POST("api/reservations/verify-qr")
    suspend fun verifyQr(@Body req: VerifyQrRequest): Response<VerifyQrResponse>

    // Migara - Prosumer creates a new booking.
    @POST("api/reservations")
    suspend fun createReservation(@Body req: CreateReservationRequest): Response<CreateReservationResponse>

    // Migara - Prosumer modifies an existing booking (time/slot).
    @PUT("api/reservations/{id}")
    suspend fun updateReservation(@Path("id") id: String, @Body req: UpdateReservationRequest): Response<Void>

    // Migara - Prosumer cancels a booking.
    @PUT("api/reservations/{id}/cancel")
    suspend fun cancelReservation(@Path("id") id: String, @Body req: CancelReservationRequest): Response<Void>

    // Shalon/Migara - GET /api/reservations?nic=&stationId=&status=&from=&to=
    // Updated to return List<ReservationResponse> to match backend and provide full details (including qrToken)
    @GET("api/reservations")
    suspend fun searchReservations(
        @Query("nic") nic: String?,
        @Query("stationId") stationId: String?,
        @Query("status") status: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): List<ReservationResponse>

    // Shalon - GET /dashboard/prosumer/{nic}/summary
    @GET("api/dashboard/prosumer/{nic}/summary")
    suspend fun getProsumerDashboardSummary(@Path("nic") nic: String): ProsumerDashboardSummary

    // Shalon - GET /stations/nearby?lat=&lng=&radiusKm=
    @GET("api/stations/nearby")
    suspend fun getNearbyStations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double,
    ): List<NearbyStation>

    // GET /api/stations - lists stations
    @GET("api/stations")
    suspend fun getStations(
        @Query("search") search: String? = null,
        @Query("status") status: String? = "Active"
    ): List<StationSummaryResponse>

    // GET /api/stations/{id} - station detail with battery slots
    @GET("api/stations/{id}")
    suspend fun getStationDetail(@Path("id") id: String): StationDetailResponse

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

    // Shalon - Grid Operator (staff) login, same endpoint the web app's LoginPage already uses.
    // Username-based, not NIC-based - a separate mechanism from Rukshan's Prosumer login above.
    @POST("api/auth/login")
    suspend fun loginStaff(@Body request: StaffLoginRequest): Response<StaffLoginResponse>
}
