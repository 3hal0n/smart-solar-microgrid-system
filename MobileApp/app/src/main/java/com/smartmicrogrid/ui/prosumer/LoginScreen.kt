// ============================================================
// File: LoginScreen.kt
// Purpose: Sign-in screen styled to mirror the web app's login branding.
//          Uses home.png as hero banner with layered gradients,
//          brand badge, and sleek inputs for Prosumers and Grid Operators.
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmicrogrid.R
import com.smartmicrogrid.data.local.ProsumerSessionDao
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ProsumerLoginRequest
import com.smartmicrogrid.data.remote.dto.StaffLoginRequest
import com.smartmicrogrid.ui.auth.StaffSession
import com.smartmicrogrid.ui.auth.StaffSessionPreferences
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
                .verticalScroll(rememberScrollState()),
        ) {
            // Hero Banner styled like web app login
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
            ) {
                // Background image: home.png
                Image(
                    painter = painterResource(id = R.drawable.home),
                    contentDescription = "Solar Microgrid Hero",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Dark gradient overlay matching webapp's slate-950 aesthetic
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x660B132B),
                                    Color(0xCC0A2540),
                                    Color(0xF00A2540),
                                ),
                            ),
                        ),
                )

                // Hero content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Top brand header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JouleMark(modifier = Modifier.width(36.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Joule",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp,
                        )
                    }

                    // Headline and tagline
                    Column {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Smart Solar Microgrid",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF67E8F9), // cyan-300
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Power, exchanged precisely.",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Coordinated battery reservations & clean microgrid dispatch.",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1), // slate-300
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            // Form container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Sign in to your account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StripeInk,
                    modifier = Modifier.align(Alignment.Start),
                )
                Text(
                    text = "Select your role to access the microgrid network.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StripeBody,
                    modifier = Modifier.align(Alignment.Start),
                )

                Spacer(modifier = Modifier.height(18.dp))

                RoleToggle(
                    selected = role,
                    onSelect = {
                        role = it
                        identifier = ""
                        errorMessage = null
                    },
                )

                Spacer(modifier = Modifier.height(18.dp))

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

                Spacer(modifier = Modifier.height(22.dp))

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
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Grid operator accounts are provisioned by Backoffice administrators.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StripeBody,
                        fontSize = 11.sp,
                    )
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
            unfocusedContainerColor = StripeSurface,
            focusedContainerColor = StripeSurface,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
