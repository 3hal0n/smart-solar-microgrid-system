// ============================================================
// File: ProfileScreen.kt
// Purpose: Prosumer profile screen - view/edit profile and request deactivation.
//          All operations go through the Web API (FAT Service pattern).
// Author: Rukshan
// ============================================================
package com.smartmicrogrid.ui.prosumer

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.data.local.ProsumerSessionDao
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ProsumerProfileResponse
import com.smartmicrogrid.data.remote.dto.ProsumerUpdateRequest
import kotlinx.coroutines.launch

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
            } catch (e: Exception) { /* Handle error */ }
            isLoading = false
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("My Profile") }) }) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            profile?.let { p ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = if (p.status == "Active") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                        Text("Status: ${p.status}", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                    }

                    if (isEditing) {
                        OutlinedTextField(value = editFullName, onValueChange = { editFullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = editAddress, onValueChange = { editAddress = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        val req = ProsumerUpdateRequest(editFullName.ifBlank { null }, editEmail.ifBlank { null }, editPhone.ifBlank { null }, editAddress.ifBlank { null })
                                        val res = ApiClient.service.updateProsumerProfile(p.nic, req)
                                        if (res.isSuccessful || res.code() == 204) {
                                            isEditing = false
                                            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) { /* Handle error */ }
                                }
                            }, modifier = Modifier.weight(1f)) { Text("Save") }
                            OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        }
                    } else {
                        ProfileField("NIC", p.nic)
                        ProfileField("Full Name", p.fullName)
                        ProfileField("Email", p.email)
                        ProfileField("Phone", p.phone ?: "Not provided")
                        ProfileField("Address", p.address ?: "Not provided")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (p.status != "Deactivated") {
                        OutlinedButton(onClick = { showDeactivateDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Text("Request Account Deactivation")
                        }
                    } else {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text("Your account has been deactivated. Contact Backoffice to reactivate.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }

                    OutlinedButton(onClick = { sessionDao.clearSession(); ApiClient.authToken = null; onLogout() }, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
                }
            }
        }
    }

    if (showDeactivateDialog) {
        AlertDialog(onDismissRequest = { showDeactivateDialog = false }, title = { Text("Deactivate Account?") },
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
                        } catch (e: Exception) { /* Handle error */ }
                    }
                }) { Text("Deactivate", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeactivateDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}