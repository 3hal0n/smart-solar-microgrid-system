package com.smartmicrogrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.ui.home.HomeActivity
import com.smartmicrogrid.ui.home.UserRole
import com.smartmicrogrid.ui.theme.SmartMicrogridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartMicrogridTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// TODO(Rukshan/Dinil): this screen is what Splash currently lands on when there's no session
// yet — replace it with the real logged-out landing/login screen. The two buttons below exist
// only so HomeActivity's role-based shell is reachable and testable before a real LoginActivity
// exists; remove them once login lands and routes into HomeActivity itself.
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(text = "Hello $name!")
        Button(onClick = { context.startActivity(HomeActivity.intentFor(context, UserRole.Prosumer)) }) {
            Text("Continue as Prosumer (temporary)")
        }
        Button(onClick = { context.startActivity(HomeActivity.intentFor(context, UserRole.GridOperator)) }) {
            Text("Continue as Grid Operator (temporary)")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartMicrogridTheme {
        Greeting("Android")
    }
}
