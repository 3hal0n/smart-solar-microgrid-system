// ============================================================
// File: MapScreen.kt
// Purpose: Nearby grid nodes map — centers on the device's location
//          (falling back to a default center when permission is
//          denied, never crashing), calls GET /stations/nearby, and
//          plots a tappable marker per station with an info window
//          showing name/capacity/available slots. Per architecture.md
//          §6 (moved from Migara to Shalon 2026-09-24 — see §4/§7)
//          and the endpoint Shalon built per §3.
//
//          Wired to the real backend (2026-09-26) via
//          data/remote/ApiClient.kt + ApiService.kt: GET
//          /stations/nearby?lat=&lng=&radiusKm=. A failed call shows
//          a retry card rather than crashing or silently showing
//          nothing — the fixture version never needed this since it
//          could never fail.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.operator

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.ui.components.IconTile
import com.smartmicrogrid.ui.components.JouleIcons
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberCameraPositionState
import com.smartmicrogrid.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Sri Lanka — same fallback center used by the web MapPicker, for consistency across clients.
private val DEFAULT_CENTER = LatLng(6.9271, 79.8612)
private const val DEFAULT_ZOOM = 12f
private const val SEARCH_RADIUS_KM = 25.0

// Renders the nearby-stations map: resolves a center point (device location or the default),
// loads stations around it, and shows loading/empty/populated states as appropriate.
@Composable
fun MapScreen(
    onStationSelect: ((String) -> Unit)? = null
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
    var stations by remember { mutableStateOf<List<NearbyStation>?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var mapRefreshToken by remember { mutableIntStateOf(0) }

    // Registers the system runtime-permission prompt; its result decides whether we try to read
    // a device location at all. Denial never crashes — it just leaves `center` for the effect
    // below to fall back to DEFAULT_CENTER.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasLocationPermission = granted
        if (!granted) {
            center = DEFAULT_CENTER
            usingDeviceLocation = false
        }
    }

    // Resolves the map's center once permission has been decided: the device's last known
    // location when granted (falling back to the default if that's unavailable), or the default
    // immediately when denied.
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

    // Loads nearby stations once a center point is known, again if the center changes, and again
    // on retry after a failure (mapRefreshToken).
    LaunchedEffect(center, mapRefreshToken) {
        val current = center ?: return@LaunchedEffect
        stations = null
        mapError = null
        runCatching { api.getNearbyStations(current.latitude, current.longitude, SEARCH_RADIUS_KM) }
            .onSuccess { stations = it }
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            mapError != null -> ErrorState(message = mapError!!, onRetry = { mapRefreshToken++ })
            center == null || stations == null -> LoadingState()
            stations!!.isEmpty() -> EmptyState()
            else -> StationsMap(center = center!!, stations = stations!!, onStationSelect = onStationSelect)
        }
        // Floats over the map (rather than pushing it down) so the map keeps the full screen.
        LocationSourceBanner(
            usingDeviceLocation = usingDeviceLocation,
            center = center,
            stationCount = stations?.size,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp),
        )
    }
}

// Reads the device's last known location, or null if unavailable/denied — callers must already
// hold ACCESS_FINE_LOCATION, which this function assumes (hence the suppression) since it's only
// ever invoked from the branch in MapScreen that has already confirmed that.
//
// Tasks.await() blocks the calling thread until the task resolves. It's pushed onto
// Dispatchers.IO here because the caller runs it from a LaunchedEffect, which defaults to the
// Main dispatcher — blocking that thread would freeze the whole UI (and risk an ANR) until the
// location lookup completes.
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

// Explains why the app wants location access before the system permission prompt appears, per
// "handle the location permission request properly with a runtime dialog".
@Composable
private fun LocationRationaleDialog(onAllow: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use your location?") },
        text = { Text("Allow location access to center the map on grid nodes near you. You can still browse stations near Colombo without it.") },
        confirmButton = {
            TextButton(onClick = onAllow) { Text("Allow") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}

// Floating card naming which center point is active (so a denied/unavailable location is visible to
// the user rather than silently substituted), plus how many stations were found.
@Composable
private fun LocationSourceBanner(
    usingDeviceLocation: Boolean,
    center: LatLng?,
    stationCount: Int?,
    modifier: Modifier = Modifier,
) {
    if (center == null) return
    val title = if (usingDeviceLocation) "Near your location" else "Near Colombo"
    val subtitle = when {
        stationCount == null -> "Finding stations…"
        !usingDeviceLocation -> "Enable location for stations near you"
        else -> "$stationCount station${if (stationCount == 1) "" else "s"} within ${SEARCH_RADIUS_KM.toInt()} km"
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(icon = JouleIcons.MapPin, size = 36.dp)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Renders a centered spinner while the center point or the station list is still loading.
@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
    }
}

// Renders the "no stations nearby" empty state as a centered card.
@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconTile(icon = JouleIcons.Hubs, size = 48.dp)
                Text(
                    text = "No stations nearby",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = "No active grid nodes within ${SEARCH_RADIUS_KM.toInt()} km of this point.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

// Renders a centered "couldn't load stations" card with a retry button — shown when
// GET /stations/nearby fails (backend unreachable, wrong API_BASE_URL, etc.).
@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

// Renders the map itself, camera centered on `center`, with one tappable marker per station.
@Composable
private fun StationsMap(
    center: LatLng,
    stations: List<NearbyStation>,
    onStationSelect: ((String) -> Unit)? = null
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, DEFAULT_ZOOM)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = com.google.maps.android.compose.MapUiSettings(
            zoomControlsEnabled = true,
            mapToolbarEnabled = true
        )
    ) {
        stations.forEach { station ->
            MarkerInfoWindowContent(
                state = com.google.maps.android.compose.rememberMarkerState(
                    position = LatLng(station.location.lat, station.location.lng),
                ),
                title = station.name,
                onInfoWindowClick = {
                    onStationSelect?.invoke(station.id)
                }
            ) {
                StationInfoWindow(station, onBookClick = { onStationSelect?.invoke(station.id) })
            }
        }
    }
}

// Renders the tap-to-show info window content: name + distance, then capacity and available slots
// as two small labelled figures.
@Composable
private fun StationInfoWindow(station: NearbyStation, onBookClick: (() -> Unit)? = null) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(text = station.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            text = "%.1f km away".format(station.distanceKm),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoFigure(label = "Capacity", value = "${station.capacityKWh} kWh")
            InfoFigure(label = "Free slots", value = station.availableSlots.toString())
        }
        if (onBookClick != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBookClick() }
            ) {
                Text(
                    text = "Tap to Reserve Slot →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// One small label-over-value pair for the info window.
@Composable
private fun InfoFigure(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}
