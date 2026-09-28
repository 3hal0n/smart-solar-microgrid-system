// ============================================================
// File: LoginScreen.kt
// Purpose: Sign-in screen using home.png image, Joule logo,
//          tagline, password eye toggle, and role selector.
// ============================================================
package com.smartmicrogrid.ui.prosumer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmicrogrid.R
import com.smartmicrogrid.data.local.ProsumerSessionDao
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ProsumerLoginRequest
import com.smartmicrogrid.data.remote.dto.StaffLoginRequest
import com.smartmicrogrid.ui.auth.StaffSession
import com.smartmicrogrid.ui.auth.StaffSessionPreferences
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.components.JouleMark
import com.smartmicrogrid.ui.home.UserRole
import com.smartmicrogrid.ui.theme.StripeAccent
import com.smartmicrogrid.ui.theme.StripeBody
import com.smartmicrogrid.ui.theme.StripeBorder
import com.smartmicrogrid.ui.theme.StripeBrandVioletSoft
import com.smartmicrogrid.ui.theme.StripeCanvas
import com.smartmicrogrid.ui.theme.StripeError
import com.smartmicrogrid.ui.theme.StripeInk
import com.smartmicrogrid.ui.theme.StripeOnPrimary
import com.smartmicrogrid.ui.theme.StripePrimary
import com.smartmicrogrid.ui.theme.StripeSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (UserRole) -> Unit, onNavigateToRegister: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionDao = remember { ProsumerSessionDao(context) }

    var role by remember { mutableStateOf(UserRole.Prosumer) }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        errorMessage = null
        if (identifier.isBlank() || password.isBlank()) {
            errorMessage = if (role == UserRole.Prosumer) "NIC and password are required" else "Username and password are required"
            return
        }
        isLoading = true
        scope.launch {
            try {
                if (role == UserRole.Prosumer) {
                    val response = ApiClient.service.loginProsumer(ProsumerLoginRequest(identifier.trim(), password))
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        ApiClient.authToken = body.token
                        sessionDao.saveSession(body.nic, body.fullName, body.token, body.status)
                        onLoginSuccess(UserRole.Prosumer)
                    } else {
                        errorMessage = when (response.code()) {
                            401 -> "Invalid NIC or password"
                            403 -> "Account is not active. Contact Backoffice."
                            else -> "Login failed: ${response.code()}"
                        }
                    }
                } else {
                    val response = ApiClient.service.loginStaff(StaffLoginRequest(identifier.trim(), password))
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        ApiClient.authToken = body.token
                        StaffSessionPreferences.save(context, StaffSession(body.token, identifier.trim(), body.fullName, body.role))
                        onLoginSuccess(UserRole.fromClaimValue(body.role))
                    } else {
                        errorMessage = when (response.code()) {
                            401 -> "Invalid username or password"
                            else -> "Login failed: ${response.code()}"
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Network error: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(containerColor = StripeCanvas) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Home Image (specified by user)
            Image(
                painter = painterResource(id = R.drawable.home),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Joule Logo & Tagline
            JouleMark(modifier = Modifier.width(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Joule",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = StripeInk,
            )
            Text(
                text = "Power, exchanged precisely.",
                style = MaterialTheme.typography.bodyMedium,
                color = StripeBody,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Form container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RoleToggle(
                    selected = role,
                    onSelect = {
                        role = it
                        identifier = ""
                        errorMessage = null
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = if (role == UserRole.Prosumer) "NIC Number" else "Username",
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field with Eye toggle icon
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) JouleIcons.EyeOff else JouleIcons.Eye,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = StripeBody,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = StripeBorder,
                        focusedBorderColor = StripePrimary,
                        unfocusedContainerColor = StripeSurface,
                        focusedContainerColor = StripeSurface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StripeError.copy(alpha = 0.1f))
                            .border(1.dp, StripeError.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = it,
                            color = StripeError,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = ::submit,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = StripePrimary, contentColor = StripeOnPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = StripeOnPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Sign In", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (role == UserRole.Prosumer) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Don't have an account? ", color = StripeBody)
                        Text("Register Now", color = StripeAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// Segmented role switch between Prosumer and Grid Operator
@Composable
private fun RoleToggle(selected: UserRole, onSelect: (UserRole) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(StripeBrandVioletSoft)
            .padding(4.dp),
    ) {
        listOf(UserRole.Prosumer to "Prosumer", UserRole.GridOperator to "Grid Operator").forEach { (value, label) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) StripeAccent else Color.Transparent)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (isSelected) StripeOnPrimary else StripeInk,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

// Reusable text field
@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = StripeBorder,
            focusedBorderColor = StripePrimary,
            unfocusedContainerColor = StripeSurface,
            focusedContainerColor = StripeSurface,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
