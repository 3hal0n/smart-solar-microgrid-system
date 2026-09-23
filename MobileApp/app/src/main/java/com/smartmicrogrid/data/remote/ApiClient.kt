// ============================================================
// File: ApiClient.kt
// Purpose: Shared Retrofit singleton for all API calls. Base URL
//          + JWT bearer attachment live here once, so no screen
//          talks to OkHttp directly. Created by Dinil for the
//          operator QR scanner; other owners should reuse this
//          file rather than create a second Retrofit instance.
// Author: Dinil (shared infra — coordinate before editing)
// ============================================================
package com.smartmicrogrid.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Emulator loopback for the host machine's localhost (10.0.2.2 is the AVD alias).
    // Change this to your IIS host once the backend is deployed.
    private const val BASE_URL = "http://10.0.2.2:5128/"

    // Filled in by login (Rukshan's screen) — the scanner never sets this itself.
    @Volatile
    var authToken: String? = null

    // Attaches the JWT to every outgoing request when present.
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

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}