// ============================================================
// File: ApiClient.kt
// Purpose: Shared Retrofit client — built jointly Day 1 per
//          architecture.md §8, same pattern as AppDbHelper.kt: one
//          singleton, one base URL, one place every screen's Api
//          interface gets built from. Base URL comes from
//          BuildConfig.API_BASE_URL (app/build.gradle.kts, itself
//          read from local.properties' API_BASE_URL — defaults to
//          10.0.2.2, the emulator's alias for the host machine's own
//          localhost, so `dotnet run`'s API is reachable with no
//          per-developer setup on an emulator).
// Author: Shalon (first to build it — the mobile app had no
//          networking layer at all until this; every screen's Api
//          interface gets its Retrofit instance from ApiClient.create
//          below rather than building its own).
// ============================================================
package com.smartmicrogrid.data.remote

import com.smartmicrogrid.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Logs full request/response bodies to Logcat (tag "OkHttp") — debug builds only in spirit
    // (this project has no release network usage yet), invaluable for a first real backend
    // integration where "why is this 400ing" is the most common question.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Builds a Retrofit implementation of the given Api interface, e.g.
    // ApiClient.create(JouleApi::class.java).
    fun <T> create(apiClass: Class<T>): T = retrofit.create(apiClass)
}
