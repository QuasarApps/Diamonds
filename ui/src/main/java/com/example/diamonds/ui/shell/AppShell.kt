package com.example.diamonds.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.ui.navigation.Screen
import com.example.diamonds.ui.navigation.startTabForRole
import com.example.diamonds.ui.navigation.tabsForRole
import com.example.diamonds.ui.placeholder.PlaceholderScreen

/**
 * The single post-authentication entry point.
 *
 * [AppShell] reads the current [UserRole] from [AppShellViewModel] and
 * renders a completely different set of bottom-nav tabs, screens, and
 * top-bar styling for **Customer** vs **Cleaner** mode.
 *
 * Both modes share the same Scaffold, NavHost, and Hilt graph — only the
 * visible routes and content differ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    onLogout: () -> Unit,
    viewModel: AppShellViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val role = session?.role ?: return // wait until session loads

    val tabs = tabsForRole(role)
    val startTab = startTabForRole(role)
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val displayName = session?.displayName
        ?: session?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (role) {
                            UserRole.CUSTOMER -> "Diamonds"
                            UserRole.CLEANER  -> "Diamonds Pro"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    TextButton(onClick = { viewModel.logout(onLogout) }) {
                        Text("Sign Out")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.screen.route,
                        onClick = {
                            innerNavController.navigate(tab.screen.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(tab.icon, fontSize = 20.sp) },
                        label = { Text(tab.label, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = innerNavController,
            startDestination = startTab.route,
            modifier = Modifier.padding(padding)
        ) {
            // ── Customer screens ───────────────────────────────────────────
            composable(Screen.CustomerHome.route) {
                CustomerHomeTab(displayName = displayName)
            }
            composable(Screen.CustomerBookings.route) {
                PlaceholderScreen(label = "My Bookings\n\nBrowse and manage your bookings here.")
            }
            composable(Screen.CustomerProfile.route) {
                PlaceholderScreen(label = "Profile\n\nManage your account settings.")
            }

            // ── Cleaner screens ────────────────────────────────────────────
            composable(Screen.CleanerDashboard.route) {
                CleanerDashboardTab(displayName = displayName)
            }
            composable(Screen.CleanerSchedule.route) {
                PlaceholderScreen(label = "Schedule\n\nView and manage your upcoming jobs.")
            }
            composable(Screen.CleanerEarnings.route) {
                PlaceholderScreen(label = "Earnings\n\nTrack your income and payouts.")
            }
            composable(Screen.CleanerProfile.route) {
                PlaceholderScreen(label = "Profile\n\nManage your cleaner profile and services.")
            }
        }
    }
}
