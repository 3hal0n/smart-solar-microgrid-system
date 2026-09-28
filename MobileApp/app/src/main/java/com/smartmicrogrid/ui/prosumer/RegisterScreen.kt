// ============================================================
// File: RegisterScreen.kt
// Purpose: Prosumer registration screen with NIC as primary key.
//          Validates input and calls backend API to create account.
// Author: Rukshan
// ============================================================
package com.smartmicrogrid.ui.prosumer

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ProsumerRegistrationRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onNavigateToLogin: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nic by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Prosumer Registration") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create Your Account (NIC is Primary Key)", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(value = nic, onValueChange = { nic = it }, label = { Text("NIC Number *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password *") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Button(onClick = {
                errorMessage = null
                when {
                    nic.isBlank() -> errorMessage = "NIC is required"
                    fullName.isBlank() -> errorMessage = "Full name is required"
                    password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                    else -> {
                        isLoading = true
                        scope.launch {
                            try {
                                val request = ProsumerRegistrationRequest(nic.trim(), fullName.trim(), email.trim(), phone.trim().ifBlank { null }, address.trim().ifBlank { null }, password)
                                val response = ApiClient.service.registerProsumer(request)
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Registration successful! Pending Backoffice activation.", Toast.LENGTH_LONG).show()
                                    onNavigateToLogin()
                                } else {
                                    errorMessage = "Registration failed: ${response.code()}"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Network error: ${e.localizedMessage}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("Register")
            }

            TextButton(onClick = onNavigateToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Already have an account? Login")
            }
        }
    }
}