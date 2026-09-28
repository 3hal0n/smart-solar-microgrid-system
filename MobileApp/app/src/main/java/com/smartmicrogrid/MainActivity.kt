// ============================================================
// File: MainActivity.kt
// Purpose: The app's real logged-out entry point — hosts Sign In
//          (Rukshan's LoginScreen, extended with Grid Operator mode)
//          and Sign Up (Rukshan's RegisterScreen), switched with a
//          simple in-memory state instead of a full nav graph since
//          there are only ever these two destinations here. Replaces
//          the old "Hello Android!" placeholder with its two
//          "temporary" role buttons — see git history for that
//          version if it's ever needed for reference.
// Author: Shalon
// ============================================================
package com.smartmicrogrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.smartmicrogrid.ui.home.HomeActivity
import com.smartmicrogrid.ui.prosumer.LoginScreen
import com.smartmicrogrid.ui.prosumer.RegisterScreen
import com.smartmicrogrid.ui.theme.SmartMicrogridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartMicrogridTheme {
                AuthHost()
            }
        }
    }
}

private enum class AuthScreen { SignIn, SignUp }

@Composable
private fun AuthHost() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(AuthScreen.SignIn) }

    when (screen) {
        AuthScreen.SignIn -> LoginScreen(
            onLoginSuccess = { role ->
                context.startActivity(HomeActivity.intentFor(context, role))
                (context as? ComponentActivity)?.finish()
            },
            onNavigateToRegister = { screen = AuthScreen.SignUp },
        )
        AuthScreen.SignUp -> RegisterScreen(
            onNavigateToLogin = { screen = AuthScreen.SignIn },
        )
    }
}
