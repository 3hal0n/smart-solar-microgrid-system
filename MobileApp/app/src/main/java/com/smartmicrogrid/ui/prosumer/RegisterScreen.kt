// ============================================================
// File: RegisterScreen.kt
// Purpose: Prosumer registration screen with NIC as primary key.
//          Validates input and calls backend API to create account.
// Author: Rukshan (visual restyle to Stripe design per
//          docs/stripe.design.md: Shalon, 2026-09-28 — form fields,
//          validation and the API call below are still his original
//          logic, untouched)
// ============================================================
package com.smartmicrogrid.ui.prosumer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ProsumerRegistrationRequest
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.theme.StripeAccent
import com.smartmicrogrid.ui.theme.StripeBody
import com.smartmicrogrid.ui.theme.StripeCanvas
import com.smartmicrogrid.ui.theme.StripeError
import com.smartmicrogrid.ui.theme.StripeInk
import com.smartmicrogrid.ui.theme.StripeOnPrimary
import com.smartmicrogrid.ui.theme.StripePrimary
import com.smartmicrogrid.ui.theme.StripeSurface
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

    Scaffold(containerColor = StripeCanvas) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StripeSurface)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onNavigateToLogin),
                contentAlignment = Alignment.Center,
            ) {
                Icon(JouleIcons.Back, contentDescription = "Back to sign in", tint = StripeInk)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Create your account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = StripeInk)
            Text("Your NIC is your account's primary identifier.", style = MaterialTheme.typography.bodyMedium, color = StripeBody)
            Spacer(modifier = Modifier.height(8.dp))

            AuthTextField(value = nic, onValueChange = { nic = it }, label = "NIC Number *")
            AuthTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name *")
            AuthTextField(value = email, onValueChange = { email = it }, label = "Email *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            AuthTextField(value = phone, onValueChange = { phone = it }, label = "Phone", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            AuthTextField(value = address, onValueChange = { address = it }, label = "Address", singleLine = false, minLines = 2)
            AuthTextField(value = password, onValueChange = { password = it }, label = "Password *", visualTransformation = PasswordVisualTransformation())

            errorMessage?.let {
                Text(it, color = StripeError, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
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
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = StripePrimary, contentColor = StripeOnPrimary),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = StripeOnPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Register", style = MaterialTheme.typography.titleSmall)
                }
            }

            TextButton(onClick = onNavigateToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Already have an account? ", color = StripeBody)
                Text("Sign in", color = StripeAccent, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
