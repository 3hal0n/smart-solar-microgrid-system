// ============================================================
// File: BookingsViewModel.kt
// Purpose: Manages state and API calls for the Prosumer Bookings screen.
//          Fetches reservations, handles creation, and cancellation.
// Author: Migara
// ============================================================
package com.smartmicrogrid.ui.prosumer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.ApiService
import com.smartmicrogrid.data.remote.dto.CancelReservationRequest
import com.smartmicrogrid.data.remote.dto.CreateReservationRequest
import com.smartmicrogrid.data.remote.dto.ReservationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BookingsUiState {
    data object Loading : BookingsUiState()
    data class Success(val reservations: List<ReservationResponse>) : BookingsUiState()
    data class Error(val message: String) : BookingsUiState()
}

class BookingsViewModel @JvmOverloads constructor(
    private val apiService: ApiService = ApiClient.service
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookingsUiState>(BookingsUiState.Loading)
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    private var currentNic: String? = null

    fun fetchReservations(nic: String) {
        currentNic = nic
        viewModelScope.launch {
            _uiState.value = BookingsUiState.Loading
            try {
                val response = apiService.searchReservations(
                    nic = nic, status = null, from = null, to = null, stationId = null
                )
                _uiState.value = BookingsUiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = BookingsUiState.Error(e.message ?: "Failed to load reservations")
            }
        }
    }

    fun createReservation(request: CreateReservationRequest) {
        viewModelScope.launch {
            try {
                val response = apiService.createReservation(request)
                if (response.isSuccessful) {
                    _actionResult.value = "Booking confirmed!"
                    currentNic?.let { fetchReservations(it) }
                } else {
                    val errorMsg = try {
                        val raw = response.errorBody()?.string()
                        if (!raw.isNullOrBlank()) {
                            val parsed = com.google.gson.JsonParser.parseString(raw).asJsonObject
                            parsed.get("message")?.asString ?: "Error: ${response.code()}"
                        } else {
                            "Failed to create booking (${response.code()})"
                        }
                    } catch (ex: Exception) {
                        "Failed to create booking (${response.code()})"
                    }
                    _actionResult.value = errorMsg
                }
            } catch (e: Exception) {
                _actionResult.value = e.message ?: "Network error"
            }
        }
    }

    fun cancelReservation(id: String, reason: String? = "Cancelled by user") {
        viewModelScope.launch {
            try {
                val response = apiService.cancelReservation(id, CancelReservationRequest(reason))
                if (response.isSuccessful) {
                    _actionResult.value = "Reservation cancelled."
                    currentNic?.let { fetchReservations(it) }
                } else {
                    val errorMsg = try {
                        val raw = response.errorBody()?.string()
                        if (!raw.isNullOrBlank()) {
                            val parsed = com.google.gson.JsonParser.parseString(raw).asJsonObject
                            parsed.get("message")?.asString ?: "Error: ${response.code()}"
                        } else {
                            "Failed to cancel (${response.code()})"
                        }
                    } catch (ex: Exception) {
                        "Failed to cancel (${response.code()})"
                    }
                    _actionResult.value = errorMsg
                }
            } catch (e: Exception) {
                _actionResult.value = e.message ?: "Network error"
            }
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }
}