// ============================================================
// File: CreateBookingScreen.kt
// Purpose: Prosumer UI to book an energy charging/discharging slot.
//          Features:
//          - Dropdown station selector populated via GET /api/stations
//          - Auto-selection if opened from map pin (Bonus Flow)
//          - Dynamic slot selection (chips) via GET /api/stations/{id}
//          - Native Date & Time picker enforcing the 7-day scheduling window
//          - Zero double-padding on top bar
// Author: Migara
// ============================================================
package com.smartmicrogrid.ui.prosumer

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.CreateReservationRequest
import com.smartmicrogrid.data.remote.dto.SlotResponse
import com.smartmicrogrid.data.remote.dto.StationSummaryResponse
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.theme.StripeAccent
import com.smartmicrogrid.ui.theme.StripeBrandVioletSoft
import com.smartmicrogrid.ui.theme.StripeInk
import com.smartmicrogrid.ui.theme.StripePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateBookingScreen(
    initialStationId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: BookingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Station list & selection state
    var stations by remember { mutableStateOf<List<StationSummaryResponse>>(emptyList()) }
    var isLoadingStations by remember { mutableStateOf(true) }
    var stationDropdownExpanded by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<StationSummaryResponse?>(null) }
    var isStationLocked by remember { mutableStateOf(initialStationId != null) }

    // Slots state
    var slots by remember { mutableStateOf<List<SlotResponse>>(emptyList()) }
    var isLoadingSlots by remember { mutableStateOf(false) }
    var selectedSlotId by remember { mutableStateOf("") }

    // Date & Time state
    var selectedDateTimeDisplay by remember { mutableStateOf("") }
    var scheduledIsoString by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    val actionResult by viewModel.actionResult.collectAsState()

    // 1. Fetch all active stations
    LaunchedEffect(Unit) {
        isLoadingStations = true
        runCatching {
            withContext(Dispatchers.IO) {
                ApiClient.service.getStations(status = "Active")
            }
        }.onSuccess { list ->
            stations = list
            if (!initialStationId.isNullOrBlank()) {
                val matched = list.firstOrNull { it.id == initialStationId }
                if (matched != null) {
                    selectedStation = matched
                }
            }
        }
        isLoadingStations = false
    }

    // 2. Fetch slots whenever the selected station changes
    LaunchedEffect(selectedStation) {
        val station = selectedStation
        selectedSlotId = ""
        slots = emptyList()
        if (station != null) {
            isLoadingSlots = true
            runCatching {
                withContext(Dispatchers.IO) {
                    ApiClient.service.getStationDetail(station.id)
                }
            }.onSuccess { detail ->
                slots = detail.slots
            }
            isLoadingSlots = false
        }
    }

    // 3. Handle booking result
    LaunchedEffect(actionResult) {
        if (actionResult != null) {
            isSubmitting = false
            if (actionResult == "Booking confirmed!") {
                onNavigateBack()
            }
        }
    }

    // 4. Function to open Date & Time Pickers with 7-day rule
    fun openDateTimePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val chosenCal = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, hourOfDay, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        // ISO 8601 formatting for backend (e.g., 2026-10-01T14:00:00Z)
                        val instant = chosenCal.toInstant()
                        scheduledIsoString = DateTimeFormatter.ISO_INSTANT.format(instant)

                        // Human-readable format for UI display
                        val friendlyFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy · HH:mm")
                            .withZone(ZoneOffset.systemDefault())
                        selectedDateTimeDisplay = friendlyFormat.format(instant)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY) + 1,
                    0,
                    true // 24-hour format
                )
                timePickerDialog.setTitle("Select Time")
                timePickerDialog.show()
            },
            currentYear,
            currentMonth,
            currentDay
        )

        // Enforce the 7-day window rule:
        // minDate = Now
        // maxDate = 7 days from now
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000L
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000L)
        datePickerDialog.setTitle("Select Date (Within 7 Days)")
        datePickerDialog.show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New Booking", fontWeight = FontWeight.SemiBold) },
                windowInsets = WindowInsets(0, 0, 0, 0), // Prevents double status bar padding!
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(JouleIcons.Back, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Reserve a battery charging or discharging slot at your preferred microgrid station.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ==========================================
            // SECTION 1: STATION SELECTION DROPDOWN
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Microgrid Station Hub",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isStationLocked && selectedStation != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = StripeBrandVioletSoft,
                            modifier = Modifier.clickable { isStationLocked = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = JouleIcons.MapPin,
                                    contentDescription = null,
                                    tint = StripePrimary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text("Map Selected (Change)", style = MaterialTheme.typography.labelSmall, color = StripePrimary)
                            }
                        }
                    }
                }

                if (isLoadingStations) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Loading nearby hubs…", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = stationDropdownExpanded,
                        onExpandedChange = {
                            if (!isStationLocked) stationDropdownExpanded = !stationDropdownExpanded
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedStation?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Station Hub") },
                            placeholder = { Text("Choose a microgrid station") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationDropdownExpanded)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StripePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = stationDropdownExpanded,
                            onDismissRequest = { stationDropdownExpanded = false }
                        ) {
                            stations.forEach { station ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(station.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Capacity: ${station.capacityKWh} kWh · ${station.totalBatterySlots} battery slots",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedStation = station
                                        isStationLocked = false
                                        stationDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 2: BATTERY SLOT SELECTION
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Battery Slot",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                if (selectedStation == null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Please choose a station hub above to see available slots.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else if (isLoadingSlots) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Fetching slot availability…", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (slots.isEmpty()) {
                    Text(
                        text = "No battery slots found for this station.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slots.forEach { slot ->
                            val isAvailable = slot.status.equals("Available", ignoreCase = true)
                            val isSelected = selectedSlotId == slot.id

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isAvailable) {
                                        selectedSlotId = slot.id
                                    }
                                },
                                enabled = isAvailable,
                                label = {
                                    Text(
                                        text = "Slot #${slot.slotNumber} · ${slot.type} (${slot.capacityKWh.toInt()} kWh)" +
                                                if (!isAvailable) " [${slot.status}]" else ""
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StripePrimary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 3: SCHEDULED TIME (7-DAY RULE PICKER)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Scheduled Reservation Time",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (scheduledIsoString.isNotBlank()) StripePrimary else MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDateTimePicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = JouleIcons.Calendar,
                            contentDescription = "Date and Time",
                            tint = if (scheduledIsoString.isNotBlank()) StripePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedDateTimeDisplay.isNotBlank()) selectedDateTimeDisplay else "Tap to choose Date & Time",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedDateTimeDisplay.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedDateTimeDisplay.isNotBlank()) StripeInk else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Permitted window: Today up to 7 days in advance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 4: ERROR / SUBMISSION
            // ==========================================
            actionResult?.let { message ->
                if (message != "Booking confirmed!") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            val canSubmit = selectedStation != null &&
                    selectedSlotId.isNotBlank() &&
                    scheduledIsoString.isNotBlank() &&
                    !isSubmitting

            Button(
                onClick = {
                    if (canSubmit) {
                        isSubmitting = true
                        val prosumerNic = com.smartmicrogrid.data.local.ProsumerSessionDao(context).getSession()?.nic
                        viewModel.createReservation(
                            CreateReservationRequest(
                                stationId = selectedStation!!.id,
                                slotId = selectedSlotId,
                                scheduledAt = scheduledIsoString,
                                prosumerNic = prosumerNic
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = StripePrimary),
                enabled = canSubmit
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirm Booking", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearActionResult()
        }
    }
}