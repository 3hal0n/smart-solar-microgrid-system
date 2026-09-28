// ============================================================
// File: TestBookingsActivity.kt
// Purpose: Temporary launcher to capture UI screenshots of the 
//          Bookings Screen using mock data. Bypasses login and 
//          network requirements. Does NOT touch real workflow files.
// Author: Migara (Temporary Test File)
// ============================================================
package com.smartmicrogrid.ui.prosumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.data.remote.dto.ReservationResponse
import com.smartmicrogrid.ui.theme.SmartMicrogridTheme

class TestBookingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartMicrogridTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MockBookingsScreen()
                }
            }
        }
    }
}

@Composable
fun MockBookingsScreen() {
    // Hardcoded mock data to show the UI perfectly without network/backend
    val mockReservations = listOf(
        ReservationResponse(
            id = "64f1a2b3c4d5e6f7g8h9i0j1",
            prosumerNic = "991234567V",
            stationId = "station-alpha-001",
            slotId = "slot-05",
            scheduledAt = "2026-10-05T14:00:00Z",
            status = "Confirmed",
            qrToken = "MOCK_QR_TOKEN_FOR_TESTING_12345",
            createdAt = "2026-09-28T10:00:00Z",
            completedAt = null,
            cancelReason = null
        ),
        ReservationResponse(
            id = "99z8y7x6w5v4u3t2s1r0q9p8",
            prosumerNic = "991234567V",
            stationId = "station-beta-002",
            slotId = "slot-12",
            scheduledAt = "2026-10-10T09:30:00Z",
            status = "Completed",
            qrToken = null,
            createdAt = "2026-09-20T08:00:00Z",
            completedAt = "2026-10-10T10:00:00Z",
            cancelReason = null
        )
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "My Bookings (Test Mode)",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Showing mock data for UI screenshots",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Reusing your REAL ReservationCard component to show exactly how it looks!
        mockReservations.forEach { reservation ->
            ReservationCard(
                reservation = reservation,
                onCancel = { /* Do nothing in test mode */ }
            )
        }
    }
}