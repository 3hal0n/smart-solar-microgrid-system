// ============================================================
// File: ApiClient.kt
// Purpose: Shared Retrofit singleton for all API calls. Base URL
//          + JWT bearer attachment live here once, so no screen
//          talks to OkHttp directly.
//          Merged 2026-09-26: Dinil and Shalon each built this file
//          independently (Dinil first, for the operator QR scanner)
//          before coordinating. Kept from Dinil's version: the
//          `authToken`/interceptor (now filled in by Migara's real
//          LoginPage, not just "Rukshan's screen" - ownership moved
//          per architecture.md's v3 restructuring) and the `.service`
//          property his ScanQrScreen.kt already calls. Kept from
//          Shalon's version: BuildConfig-driven base URL (so a
//          physical device or a different port doesn't need a code
//          change, just local.properties) and the generic `create<T>`
//          factory, since ApiService.kt now holds every screen's
//          endpoints per Dinil's "append here" convention below.
// Author: Dinil + Shalon (shared infra - coordinate before editing)
// ============================================================
package com.smartmicrogrid.data.remote

import com.smartmicrogrid.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Filled in by LoginPage's role-based session (Migara) - screens that don't require auth
    // (e.g. the mobile Prosumer flows, still pre-login) simply never set this.
    @Volatile
    var authToken: String? = null

    // Attaches the JWT to every outgoing request once one exists.
    private val authInterceptor = Interceptor { chain ->
        val token = authToken
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // The one shared endpoint interface - see ApiService.kt. Every screen calls ApiClient.service.
    val service: ApiService by lazy { retrofit.create(ApiService::class.java) }

    // Escape hatch for a second interface if one's ever genuinely needed; unused today now that
    // ApiService.kt holds everyone's endpoints.
    fun <T> create(apiClass: Class<T>): T = retrofit.create(apiClass)
}
