// ============================================================
// File: BookingsScreen.kt
// Purpose: UI for Prosumers to view, create, and manage their
//          energy bookings, including QR code display.
// Author: Migara
// ============================================================

package com.smartmicrogrid.ui.prosumer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmicrogrid.data.remote.dto.ReservationResponse
import com.smartmicrogrid.util.QrCodeHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    prosumerNic: String = "TEST-NIC-123", // Replace with actual logged-in user NIC
    viewModel: BookingsViewModel = viewModel() // In a real app, inject this via Hilt/Koin
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    // Fetch data on first composition
    LaunchedEffect(prosumerNic) {
        viewModel.fetchReservations(prosumerNic)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings") },
                actions = {
                    IconButton(onClick = { /* TODO: Open Create Booking Dialog */ }) {
                        Text("+", fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is BookingsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is BookingsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.fetchReservations(prosumerNic) }) {
                            Text("Retry")
                        }
                    }
                }
                is BookingsUiState.Success -> {
                    if (state.reservations.isEmpty()) {
                        Text(
                            "No bookings found.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.reservations) { reservation ->
                                ReservationCard(reservation, oncancel = {
                                    viewModel.cancelReservation(reservation.id)
                                })
                            }
                        }
                    }
                }
            }

            // Show action result toast/snackbar
            actionResult?.let { message ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearActionResult() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }
}

@Composable
fun ReservationCard(
    reservation: ReservationResponse,
    oncancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Booking #${reservation.id.takeLast(6)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
                // Status Badge
                Surface(
                    color = when (reservation.status) {
                        "Confirmed" -> MaterialTheme.colorScheme.primaryContainer
                        "Completed" -> MaterialTheme.colorScheme.secondaryContainer
                        "Cancelled" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = reservation.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Format the date nicely
            val formattedDate = try {
                val dt = LocalDateTime.parse(reservation.scheduledAt.replace("Z", ""))
                dt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
            } catch (e: Exception) {
                reservation.scheduledAt
            }

            Text("Scheduled: $formattedDate")
            Text("Station ID: ${reservation.stationId}")
            Text("Slot ID: ${reservation.slotId}")

            // Show QR Code if Confirmed and token exists
            if (reservation.status == "Confirmed" && !reservation.qrToken.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Scan at the station", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Generate QR Bitmap using our helper
                    val qrBitmap = remember(reservation.qrToken) {
                        QrCodeHelper.generateQrCodeBitmap(reservation.qrToken, size = 180)
                    }

                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code for reservation ${reservation.id}",
                            modifier = Modifier.size(180.dp)
                        )
                    } else {
                        Text("Failed to generate QR Code", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Cancel Button (Only for Confirmed bookings)
            if (reservation.status == "Confirmed") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = oncancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Booking")
                }
            }
        }
    }
}