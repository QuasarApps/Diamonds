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
import com.example.diamonds.domain.repository.UserSession
import com.example.diamonds.ui.booking.BookingConfirmationScreen
import com.example.diamonds.ui.booking.BookingDetailScreen
import com.example.diamonds.ui.booking.BookingFormScreen
import com.example.diamonds.ui.booking.BookingsListScreen
import com.example.diamonds.ui.booking.ProviderRatingsScreen
import com.example.diamonds.ui.booking.ProviderSearchScreen
import com.example.diamonds.ui.booking.ReviewScreen
import com.example.diamonds.ui.booking.ServiceListScreen
import com.example.diamonds.ui.cleaner.CleanerBookingRequestsScreen
import com.example.diamonds.ui.cleaner.CleanerEarningsScreen
import com.example.diamonds.ui.cleaner.CleanerProfileScreen
import com.example.diamonds.ui.cleaner.CleanerScheduleScreen
import com.example.diamonds.ui.cleaner.ServiceManagementScreen
import com.example.diamonds.ui.company.CompanyBookingsScreen
import com.example.diamonds.ui.company.CompanyDashboardScreen
import com.example.diamonds.ui.company.CompanyEarningsScreen
import com.example.diamonds.ui.company.CompanyTeamScreen
import com.example.diamonds.ui.customer.CustomerProfileScreen
import com.example.diamonds.ui.payment.PaymentHistoryScreen
import com.example.diamonds.ui.payment.PaymentScreen
import com.example.diamonds.ui.payment.PaymentSuccessScreen
import com.example.diamonds.ui.navigation.Screen
import com.example.diamonds.ui.navigation.startTabForSession
import com.example.diamonds.ui.navigation.tabsForSession

/** Routes where the bottom bar should be hidden (deep booking flow). */
private val routesWithoutBottomBar = setOf(
    Screen.ProviderSearch.route,
    Screen.ServiceList().route,
    Screen.BookingForm().route,
    Screen.BookingConfirmation().route,
    Screen.BookingDetail().route,
    Screen.ReviewBooking().route,
    Screen.ProviderRatings().route,
    Screen.Payment().route,
    Screen.PaymentSuccess().route,
    Screen.PaymentHistory.route,
    Screen.CleanerServiceManage.route,
    Screen.CompanyServiceManage.route
)

/** Derive the top-bar title from the current route and session context. */
private fun titleForRoute(route: String?, session: UserSession): String {
    val appName = if (session.role == UserRole.CLEANER) "Diamonds Pro" else "Diamonds"
    return when {
        route == null -> appName
        route.startsWith("customer/search")   -> "Find a Cleaner"
        route.startsWith("customer/services") -> "Services"
        route.startsWith("customer/book")     -> "Book Service"
        route.startsWith("customer/confirm")  -> "Booking Confirmed"
        route.startsWith("customer/booking")  -> "Booking Details"
        route.startsWith("customer/review")   -> "Leave a Review"
        route.startsWith("customer/ratings")  -> "Ratings & Reviews"
        route.startsWith("customer/pay/success") -> "Payment Successful"
        route.startsWith("customer/pay")      -> "Secure Payment"
        route.startsWith("customer/payments") -> "Payment History"
        route.startsWith("customer/profile")  -> "My Profile"
        route.startsWith("cleaner/requests")  -> "Booking Requests"
        route.startsWith("cleaner/schedule")  -> "My Schedule"
        route.startsWith("cleaner/earnings")  -> "Earnings"
        route.startsWith("cleaner/profile")   -> "My Profile"
        route.startsWith("cleaner/services")  -> "Manage Services"
        route.startsWith("company/dashboard") -> "Overview"
        route.startsWith("company/bookings")  -> "All Bookings"
        route.startsWith("company/team")      -> "My Team"
        route.startsWith("company/earnings")  -> "Revenue"
        route.startsWith("company/profile")   -> "Company Profile"
        route.startsWith("company/services")  -> "Manage Services"
        else -> appName
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    onLogout: () -> Unit,
    viewModel: AppShellViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val s = session ?: return

    val tabs        = tabsForSession(s)
    val startTab    = startTabForSession(s)
    val innerNav    = rememberNavController()
    val backEntry   by innerNav.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    val displayName = s.displayName
        ?: s.email.substringBefore("@").replaceFirstChar { it.uppercase() }

    val isTabRoot = routesWithoutBottomBar.none {
        currentRoute?.startsWith(it.substringBefore("{")) == true
    }
    val title = titleForRoute(currentRoute, s)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (!isTabRoot) {
                        IconButton(onClick = { innerNav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                            onClick  = {
                                innerNav.navigate(tab.screen.route) {
                                    popUpTo(innerNav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
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
            navController    = innerNav,
            startDestination = startTab.route,
            modifier         = Modifier.padding(padding)
        ) {
            // ── Customer screens ───────────────────────────────────────────
            composable(Screen.CustomerHome.route) {
                CustomerHomeTab(
                    displayName    = displayName,
                    onStartBooking = { innerNav.navigate(Screen.ProviderSearch.route) }
                )
            }
            composable(Screen.CustomerBookings.route) {
                BookingsListScreen(
                    onBookingSelected = { id -> innerNav.navigate(Screen.BookingDetail().route(id)) },
                    onStartBooking    = { innerNav.navigate(Screen.ProviderSearch.route) }
                )
            }
            composable(Screen.CustomerProfile.route) {
                CustomerProfileScreen(
                    session          = s,
                    onPaymentHistory = { innerNav.navigate(Screen.PaymentHistory.route) },
                    onMyBookings     = {
                        innerNav.navigate(Screen.CustomerBookings.route) {
                            popUpTo(innerNav.graph.findStartDestination().id) { inclusive = false }
                        }
                    },
                    onSignOut = { viewModel.logout(onLogout) }
                )
            }

            // ── Customer booking flow ──────────────────────────────────────
            composable(Screen.ProviderSearch.route) {
                ProviderSearchScreen(
                    onProviderSelected = { pid -> innerNav.navigate(Screen.ServiceList().route(pid)) },
                    onViewRatings      = { pid -> innerNav.navigate(Screen.ProviderRatings().route(pid)) }
                )
            }
            composable(Screen.ServiceList().route) { entry ->
                val pid = entry.arguments?.getString("providerId") ?: return@composable
                ServiceListScreen(
                    providerId        = pid,
                    onServiceSelected = { sid, p -> innerNav.navigate(Screen.BookingForm().route(p, sid)) }
                )
            }
            composable(Screen.BookingForm().route) { entry ->
                val pid = entry.arguments?.getString("providerId") ?: return@composable
                val sid = entry.arguments?.getString("serviceId")  ?: return@composable
                BookingFormScreen(
                    providerId       = pid,
                    serviceId        = sid,
                    onBookingCreated = { id ->
                        innerNav.navigate(Screen.BookingConfirmation().route(id)) {
                            popUpTo(Screen.ProviderSearch.route) { inclusive = false }
                        }
                    }
                )
            }
            composable(Screen.BookingConfirmation().route) { entry ->
                val bid = entry.arguments?.getString("bookingId") ?: return@composable
                BookingConfirmationScreen(
                    bookingId      = bid,
                    onPayNow       = { id -> innerNav.navigate(Screen.Payment().route(id)) },
                    onViewBookings = {
                        innerNav.navigate(Screen.CustomerBookings.route) {
                            popUpTo(innerNav.graph.findStartDestination().id) { inclusive = false }
                        }
                    },
                    onBookAnother  = {
                        innerNav.navigate(Screen.ProviderSearch.route) {
                            popUpTo(Screen.CustomerBookings.route) { inclusive = false }
                        }
                    }
                )
            }
            composable(Screen.BookingDetail().route) { entry ->
                val bid = entry.arguments?.getString("bookingId") ?: return@composable
                BookingDetailScreen(
                    bookingId      = bid,
                    onCancelled    = { innerNav.popBackStack() },
                    onLeaveReview  = { bookingId, providerId ->
                        innerNav.navigate(Screen.ReviewBooking().route(bookingId, providerId))
                    }
                )
            }
            composable(Screen.ReviewBooking().route) { entry ->
                val bid = entry.arguments?.getString("bookingId")  ?: return@composable
                val pid = entry.arguments?.getString("providerId") ?: return@composable
                ReviewScreen(
                    bookingId         = bid,
                    providerId        = pid,
                    onReviewSubmitted = { innerNav.popBackStack() }
                )
            }
            composable(Screen.ProviderRatings().route) { entry ->
                val pid = entry.arguments?.getString("providerId") ?: return@composable
                ProviderRatingsScreen(providerId = pid)
            }
            composable(Screen.Payment().route) { entry ->
                val bid = entry.arguments?.getString("bookingId") ?: return@composable
                PaymentScreen(
                    bookingId        = bid,
                    onPaymentSuccess = { payId ->
                        innerNav.navigate(Screen.PaymentSuccess().route(payId)) {
                            popUpTo(Screen.Payment().route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        innerNav.navigate(Screen.CustomerBookings.route) {
                            popUpTo(innerNav.graph.findStartDestination().id) { inclusive = false }
                        }
                    }
                )
            }
            composable(Screen.PaymentSuccess().route) { entry ->
                val payId = entry.arguments?.getString("paymentId") ?: return@composable
                PaymentSuccessScreen(
                    paymentId    = payId,
                    onViewBookings = {
                        innerNav.navigate(Screen.CustomerBookings.route) {
                            popUpTo(innerNav.graph.findStartDestination().id) { inclusive = false }
                        }
                    },
                    onDone = {
                        innerNav.navigate(Screen.CustomerHome.route) {
                            popUpTo(innerNav.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.PaymentHistory.route) {
                PaymentHistoryScreen()
            }

            // ── Independent / Employed Cleaner screens ─────────────────────
            composable(Screen.CleanerDashboard.route) {
                CleanerDashboardTab(session = s)
            }
            composable(Screen.CleanerRequests.route) {
                CleanerBookingRequestsScreen()
            }
            composable(Screen.CleanerSchedule.route) {
                CleanerScheduleScreen()
            }
            composable(Screen.CleanerEarnings.route) {
                CleanerEarningsScreen()
            }
            composable(Screen.CleanerProfile.route) {
                CleanerProfileScreen(
                    session          = s,
                    onManageServices = { innerNav.navigate(Screen.CleanerServiceManage.route) }
                )
            }
            composable(Screen.CleanerServiceManage.route) {
                ServiceManagementScreen()
            }

            // ── Company screens ────────────────────────────────────────────
            composable(Screen.CompanyDashboard.route) {
                CompanyDashboardScreen()
            }
            composable(Screen.CompanyBookings.route) {
                CompanyBookingsScreen()
            }
            composable(Screen.CompanyTeam.route) {
                CompanyTeamScreen()
            }
            composable(Screen.CompanyEarnings.route) {
                CompanyEarningsScreen()
            }
            composable(Screen.CompanyProfile.route) {
                CleanerProfileScreen(
                    session          = s,
                    onManageServices = { innerNav.navigate(Screen.CompanyServiceManage.route) }
                )
            }
            composable(Screen.CompanyServiceManage.route) {
                ServiceManagementScreen()
            }
        }
    }
}
