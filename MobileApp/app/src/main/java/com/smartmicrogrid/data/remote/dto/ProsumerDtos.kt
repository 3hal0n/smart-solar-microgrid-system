// ============================================================
// File: ProsumerDtos.kt
// Purpose: Data Transfer Objects for Prosumer API communication.
//          Matches the backend C# DTOs exactly (JSON contract).
// Author: Rukshan
// ============================================================
package com.smartmicrogrid.data.remote.dto

import com.google.gson.annotations.SerializedName

// Request: Prosumer Registration
data class ProsumerRegistrationRequest(
    @SerializedName("nic") val nic: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("password") val password: String
)

// Request: Prosumer Login
data class ProsumerLoginRequest(
    @SerializedName("nic") val nic: String,
    @SerializedName("password") val password: String
)

// Response: Prosumer Login Success
data class ProsumerLoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("role") val role: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("nic") val nic: String,
    @SerializedName("status") val status: String
)

// Response: Prosumer Profile Details
data class ProsumerProfileResponse(
    @SerializedName("id") val id: String,
    @SerializedName("nic") val nic: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("status") val status: String,
    @SerializedName("deactivationRequestedAt") val deactivationRequestedAt: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

// Request: Update Prosumer Profile
data class ProsumerUpdateRequest(
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?
)