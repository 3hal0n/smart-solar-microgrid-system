// ============================================================
// File: BookingsScreen.kt
// Purpose: Enhanced Prosumer bookings list — polished cards with
//          collapsible QR code sections, status badges, formatted
//          timestamps, and the station name pulled from the response.
// Author: Migara (enhanced by Shalon 2026-09-29)
// ============================================================
package com.smartmicrogrid.ui.prosumer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmicrogrid.data.remote.dto.CreateReservationRequest
import com.smartmicrogrid.data.remote.dto.ReservationResponse
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.theme.*
import com.smartmicrogrid.util.QrCodeHelper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Formats "2026-10-01T14:00:00Z" → "Oct 1, 2026 · 2:00 PM"
private fun formatScheduledAt(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val instant = Instant.parse(iso)
        val zdt = instant.atZone(ZoneId.systemDefault())
        val datePart = DateTimeFormatter.ofPattern("MMM d, yyyy").format(zdt)
        val timePart = DateTimeFormatter.ofPattern("h:mm a").format(zdt)
        "$datePart · $timePart"
    } catch (_: Exception) {
        iso
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    prosumerNic: String? = null,
    onNavigateToCreate: (() -> Unit)? = null,
    viewModel: BookingsViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectiveNic = remember(prosumerNic) {
        prosumerNic ?: com.smartmicrogrid.data.local.ProsumerSessionDao(context).getSession()?.nic ?: "TEST-NIC-123"
    }

    val uiState by viewModel.uiState.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var stationId by remember { mutableStateOf("") }
    var slotId by remember { mutableStateOf("") }
    var scheduledAt by remember { mutableStateOf("") }

    LaunchedEffect(effectiveNic) {
        viewModel.fetchReservations(effectiveNic)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Bookings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StripeInk,
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StripeSurface,
                    actionIconContentColor = StripePrimary,
                ),
                actions = {
                    IconButton(onClick = {
                        if (onNavigateToCreate != null) {
                            onNavigateToCreate()
                        } else {
                            showCreateDialog = true
                        }
                    }) {
                        Icon(
                            imageVector = JouleIcons.Calendar,
                            contentDescription = "New booking",
                            tint = StripePrimary,
                        )
                    }
                },
            )
        },
        containerColor = StripeCanvas,
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is BookingsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = StripePrimary,
                        strokeWidth = 3.dp,
                    )
                }
                is BookingsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Couldn't load bookings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StripeInk,
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = StripeMuted,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = { viewModel.fetchReservations(effectiveNic) },
                            colors = ButtonDefaults.buttonColors(containerColor = StripePrimary),
                            shape = RoundedCornerShape(50),
                        ) { Text("Retry") }
                    }
                }
                is BookingsUiState.Success -> {
                    if (state.reservations.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StripeBrandVioletSoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = JouleIcons.Calendar,
                                    contentDescription = null,
                                    tint = StripePrimary,
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                            Text(
                                "No bookings yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = StripeInk,
                            )
                            Text(
                                "Tap the calendar icon above to reserve an energy slot at a nearby station.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StripeMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.reservations) { reservation ->
                                ReservationCard(
                                    reservation = reservation,
                                    onCancel = { viewModel.cancelReservation(reservation.id) },
                                )
                            }
                        }
                    }
                }
            }

            actionResult?.let { message ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearActionResult() }) {
                            Text("Dismiss", color = StripePrimary)
                        }
                    },
                    containerColor = StripeInk,
                    contentColor = Color.White,
                ) { Text(message) }
            }
        }
    }

    // CREATE BOOKING DIALOG (legacy fallback — normal path now navigates to CreateBookingScreen)
    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StripeSurface,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                tonalElevation = 4.dp,
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Create New Booking",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StripeInk,
                    )
                    OutlinedTextField(value = stationId, onValueChange = { stationId = it }, label = { Text("Station ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = slotId, onValueChange = { slotId = it }, label = { Text("Slot ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = scheduledAt, onValueChange = { scheduledAt = it }, label = { Text("Time (e.g., 2026-10-01T14:00:00Z)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = StripeMuted) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (stationId.isNotBlank() && slotId.isNotBlank() && scheduledAt.isNotBlank()) {
                                    viewModel.createReservation(CreateReservationRequest(stationId, slotId, scheduledAt))
                                    showCreateDialog = false; stationId = ""; slotId = ""; scheduledAt = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StripePrimary),
                            shape = RoundedCornerShape(50),
                        ) { Text("Confirm") }
                    }
                }
            }
        }
    }
}

// Status-to-color mapping
private data class StatusStyle(val bg: Color, val text: Color, val label: String)
private fun statusStyle(status: String): StatusStyle = when (status) {
    "Confirmed"  -> StatusStyle(StripeSuccessContainer, StripeSuccess, "Confirmed")
    "Completed"  -> StatusStyle(StripeCyanContainer, StripeOnCyanContainer, "Completed")
    "Cancelled"  -> StatusStyle(StripeErrorContainer, StripeError, "Cancelled")
    else         -> StatusStyle(StripeTray, StripeMuted, status)
}

@Composable
fun ReservationCard(reservation: ReservationResponse, onCancel: () -> Unit) {
    var qrExpanded by remember { mutableStateOf(false) }
    val style = statusStyle(reservation.status)
    val shortId = reservation.id.takeLast(6)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StripeBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StripeSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Header ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Booking #$shortId",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StripeInk,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatScheduledAt(reservation.scheduledAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = StripeMuted,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(style.bg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = style.text,
                    )
                }
            }

            HorizontalDivider(color = StripeBorder)

            // ── Detail Row ───────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DetailChip(label = "Station", value = reservation.stationId.takeLast(8), modifier = Modifier.weight(1f))
                DetailChip(label = "Slot", value = reservation.slotId?.takeLast(6) ?: "—", modifier = Modifier.weight(1f))
            }

            // ── QR Code toggle (Confirmed only) ──────────────
            if (reservation.status == "Confirmed" && !reservation.qrToken.isNullOrEmpty()) {
                HorizontalDivider(color = StripeBorder)
                // Toggle row
                TextButton(
                    onClick = { qrExpanded = !qrExpanded },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = if (qrExpanded) JouleIcons.Check else JouleIcons.Scan,
                        contentDescription = null,
                        tint = StripePrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (qrExpanded) "Hide QR Code" else "Show QR Code for Scan",
                        style = MaterialTheme.typography.labelMedium,
                        color = StripePrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (qrExpanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = StripeMuted,
                    )
                }

                // Expandable QR Section
                AnimatedVisibility(
                    visible = qrExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StripeCanvas)
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Present at the station node",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StripeInk,
                        )
                        // QR frame card
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, StripeBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val qrBitmap = remember(reservation.qrToken) {
                                QrCodeHelper.generateQrCodeBitmap(reservation.qrToken, size = 220)
                            }
                            if (qrBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "Reservation QR Code",
                                    modifier = Modifier.size(220.dp),
                                )
                            } else {
                                Text("QR unavailable", color = StripeMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(
                            "Token: …${reservation.qrToken.takeLast(12)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StripeMuted,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                    }
                }
            }

            // ── Cancel Button ────────────────────────────────
            if (reservation.status == "Confirmed") {
                HorizontalDivider(color = StripeBorder)
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = StripeError),
                ) {
                    Text(
                        "Cancel Booking",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StripeTray)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = StripeMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = StripeInk,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        )
    }
}