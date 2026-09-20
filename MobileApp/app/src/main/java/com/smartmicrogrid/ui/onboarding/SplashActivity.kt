// ============================================================
// File: SplashActivity.kt
// Purpose: App launcher activity. Shows a brief branded splash, then
//          routes to OnboardingActivity on first run only (per
//          OnboardingPreferences) or straight to MainActivity on
//          every later launch. Pure UI routing — no business logic.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smartmicrogrid.MainActivity
import com.smartmicrogrid.ui.theme.SmartMicrogridTheme
import kotlinx.coroutines.delay

private const val SPLASH_DELAY_MS = 900L

class SplashActivity : ComponentActivity() {

    // Shows the splash content, waits briefly, then routes to onboarding or straight to the app.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartMicrogridTheme {
                SplashScreenContent(onTimeout = ::navigateNext)
            }
        }
    }

    // Decides the next screen based on whether onboarding has already been shown, then finishes
    // this activity so it never sits in the back stack.
    private fun navigateNext() {
        val destination = if (OnboardingPreferences.hasSeenOnboarding(this)) {
            MainActivity::class.java
        } else {
            OnboardingActivity::class.java
        }
        startActivity(Intent(this, destination))
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Smart Microgrid",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
