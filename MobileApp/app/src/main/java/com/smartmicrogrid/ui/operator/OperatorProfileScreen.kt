// ============================================================
// File: OperatorProfileScreen.kt
// Purpose: Grid Operator profile screen - displays staff session
//          details, operator roles/permissions, and provides a
//          secure logout button to clear credentials and return to
//          MainActivity.
// ============================================================
package com.smartmicrogrid.ui.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.ui.auth.StaffSessionPreferences
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val staffSession = remember { StaffSessionPreferences.read(context) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Operator Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StripeSurface,
                    titleContentColor = StripeInk,
                ),
            )
        },
        containerColor = StripeCanvas,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Operator Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StripeSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StripeBorder, RoundedCornerShape(16.dp)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Avatar circle
                    val initials = (staffSession?.fullName ?: "GO")
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(StripeBrandVioletSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (initials.isNotEmpty()) initials else "OP",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = StripePrimary,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = staffSession?.fullName ?: "Grid Operator",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StripeInk,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "@${staffSession?.username ?: "operator"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StripeMuted,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Role Pill Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = StripeCyanContainer,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(StripeCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = staffSession?.role ?: "Grid Operator",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = StripeOnCyanContainer,
                            )
                        }
                    }
                }
            }

            // Information Section Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StripeSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StripeBorder, RoundedCornerShape(16.dp)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        "Account Information",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StripeInk,
                    )

                    HorizontalDivider(color = StripeBorder)

                    InfoRow(label = "Username", value = staffSession?.username ?: "—")
                    InfoRow(label = "Full Name", value = staffSession?.fullName ?: "—")
                    InfoRow(label = "Assigned Role", value = staffSession?.role ?: "GridOperator")
                    InfoRow(label = "Account Status", value = "Active", valueColor = StripeSuccess)
                    InfoRow(
                        label = "Clearance",
                        value = "Station Verification & Transfer",
                        valueColor = StripeBody,
                    )
                }
            }

            // System Capabilities Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StripeSurfaceAlt),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StripeBorder, RoundedCornerShape(16.dp)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Operator Privileges",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = StripeInk,
                    )
                    Text(
                        "• Scan Prosumer QR codes at solar charging nodes\n" +
                        "• Validate energy slot reservation token authenticity\n" +
                        "• Finalise physical battery power transfers in real time\n" +
                        "• Inspect microgrid hub coordinates and declared capacities",
                        style = MaterialTheme.typography.bodySmall,
                        color = StripeBody,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StripeErrorContainer,
                    contentColor = StripeOnErrorContainer,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Icon(
                    imageVector = JouleIcons.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Sign Out",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Sign Out",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StripeInk,
                )
            },
            text = {
                Text(
                    "Are you sure you want to end your operator session? You will need your username and password to log in again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StripeBody,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        StaffSessionPreferences.clear(context)
                        ApiClient.authToken = null
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StripeError,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Cancel", color = StripeInk)
                }
            },
            containerColor = StripeSurface,
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = StripeInk,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = StripeMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}
