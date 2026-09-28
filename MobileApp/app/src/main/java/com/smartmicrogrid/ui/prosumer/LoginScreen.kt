// ============================================================
// File: LoginScreen.kt
// Purpose: Prosumer login screen. Saves session to SQLite on success.
// Author: Rukshan
// ============================================================
package com.smartmicrogrid.ui.prosumer

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.data.local.ProsumerSessionDao
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ProsumerLoginRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionDao = remember { ProsumerSessionDao(context) }
    var nic by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Prosumer Login") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Welcome Back", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(value = nic, onValueChange = { nic = it }, label = { Text("NIC Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)

            errorMessage?.let { Spacer(modifier = Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {
                errorMessage = null
                if (nic.isBlank() || password.isBlank()) {
                    errorMessage = "NIC and Password are required"
                } else {
                    isLoading = true
                    scope.launch {
                        try {
                            val request = ProsumerLoginRequest(nic.trim(), password)
                            val response = ApiClient.service.loginProsumer(request)
                            if (response.isSuccessful && response.body() != null) {
                                val loginResp = response.body()!!
                                // 1. Set global token for ApiClient Interceptor
                                ApiClient.authToken = loginResp.token
                                // 2. Save to SQLite for local persistence
                                sessionDao.saveSession(loginResp.nic, loginResp.fullName, loginResp.token, loginResp.status)
                                onLoginSuccess()
                            } else {
                                errorMessage = when (response.code()) {
                                    401 -> "Invalid NIC or password"
                                    403 -> "Account is not active. Contact Backoffice."
                                    else -> "Login failed: ${response.code()}"
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Network error: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("Login")
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateToRegister) { Text("Don't have an account? Register") }
        }
    }
}