// ============================================================
// File: ProfileScreen.kt
// Purpose: Prosumer profile screen — polished avatar header, editable
//          fields, account status, and logout. No unnecessary top
//          spacing; Scaffold has no TopAppBar so content starts right
//          below the system status bar. All operations go through
//          the Web API (FAT Service pattern).
// Author: Rukshan (enhanced 2026-09-29)
// ============================================================
package com.smartmicrogrid.ui.prosumer

import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmicrogrid.data.local.ProsumerSessionDao
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ProsumerProfileResponse
import com.smartmicrogrid.data.remote.dto.ProsumerUpdateRequest
import com.smartmicrogrid.ui.components.JouleIcons
import kotlinx.coroutines.launch

// Generate avatar initials from name
private fun initials(name: String?): String {
    if (name.isNullOrBlank()) return "?"
    return name.trim().split("\\s+".toRegex())
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it[0].uppercase() }
}

// Deterministic pastel color from initials
private fun avatarColor(name: String?): Color {
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF0EA5E9),
        Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEC4899),
    )
    val idx = (name?.sumOf { it.code } ?: 0) % colors.size
    return colors[idx]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionDao = remember { ProsumerSessionDao(context) }
    val session = remember { sessionDao.getSession() }

    var profile by remember { mutableStateOf<ProsumerProfileResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var editFullName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        session?.let {
            try {
                val response = ApiClient.service.getProsumerProfile(it.nic)
                if (response.isSuccessful && response.body() != null) {
                    profile = response.body()
                    editFullName = profile!!.fullName
                    editEmail = profile!!.email
                    editPhone = profile!!.phone ?: ""
                    editAddress = profile!!.address ?: ""
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // No topBar — content starts directly below status bar insets
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            }
        } else {
            profile?.let { p ->
                val color = avatarColor(p.fullName)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // ── Avatar hero header ─────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.12f), MaterialTheme.colorScheme.background))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp),
                        ) {
                            // Avatar circle with initials
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = initials(p.fullName),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                    ),
                                    color = Color.White,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = p.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = p.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Status pill
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (p.status == "Active")
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    text = p.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (p.status == "Active")
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }

                    // ── Profile fields ─────────────────────────
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        if (isEditing) {
                            Text("Edit Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            OutlinedTextField(value = editFullName, onValueChange = { editFullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = editAddress, onValueChange = { editAddress = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val req = ProsumerUpdateRequest(
                                                    editFullName.ifBlank { null },
                                                    editEmail.ifBlank { null },
                                                    editPhone.ifBlank { null },
                                                    editAddress.ifBlank { null },
                                                )
                                                val res = ApiClient.service.updateProsumerProfile(p.nic, req)
                                                if (res.isSuccessful || res.code() == 204) {
                                                    isEditing = false
                                                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                ) { Text("Save") }
                            }
                        } else {
                            // Info card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProfileRow(icon = JouleIcons.User, label = "NIC", value = p.nic)
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ProfileRow(icon = JouleIcons.MapPin, label = "Phone", value = p.phone ?: "Not provided")
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ProfileRow(icon = JouleIcons.MapPin, label = "Address", value = p.address ?: "Not provided")
                                }
                            }

                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(JouleIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Danger zone
                        if (p.status != "Deactivated") {
                            OutlinedButton(
                                onClick = { showDeactivateDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) { Text("Request Account Deactivation") }
                        } else {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                Text(
                                    "Your account is deactivated. Contact Backoffice to reactivate.",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        // Logout
                        OutlinedButton(
                            onClick = { sessionDao.clearSession(); ApiClient.authToken = null; onLogout() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(JouleIcons.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Logout")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Couldn't load profile.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text("Deactivate Account?") },
            text = { Text("Are you sure? You will not be able to login until a Backoffice officer reactivates it.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeactivateDialog = false
                    scope.launch {
                        try {
                            val res = ApiClient.service.requestDeactivation(session!!.nic)
                            if (res.isSuccessful || res.code() == 204) {
                                sessionDao.updateStatus("Deactivated")
                                sessionDao.clearSession()
                                ApiClient.authToken = null
                                onLogout()
                            }
                        } catch (_: Exception) {}
                    }
                }) { Text("Deactivate", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeactivateDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}