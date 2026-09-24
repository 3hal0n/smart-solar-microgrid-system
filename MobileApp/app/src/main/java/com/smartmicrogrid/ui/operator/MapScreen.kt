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
//          TODO(swap-in-real-api): this screen currently reads
//          NearbyStationFixtures instead of the network, for the
//          same reason as ProsumerDashboardScreen.kt: no shared
//          Retrofit client (data/remote/ApiClient.kt) exists in this
//          repo yet, and this branch hasn't been merged to main.
//          Once both are true, replace the loadStations() call below
//          with a real GET /stations/nearby?lat=&lng=&radiusKm= call
//          — NearbyStationFixtures.findNearby has the exact same
//          signature/return shape on purpose, so this should be a
//          one-line data-source swap.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.operator

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberCameraPositionState

// Sri Lanka — same fallback center used by the web MapPicker, for consistency across clients.
private val DEFAULT_CENTER = LatLng(6.9271, 79.8612)
private const val DEFAULT_ZOOM = 12f
private const val SEARCH_RADIUS_KM = 25.0

// Renders the nearby-stations map: resolves a center point (device location or the default),
// loads stations around it, and shows loading/empty/populated states as appropriate.
@Composable
fun MapScreen() {
    val context = LocalContext.current

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

    // Loads nearby stations once a center point is known, and again if the center changes.
    LaunchedEffect(center) {
        val current = center ?: return@LaunchedEffect
        stations = null
        stations = NearbyStationFixtures.findNearby(current.latitude, current.longitude, SEARCH_RADIUS_KM)
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

    Column(modifier = Modifier.fillMaxSize()) {
        LocationSourceBanner(usingDeviceLocation = usingDeviceLocation, center = center)
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                center == null || stations == null -> LoadingState()
                stations!!.isEmpty() -> EmptyState()
                else -> StationsMap(center = center!!, stations = stations!!)
            }
        }
    }
}

// Reads the device's last known location, or null if unavailable/denied — callers must already
// hold ACCESS_FINE_LOCATION, which this function assumes (hence the suppression) since it's only
// ever invoked from the branch in MapScreen that has already confirmed that.
@SuppressLint("MissingPermission")
private suspend fun readLastKnownLocation(context: android.content.Context): LatLng? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try {
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

// Small banner naming which center point is active, so a denied/unavailable location is visible
// to the user rather than silently substituted.
@Composable
private fun LocationSourceBanner(usingDeviceLocation: Boolean, center: LatLng?) {
    if (center == null) return
    val message = if (usingDeviceLocation) {
        "Showing stations near your location"
    } else {
        "Showing stations near Colombo — enable location for stations near you"
    }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

// Renders a centered spinner while the center point or the station list is still loading.
@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// Renders the "no stations nearby" empty state.
@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No stations nearby.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Renders the map itself, camera centered on `center`, with one tappable marker per station.
@Composable
private fun StationsMap(center: LatLng, stations: List<NearbyStation>) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, DEFAULT_ZOOM)
    }

    GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
        stations.forEach { station ->
            MarkerInfoWindowContent(
                state = com.google.maps.android.compose.rememberUpdatedMarkerState(
                    position = LatLng(station.location.lat, station.location.lng),
                ),
                title = station.name,
            ) {
                StationInfoWindow(station)
            }
        }
    }
}

// Renders the tap-to-show info window content: name, capacity, and available slots.
@Composable
private fun StationInfoWindow(station: NearbyStation) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = station.name, fontWeight = FontWeight.Bold)
        Text(text = "Capacity: ${station.capacityKWh} kWh", style = MaterialTheme.typography.bodySmall)
        Text(text = "Available slots: ${station.availableSlots}", style = MaterialTheme.typography.bodySmall)
        Text(
            text = "%.1f km away".format(station.distanceKm),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
