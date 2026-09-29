// ============================================================
// File: ProsumerDashboardScreen.kt
// Purpose: Prosumer dashboard, per architecture.md §6/§7 (Shalon):
//          active/pending/approved-future counts, current bookings,
//          pending reservations (needs check-in), booking history and
//          search/filter - backed by GET /dashboard/prosumer/{nic}/
//          summary and GET /reservations (FAT service: this screen only
//          chooses which filters to send, never computes business
//          rules). Cache-first via DashboardCacheDao, then refresh; a
//          failed refresh keeps last-known data with a dismissible
//          banner.
//
//          Premium revamp (2026-09): greeting header, ink "next
//          booking" hero, compact metric tiles, a chip row + filter
//          bottom sheet (date pickers instead of typed YYYY-MM-DD
//          fields), date-block booking rows with real station names,
//          and pull-to-refresh. Section definitions are unchanged from
//          the 2026-09-25 review: "Upcoming" = Confirmed, "Needs
//          check-in" = Confirmed AND scheduledAt <= now (matches the
//          Pending tile), "History" = Completed/Cancelled.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.R
import com.smartmicrogrid.data.local.AppDbHelper
import com.smartmicrogrid.data.local.DashboardCacheDao
import com.smartmicrogrid.data.local.ProsumerSessionDao
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ReservationResponse
import com.smartmicrogrid.ui.components.ControlShape
import com.smartmicrogrid.ui.components.DateBlock
import com.smartmicrogrid.ui.components.EmptyState
import com.smartmicrogrid.ui.components.ErrorBanner
import com.smartmicrogrid.ui.components.Hairline
import com.smartmicrogrid.ui.components.JouleCard
import com.smartmicrogrid.ui.components.JouleChip
import com.smartmicrogrid.ui.components.JouleIconButton
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.components.JoulePrimaryButton
import com.smartmicrogrid.ui.components.JouleSecondaryButton
import com.smartmicrogrid.ui.components.JouleTextField
import com.smartmicrogrid.ui.components.MetricTile
import com.smartmicrogrid.ui.components.PillShape
import com.smartmicrogrid.ui.components.SectionHeader
import com.smartmicrogrid.ui.components.StatusPill
import com.smartmicrogrid.ui.components.softShadow
import com.smartmicrogrid.ui.theme.StripeBody
import com.smartmicrogrid.ui.theme.StripeBorder
import com.smartmicrogrid.ui.theme.StripeBrandVioletSoft
import com.smartmicrogrid.ui.theme.StripeCanvas
import com.smartmicrogrid.ui.theme.StripeCyan
import com.smartmicrogrid.ui.theme.StripeCyanContainer
import com.smartmicrogrid.ui.theme.StripeError
import com.smartmicrogrid.ui.theme.StripeErrorContainer
import com.smartmicrogrid.ui.theme.StripeInk
import com.smartmicrogrid.ui.theme.StripeMuted
import com.smartmicrogrid.ui.theme.StripeOnErrorContainer
import com.smartmicrogrid.ui.theme.StripePrimary
import com.smartmicrogrid.ui.theme.StripeSurface
import com.smartmicrogrid.ui.theme.StripeWarning
import com.smartmicrogrid.ui.theme.StripeWarningContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val STATUS_FILTERS = listOf("All", "Confirmed", "Completed", "Cancelled")
private val DATE_INPUT_PATTERN = Regex("""^\d{4}-\d{2}-\d{2}$""")
private const val HISTORY_PREVIEW = 5

// The signature gradient stop from the JouleMark - used only as a thin accent stripe.
private val SignatureCyan = Color(0xFF11EFE3)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProsumerDashboardScreen(
    onBookSlot: () -> Unit = {},
    onOpenBookings: () -> Unit = {},
    onNavigateToSignIn: () -> Unit = {},
) {
    val context = LocalContext.current
    val api = remember { ApiClient.service }
    val session = remember { ProsumerSessionDao(context).getSession() }
    val prosumerNic = remember { session?.nic ?: FIXTURE_PROSUMER_NIC }

    var summary by remember { mutableStateOf<ProsumerDashboardSummary?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

    var reservations by remember { mutableStateOf<List<ReservationResponse>>(emptyList()) }
    var reservationsError by remember { mutableStateOf<String?>(null) }
    var stationNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var statusFilter by remember { mutableStateOf("All") }
    var stationFilter by remember { mutableStateOf("") }
    var fromFilter by remember { mutableStateOf("") }
    var toFilter by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showAllHistory by remember { mutableStateOf(false) }

    // Cache-first, then refresh (see file header). A manual refresh skips the cache re-read.
    LaunchedEffect(refreshToken) {
        loadError = null
        if (refreshToken == 0) {
            val cached = runCatching {
                withContext(Dispatchers.IO) {
                    AppDbHelper(context).readableDatabase.use { db -> DashboardCacheDao.read(db, prosumerNic) }
                }
            }.getOrNull()
            if (cached != null) summary = cached
        }

        runCatching { api.getProsumerDashboardSummary(prosumerNic) }
            .onSuccess { fresh ->
                summary = fresh
                runCatching {
                    withContext(Dispatchers.IO) {
                        AppDbHelper(context).writableDatabase.use { db -> DashboardCacheDao.write(db, prosumerNic, fresh) }
                    }
                }
            }
            .onFailure { throwable ->
                if (throwable is retrofit2.HttpException && (throwable.code() == 401 || throwable.code() == 403)) {
                    onNavigateToSignIn()
                } else {
                    loadError = if (summary == null) {
                        "Cannot connect to server. Check your network connection or sign in again."
                    } else {
                        "Couldn't refresh dashboard - showing last saved data."
                    }
                }
            }
        refreshing = false
    }

    // Display-only lookup so rows show station names instead of raw ids; failure just falls back
    // to the short id, it never blocks the screen.
    LaunchedEffect(refreshToken) {
        runCatching { api.getStations(search = null, status = null) }
            .onSuccess { list -> stationNames = list.associate { it.id to it.name } }
    }

    val fromDateValid = fromFilter.isBlank() || DATE_INPUT_PATTERN.matches(fromFilter)
    val toDateValid = toFilter.isBlank() || DATE_INPUT_PATTERN.matches(toFilter)

    // Debounced GET /reservations - "All" is a UI-only sentinel, sent as no status param.
    LaunchedEffect(statusFilter, stationFilter, fromFilter, toFilter, refreshToken) {
        delay(300)
        reservationsError = null
        runCatching {
            api.searchReservations(
                nic = prosumerNic,
                stationId = stationFilter.ifBlank { null },
                status = statusFilter.takeIf { it != "All" },
                from = fromFilter.takeIf { fromDateValid && it.isNotBlank() },
                to = toFilter.takeIf { toDateValid && it.isNotBlank() },
            )
        }.onSuccess { reservations = it }
            .onFailure { throwable ->
                if (throwable is retrofit2.HttpException && (throwable.code() == 401 || throwable.code() == 403)) {
                    onNavigateToSignIn()
                } else {
                    reservationsError = "Couldn't load bookings. Pull down to try again."
                }
            }
    }

    val now = Instant.now().toString()
    val current = reservations.filter { it.status == "Confirmed" }.sortedBy { it.scheduledAt }
    val needsCheckIn = current.filter { it.scheduledAt <= now }
    val upcoming = current.filter { it.scheduledAt > now }
    val history = reservations.filter { it.status == "Completed" || it.status == "Cancelled" }
        .sortedByDescending { it.scheduledAt }
    val nextBooking = upcoming.firstOrNull() ?: needsCheckIn.firstOrNull()
    val advancedFilterCount = listOf(stationFilter, fromFilter, toFilter).count { it.isNotBlank() }

    fun nameFor(stationId: String) = stationNames[stationId] ?: "Station ${stationId.takeLast(6)}"

    // If there is no cached or live data and a connection/network error occurred, show a prominent recovery screen
    if (summary == null && loadError != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StripeCanvas)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            JouleCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(StripeErrorContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            JouleIcons.Pulse,
                            contentDescription = null,
                            tint = StripeError,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Text(
                        text = "Connection Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = StripeInk,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    Text(
                        text = "Unable to reach the smart grid server. Check your network connection (file sharing / Wi-Fi / USB tethering) or return to sign in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StripeBody,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    JoulePrimaryButton(
                        text = "Go to Sign In",
                        onClick = onNavigateToSignIn,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    JouleSecondaryButton(
                        text = "Retry Connection",
                        onClick = { refreshing = true; refreshToken++ },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(StripeCanvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GreetingHeader(
                name = session?.fullName,
                onRefresh = { refreshing = true; refreshToken++ },
            )

            loadError?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ControlShape)
                        .background(StripeErrorContainer)
                        .border(1.dp, StripeError.copy(alpha = 0.2f), ControlShape)
                        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(StripeError))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = StripeOnErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onNavigateToSignIn) {
                        Text("Sign In", color = StripeError, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            reservationsError?.let { ErrorBanner(message = it, onDismiss = { reservationsError = null }) }

            NextBookingHero(
                booking = nextBooking,
                stationName = nextBooking?.let { nameFor(it.stationId) },
                onBookSlot = onBookSlot,
                onOpenBookings = onOpenBookings,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricTile(
                    label = "Active",
                    value = summary?.activeCount,
                    icon = JouleIcons.Pulse,
                    accent = StripePrimary,
                    accentSoft = StripeBrandVioletSoft,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                MetricTile(
                    label = "Needs check-in",
                    value = summary?.pendingCount,
                    icon = JouleIcons.Bolt,
                    accent = StripeWarning,
                    accentSoft = StripeWarningContainer,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                MetricTile(
                    label = "Upcoming",
                    value = summary?.approvedFutureCount,
                    icon = JouleIcons.Calendar,
                    accent = StripeCyan,
                    accentSoft = StripeCyanContainer,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }

            // Filters: status chips always visible; station/date live in a bottom sheet.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(title = "Your bookings", subtitle = "Filter by status, station or date")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    STATUS_FILTERS.forEach { status ->
                        JouleChip(label = status, selected = statusFilter == status, onClick = { statusFilter = status })
                    }
                    FiltersChip(count = advancedFilterCount, onClick = { showFilterSheet = true })
                }
                if (advancedFilterCount > 0) {
                    ActiveFilterSummary(
                        station = stationFilter,
                        from = fromFilter,
                        to = toFilter,
                        onClear = { stationFilter = ""; fromFilter = ""; toFilter = "" },
                    )
                }
            }

            AnimatedVisibility(visible = needsCheckIn.isNotEmpty()) {
                BookingGroup(
                    title = "Needs check-in",
                    subtitle = "Past start time - show your QR at the hub",
                    items = needsCheckIn,
                    nameFor = ::nameFor,
                    highlightFirst = true,
                )
            }

            BookingGroup(
                title = "Upcoming",
                subtitle = null,
                items = upcoming,
                nameFor = ::nameFor,
                emptyTitle = "Nothing scheduled",
                emptyBody = "Confirmed bookings that match your filters appear here.",
                onEmptyAction = onBookSlot,
                emptyActionLabel = "Book a slot",
            )

            BookingGroup(
                title = "History",
                subtitle = null,
                items = if (showAllHistory) history else history.take(HISTORY_PREVIEW),
                nameFor = ::nameFor,
                emptyTitle = "No history yet",
                emptyBody = "Completed and cancelled bookings will show up here.",
                actionLabel = when {
                    history.size <= HISTORY_PREVIEW -> null
                    showAllHistory -> "Show less"
                    else -> "Show all ${history.size}"
                },
                onAction = { showAllHistory = !showAllHistory },
            )
        }
    }

    if (showFilterSheet) {
        FilterSheet(
            station = stationFilter,
            onStationChange = { stationFilter = it },
            from = fromFilter,
            onFromChange = { fromFilter = it },
            to = toFilter,
            onToChange = { toFilter = it },
            onReset = { stationFilter = ""; fromFilter = ""; toFilter = "" },
            onDismiss = { showFilterSheet = false },
        )
    }
}

// ---------- Header ----------

private val EYEBROW_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

@Composable
private fun GreetingHeader(name: String?, onRefresh: () -> Unit) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val firstName = name?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${LocalDate.now().format(EYEBROW_FORMAT)} · Prosumer".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = StripeMuted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (firstName != null) "$greeting, $firstName" else greeting,
                style = MaterialTheme.typography.headlineMedium,
                color = StripeInk,
                maxLines = 2,
            )
        }
        JouleIconButton(icon = JouleIcons.Refresh, contentDescription = "Refresh dashboard", onClick = onRefresh)
    }
}

// ---------- Hero ----------

private val HERO_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault())

private fun relativeLabel(iso: String): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
    val minutes = Duration.between(Instant.now(), instant).toMinutes()
    return when {
        minutes < 0 -> "Check-in due"
        minutes < 60 -> "In ${minutes}m"
        minutes < 60 * 24 -> "In ${minutes / 60}h"
        else -> "In ${minutes / (60 * 24)}d"
    }
}

@Composable
private fun NextBookingHero(
    booking: ReservationResponse?,
    stationName: String?,
    onBookSlot: () -> Unit,
    onOpenBookings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(RoundedCornerShape(20.dp), 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(StripeInk),
    ) {
        // Thin signature stripe along the top edge - a narrow accent, never a page-wide gradient.
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(StripePrimary, SignatureCyan))),
        )
        Image(
            painter = painterResource(R.drawable.home),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 12.dp)
                .size(104.dp),
        )
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp)) {
            Text(
                text = if (booking != null) "NEXT BOOKING" else "NO UPCOMING BOOKINGS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(8.dp))
            if (booking != null) {
                Text(
                    text = stationName.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.62f),
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(JouleIcons.Clock, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = runCatching { HERO_DATE_FORMAT.format(Instant.parse(booking.scheduledAt)) }.getOrDefault(booking.scheduledAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(relativeLabel(booking.scheduledAt), style = MaterialTheme.typography.labelSmall, color = SignatureCyan)
                }
                Spacer(Modifier.height(18.dp))
                OnDarkButton(text = "View bookings", onClick = onOpenBookings)
            } else {
                Text(
                    text = "Reserve a battery slot at a nearby hub",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(0.62f),
                )
                Spacer(Modifier.height(18.dp))
                JoulePrimaryButton(text = "Book a slot", onClick = onBookSlot, leadingIcon = JouleIcons.Plus, modifier = Modifier.height(46.dp))
            }
        }
    }
}

@Composable
private fun OnDarkButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(Color.White)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = StripeInk)
        Spacer(Modifier.width(6.dp))
        Icon(JouleIcons.ChevronRight, contentDescription = null, tint = StripeInk, modifier = Modifier.size(16.dp))
    }
}

// ---------- Filters ----------

@Composable
private fun FiltersChip(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(if (count > 0) StripeBrandVioletSoft else StripeSurface)
            .border(1.dp, if (count > 0) StripePrimary.copy(alpha = 0.35f) else StripeBorder, PillShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(JouleIcons.Sliders, contentDescription = null, tint = StripePrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (count > 0) "Filters · $count" else "Filters", style = MaterialTheme.typography.labelMedium, color = StripePrimary)
    }
}

@Composable
private fun ActiveFilterSummary(station: String, from: String, to: String, onClear: () -> Unit) {
    val parts = buildList {
        if (station.isNotBlank()) add("Station “$station”")
        if (from.isNotBlank() || to.isNotBlank()) add("${from.ifBlank { "…" }} → ${to.ifBlank { "…" }}")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(parts.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = StripeBody, modifier = Modifier.weight(1f))
        TextButton(onClick = onClear) { Text("Clear", color = StripePrimary, style = MaterialTheme.typography.labelMedium) }
    }
}

private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    station: String,
    onStationChange: (String) -> Unit,
    from: String,
    onFromChange: (String) -> Unit,
    to: String,
    onToChange: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = StripeSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Filter bookings", style = MaterialTheme.typography.titleLarge, color = StripeInk)
            JouleTextField(
                value = station,
                onValueChange = onStationChange,
                label = "Station ID",
                placeholder = "Contains…",
                leadingIcon = JouleIcons.Search,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(label = "From", value = from, onClick = { pickingFrom = true }, modifier = Modifier.weight(1f))
                DateField(label = "To", value = to, onClick = { pickingTo = true }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                JouleSecondaryButton(text = "Reset", onClick = onReset, modifier = Modifier.weight(1f))
                JoulePrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.weight(1f))
            }
        }
    }

    if (pickingFrom) DatePick(initial = from, onPicked = { onFromChange(it); pickingFrom = false }, onDismiss = { pickingFrom = false })
    if (pickingTo) DatePick(initial = to, onPicked = { onToChange(it); pickingTo = false }, onDismiss = { pickingTo = false })
}

@Composable
private fun DateField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = StripeInk)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, StripeBorder, RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(JouleIcons.Calendar, contentDescription = null, tint = StripeMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(value.ifBlank { "Any date" }, style = MaterialTheme.typography.bodyMedium, color = if (value.isBlank()) StripeMuted else StripeInk)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePick(initial: String, onPicked: (String) -> Unit, onDismiss: () -> Unit) {
    val initialMillis = runCatching {
        LocalDate.parse(initial).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }.getOrNull()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(ISO_DATE))
                } ?: onDismiss()
            }) { Text("Apply", color = StripePrimary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = StripeBody) } },
    ) {
        DatePicker(state = state)
    }
}

// ---------- Booking lists ----------

private val ROW_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE · HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault())

@Composable
private fun BookingGroup(
    title: String,
    subtitle: String?,
    items: List<ReservationResponse>,
    nameFor: (String) -> String,
    highlightFirst: Boolean = false,
    emptyTitle: String = "",
    emptyBody: String = "",
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = if (items.isNotEmpty()) "$title · ${items.size}" else title,
            subtitle = subtitle,
            actionLabel = actionLabel,
            onAction = onAction,
        )
        JouleCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            if (items.isEmpty()) {
                EmptyState(
                    icon = JouleIcons.Calendar,
                    title = emptyTitle,
                    body = emptyBody,
                    actionLabel = emptyActionLabel,
                    onAction = onEmptyAction,
                )
            } else {
                items.forEachIndexed { index, reservation ->
                    if (index > 0) Hairline()
                    BookingRow(reservation, nameFor(reservation.stationId), highlight = highlightFirst && index == 0)
                }
            }
        }
    }
}

@Composable
private fun BookingRow(reservation: ReservationResponse, stationName: String, highlight: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DateBlock(iso = reservation.scheduledAt, highlight = highlight)
        Column(modifier = Modifier.weight(1f)) {
            Text(stationName, style = MaterialTheme.typography.titleSmall, color = StripeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${runCatching { ROW_TIME_FORMAT.format(Instant.parse(reservation.scheduledAt)) }.getOrDefault("")} · Slot ${reservation.slotId.takeLast(4)}",
                style = MaterialTheme.typography.bodySmall,
                color = StripeMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StatusPill(reservation.status)
    }
}
