// ============================================================
// File: SplashActivity.kt
// Purpose: App launcher activity. Shows a brief Joule-branded splash
//          (gradient mark on navy, wordmark, tagline), then routes:
//          onboarding on first run only (per OnboardingPreferences),
//          straight into HomeActivity if a saved session already
//          exists (Prosumer via ProsumerSessionDao or staff via
//          StaffSessionPreferences — added 2026-09-28 so signing in
//          once doesn't mean signing in on every relaunch), or Sign
//          In (MainActivity) otherwise. Pure UI routing — no business
//          logic.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.MainActivity
import com.smartmicrogrid.data.local.ProsumerSessionDao
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.ui.auth.StaffSessionPreferences
import com.smartmicrogrid.ui.components.JouleMark
import com.smartmicrogrid.ui.home.HomeActivity
import com.smartmicrogrid.ui.home.UserRole
import com.smartmicrogrid.ui.theme.StripeInk
import com.smartmicrogrid.ui.theme.SmartMicrogridTheme
import kotlinx.coroutines.delay

private const val SPLASH_DELAY_MS = 900L

class SplashActivity : ComponentActivity() {

    // Shows the splash content, waits briefly, then routes to onboarding, straight into the app
    // (if a session is already saved), or Sign In.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartMicrogridTheme {
                SplashScreenContent(onTimeout = ::navigateNext)
            }
        }
    }

    // Decides the next screen: onboarding first, then an existing session (skip Sign In
    // entirely), then Sign In. Finishes this activity so it never sits in the back stack.
    private fun navigateNext() {
        if (!OnboardingPreferences.hasSeenOnboarding(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        val prosumerSession = ProsumerSessionDao(this).getSession()
        val staffSession = StaffSessionPreferences.read(this)
        val intent = when {
            prosumerSession != null -> {
                ApiClient.authToken = prosumerSession.token
                HomeActivity.intentFor(this, UserRole.Prosumer)
            }
            staffSession != null -> {
                ApiClient.authToken = staffSession.token
                HomeActivity.intentFor(this, UserRole.fromClaimValue(staffSession.role))
            }
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}

// Renders the branded splash UI and fires onTimeout once after a short delay.
@Composable
private fun SplashScreenContent(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DELAY_MS)
        onTimeout()
    }
    Surface(modifier = Modifier.fillMaxSize(), color = StripeInk) {
        // enableEdgeToEdge() draws this Surface behind the system status/nav bars — safeDrawingPadding()
        // keeps the wordmark clear of them instead of risking it sitting under a status bar cutout.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                JouleMark(modifier = Modifier.width(96.dp))
                Text(
                    text = "Joule",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    text = "Power, exchanged precisely.",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
