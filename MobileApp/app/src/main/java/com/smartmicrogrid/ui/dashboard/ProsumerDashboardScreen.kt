// ============================================================
// File: ProsumerDashboardScreen.kt
// Purpose: Prosumer dashboard — stat tiles for active/pending/
//          approved-future counts, a pending-bookings list, a
//          booking-history list, and search/filter controls, per
//          architecture.md §6 (moved from Migara to Shalon
//          2026-09-24 — see §4/§6/§7) and the endpoints Shalon built
//          per §3: GET /dashboard/prosumer/{nic}/summary and
//          GET /reservations.
//
//          TODO(swap-in-real-api): this screen currently reads
//          ProsumerDashboardFixtures instead of the network, because
//          (a) data/remote/ApiClient.kt — the shared Retrofit client
//          architecture.md §8 calls for — doesn't exist anywhere in
//          this repo yet, and (b) the backend branch that built these
//          two endpoints hasn't been merged to main. Once both are
//          true, replace loadDashboardData() below with real calls:
//            GET /dashboard/prosumer/{nic}/summary -> ProsumerDashboardSummary
//            GET /reservations?nic={nic}&status=&stationId=&from=&to= -> List<ReservationListItem>
//          The response shapes in ProsumerDashboardModels.kt already
//          match the JSON field-for-field, so this should be a
//          data-source swap, not a model rewrite. All filtering
//          shown here (status/station) already matches what the
//          server-side query params support — this screen never
//          invents its own filtering rule, it only chooses which
//          query params to send, per the FAT service pattern.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val STATUS_FILTERS = listOf("All", "Confirmed", "Completed", "Cancelled")

// Renders the full dashboard: stat tiles, filter controls, then the pending and history lists —
// all derived from a single loaded data source (fixture today, the real API once it's wired).
@Composable
fun ProsumerDashboardScreen() {
    val summary = remember { ProsumerDashboardFixtures.summary }
    val allReservations = remember { ProsumerDashboardFixtures.reservations }

    var statusFilter by remember { mutableStateOf("All") }
    var stationFilter by remember { mutableStateOf("") }

    // Applies the currently-selected filters to the loaded list. This mirrors what
    // GET /reservations's status/stationId query params already do server-side — once the real
    // API call lands, these same two values become query params instead of a local filter.
    val filtered = remember(statusFilter, stationFilter, allReservations) {
        allReservations.filter { reservation ->
            (statusFilter == "All" || reservation.status == statusFilter) &&
                (stationFilter.isBlank() || reservation.stationId.contains(stationFilter, ignoreCase = true))
        }
    }
    val pending = filtered.filter { it.status == "Confirmed" }
    val history = filtered.filter { it.status == "Completed" || it.status == "Cancelled" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(text = "Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "NIC $FIXTURE_PROSUMER_NIC",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        StatTilesRow(summary = summary, modifier = Modifier.padding(top = 16.dp))

        FilterControls(
            statusFilter = statusFilter,
            onStatusFilterChange = { statusFilter = it },
            stationFilter = stationFilter,
            onStationFilterChange = { stationFilter = it },
            modifier = Modifier.padding(top = 20.dp),
        )

        ReservationSection(
            title = "Pending bookings",
            reservations = pending,
            emptyMessage = "No pending bookings match this filter.",
            modifier = Modifier.padding(top = 20.dp),
        )

        ReservationSection(
            title = "Booking history",
            reservations = history,
            emptyMessage = "No booking history matches this filter.",
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

// Renders the three summary stat tiles (active/pending/approved-future) in a row.
@Composable
private fun StatTilesRow(summary: ProsumerDashboardSummary, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(label = "Active", value = summary.activeCount, modifier = Modifier.weight(1f))
        StatTile(label = "Pending", value = summary.pendingCount, modifier = Modifier.weight(1f))
        StatTile(label = "Approved future", value = summary.approvedFutureCount, modifier = Modifier.weight(1f))
    }
}

// Renders a single stat tile: a large number over a label, inside a card.
@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Renders the status filter chips and the station-id search field.
@Composable
private fun FilterControls(
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit,
    stationFilter: String,
    onStationFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = "Filter", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            STATUS_FILTERS.forEach { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onStatusFilterChange(status) },
                    label = { Text(status) },
                )
            }
        }
        OutlinedTextField(
            value = stationFilter,
            onValueChange = onStationFilterChange,
            label = { Text("Station ID contains") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}

// Renders a titled section: a list of reservation rows, or an empty-state message.
@Composable
private fun ReservationSection(
    title: String,
    reservations: List<ReservationListItem>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = "$title (${reservations.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (reservations.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                reservations.forEach { reservation ->
                    ReservationRow(reservation)
                    HorizontalDivider()
                }
            }
        }
    }
}

// Renders one reservation as a compact row: station/slot + scheduled time on the left, a status
// pill on the right.
@Composable
private fun ReservationRow(reservation: ReservationListItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = "Station ${reservation.stationId.takeLast(6)}", fontWeight = FontWeight.Bold)
            Text(
                text = "Slot ${reservation.slotId.takeLast(6)} · ${reservation.scheduledAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusPill(status = reservation.status)
    }
}

// Renders a small colored pill naming a reservation's status.
@Composable
private fun StatusPill(status: String) {
    val containerColor = when (status) {
        "Confirmed" -> MaterialTheme.colorScheme.primaryContainer
        "Completed" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
