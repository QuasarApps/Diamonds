package com.example.diamonds.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.example.diamonds.ui.booking.BookingConfirmationScreen
import com.example.diamonds.ui.booking.BookingDetailScreen
import com.example.diamonds.ui.booking.BookingFormScreen
import com.example.diamonds.ui.booking.BookingsListScreen
import com.example.diamonds.ui.booking.ProviderSearchScreen
import com.example.diamonds.ui.booking.ServiceListScreen
import com.example.diamonds.ui.cleaner.CleanerBookingRequestsScreen
import com.example.diamonds.ui.cleaner.CleanerScheduleScreen
import com.example.diamonds.ui.navigation.Screen
import com.example.diamonds.ui.navigation.startTabForRole
import com.example.diamonds.ui.navigation.tabsForRole
import com.example.diamonds.ui.placeholder.PlaceholderScreen

/** Routes where the bottom bar should be hidden (deep booking flow). */
private val routesWithoutBottomBar = setOf(
    Screen.ProviderSearch.route,
    Screen.ServiceList().route,
    Screen.BookingForm().route,
    Screen.BookingConfirmation().route,
    Screen.BookingDetail().route
)

/** Map route prefix → top-bar title. */
private fun titleForRoute(route: String?, role: UserRole): String = when {
    route == null -> if (role == UserRole.CLEANER) "Diamonds Pro" else "Diamonds"
    route.startsWith("customer/search")  -> "Find a Cleaner"
    route.startsWith("customer/services")-> "Services"
    route.startsWith("customer/book")    -> "Book Service"
    route.startsWith("customer/confirm") -> "Booking Confirmed"
    route.startsWith("customer/booking") -> "Booking Details"
    route.startsWith("cleaner/requests") -> "Booking Requests"
    route.startsWith("cleaner/schedule") -> "My Schedule"
    route.startsWith("cleaner/earnings") -> "Earnings"
    route.startsWith("cleaner/profile")  -> "My Profile"
    role == UserRole.CLEANER             -> "Diamonds Pro"
    else                                 -> "Diamonds"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    onLogout: () -> Unit,
    viewModel: AppShellViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val role = session?.role ?: return

    val tabs = tabsForRole(role)
    val startTab = startTabForRole(role)
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val displayName = session?.displayName
        ?: session?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        ?: ""

    val isTabRoot = routesWithoutBottomBar.none { currentRoute?.startsWith(it.substringBefore("{")) == true }
    val title = titleForRoute(currentRoute, role)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (!isTabRoot) {
                        IconButton(onClick = { innerNavController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isTabRoot) {
                        TextButton(onClick = { viewModel.logout(onLogout) }) { Text("Sign Out") }
                    }
                }
            )
        },
        bottomBar = {
            if (isTabRoot) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.screen.route,
                            onClick = {
                                innerNavController.navigate(tab.screen.route) {
                                    popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon  = { Text(tab.icon, fontSize = 20.sp) },
                            label = { Text(tab.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = innerNavController,
            startDestination = startTab.route,
            modifier = Modifier.padding(padding)
        ) {
            // ── Customer tab roots ─────────────────────────────────────────
            composable(Screen.CustomerHome.route) {
                CustomerHomeTab(
                    displayName = displayName,
                    onStartBooking = { innerNavController.navigate(Screen.ProviderSearch.route) }
                )
            }
            composable(Screen.CustomerBookings.route) {
                BookingsListScreen(
                    onBookingSelected = { id -> innerNavController.navigate(Screen.BookingDetail().route(id)) },
                    onStartBooking    = { innerNavController.navigate(Screen.ProviderSearch.route) }
                )
            }
            composable(Screen.CustomerProfile.route) {
                PlaceholderScreen(label = "Profile\n\nManage your account settings.")
            }

            // ── Customer booking flow ──────────────────────────────────────
            composable(Screen.ProviderSearch.route) {
                ProviderSearchScreen(
                    onProviderSelected = { pid -> innerNavController.navigate(Screen.ServiceList().route(pid)) }
                )
            }
            composable(Screen.ServiceList().route) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: return@composable
                ServiceListScreen(
                    providerId = providerId,
                    onServiceSelected = { sid, pid -> innerNavController.navigate(Screen.BookingForm().route(pid, sid)) }
                )
            }
            composable(Screen.BookingForm().route) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: return@composable
                val serviceId  = backStackEntry.arguments?.getString("serviceId")  ?: return@composable
                BookingFormScreen(
                    providerId = providerId,
                    serviceId  = serviceId,
                    onBookingCreated = { id ->
                        innerNavController.navigate(Screen.BookingConfirmation().route(id)) {
                            popUpTo(Screen.ProviderSearch.route) { inclusive = false }
                        }
                    }
                )
            }
            composable(Screen.BookingConfirmation().route) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: return@composable
                BookingConfirmationScreen(
                    bookingId    = bookingId,
                    onViewBookings = {
                        innerNavController.navigate(Screen.CustomerBookings.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) { inclusive = false }
                        }
                    },
                    onBookAnother = {
                        innerNavController.navigate(Screen.ProviderSearch.route) {
                            popUpTo(Screen.CustomerBookings.route) { inclusive = false }
                        }
                    }
                )
            }
            composable(Screen.BookingDetail().route) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: return@composable
                BookingDetailScreen(
                    bookingId   = bookingId,
                    onCancelled = { innerNavController.popBackStack() }
                )
            }

            // ── Cleaner tab roots ──────────────────────────────────────────
            composable(Screen.CleanerDashboard.route) {
                CleanerDashboardTab(displayName = displayName)
            }
            composable(Screen.CleanerRequests.route) {
                CleanerBookingRequestsScreen()
            }
            composable(Screen.CleanerSchedule.route) {
                CleanerScheduleScreen()
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
