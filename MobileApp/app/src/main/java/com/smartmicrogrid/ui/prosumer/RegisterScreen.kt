// ============================================================
// File: RegisterScreen.kt
// Purpose: Prosumer registration screen styled with signup.png
//          hero banner, layered gradient, and structured input form.
// ============================================================
package com.smartmicrogrid.ui.prosumer

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmicrogrid.R
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
                .verticalScroll(rememberScrollState()),
        ) {
            // Hero Banner styled like web app with signup.png
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                // Background image: signup.png
                Image(
                    painter = painterResource(id = R.drawable.signup),
                    contentDescription = "Prosumer Registration Hero",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Dark gradient overlay
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

                // Back button & Hero branding
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onNavigateToLogin,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = JouleIcons.Back,
                            contentDescription = "Back to sign in",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Column {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "Prosumer Registration",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF67E8F9),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create your account",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "Your National Identity Card (NIC) is your account identifier.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                        )
                    }
                }
            }

            // Form container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AuthTextField(value = nic, onValueChange = { nic = it }, label = "NIC Number *")
                AuthTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name *")
                AuthTextField(value = email, onValueChange = { email = it }, label = "Email Address *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                AuthTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                AuthTextField(value = address, onValueChange = { address = it }, label = "Residential Address", singleLine = false, minLines = 2)
                AuthTextField(value = password, onValueChange = { password = it }, label = "Password *", visualTransformation = PasswordVisualTransformation())

                errorMessage?.let {
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

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        errorMessage = null
                        when {
                            nic.isBlank() -> errorMessage = "NIC is required"
                            fullName.isBlank() -> errorMessage = "Full name is required"
                            email.isBlank() -> errorMessage = "Email address is required"
                            password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                            else -> {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val request = ProsumerRegistrationRequest(
                                            nic = nic.trim(),
                                            fullName = fullName.trim(),
                                            email = email.trim(),
                                            phone = phone.trim().ifBlank { null },
                                            address = address.trim().ifBlank { null },
                                            password = password,
                                        )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = StripeOnPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Create Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }

                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Already have an account? ", color = StripeBody)
                    Text("Sign in", color = StripeAccent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
