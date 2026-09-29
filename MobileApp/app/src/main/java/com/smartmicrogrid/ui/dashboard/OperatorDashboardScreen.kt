// ============================================================
// File: OperatorDashboardScreen.kt
// Purpose: Grid operator dashboard — confirmed/completed-today
//          counts plus pending-by-station breakdown. Reads live
//          from GET /api/dashboard/operator/summary. No business
//          rule is computed here; the API returns the numbers.
// Author: Dinil
// ============================================================
package com.smartmicrogrid.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.OperatorSummaryResponse
import kotlinx.coroutines.launch

@Composable
fun OperatorDashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var summary by remember { mutableStateOf<OperatorSummaryResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Fetches the summary from the API. Runs on composition.
    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            val resp = ApiClient.service.getOperatorSummary()
            if (resp.isSuccessful && resp.body() != null) {
                summary = resp.body()
            } else {
                error = "Failed to load dashboard (${resp.code()})"
            }
        } catch (e: Exception) {
            error = "Network error: ${e.message}"
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Operator Dashboard", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Today's verification activity, live from the API.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Column
        }

        if (error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("✗ ${error!!}", color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                val resp = ApiClient.service.getOperatorSummary()
                                if (resp.isSuccessful) summary = resp.body()
                                else error = "Failed (${resp.code()})"
                            } catch (e: Exception) {
                                error = "Network error: ${e.message}"
                            } finally { loading = false }
                        }
                    }) { Text("Retry") }
                }
            }
            return@Column
        }

        summary?.let { s ->
            // Counts row — two big cards side by side.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CountCard(
                    modifier = Modifier.weight(1f),
                    label = "Confirmed Today",
                    value = s.confirmedTodayCount,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
                CountCard(
                    modifier = Modifier.weight(1f),
                    label = "Completed Today",
                    value = s.completedTodayCount,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("Pending by station", style = MaterialTheme.typography.titleMedium)

            if (s.pendingByStation.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "No pending reservations.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.pendingByStation, key = { it.stationId }) { row ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        row.stationName.ifBlank { row.stationId.takeLast(8) },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "Station ${row.stationId.takeLast(8)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    row.pendingCount.toString(),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Small summary card showing one integer metric.
@Composable
private fun CountCard(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(value.toString(), style = MaterialTheme.typography.displaySmall)
        }
    }
}