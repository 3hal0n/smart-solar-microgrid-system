// ============================================================
// File: HomeActivity.kt
// Purpose: Role-based Home shell — a single Activity hosting a
//          Jetpack Navigation Compose nav-graph with a bottom nav
//          bar, meant to be launched after login. Destinations are
//          wired by route name to each owner's real screen
//          composable per architecture.md §6: Dashboard is Migara's
//          (ui/dashboard/*), Bookings/Profile are Dinil's
//          (ui/prosumer/*) for the Prosumer role, and Scan QR/Map are
//          Dinil's & _____ (ui/operator/*) for the Grid Operator role.
//          Several of those destinations are still placeholders (see
//          their own TODO(<owner>) files) — this file never needs to
//          change once the real screens land, since it only
//          references them by route + function name.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.dashboard.OperatorDashboardScreen
import com.smartmicrogrid.ui.dashboard.ProsumerDashboardScreen
import com.smartmicrogrid.ui.operator.MapScreen
import com.smartmicrogrid.ui.operator.ScanQrScreen
import com.smartmicrogrid.ui.prosumer.BookingsScreen
import com.smartmicrogrid.ui.prosumer.ProfileScreen
import com.smartmicrogrid.ui.theme.SmartMicrogridTheme

// One bottom-nav tab: its route, label and icon (from the shared JouleIcons set — kept
// dependency-free rather than pulling in Material Icons Extended for a handful of icons).
private data class HomeDestination(val route: String, val label: String, val icon: ImageVector)

private val PROSUMER_DESTINATIONS = listOf(
    HomeDestination(HomeRoutes.PROSUMER_DASHBOARD, "Dashboard", JouleIcons.Grid),
    HomeDestination(HomeRoutes.PROSUMER_BOOKINGS, "Bookings", JouleIcons.Calendar),
    HomeDestination(HomeRoutes.PROSUMER_MAP, "Map", JouleIcons.MapPin),
    HomeDestination(HomeRoutes.PROSUMER_PROFILE, "Profile", JouleIcons.User),
)

private val OPERATOR_DESTINATIONS = listOf(
    HomeDestination(HomeRoutes.OPERATOR_DASHBOARD, "Dashboard", JouleIcons.Grid),
    HomeDestination(HomeRoutes.OPERATOR_SCAN_QR, "Scan QR", JouleIcons.Scan),
    HomeDestination(HomeRoutes.OPERATOR_MAP, "Map", JouleIcons.MapPin),
)

class HomeActivity : ComponentActivity() {

    // Reads the caller-supplied role (see HomeRoutes.EXTRA_ROLE) and renders that role's shell.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val role = UserRole.fromClaimValue(intent.getStringExtra(HomeRoutes.EXTRA_ROLE))
        setContent {
            SmartMicrogridTheme {
                HomeShell(role = role)
            }
        }
    }

    companion object {
        // Builds the Intent a caller (e.g. a future LoginActivity, once the user's role is known
        // from the login response) uses to enter this shell.
        fun intentFor(context: Context, role: UserRole): Intent =
            Intent(context, HomeActivity::class.java).putExtra(HomeRoutes.EXTRA_ROLE, role.claimValue)
    }
}

// Picks the destination set for the given role and renders the shared bottom-nav + NavHost shell.
@Composable
private fun HomeShell(role: UserRole) {
    val destinations = if (role == UserRole.GridOperator) OPERATOR_DESTINATIONS else PROSUMER_DESTINATIONS
    NavGraphShell(destinations = destinations)
}

// Renders a Scaffold with a bottom NavigationBar and a NavHost registering every destination this
// shell knows about. Routes are the single thing connecting this file to each owner's real
// screen, so this graph never needs editing once a placeholder is swapped for the real one.
@Composable
private fun NavGraphShell(destinations: List<HomeDestination>) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                // Standard "switch tabs" navigation options: don't pile up back-stack
                                // entries per tab switch, and restore each tab's own state on return.
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                            label = { Text(destination.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = destinations.first().route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(HomeRoutes.PROSUMER_DASHBOARD) { ProsumerDashboardScreen() }
            composable(HomeRoutes.PROSUMER_BOOKINGS) { BookingsScreen() }
            composable(HomeRoutes.PROSUMER_MAP) { MapScreen() }
            composable(HomeRoutes.PROSUMER_PROFILE) { ProfileScreen() }
            composable(HomeRoutes.OPERATOR_DASHBOARD) { OperatorDashboardScreen() }
            composable(HomeRoutes.OPERATOR_SCAN_QR) { ScanQrScreen() }
            composable(HomeRoutes.OPERATOR_MAP) { MapScreen() }
        }
    }
}
