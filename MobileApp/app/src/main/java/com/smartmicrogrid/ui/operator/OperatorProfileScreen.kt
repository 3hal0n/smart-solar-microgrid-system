// ============================================================
// File: OperatorProfileScreen.kt
// Purpose: Grid Operator profile screen — displays staff session
//          details, avatar photo, privileges, and logout button.
//          No extra top spacing; clean Stripe design styling.
// ============================================================
package com.smartmicrogrid.ui.operator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.ui.auth.StaffSessionPreferences
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.theme.*
import java.io.ByteArrayOutputStream

private fun decodeBase64ToBitmap(base64Str: String?): ImageBitmap? {
    if (base64Str.isNullOrBlank()) return null
    return try {
        val clean = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
        val decoded = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val staffSession = remember { StaffSessionPreferences.read(context) }
    var avatarBase64 by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val base64 = "data:image/jpeg;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                avatarBase64 = base64
                Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val initials = (staffSession?.fullName ?: "GO")
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()

                    val avatarBitmap = decodeBase64ToBitmap(avatarBase64)

                    // Avatar with upload trigger
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(StripeBrandVioletSoft)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { imagePickerLauncher.launch("image/*") },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = if (initials.isNotEmpty()) initials else "OP",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = StripePrimary,
                            )
                        }

                        // Camera overlay badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(StripePrimary)
                                .border(2.dp, StripeSurface, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = JouleIcons.Camera,
                                contentDescription = "Upload photo",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = staffSession?.fullName ?: "Grid Operator",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StripeInk,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "@${staffSession?.username ?: "operator"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StripeMuted,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Role Pill Badge (Violet/Primary instead of cyan)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = StripeBrandVioletSoft,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(StripePrimary),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = staffSession?.role ?: "Grid Operator",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = StripePrimary,
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
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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

            Spacer(modifier = Modifier.height(4.dp))

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
                    modifier = Modifier.size(18.dp),
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
