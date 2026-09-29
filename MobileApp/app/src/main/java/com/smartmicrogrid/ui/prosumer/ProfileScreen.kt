// ============================================================
// File: ProfileScreen.kt
// Purpose: Prosumer profile screen - avatar with gallery photo upload,
//          editable fields, account status, deactivation, and logout.
//          No extra top spacing; content starts directly below insets.
// ============================================================
package com.smartmicrogrid.ui.prosumer

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
import com.smartmicrogrid.data.local.ProsumerSessionDao
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ProsumerProfileResponse
import com.smartmicrogrid.data.remote.dto.ProsumerUpdateRequest
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.theme.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

// Generate avatar initials from name
private fun initials(name: String?): String {
    if (name.isNullOrBlank()) return "?"
    return name.trim().split("\\s+".toRegex())
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it[0].uppercase() }
}

// Decode base64 data string to Compose ImageBitmap
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
fun ProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionDao = remember { ProsumerSessionDao(context) }
    val session = remember { sessionDao.getSession() }

    var profile by remember { mutableStateOf<ProsumerProfileResponse?>(null) }
    var avatarBase64 by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var editFullName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    // Image picker launcher for photo upload
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

                session?.let { s ->
                    scope.launch {
                        try {
                            val req = ProsumerUpdateRequest(
                                fullName = null,
                                email = null,
                                phone = null,
                                address = null,
                                profilePicture = base64,
                            )
                            ApiClient.service.updateProsumerProfile(s.nic, req)
                            Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            Toast.makeText(context, "Failed to save picture", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to load selected photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        session?.let {
            try {
                val response = ApiClient.service.getProsumerProfile(it.nic)
                if (response.isSuccessful && response.body() != null) {
                    val p = response.body()!!
                    profile = p
                    avatarBase64 = p.profilePicture
                    editFullName = p.fullName
                    editEmail = p.email
                    editPhone = p.phone ?: ""
                    editAddress = p.address ?: ""
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    Scaffold(
        containerColor = StripeCanvas,
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StripePrimary, strokeWidth = 3.dp)
            }
        } else {
            profile?.let { p ->
                val avatarBitmap = decodeBase64ToBitmap(avatarBase64)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Header section (No extra top spacing)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Clickable Avatar with unclipped Photo Upload badge
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Avatar circle
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(StripeBrandVioletSoft)
                                    .border(3.dp, StripeSurface, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (avatarBitmap != null) {
                                    Image(
                                        bitmap = avatarBitmap,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Text(
                                        text = initials(p.fullName),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 28.sp,
                                        ),
                                        color = StripePrimary,
                                    )
                                }
                            }

                            // Camera badge (unclipped)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(StripePrimary)
                                    .border(2.5.dp, StripeSurface, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = JouleIcons.Camera,
                                    contentDescription = "Change photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = p.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StripeInk,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = p.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = StripeBody,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Status pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (p.status == "Active") StripeBrandVioletSoft else StripeErrorContainer,
                        ) {
                            Text(
                                text = p.status,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (p.status == "Active") StripePrimary else StripeError,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // Profile fields
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (isEditing) {
                            Text("Edit Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = StripeInk)
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
                                                    fullName = editFullName.ifBlank { null },
                                                    email = editEmail.ifBlank { null },
                                                    phone = editPhone.ifBlank { null },
                                                    address = editAddress.ifBlank { null },
                                                    profilePicture = avatarBase64,
                                                )
                                                val res = ApiClient.service.updateProsumerProfile(p.nic, req)
                                                if (res.isSuccessful || res.code() == 204) {
                                                    isEditing = false
                                                    profile = p.copy(fullName = editFullName, email = editEmail, phone = editPhone, address = editAddress)
                                                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StripePrimary, contentColor = StripeOnPrimary),
                                ) { Text("Save") }
                            }
                        } else {
                            // Info card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = StripeSurface,
                                tonalElevation = 1.dp,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StripeBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProfileRow(icon = JouleIcons.User, label = "NIC", value = p.nic)
                                    HorizontalDivider(color = StripeBorder)
                                    ProfileRow(icon = JouleIcons.MapPin, label = "Phone", value = p.phone ?: "Not provided")
                                    HorizontalDivider(color = StripeBorder)
                                    ProfileRow(icon = JouleIcons.MapPin, label = "Address", value = p.address ?: "Not provided")
                                }
                            }

                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StripePrimary, contentColor = StripeOnPrimary),
                            ) {
                                Icon(JouleIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile")
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Danger zone
                        if (p.status != "Deactivated") {
                            OutlinedButton(
                                onClick = { showDeactivateDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StripeError),
                            ) { Text("Request Account Deactivation") }
                        } else {
                            Surface(shape = RoundedCornerShape(12.dp), color = StripeErrorContainer) {
                                Text(
                                    "Your account is deactivated. Contact Backoffice to reactivate.",
                                    modifier = Modifier.padding(16.dp),
                                    color = StripeOnErrorContainer,
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
                Text("Couldn't load profile.", color = StripeBody)
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
                }) { Text("Deactivate", color = StripeError) }
            },
            dismissButton = { TextButton(onClick = { showDeactivateDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = StripePrimary, modifier = Modifier.size(18.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = StripeBody)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = StripeInk)
        }
    }
}