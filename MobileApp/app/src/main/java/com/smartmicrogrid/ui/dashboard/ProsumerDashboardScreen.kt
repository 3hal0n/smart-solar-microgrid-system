// ============================================================
// File: ProsumerDashboardScreen.kt
// Purpose: Prosumer dashboard — stat tiles for active/pending/
//          approved-future counts, a current-bookings list, a
//          pending-reservations (needs check-in) list, a
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
//          true, replace ProsumerDashboardFixtures.loadSummary() with
//          a real call:
//            GET /dashboard/prosumer/{nic}/summary -> ProsumerDashboardSummary
//            GET /reservations?nic={nic}&status=&stationId=&from=&to= -> List<ReservationListItem>
//          The response shapes in ProsumerDashboardModels.kt already
//          match the JSON field-for-field, so this should be a
//          data-source swap, not a model rewrite. All filtering shown
//          here (status/station/from/to) already matches what the
//          server-side query params support — this screen never
//          invents its own filtering rule, it only chooses which
//          query params to send, per the FAT service pattern.
//
//          Cache-first, then refresh: on open, the last-cached
//          summary (DashboardCacheDao, architecture.md §8) renders
//          immediately if one exists, then the fixture/API call runs
//          and both the screen and the cache are updated once it
//          resolves — so returning to this screen never shows a
//          blank loading state when there's already something to
//          show. A failed refresh leaves the last-known data on
//          screen with an inline error banner rather than clearing it.
//
//          Reviewed 2026-09-25 against architecture.md §7's "Booking
//          Views and Operational Dashboards" 5 sub-items and fixed:
//          the "pending" list used to show every Confirmed booking
//          (future and past-due alike) under the same label the
//          "Pending" stat tile uses for a narrower, past-due-only
//          definition (DashboardService.GetProsumerSummaryAsync:
//          Confirmed AND scheduledAt <= now), so the two disagreed on
//          screen. Each of the 5 sub-items now has its own,
//          distinctly-labeled section computed with the exact same
//          definition as its matching stat tile:
//            - "Current bookings" = every filtered Confirmed booking
//              (matches the Active tile 1:1).
//            - "Pending reservations — needs check-in" = filtered
//              Confirmed AND scheduledAt <= now (matches the Pending
//              tile 1:1; this is the "pending reservations" sub-item).
//            - "Booking history" = Completed/Cancelled, unchanged.
//            - "Approved future" count = the existing stat tile.
//          Filter criteria now also covers a scheduledAt date range
//          (from/to), matching two more of GET /reservations's query
//          params, not just status/stationId.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.smartmicrogrid.data.local.AppDbHelper
import com.smartmicrogrid.data.local.DashboardCacheDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

private val STATUS_FILTERS = listOf("All", "Confirmed", "Completed", "Cancelled")
private val DATE_INPUT_PATTERN = Regex("""^\d{4}-\d{2}-\d{2}$""")

// Renders the full dashboard: stat tiles, filter controls, then the current/pending/history
// lists — all derived from a single loaded data source (fixture today, the real API once it's
// wired).
@Composable
fun ProsumerDashboardScreen() {
    val context = LocalContext.current
    val allReservations = remember { ProsumerDashboardFixtures.reservations }

    var summary by remember { mutableStateOf<ProsumerDashboardSummary?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableStateOf(0) }

    var statusFilter by remember { mutableStateOf("All") }
    var stationFilter by remember { mutableStateOf("") }
    var fromFilter by remember { mutableStateOf("") }
    var toFilter by remember { mutableStateOf("") }

    // Cache-first, then refresh. On the very first load (refreshToken == 0) show whatever was
    // cached last, if anything, before the fresh call resolves. A manual refresh (refreshToken >
    // 0) skips re-reading the cache — it would just flash the old value in front of the new one.
    // If the fresh call fails, the last-known summary stays on screen and an error banner shows
    // instead of the screen going blank.
    LaunchedEffect(refreshToken) {
        loadError = null
        if (refreshToken == 0) {
            val cached = runCatching {
                withContext(Dispatchers.IO) {
                    AppDbHelper(context).readableDatabase.use { db ->
                        DashboardCacheDao.read(db, FIXTURE_PROSUMER_NIC)
                    }
                }
            }.getOrNull()
            if (cached != null) {
                summary = cached
            }
        }

        runCatching { ProsumerDashboardFixtures.loadSummary() }
            .onSuccess { fresh ->
                summary = fresh
                runCatching {
                    withContext(Dispatchers.IO) {
                        AppDbHelper(context).writableDatabase.use { db ->
                            DashboardCacheDao.write(db, FIXTURE_PROSUMER_NIC, fresh)
                        }
                    }
                }
            }
            .onFailure {
                loadError = "Couldn't refresh the dashboard. Showing the last saved data."
            }
    }

    val fromDateValid = fromFilter.isBlank() || DATE_INPUT_PATTERN.matches(fromFilter)
    val toDateValid = toFilter.isBlank() || DATE_INPUT_PATTERN.matches(toFilter)

    // Applies the currently-selected filters to the loaded list. This mirrors what
    // GET /reservations's status/stationId/from/to query params already do server-side — once the
    // real API call lands, these same values become query params instead of a local filter.
    val filtered = remember(statusFilter, stationFilter, fromFilter, toFilter, allReservations) {
        allReservations.filter { reservation ->
            (statusFilter == "All" || reservation.status == statusFilter) &&
                (stationFilter.isBlank() || reservation.stationId.contains(stationFilter, ignoreCase = true)) &&
                (!fromDateValid || fromFilter.isBlank() || reservation.scheduledAt >= "${fromFilter}T00:00:00Z") &&
                (!toDateValid || toFilter.isBlank() || reservation.scheduledAt <= "${toFilter}T23:59:59Z")
        }
    }
    val now = remember { Instant.now().toString() }
    val current = filtered.filter { it.status == "Confirmed" }
    val needsCheckIn = current.filter { it.scheduledAt <= now }
    val history = filtered.filter { it.status == "Completed" || it.status == "Cancelled" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "NIC $FIXTURE_PROSUMER_NIC",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = { refreshToken++ }) {
                Text("Refresh")
            }
        }

        if (loadError != null) {
            Text(
                text = loadError.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        StatTilesRow(summary = summary, modifier = Modifier.padding(top = 16.dp))

        FilterControls(
            statusFilter = statusFilter,
            onStatusFilterChange = { statusFilter = it },
            stationFilter = stationFilter,
            onStationFilterChange = { stationFilter = it },
            fromFilter = fromFilter,
            onFromFilterChange = { fromFilter = it },
            fromDateValid = fromDateValid,
            toFilter = toFilter,
            onToFilterChange = { toFilter = it },
            toDateValid = toDateValid,
            modifier = Modifier.padding(top = 20.dp),
        )

        ReservationSection(
            title = "Current bookings",
            reservations = current,
            emptyMessage = "No current bookings match this filter.",
            modifier = Modifier.padding(top = 20.dp),
        )

        ReservationSection(
            title = "Pending reservations — needs check-in",
            reservations = needsCheckIn,
            emptyMessage = "No pending reservations match this filter.",
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

// Renders the three summary stat tiles (active/pending/approved-future) in a row. `summary` is
// null only before either the cache read or the first refresh has resolved — tiles show an em
// dash rather than a misleading "0" in that brief window.
@Composable
private fun StatTilesRow(summary: ProsumerDashboardSummary?, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(label = "Active", value = summary?.activeCount, modifier = Modifier.weight(1f))
        StatTile(label = "Pending", value = summary?.pendingCount, modifier = Modifier.weight(1f))
        StatTile(label = "Approved future", value = summary?.approvedFutureCount, modifier = Modifier.weight(1f))
    }
}

// Renders a single stat tile: a large number (or an em dash while nothing's loaded yet) over a
// label, inside a card.
@Composable
private fun StatTile(label: String, value: Int?, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = value?.toString() ?: "—", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Renders the status filter chips, the station-id search field, and a scheduledAt date-range
// filter (from/to, "YYYY-MM-DD"), covering 4 of GET /reservations's query params.
@Composable
private fun FilterControls(
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit,
    stationFilter: String,
    onStationFilterChange: (String) -> Unit,
    fromFilter: String,
    onFromFilterChange: (String) -> Unit,
    fromDateValid: Boolean,
    toFilter: String,
    onToFilterChange: (String) -> Unit,
    toDateValid: Boolean,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = fromFilter,
                onValueChange = onFromFilterChange,
                label = { Text("From (YYYY-MM-DD)") },
                singleLine = true,
                isError = !fromDateValid,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = toFilter,
                onValueChange = onToFilterChange,
                label = { Text("To (YYYY-MM-DD)") },
                singleLine = true,
                isError = !toDateValid,
                modifier = Modifier.weight(1f),
            )
        }
        if (!fromDateValid || !toDateValid) {
            Text(
                text = "Dates must be in YYYY-MM-DD format — ignored until fixed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
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
