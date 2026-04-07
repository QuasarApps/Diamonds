package com.example.diamonds.ui.navigation

import com.example.diamonds.domain.repository.UserRole

/**
 * Describes a single tab in the bottom navigation bar.
 *
 * The visible tabs change depending on [UserRole]:
 *  - [UserRole.CUSTOMER] sees [customerTabs]
 *  - [UserRole.CLEANER]  sees [cleanerTabs]
 */
data class BottomTab(
    val screen: Screen,
    val label: String,
    /** Unicode/emoji placeholder – replace with real icons when vector assets are added. */
    val icon: String
)

val customerTabs = listOf(
    BottomTab(Screen.CustomerHome,     "Home",     "🏠"),
    BottomTab(Screen.CustomerBookings, "Bookings", "📋"),
    BottomTab(Screen.CustomerProfile,  "Profile",  "👤"),
)

val cleanerTabs = listOf(
    BottomTab(Screen.CleanerDashboard, "Dashboard", "📊"),
    BottomTab(Screen.CleanerRequests,  "Requests",  "🔔"),
    BottomTab(Screen.CleanerSchedule,  "Schedule",  "📅"),
    BottomTab(Screen.CleanerEarnings,  "Earnings",  "💰"),
    BottomTab(Screen.CleanerProfile,   "Profile",   "👤"),
)

fun tabsForRole(role: UserRole): List<BottomTab> = when (role) {
    UserRole.CUSTOMER -> customerTabs
    UserRole.CLEANER  -> cleanerTabs
}

fun startTabForRole(role: UserRole): Screen = when (role) {
    UserRole.CUSTOMER -> Screen.CustomerHome
    UserRole.CLEANER  -> Screen.CleanerDashboard
}
