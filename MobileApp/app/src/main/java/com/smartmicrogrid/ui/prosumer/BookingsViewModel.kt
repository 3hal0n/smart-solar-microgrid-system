// ============================================================
// File: BookingsViewModel.kt
// Purpose: Manages state and API calls for the Prosumer Bookings screen.
//          Fetches reservations, handles creation, and cancellation.
// Author: Migara
// ============================================================

package com.smartmicrogrid.ui.prosumer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmicrogrid.data.remote.ApiService
import com.smartmicrogrid.data.remote.dto.CancelReservationRequest
import com.smartmicrogrid.data.remote.dto.CreateReservationRequest
import com.smartmicrogrid.data.remote.dto.ReservationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Sealed class to represent the UI state
sealed class BookingsUiState {
    object Loading : BookingsUiState()
    data class Success(val reservations: List<ReservationResponse>) : BookingsUiState()
    data class Error(val message: String) : BookingsUiState()
}

class BookingsViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookingsUiState>(BookingsUiState.Loading)
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    // Fetch reservations for a specific prosumer NIC
    fun fetchReservations(nic: String) {
        viewModelScope.launch {
            _uiState.value = BookingsUiState.Loading
            try {
                // Fetch only "Confirmed" or "Completed" reservations for this user
                val response = apiService.searchReservations(
                    nic = nic,
                    status = null, // Fetch all statuses, UI can filter if needed
                    from = null,
                    to = null,
                    stationId = null
                )
                _uiState.value = BookingsUiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = BookingsUiState.Error(e.message ?: "Failed to load reservations")
            }
        }
    }

    // Create a new reservation
    fun createReservation(request: CreateReservationRequest) {
        viewModelScope.launch {
            _uiState.value = BookingsUiState.Loading
            try {
                val response = apiService.createReservation(request)
                if (response.isSuccessful && response.body() != null) {
                    _actionResult.value = "Booking confirmed! QR Code generated."
                    // Refresh the list (assuming we have the NIC, in a real app we'd pass it or fetch current user)
                    // For now, we'll let the UI handle the refresh or we can store the NIC in the VM.
                } else {
                    _actionResult.value = response.errorBody()?.string() ?: "Failed to create booking"
                }
            } catch (e: Exception) {
                _actionResult.value = e.message ?: "Network error"
            }
        }
    }

    // Cancel an existing reservation
    fun cancelReservation(id: String, reason: String? = "Cancelled by user") {
        viewModelScope.launch {
            _uiState.value = BookingsUiState.Loading
            try {
                val response = apiService.cancelReservation(id, CancelReservationRequest(reason))
                if (response.isSuccessful) {
                    _actionResult.value = "Reservation cancelled successfully."
                } else {
                    _actionResult.value = response.errorBody()?.string() ?: "Failed to cancel"
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