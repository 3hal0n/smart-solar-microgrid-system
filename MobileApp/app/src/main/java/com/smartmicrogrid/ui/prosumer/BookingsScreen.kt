// ============================================================
// File: BookingsScreen.kt
// Purpose: UI for Prosumers to view, create, and manage their
//          energy bookings, including QR code display and
//          a local Dialog for creating new bookings.
// Author: Migara
// ============================================================
package com.smartmicrogrid.ui.prosumer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmicrogrid.data.remote.dto.CreateReservationRequest
import com.smartmicrogrid.data.remote.dto.ReservationResponse
import com.smartmicrogrid.util.QrCodeHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    prosumerNic: String = "TEST-NIC-123", // Replace with actual logged-in user NIC from Auth
    viewModel: BookingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var stationId by remember { mutableStateOf("") }
    var slotId by remember { mutableStateOf("") }
    var scheduledAt by remember { mutableStateOf("") }

    LaunchedEffect(prosumerNic) {
        viewModel.fetchReservations(prosumerNic)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Text("+", fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is BookingsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is BookingsUiState.Error -> {
                    Column(modifier = Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.fetchReservations(prosumerNic) }) { Text("Retry") }
                    }
                }
                is BookingsUiState.Success -> {
                    if (state.reservations.isEmpty()) {
                        Text("No bookings found.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.reservations) { reservation ->
                                ReservationCard(reservation, onCancel = { viewModel.cancelReservation(reservation.id) })
                            }
                        }
                    }
                }
            }

            actionResult?.let { message ->
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), action = {
                    TextButton(onClick = { viewModel.clearActionResult() }) { Text("Dismiss") }
                }) { Text(message) }
            }
        }
    }

    // CREATE BOOKING DIALOG
    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create New Booking", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = stationId,
                        onValueChange = { stationId = it },
                        label = { Text("Station ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = slotId,
                        onValueChange = { slotId = it },
                        label = { Text("Slot ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = scheduledAt,
                        onValueChange = { scheduledAt = it },
                        label = { Text("Time (e.g., 2026-10-01T14:00:00Z)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (stationId.isNotBlank() && slotId.isNotBlank() && scheduledAt.isNotBlank()) {
                                viewModel.createReservation(CreateReservationRequest(stationId, slotId, scheduledAt))
                                showCreateDialog = false
                                stationId = ""
                                slotId = ""
                                scheduledAt = ""
                            }
                        }) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationCard(reservation: ReservationResponse, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // Safe, clean way to get the last 6 characters
                val shortId = reservation.id.takeLast(6)
                Text(text = "Booking #$shortId", fontWeight = FontWeight.Bold)
                Surface(color = if (reservation.status == "Confirmed") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Text(text = reservation.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Scheduled: ${reservation.scheduledAt}")
            Text("Station ID: ${reservation.stationId}")

            if (reservation.status == "Confirmed" && !reservation.qrToken.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Scan at the station")
                    Spacer(modifier = Modifier.height(8.dp))
                    val qrBitmap = remember(reservation.qrToken) { QrCodeHelper.generateQrCodeBitmap(reservation.qrToken, size = 180) }
                    if (qrBitmap != null) {
                        androidx.compose.foundation.Image(
                            // CORRECT (Just pass the bitmap directly)
                            bitmap = qrBitmap,
                            contentDescription = "QR Code",
                            modifier = Modifier.size(180.dp)
                        )
                    }
                }
            }
            if (reservation.status == "Confirmed") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onCancel, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Cancel Booking")
                }
            }
        }
    }
}