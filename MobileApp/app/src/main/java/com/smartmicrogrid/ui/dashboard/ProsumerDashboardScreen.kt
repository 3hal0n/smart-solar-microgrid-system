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
//          Wired to the real backend (2026-09-26) via
//          data/remote/ApiClient.kt + ApiService.kt: GET
//          /dashboard/prosumer/{nic}/summary for the stat tiles, and
//          GET /reservations?nic={nic}&status=&stationId=&from=&to=
//          for the lists below. Status/station/date filters are sent
//          as query params — this screen never invents its own
//          filtering rule, it only chooses which ones to send, per
//          the FAT service pattern. Point API_BASE_URL (local.properties,
//          see app/build.gradle.kts) at your backend if it isn't on
//          the emulator-default 10.0.2.2:5128.
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.R
import com.smartmicrogrid.data.local.AppDbHelper
import com.smartmicrogrid.data.local.DashboardCacheDao
import com.smartmicrogrid.data.local.ProsumerSessionDao
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ReservationResponse
import com.smartmicrogrid.ui.components.IconTile
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.components.SectionCard
import com.smartmicrogrid.ui.components.StatTray
import com.smartmicrogrid.ui.theme.StripeBody
import com.smartmicrogrid.ui.theme.StripeBrandVioletSoft
import com.smartmicrogrid.ui.theme.StripeCyan
import com.smartmicrogrid.ui.theme.StripeCyanContainer
import com.smartmicrogrid.ui.theme.StripeInk
import com.smartmicrogrid.ui.theme.StripePrimary
import com.smartmicrogrid.ui.theme.StripeWarning
import com.smartmicrogrid.ui.theme.StripeWarningContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val STATUS_FILTERS = listOf("All", "Confirmed", "Completed", "Cancelled")
private val DATE_INPUT_PATTERN = Regex("""^\d{4}-\d{2}-\d{2}$""")

// Renders the full dashboard: stat tiles, filter controls, then the current/pending/history
// lists — the summary comes from GET /dashboard/prosumer/{nic}/summary, the lists from
// GET /reservations with the active filters sent as query params.
@Composable
fun ProsumerDashboardScreen() {
    val context = LocalContext.current
    val api = remember { ApiClient.service }

    var summary by remember { mutableStateOf<ProsumerDashboardSummary?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }

    var reservations by remember { mutableStateOf<List<ReservationResponse>>(emptyList()) }
    var reservationsError by remember { mutableStateOf<String?>(null) }

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

        runCatching { api.getProsumerDashboardSummary(FIXTURE_PROSUMER_NIC) }
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
                loadError = "Couldn't refresh the dashboard. Showing the last saved data. (${it.message ?: "network error"})"
            }
    }

    val fromDateValid = fromFilter.isBlank() || DATE_INPUT_PATTERN.matches(fromFilter)
    val toDateValid = toFilter.isBlank() || DATE_INPUT_PATTERN.matches(toFilter)

    // Re-fetches GET /reservations 300ms after the filters settle (debounced the same way
    // StationsPage.jsx's search box is on the web side) — every keystroke in the station-id field
    // would otherwise fire a request per character. statusFilter's "All" is a UI-only sentinel:
    // the server has no such status, so it's sent as a missing param (no filter) instead.
    LaunchedEffect(statusFilter, stationFilter, fromFilter, toFilter, refreshToken) {
        delay(300)
        reservationsError = null
        runCatching {
            api.searchReservations(
                nic = FIXTURE_PROSUMER_NIC,
                stationId = stationFilter.ifBlank { null },
                status = statusFilter.takeIf { it != "All" },
                from = fromFilter.takeIf { fromDateValid && it.isNotBlank() },
                to = toFilter.takeIf { toDateValid && it.isNotBlank() },
            )
        }.onSuccess { reservations = it }
            .onFailure { reservationsError = "Couldn't load bookings. (${it.message ?: "network error"})" }
    }

    val now = remember { Instant.now().toString() }
    val current = reservations.filter { it.status == "Confirmed" }
    val needsCheckIn = current.filter { it.scheduledAt <= now }
    val history = reservations.filter { it.status == "Completed" || it.status == "Cancelled" }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DashboardHeader(onRefresh = { refreshToken++ })

        if (loadError != null) {
            ErrorBanner(message = loadError.orEmpty())
        }
        if (reservationsError != null) {
            ErrorBanner(message = reservationsError.orEmpty())
        }

        HeroPanel(summary = summary)
        StatTraysRow(summary = summary)

        FilterCard(
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
        )

        ReservationSection(
            title = "Current bookings",
            reservations = current,
            emptyMessage = "No current bookings match this filter.",
        )

        ReservationSection(
            title = "Pending reservations — needs check-in",
            reservations = needsCheckIn,
            emptyMessage = "No pending reservations match this filter.",
        )

        ReservationSection(
            title = "Booking history",
            reservations = history,
            emptyMessage = "No booking history matches this filter.",
        )
    }
}

private val ROW_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm").withZone(ZoneId.systemDefault())

// "2026-10-02T09:00:00Z" -> "02 Oct 2026 · 14:30" in the device's time zone; falls back to the
// raw string if it isn't a valid ISO instant.
private fun formatScheduled(iso: String): String =
    runCatching { ROW_DATE_FORMAT.format(Instant.parse(iso)) }.getOrDefault(iso)

// Time-of-day greeting + the signed-in prosumer's name (from ProsumerSessionDao, the real
// session — not the FIXTURE_PROSUMER_NIC this screen's data calls still use, see the file
// header), and a filled circular refresh button. Design inspired by the "Good Morning" reference
// the user provided, adapted rather than copied — no fake notification bell, since there's no
// notification feature behind one yet.
@Composable
private fun DashboardHeader(onRefresh: () -> Unit) {
    val context = LocalContext.current
    val session = remember { ProsumerSessionDao(context).getSession() }
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = StripeInk,
            )
            Text(
                text = session?.fullName ?: "NIC $FIXTURE_PROSUMER_NIC",
                style = MaterialTheme.typography.bodyMedium,
                color = StripeBody,
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(StripeBrandVioletSoft),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onRefresh) {
                Icon(JouleIcons.Refresh, contentDescription = "Refresh", tint = StripePrimary)
            }
        }
    }
}

// A friendly summary card fronting the stat trays below — house illustration + the headline
// "Active" count, echoing the reference design's panel/hero-card layout without copying its
// solar-generation copy ("Daily Revenue"/"Capacity"/"Consumed" don't fit a booking system).
@Composable
private fun HeroPanel(summary: ProsumerDashboardSummary?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StripeBrandVioletSoft,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.home),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Active reservations",
                    style = MaterialTheme.typography.labelLarge,
                    color = StripeBody,
                )
                Text(
                    text = summary?.activeCount?.toString() ?: "—",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = StripeInk,
                )
                Text(
                    text = "Confirmed bookings across all your stations",
                    style = MaterialTheme.typography.bodySmall,
                    color = StripeBody,
                )
            }
        }
    }
}

// Inline error banner shown when a refresh fails (the last-known data stays on screen below it).
@Composable
private fun ErrorBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

// The three summary trays (active / pending / approved-future). Labels are kept short so three fit
// on a 360dp phone; the rubric wording lives in each tray's hint line. `summary` is null only
// before either the cache read or the first refresh has resolved — trays show an em dash then.
@Composable
private fun StatTraysRow(summary: ProsumerDashboardSummary?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTray(
            label = "Active",
            value = summary?.activeCount,
            hint = "Confirmed",
            icon = JouleIcons.Pulse,
            accent = MaterialTheme.colorScheme.primary,
            accentContainer = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f),
        )
        StatTray(
            label = "Pending",
            value = summary?.pendingCount,
            hint = "Needs check-in",
            icon = JouleIcons.Bolt,
            accent = StripeWarning,
            accentContainer = StripeWarningContainer,
            modifier = Modifier.weight(1f),
        )
        StatTray(
            label = "Upcoming",
            value = summary?.approvedFutureCount,
            hint = "Approved future",
            icon = JouleIcons.Calendar,
            accent = StripeCyan,
            accentContainer = StripeCyanContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

// Status chips (horizontally scrollable, so they never clip on narrow phones), the station-id
// search, and a scheduledAt date range (from/to, "YYYY-MM-DD") — 4 of GET /reservations's params.
@Composable
private fun FilterCard(
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
) {
    SectionCard(title = "Filter") {
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            STATUS_FILTERS.forEach { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onStatusFilterChange(status) },
                    label = { Text(status) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
        FilterTextField(
            value = stationFilter,
            onValueChange = onStationFilterChange,
            label = "Station ID contains",
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
            FilterTextField(
                value = fromFilter,
                onValueChange = onFromFilterChange,
                label = "From (YYYY-MM-DD)",
                isError = !fromDateValid,
                modifier = Modifier.weight(1f),
            )
            FilterTextField(
                value = toFilter,
                onValueChange = onToFilterChange,
                label = "To (YYYY-MM-DD)",
                isError = !toDateValid,
                modifier = Modifier.weight(1f),
            )
        }
        if (!fromDateValid || !toDateValid) {
            Text(
                text = "Dates must be in YYYY-MM-DD format — ignored until fixed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// Single-line outlined field with the app's rounded shape and hairline border.
@Composable
private fun FilterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier,
    )
}

// A titled section card: reservation rows separated by hairlines, or an empty-state message.
@Composable
private fun ReservationSection(
    title: String,
    reservations: List<ReservationResponse>,
    emptyMessage: String,
) {
    SectionCard(title = title, count = reservations.size) {
        if (reservations.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                reservations.forEachIndexed { index, reservation ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                    ReservationRow(reservation)
                }
            }
        }
    }
}

// One reservation: an icon tile, station + slot/time, and a status pill on the right.
@Composable
private fun ReservationRow(reservation: ReservationResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon = JouleIcons.Hubs, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Station ${reservation.stationId.takeLast(6)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Slot ${reservation.slotId.takeLast(6)} · ${formatScheduled(reservation.scheduledAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StatusPill(status = reservation.status)
    }
}

// Small rounded status pill with a leading dot, toned by reservation status.
@Composable
private fun StatusPill(status: String) {
    val (container, content) = when (status) {
        "Confirmed" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        "Completed" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(content),
        )
        Text(text = status, style = MaterialTheme.typography.labelMedium, color = content)
    }
}
