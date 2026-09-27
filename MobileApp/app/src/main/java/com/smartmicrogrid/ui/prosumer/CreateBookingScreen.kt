// ============================================================
// File: CreateBookingScreen.kt
// Purpose: UI for Prosumers to create a new energy slot reservation.
// Author: Migara
// ============================================================
package com.smartmicrogrid.ui.prosumer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmicrogrid.data.remote.dto.CreateReservationRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBookingScreen(
    onNavigateBack: () -> Unit,
    viewModel: BookingsViewModel = viewModel()
) {
    var stationId by remember { mutableStateOf("") }
    var slotId by remember { mutableStateOf("") }
    var scheduledAt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Observe the action result from ViewModel
    val actionResult by viewModel.actionResult.collectAsState()

    LaunchedEffect(actionResult) {
        if (actionResult != null) {
            isLoading = false
            if (actionResult == "Booking confirmed!") {
                onNavigateBack() // Go back to list on success
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Booking") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Enter the details of the slot you wish to reserve.", style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = stationId,
                onValueChange = { stationId = it },
                label = { Text("Station ID") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = slotId,
                onValueChange = { slotId = it },
                label = { Text("Slot ID") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = scheduledAt,
                onValueChange = { scheduledAt = it },
                label = { Text("Scheduled Time (e.g., 2026-10-01T14:00:00Z)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (stationId.isNotBlank() && slotId.isNotBlank() && scheduledAt.isNotBlank()) {
                        isLoading = true
                        viewModel.createReservation(CreateReservationRequest(stationId, slotId, scheduledAt))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Confirm Booking")
                }
            }
        }
    }
}