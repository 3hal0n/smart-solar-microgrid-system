// ============================================================
// File: ApiService.kt
// Purpose: Retrofit interface. Dinil owns the verify-qr call
//          (operator scan). Other owners append their endpoints
//          here rather than creating separate interfaces.
// Author: Dinil (shared infra — coordinate before editing)
// ============================================================
package com.smartmicrogrid.data.remote

import com.smartmicrogrid.data.remote.dto.VerifyQrRequest
import com.smartmicrogrid.data.remote.dto.VerifyQrResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    // Dinil — Grid Operator scans a Prosumer's QR, server flips Confirmed -> Completed.
    @POST("api/reservations/verify-qr")
    suspend fun verifyQr(@Body req: VerifyQrRequest): Response<VerifyQrResponse>
}