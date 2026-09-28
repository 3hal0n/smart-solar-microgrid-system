// ============================================================
// File: LoginScreen.kt
// Purpose: Sign-in screen. Prosumer mode (NIC + password) saves the
//          session to SQLite via ProsumerSessionDao on success — this
//          part is Rukshan's original logic, kept as-is. Grid Operator
//          mode (username + password) was added alongside it since
//          both roles need one entry point: it calls the existing
//          staff login endpoint (POST /api/auth/login, Migara's — the
//          same one the web app's LoginPage already uses) and saves
//          its session via StaffSessionPreferences instead.
// Author: Rukshan (extended by Shalon: Grid Operator mode + Stripe
//          visual design per docs/stripe.design.md, 2026-09-28)
// ============================================================
package com.smartmicrogrid.ui.prosumer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.R
import com.smartmicrogrid.data.local.ProsumerSessionDao
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ProsumerLoginRequest
import com.smartmicrogrid.data.remote.dto.StaffLoginRequest
import com.smartmicrogrid.ui.auth.StaffSession
import com.smartmicrogrid.ui.auth.StaffSessionPreferences
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (UserRole) -> Unit, onNavigateToRegister: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionDao = remember { ProsumerSessionDao(context) }

    var role by remember { mutableStateOf(UserRole.Prosumer) }
    var identifier by remember { mutableStateOf("") } // NIC for Prosumer, username for Grid Operator
    var password by remember { mutableStateOf("") }
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Hero panel — stands in for a stock photo (see docs/stripe.design.md's
            // hero-angled-panel token): a soft violet gradient card framing the app's own
            // house-with-panels illustration rather than imagery we don't have rights to.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(StripeBrandVioletSoft, StripeCanvas))),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.home),
                    contentDescription = null,
                    modifier = Modifier.size(140.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Sign in", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = StripeInk)
            Spacer(modifier = Modifier.height(20.dp))

            RoleToggle(selected = role, onSelect = { role = it; identifier = ""; errorMessage = null })
            Spacer(modifier = Modifier.height(20.dp))

            AuthTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = if (role == UserRole.Prosumer) "NIC Number" else "Username",
            )
            Spacer(modifier = Modifier.height(12.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                visualTransformation = PasswordVisualTransformation(),
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = StripeError, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = ::submit,
                enabled = !isLoading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = StripePrimary, contentColor = StripeOnPrimary),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = StripeOnPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Sign In", style = MaterialTheme.typography.titleSmall)
                }
            }

            if (role == UserRole.Prosumer) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateToRegister) {
                    Text("Don't have an account? ", color = StripeBody)
                    Text("Register Now", color = StripeAccent, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// A pill segmented control (docs/stripe.design.md's segmented-control token) switching between
// the two roles the mobile app serves — see UserRole.kt. Grid Operator accounts are
// Backoffice-provisioned, not self-registered, so this is also what decides whether the
// "Register Now" link below shows at all.
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

// internal (not private) so RegisterScreen.kt in this same package can reuse it too.
@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
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
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
