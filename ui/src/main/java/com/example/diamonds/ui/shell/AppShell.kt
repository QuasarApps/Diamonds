package com.example.diamonds.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.diamonds.domain.model.NotificationType
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
import com.example.diamonds.ui.chat.ChatScreen
import com.example.diamonds.ui.chat.ChatViewModel
import com.example.diamonds.ui.chat.ConversationListScreen
import com.example.diamonds.ui.cleaner.CleanerBookingRequestsScreen
import com.example.diamonds.ui.cleaner.CleanerEarningsScreen
import com.example.diamonds.ui.cleaner.CleanerProfileScreen
import com.example.diamonds.ui.cleaner.CleanerScheduleScreen
import com.example.diamonds.ui.cleaner.ServiceEditScreen
import com.example.diamonds.ui.cleaner.ServiceManagementScreen
import com.example.diamonds.ui.company.CompanyBookingsScreen
import com.example.diamonds.ui.company.CompanyDashboardScreen
import com.example.diamonds.ui.company.CompanyEarningsScreen
import com.example.diamonds.ui.company.CompanyTeamScreen
import com.example.diamonds.ui.components.ConfirmationDialog
import com.example.diamonds.ui.components.OfflineBanner
import com.example.diamonds.ui.customer.CustomerProfileScreen
import com.example.diamonds.ui.map.BookingMapScreen
import com.example.diamonds.ui.map.ProviderTrackingScreen
import com.example.diamonds.ui.navigation.Screen
import com.example.diamonds.ui.navigation.startTabForSession
import com.example.diamonds.ui.navigation.tabsForSession
import com.example.diamonds.ui.notification.NotificationPreferencesScreen
import com.example.diamonds.ui.notification.NotificationScreen
import com.example.diamonds.ui.notification.NotificationViewModel
import com.example.diamonds.ui.payment.PaymentHistoryScreen
import com.example.diamonds.ui.payment.PaymentScreen
import com.example.diamonds.ui.payment.PaymentSuccessScreen
import com.example.diamonds.ui.subscription.RecurringBookingSetupScreen
import com.example.diamonds.ui.subscription.SubscriptionManagementScreen
import com.example.diamonds.ui.sync.SyncStatusScreen

/** Deep-link URI scheme used for in-app links and push notifications. */
private const val DEEP_LINK_SCHEME = "diamonds"

/**
 * Nested-graph route prefixes — the outer NavHost navigates between these
 * "tab graph" routes; each graph owns its own back stack independently.
 */
private object TabGraph {
    const val CustomerHome = "graph/customer/home"
    const val CustomerBookings = "graph/customer/bookings"
    const val CustomerProfile = "graph/customer/profile"
    const val CleanerDashboard = "graph/cleaner/dashboard"
    const val CleanerRequests = "graph/cleaner/requests"
    const val CleanerSchedule = "graph/cleaner/schedule"
    const val CleanerEarnings = "graph/cleaner/earnings"
    const val CleanerProfile = "graph/cleaner/profile"
    const val CompanyDashboard = "graph/company/dashboard"
    const val CompanyBookings = "graph/company/bookings"
    const val CompanyTeam = "graph/company/team"
    const val CompanyEarnings = "graph/company/earnings"
    const val CompanyProfile = "graph/company/profile"
    const val Notifications = "graph/notifications"
    const val Chat = "graph/chat"
}

/** Map each bottom-tab [Screen] to its nested graph route. */
private fun graphRouteForTab(tabRoute: String): String = when (tabRoute) {
    Screen.CustomerHome.route -> TabGraph.CustomerHome
    Screen.CustomerBookings.route -> TabGraph.CustomerBookings
    Screen.CustomerProfile.route -> TabGraph.CustomerProfile
    Screen.CleanerDashboard.route -> TabGraph.CleanerDashboard
    Screen.CleanerRequests.route -> TabGraph.CleanerRequests
    Screen.CleanerSchedule.route -> TabGraph.CleanerSchedule
    Screen.CleanerEarnings.route -> TabGraph.CleanerEarnings
    Screen.CleanerProfile.route -> TabGraph.CleanerProfile
    Screen.CompanyDashboard.route -> TabGraph.CompanyDashboard
    Screen.CompanyBookings.route -> TabGraph.CompanyBookings
    Screen.CompanyTeam.route -> TabGraph.CompanyTeam
    Screen.CompanyEarnings.route -> TabGraph.CompanyEarnings
    Screen.CompanyProfile.route -> TabGraph.CompanyProfile
    Screen.ConversationList.route -> TabGraph.Chat
    else -> tabRoute
}

/**
 * Returns true when the current destination is the root screen of its tab
 * graph (i.e. the user has not navigated deeper within the tab).
 * Also returns true when in a top-level graph (the start destination of
 * the NavHost's nav graph, i.e. one of the tab graph routes).
 */
private fun NavController.isAtTabRoot(): Boolean {
    val entry = currentBackStackEntry ?: return true
    val dest = entry.destination
    val parent = dest.parent ?: return true
    // At root if current destination is the start of its parent graph
    return dest.id == parent.startDestinationId
}

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
        route.startsWith("cleaner/services/edit") -> "Edit Service"
        route.startsWith("cleaner/services")  -> "Manage Services"
        route.startsWith("company/dashboard") -> "Overview"
        route.startsWith("company/bookings")  -> "All Bookings"
        route.startsWith("company/team")      -> "My Team"
        route.startsWith("company/earnings")  -> "Revenue"
        route.startsWith("company/profile")   -> "Company Profile"
        route.startsWith("company/services/edit") -> "Edit Service"
        route.startsWith("company/services")  -> "Manage Services"
        route.startsWith("notifications/preferences") -> "Notification Settings"
        route.startsWith("notifications") -> "Notifications"
        route.startsWith("sync_status") -> "Sync Status"
        route.startsWith("customer/map") -> "Pick Location"
        route.startsWith("customer/tracking") -> "Track Cleaner"
        route.startsWith("chat/conversations") -> "Messages"
        route.startsWith("chat/") -> "Chat"
        else -> appName
    }
}

/** Shared transition duration for navigation animations (ms). */
private const val NAV_ANIM_DURATION = 300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    onLogout: () -> Unit,
    viewModel: AppShellViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val unreadMessageCount by chatViewModel.unreadMessageCount.collectAsState()
    val s = session ?: return

    val tabs = tabsForSession(s)
    val startTab = startTabForSession(s)

    // Single NavController for the whole shell; tab graphs are nested inside.
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    val displayName = s.displayName
        ?: s.email.substringBefore("@").replaceFirstChar { it.uppercase() }

    // Determine which tab graph is currently active by walking up the hierarchy.
    // The active graph route is the parent graph of the current destination.
    val activeGraphRoute = backEntry?.destination?.parent?.route

    // The tab-bar tab that should appear "selected".
    val selectedTabRoute =
        tabs.firstOrNull { graphRouteForTab(it.screen.route) == activeGraphRoute }?.screen?.route
            ?: tabs.firstOrNull { it.screen.route == currentRoute }?.screen?.route
            ?: startTab.route

    // Show back arrow when we are NOT at the root of the active tab graph.
    val atTabRoot = navController.isAtTabRoot()

    // Whether the current tab is the default start tab.
    val isOnStartTab = selectedTabRoute == startTab.route

    val title = titleForRoute(currentRoute, s)

    // ── Back press handling ─────────────────────────────────────────────────
    // When at the root of any tab: if it's NOT the start tab, navigate to the
    // start tab instead of crossing to a previous tab on the back stack.
    // If already on the start tab at root, let the system handle the back
    // press (which will exit the app).
    if (atTabRoot && !isOnStartTab) {
        BackHandler {
            val startGraphRoute = graphRouteForTab(startTab.route)
            navController.navigate(startGraphRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // ── Sign-out confirmation dialog ────────────────────────────────────────
    var showSignOutDialog by remember { mutableStateOf(false) }
    if (showSignOutDialog) {
        ConfirmationDialog(
            title        = "Sign Out?",
            message      = "You'll need to sign in again to access your account.",
            confirmLabel = "Sign Out",
            dismissLabel = "Cancel",
            isDestructive = true,
            onConfirm    = { showSignOutDialog = false; viewModel.logout(onLogout) },
            onDismiss    = { showSignOutDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (!atTabRoot) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    if (atTabRoot) {
                        // Sync status indicator (tappable when ops are pending)
                        if (pendingSyncCount > 0) {
                            TextButton(onClick = {
                                navController.navigate(Screen.SyncStatus.route) {
                                    launchSingleTop = true
                                }
                            }) {
                                Text("⏳ $pendingSyncCount")
                            }
                        }
                        // Chat icon with unread message badge
                        IconButton(onClick = {
                            navController.navigate(TabGraph.Chat) {
                                launchSingleTop = true
                            }
                        }) {
                            BadgedBox(
                                badge = {
                                    if (unreadMessageCount > 0) {
                                        Badge { Text(if (unreadMessageCount > 99) "99+" else "$unreadMessageCount") }
                                    }
                                }
                            ) {
                                Text("💬", fontSize = 20.sp)
                            }
                        }
                        // Notification bell with unread badge
                        IconButton(onClick = {
                            navController.navigate(TabGraph.Notifications) {
                                launchSingleTop = true
                            }
                        }) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text(if (unreadCount > 99) "99+" else "$unreadCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
                        }
                        TextButton(onClick = { showSignOutDialog = true }) { Text("Sign Out") }
                    }
                }
            )
        },
        bottomBar = {
            // Always show the bottom bar (it is the tab root concept now);
            // individual deep screens are inside the tab graph, not outside it.
            NavigationBar {
                tabs.forEach { tab ->
                    val isSelected = tab.screen.route == selectedTabRoute
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            val graphRoute = graphRouteForTab(tab.screen.route)
                            if (isSelected) {
                                // Already on this tab — pop to the tab root if deep
                                navController.popBackStack(
                                    destinationId = navController.currentBackStackEntry
                                        ?.destination?.parent?.startDestinationId
                                        ?: return@NavigationBarItem,
                                    inclusive = false
                                )
                            } else {
                                navController.navigate(graphRoute) {
                                    // Pop up to (but not including) the NavHost's start destination
                                    // so we don't accumulate tab graphs on the back stack.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Text(tab.icon, fontSize = 20.sp) },
                        label = { Text(tab.label, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
        ) {
            // ── Offline banner ──────────────────────────────────────────────
            if (!isOnline) {
                OfflineBanner()
            }

            NavHost(
                navController = navController,
                startDestination = graphRouteForTab(startTab.route),
                // ── Default navigation transitions ─────────────────────────
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        tween(NAV_ANIM_DURATION)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        tween(NAV_ANIM_DURATION)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        tween(NAV_ANIM_DURATION)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        tween(NAV_ANIM_DURATION)
                    )
                },
                modifier = Modifier.weight(1f)
            ) {

                // ════════════════════════════════════════════════════════════
                // CUSTOMER TAB GRAPHS
                // ════════════════════════════════════════════════════════════

                // ── Home tab ─────────────────────────────────────────────
                navigation(
                    route = TabGraph.CustomerHome,
                    startDestination = Screen.CustomerHome.route
                ) {
                    composable(
                        Screen.CustomerHome.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CustomerHomeTab(
                            displayName = displayName,
                            onStartBooking = { navController.navigate(Screen.ProviderSearch.route) }
                        )
                    }
                    composable(Screen.ProviderSearch.route) {
                        ProviderSearchScreen(
                            onProviderSelected = { pid ->
                                navController.navigate(
                                    Screen.ServiceList().route(pid)
                                )
                            },
                            onViewRatings = { pid ->
                                navController.navigate(
                                    Screen.ProviderRatings().route(pid)
                                )
                            }
                        )
                    }
                    composable(
                        route = Screen.ServiceList().route,
                        arguments = listOf(navArgument("providerId") { type = NavType.StringType })
                    ) { entry ->
                        val pid = entry.arguments?.getString("providerId") ?: return@composable
                        ServiceListScreen(
                            providerId = pid,
                            onServiceSelected = { sid, p ->
                                navController.navigate(
                                    Screen.BookingForm().route(p, sid)
                                )
                            }
                        )
                    }
                    composable(
                        route = Screen.BookingForm().route,
                        arguments = listOf(
                            navArgument("providerId") { type = NavType.StringType },
                            navArgument("serviceId") { type = NavType.StringType }
                        )
                    ) { entry ->
                        val pid = entry.arguments?.getString("providerId") ?: return@composable
                        val sid = entry.arguments?.getString("serviceId") ?: return@composable
                        val mapLat = entry.savedStateHandle.get<Double>("map_lat")
                        val mapLng = entry.savedStateHandle.get<Double>("map_lng")
                        val mapAddr = entry.savedStateHandle.get<String>("map_address")
                        BookingFormScreen(
                            providerId = pid,
                            serviceId = sid,
                            onBookingCreated = { id ->
                                navController.navigate(Screen.BookingConfirmation().route(id)) {
                                    popUpTo(Screen.ProviderSearch.route) { inclusive = false }
                                }
                            },
                            onPickOnMap = { navController.navigate(Screen.BookingMap.route) },
                            mapLat = mapLat,
                            mapLng = mapLng,
                            mapAddress = mapAddr
                        )
                    }
                    composable(Screen.BookingMap.route) {
                        BookingMapScreen(
                            onLocationConfirmed = { lat, lng, address ->
                                navController.previousBackStackEntry?.savedStateHandle?.apply {
                                    set("map_lat", lat)
                                    set("map_lng", lng)
                                    set("map_address", address)
                                }
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Screen.BookingConfirmation().route,
                        arguments = listOf(navArgument("bookingId") { type = NavType.StringType }),
                        deepLinks = listOf(navDeepLink {
                            uriPattern = "$DEEP_LINK_SCHEME://customer/confirm/{bookingId}"
                        })
                    ) { entry ->
                        val bid = entry.arguments?.getString("bookingId") ?: return@composable
                        BackHandler {
                            navController.navigate(graphRouteForTab(Screen.CustomerBookings.route)) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        BookingConfirmationScreen(
                            bookingId = bid,
                            onPayNow = { id -> navController.navigate(Screen.Payment().route(id)) },
                            onViewBookings = {
                                navController.navigate(graphRouteForTab(Screen.CustomerBookings.route)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onBookAnother = {
                                navController.navigate(Screen.ProviderSearch.route) {
                                    popUpTo(Screen.CustomerHome.route) { inclusive = false }
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.Payment().route,
                        arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
                    ) { entry ->
                        val bid = entry.arguments?.getString("bookingId") ?: return@composable
                        PaymentScreen(
                            bookingId = bid,
                            onPaymentSuccess = { payId ->
                                navController.navigate(Screen.PaymentSuccess().route(payId)) {
                                    popUpTo(Screen.Payment().route) { inclusive = true }
                                }
                            },
                            onSkip = {
                                navController.navigate(graphRouteForTab(Screen.CustomerBookings.route)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.PaymentSuccess().route,
                        arguments = listOf(navArgument("paymentId") { type = NavType.StringType })
                    ) { entry ->
                        val payId = entry.arguments?.getString("paymentId") ?: return@composable
                        BackHandler {
                            navController.navigate(graphRouteForTab(Screen.CustomerHome.route)) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                            }
                        }
                        PaymentSuccessScreen(
                            paymentId = payId,
                            onViewBookings = {
                                navController.navigate(graphRouteForTab(Screen.CustomerBookings.route)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onDone = {
                                navController.navigate(graphRouteForTab(Screen.CustomerHome.route)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.ProviderRatings().route,
                        arguments = listOf(navArgument("providerId") { type = NavType.StringType })
                    ) { entry ->
                        val pid = entry.arguments?.getString("providerId") ?: return@composable
                        ProviderRatingsScreen(providerId = pid)
                    }
                }

                // ── Bookings tab ──────────────────────────────────────────
                navigation(
                    route = TabGraph.CustomerBookings,
                    startDestination = Screen.CustomerBookings.route
                ) {
                    composable(
                        Screen.CustomerBookings.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        BookingsListScreen(
                            onBookingSelected = { id ->
                                navController.navigate(
                                    Screen.BookingDetail().route(id)
                                )
                            },
                            onStartBooking = {
                                navController.navigate(graphRouteForTab(Screen.CustomerHome.route)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.BookingDetail().route,
                        arguments = listOf(navArgument("bookingId") { type = NavType.StringType }),
                        deepLinks = listOf(navDeepLink {
                            uriPattern = "$DEEP_LINK_SCHEME://customer/booking/{bookingId}"
                        })
                    ) { entry ->
                        val bid = entry.arguments?.getString("bookingId") ?: return@composable
                        BookingDetailScreen(
                            bookingId = bid,
                            onCancelled = { navController.popBackStack() },
                            onLeaveReview = { bookingId, providerId ->
                                navController.navigate(
                                    Screen.ReviewBooking().route(bookingId, providerId)
                                )
                            },
                            onTrackCleaner = { bookingId ->
                                navController.navigate(Screen.ProviderTracking().route(bookingId))
                            },
                            onOpenChat = { params ->
                                chatViewModel.openOrCreateConversation(
                                    bookingId = params.bookingId,
                                    clientId = params.clientId,
                                    clientName = params.clientName,
                                    providerId = params.providerId,
                                    providerName = params.providerName,
                                    onReady = { conversationId ->
                                        navController.navigate(
                                            Screen.Chat().route(conversationId)
                                        ) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        )
                    }
                    composable(
                        route = Screen.ReviewBooking().route,
                        arguments = listOf(
                            navArgument("bookingId") { type = NavType.StringType },
                            navArgument("providerId") { type = NavType.StringType }
                        )
                    ) { entry ->
                        val bid = entry.arguments?.getString("bookingId") ?: return@composable
                        val pid = entry.arguments?.getString("providerId") ?: return@composable
                        ReviewScreen(
                            bookingId = bid,
                            providerId = pid,
                            onReviewSubmitted = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Screen.ProviderTracking().route,
                        arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
                    ) { entry ->
                        val bid = entry.arguments?.getString("bookingId") ?: return@composable
                        ProviderTrackingScreen(bookingId = bid)
                    }
                }

                // ── Profile tab ───────────────────────────────────────────
                navigation(
                    route = TabGraph.CustomerProfile,
                    startDestination = Screen.CustomerProfile.route
                ) {
                    composable(
                        Screen.CustomerProfile.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CustomerProfileScreen(
                            session = s,
                            onPaymentHistory = { navController.navigate(Screen.PaymentHistory.route) },
                            onSubscriptions = { navController.navigate(Screen.SubscriptionManagement.route) },
                            onMyBookings = {
                                navController.navigate(graphRouteForTab(Screen.CustomerBookings.route)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onSignOut = { showSignOutDialog = true }
                        )
                    }
                    composable(Screen.PaymentHistory.route) {
                        PaymentHistoryScreen()
                    }
                    composable(Screen.SubscriptionManagement.route) {
                        SubscriptionManagementScreen()
                    }
                    composable(
                        route = Screen.RecurringBookingSetup().route,
                        arguments = listOf(
                            navArgument("providerId") { type = NavType.StringType },
                            navArgument("serviceId") { type = NavType.StringType }
                        )
                    ) { entry ->
                        val pid = entry.arguments?.getString("providerId") ?: return@composable
                        val sid = entry.arguments?.getString("serviceId") ?: return@composable
                        RecurringBookingSetupScreen(
                            providerId = pid,
                            serviceId = sid,
                            onSuccess = { navController.popBackStack() }
                        )
                    }
                }

                // ════════════════════════════════════════════════════════════
                // CLEANER TAB GRAPHS
                // ════════════════════════════════════════════════════════════

                navigation(
                    route = TabGraph.CleanerDashboard,
                    startDestination = Screen.CleanerDashboard.route
                ) {
                    composable(
                        Screen.CleanerDashboard.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CleanerDashboardTab(
                            session = s,
                            onNavigateToRequests = {
                                navController.navigate(graphRouteForTab(Screen.CleanerRequests.route)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }

                navigation(
                    route = TabGraph.CleanerRequests,
                    startDestination = Screen.CleanerRequests.route
                ) {
                    composable(
                        Screen.CleanerRequests.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CleanerBookingRequestsScreen()
                    }
                }

                navigation(
                    route = TabGraph.CleanerSchedule,
                    startDestination = Screen.CleanerSchedule.route
                ) {
                    composable(
                        Screen.CleanerSchedule.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CleanerScheduleScreen()
                    }
                }

                navigation(
                    route = TabGraph.CleanerEarnings,
                    startDestination = Screen.CleanerEarnings.route
                ) {
                    composable(
                        Screen.CleanerEarnings.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CleanerEarningsScreen()
                    }
                }

                navigation(
                    route = TabGraph.CleanerProfile,
                    startDestination = Screen.CleanerProfile.route
                ) {
                    composable(
                        Screen.CleanerProfile.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CleanerProfileScreen(
                            session = s,
                            onManageServices = { navController.navigate(Screen.CleanerServiceManage.route) }
                        )
                    }
                    composable(Screen.CleanerServiceManage.route) {
                        ServiceManagementScreen(
                            onAddService = {
                                navController.navigate(
                                    Screen.CleanerServiceEdit().route("new")
                                )
                            },
                            onEditService = { id ->
                                navController.navigate(
                                    Screen.CleanerServiceEdit().route(id)
                                )
                            }
                        )
                    }
                    composable(
                        route = Screen.CleanerServiceEdit().route,
                        arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
                    ) { entry ->
                        val sid = entry.arguments?.getString("serviceId") ?: return@composable
                        ServiceEditScreen(
                            serviceId = sid,
                            onDone = { navController.popBackStack() })
                    }
                }

                // ════════════════════════════════════════════════════════════
                // COMPANY TAB GRAPHS
                // ════════════════════════════════════════════════════════════

                navigation(
                    route = TabGraph.CompanyDashboard,
                    startDestination = Screen.CompanyDashboard.route
                ) {
                    composable(
                        Screen.CompanyDashboard.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CompanyDashboardScreen()
                    }
                }

                navigation(
                    route = TabGraph.CompanyBookings,
                    startDestination = Screen.CompanyBookings.route
                ) {
                    composable(
                        Screen.CompanyBookings.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CompanyBookingsScreen()
                    }
                }

                navigation(
                    route = TabGraph.CompanyTeam,
                    startDestination = Screen.CompanyTeam.route
                ) {
                    composable(
                        Screen.CompanyTeam.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CompanyTeamScreen()
                    }
                }

                navigation(
                    route = TabGraph.CompanyEarnings,
                    startDestination = Screen.CompanyEarnings.route
                ) {
                    composable(
                        Screen.CompanyEarnings.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CompanyEarningsScreen()
                    }
                }

                navigation(
                    route = TabGraph.CompanyProfile,
                    startDestination = Screen.CompanyProfile.route
                ) {
                    composable(
                        Screen.CompanyProfile.route,
                        enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
                        popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                        popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
                    ) {
                        CleanerProfileScreen(
                            session = s,
                            onManageServices = { navController.navigate(Screen.CompanyServiceManage.route) }
                        )
                    }
                    composable(Screen.CompanyServiceManage.route) {
                        ServiceManagementScreen(
                            onAddService = {
                                navController.navigate(
                                    Screen.CompanyServiceEdit().route("new")
                                )
                            },
                            onEditService = { id ->
                                navController.navigate(
                                    Screen.CompanyServiceEdit().route(id)
                                )
                            }
                        )
                    }
                    composable(
                        route = Screen.CompanyServiceEdit().route,
                        arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
                    ) { entry ->
                        val sid = entry.arguments?.getString("serviceId") ?: return@composable
                        ServiceEditScreen(
                            serviceId = sid,
                            onDone = { navController.popBackStack() })
                    }
                }

                // ════════════════════════════════════════════════════════════
                // NOTIFICATIONS (shared overlay graph, accessible from all tabs)
                // ════════════════════════════════════════════════════════════

                navigation(
                    route = TabGraph.Notifications,
                    startDestination = Screen.Notifications.route
                ) {
                    composable(Screen.Notifications.route) {
                        NotificationScreen(
                            onNotificationClick = { notification ->
                                val ref = notification.referenceId
                                if (ref != null) {
                                    when (notification.type) {
                                        NotificationType.BOOKING_UPDATE ->
                                            navController.navigate(
                                                Screen.BookingDetail().route(ref)
                                            )

                                        NotificationType.PAYMENT ->
                                            navController.navigate(Screen.PaymentHistory.route)

                                        else -> Unit
                                    }
                                }
                            }
                        )
                    }
                    composable(Screen.NotificationPreferences.route) {
                        NotificationPreferencesScreen()
                    }
                }

                // ════════════════════════════════════════════════════════════
                // CHAT (shared overlay graph, accessible from all tabs via top bar)
                // ════════════════════════════════════════════════════════════

                navigation(
                    route = TabGraph.Chat,
                    startDestination = Screen.ConversationList.route
                ) {
                    composable(Screen.ConversationList.route) {
                        ConversationListScreen(
                            onConversationSelected = { convId ->
                                navController.navigate(Screen.Chat().route(convId))
                            }
                        )
                    }
                    composable(
                        route = Screen.Chat().route,
                        arguments = listOf(navArgument("conversationId") {
                            type = NavType.StringType
                        })
                    ) { entry ->
                        val convId =
                            entry.arguments?.getString("conversationId") ?: return@composable
                        ChatScreen(
                            conversationId = convId,
                            currentUserId = s.userId
                        )
                    }
                }

                // ════════════════════════════════════════════════════════════
                // SYNC STATUS (standalone destination, accessible from top bar)
                // ════════════════════════════════════════════════════════════
                composable(Screen.SyncStatus.route) {
                    SyncStatusScreen()
                }
            }
        }
    }
}
