// ============================================================
// File: MapScreen.kt
// Purpose: Microgrid nodes map for both Prosumer and Operator.
//          Defaults to showing all stations with an expandable
//          filter panel (name search, available slots, min capacity).
// ============================================================
package com.smartmicrogrid.ui.operator

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberCameraPositionState
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.ui.components.IconTile
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Sri Lanka Center default
private val DEFAULT_CENTER = LatLng(6.9271, 79.8612)
private const val DEFAULT_ZOOM = 11f
private const val SEARCH_RADIUS_ALL_KM = 500.0 // Load all stations by default

@Composable
fun MapScreen(
    onStationSelect: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val api = remember { ApiClient.service }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var showRationaleDialog by remember { mutableStateOf(!hasLocationPermission) }
    var center by remember { mutableStateOf<LatLng?>(null) }
    var usingDeviceLocation by remember { mutableStateOf(false) }
    var allStations by remember { mutableStateOf<List<NearbyStation>?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var mapRefreshToken by remember { mutableIntStateOf(0) }

    // Filter states
    var isFilterExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterAvailableOnly by remember { mutableStateOf(false) }
    var filterMinCapacityKWh by remember { mutableDoubleStateOf(0.0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasLocationPermission = granted
        if (!granted) {
            center = DEFAULT_CENTER
            usingDeviceLocation = false
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            if (center == null) {
                center = DEFAULT_CENTER
                usingDeviceLocation = false
            }
            return@LaunchedEffect
        }
        val resolved = readLastKnownLocation(context)
        center = resolved ?: DEFAULT_CENTER
        usingDeviceLocation = resolved != null
    }

    LaunchedEffect(center, mapRefreshToken) {
        val current = center ?: return@LaunchedEffect
        allStations = null
        mapError = null
        runCatching { api.getNearbyStations(current.latitude, current.longitude, SEARCH_RADIUS_ALL_KM) }
            .onSuccess { allStations = it }
            .onFailure { mapError = it.message ?: "Couldn't reach the server." }
    }

    if (showRationaleDialog) {
        LocationRationaleDialog(
            onAllow = {
                showRationaleDialog = false
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = {
                showRationaleDialog = false
                center = DEFAULT_CENTER
                usingDeviceLocation = false
            },
        )
    }

    // Filter computation
    val filteredStations = remember(allStations, searchQuery, filterAvailableOnly, filterMinCapacityKWh) {
        allStations?.filter { s ->
            val matchesSearch = searchQuery.isBlank() || s.name.contains(searchQuery, ignoreCase = true)
            val matchesSlots = !filterAvailableOnly || s.availableSlots > 0
            val matchesCapacity = filterMinCapacityKWh <= 0.0 || s.capacityKWh >= filterMinCapacityKWh
            matchesSearch && matchesSlots && matchesCapacity
        }
    }

    val activeFilterCount = (if (searchQuery.isNotBlank()) 1 else 0) +
        (if (filterAvailableOnly) 1 else 0) +
        (if (filterMinCapacityKWh > 0.0) 1 else 0)

    Box(modifier = Modifier.fillMaxSize().background(StripeCanvas)) {
        when {
            mapError != null -> ErrorState(message = mapError!!, onRetry = { mapRefreshToken++ })
            center == null || allStations == null -> LoadingState()
            filteredStations != null && filteredStations.isEmpty() -> {
                StationsMap(center = center!!, stations = emptyList(), onStationSelect = onStationSelect)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 130.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StripeSurface,
                        border = BorderStroke(1.dp, StripeBorder),
                        shadowElevation = 6.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("No stations match filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StripeInk)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Try adjusting or resetting your filter criteria.", style = MaterialTheme.typography.bodySmall, color = StripeBody, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    filterAvailableOnly = false
                                    filterMinCapacityKWh = 0.0
                                },
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            }
            else -> StationsMap(center = center!!, stations = filteredStations ?: allStations!!, onStationSelect = onStationSelect)
        }

        // Floating Filter & Status Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Main Top Bar Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StripeSurface,
                border = BorderStroke(1.dp, StripeBorder),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = JouleIcons.MapPin,
                            contentDescription = null,
                            tint = StripePrimary,
                            modifier = Modifier.size(22.dp),
                        )
                        Column {
                            Text(
                                text = if (usingDeviceLocation) "Near Your Location" else "All Microgrid Hubs",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StripeInk,
                            )
                            Text(
                                text = if (filteredStations != null && allStations != null) {
                                    "${filteredStations.size} of ${allStations!!.size} stations shown"
                                } else {
                                    "Loading stations…"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = StripeMuted,
                                fontSize = 11.sp,
                            )
                        }
                    }

                    // Filter Expand Toggle Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isFilterExpanded || activeFilterCount > 0) StripeBrandVioletSoft else StripeSurfaceAlt,
                        border = BorderStroke(
                            1.dp,
                            if (activeFilterCount > 0) StripePrimary else StripeBorder,
                        ),
                        modifier = Modifier.clickable { isFilterExpanded = !isFilterExpanded },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = JouleIcons.Filter,
                                contentDescription = "Filter",
                                tint = if (activeFilterCount > 0) StripePrimary else StripeInk,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = if (activeFilterCount > 0) "Filter ($activeFilterCount)" else "Filter",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeFilterCount > 0) StripePrimary else StripeInk,
                            )
                        }
                    }
                }
            }

            // Expandable Filter Panel
            AnimatedVisibility(
                visible = isFilterExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StripeSurface,
                    border = BorderStroke(1.dp, StripeBorder),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Filter Stations",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StripeInk,
                            )
                            if (activeFilterCount > 0) {
                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        filterAvailableOnly = false
                                        filterMinCapacityKWh = 0.0
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    Text("Reset all", style = MaterialTheme.typography.labelSmall, color = StripePrimary)
                                }
                            }
                        }

                        // Search by name
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search station name or city…", fontSize = 13.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(JouleIcons.Search, contentDescription = null, tint = StripeMuted, modifier = Modifier.size(16.dp))
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = StripeBorder,
                                focusedBorderColor = StripePrimary,
                                unfocusedContainerColor = StripeSurfaceAlt,
                                focusedContainerColor = StripeSurfaceAlt,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Availability filter chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Slot Availability", style = MaterialTheme.typography.labelSmall, color = StripeMuted, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterOptionChip(
                                    label = "All Stations",
                                    selected = !filterAvailableOnly,
                                    onSelect = { filterAvailableOnly = false },
                                )
                                FilterOptionChip(
                                    label = "Available Slots Only",
                                    selected = filterAvailableOnly,
                                    onSelect = { filterAvailableOnly = true },
                                )
                            }
                        }

                        // Minimum Capacity filter chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Minimum Capacity", style = MaterialTheme.typography.labelSmall, color = StripeMuted, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    0.0 to "Any",
                                    50.0 to "≥ 50 kW",
                                    100.0 to "≥ 100 kW",
                                    200.0 to "≥ 200 kW",
                                ).forEach { (cap, label) ->
                                    FilterOptionChip(
                                        label = label,
                                        selected = filterMinCapacityKWh == cap,
                                        onSelect = { filterMinCapacityKWh = cap },
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { isFilterExpanded = false },
                            colors = ButtonDefaults.buttonColors(containerColor = StripePrimary, contentColor = StripeOnPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                        ) {
                            Text("Apply Filters", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterOptionChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) StripePrimary else StripeSurfaceAlt,
        border = BorderStroke(1.dp, if (selected) StripePrimary else StripeBorder),
        modifier = Modifier.clickable(onClick = onSelect),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else StripeInk,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@SuppressLint("MissingPermission")
private suspend fun readLastKnownLocation(context: android.content.Context): LatLng? =
    withContext(Dispatchers.IO) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        try {
            val location = com.google.android.gms.tasks.Tasks.await(client.lastLocation)
            location?.let { LatLng(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }

@Composable
private fun LocationRationaleDialog(onAllow: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use your location?") },
        text = { Text("Allow location access to center the map on grid nodes near you. You can still browse all microgrid stations without it.") },
        confirmButton = {
            TextButton(onClick = onAllow) { Text("Allow") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = StripePrimary, strokeWidth = 3.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StripeSurface,
            border = BorderStroke(1.dp, StripeBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconTile(icon = JouleIcons.Bolt, size = 48.dp)
                Text(
                    text = "Couldn't load stations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StripeInk,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StripeBody,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun StationsMap(
    center: LatLng,
    stations: List<NearbyStation>,
    onStationSelect: ((String) -> Unit)? = null,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, DEFAULT_ZOOM)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = com.google.maps.android.compose.MapUiSettings(
            zoomControlsEnabled = true,
            mapToolbarEnabled = true,
        ),
    ) {
        stations.forEach { station ->
            MarkerInfoWindowContent(
                state = com.google.maps.android.compose.rememberMarkerState(
                    position = LatLng(station.location.lat, station.location.lng),
                ),
                title = station.name,
                onInfoWindowClick = {
                    onStationSelect?.invoke(station.id)
                },
            ) {
                StationInfoWindow(station, onBookClick = { onStationSelect?.invoke(station.id) })
            }
        }
    }
}

@Composable
private fun StationInfoWindow(station: NearbyStation, onBookClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StripeSurface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, StripeBorder),
        modifier = Modifier.widthIn(min = 260.dp, max = 320.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StripeInk,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = StripeBrandVioletSoft,
                ) {
                    Text(
                        text = "%.1f km".format(station.distanceKm),
                        style = MaterialTheme.typography.labelSmall,
                        color = StripePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoChip(label = "Capacity", value = "${station.capacityKWh} kWh", modifier = Modifier.weight(1f))
                val slotsColor = when {
                    station.availableSlots == 0 -> StripeError
                    station.availableSlots <= 2  -> StripeAccent
                    else                          -> StripeSuccess
                }
                InfoChip(
                    label = "Free slots",
                    value = station.availableSlots.toString(),
                    valueColor = slotsColor,
                    modifier = Modifier.weight(1f),
                )
            }

            if (onBookClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBookClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StripePrimary,
                        contentColor = StripeOnPrimary,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 10.dp),
                ) {
                    Text(
                        text = "Reserve Slot →",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = StripeInk,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = StripeSurfaceAlt,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = StripeMuted,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
        }
    }
}
