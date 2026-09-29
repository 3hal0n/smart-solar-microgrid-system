// ============================================================
// File: StaffAuthDtos.kt
// Purpose: DTOs for staff (Grid Operator/Backoffice) login via the
//          existing POST /api/auth/login (AuthController.cs, Migara)
//          - the same endpoint the web app's LoginPage already uses.
//          Kept separate from ProsumerDtos.kt since staff accounts are
//          username-based, not NIC-based - a different login mechanism
//          entirely from Rukshan's Prosumer auth flow.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StaffLoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class StaffLoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("role") val role: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("userId") val userId: String
)
