package com.example.diamonds.ui.navigation

import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession

/**
 * Describes a single tab in the bottom navigation bar.
 *
 * Three distinct tab sets exist:
 *  - [customerTabs]  – what a Customer sees
 *  - [cleanerTabs]   – what an Independent or Employed cleaner sees
 *  - [companyTabs]   – what a Company account sees
 */
data class BottomTab(
    val screen: Screen,
    val label: String,
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

val companyTabs = listOf(
    BottomTab(Screen.CompanyDashboard, "Overview",  "🏢"),
    BottomTab(Screen.CompanyBookings,  "Bookings",  "📋"),
    BottomTab(Screen.CompanyTeam,      "Team",      "👥"),
    BottomTab(Screen.CompanyEarnings,  "Earnings",  "💰"),
    BottomTab(Screen.CompanyProfile,   "Profile",   "👤"),
)

fun tabsForSession(session: UserSession): List<BottomTab> = when {
    session.role == UserRole.CUSTOMER          -> customerTabs
    session.cleanerType == CleanerType.COMPANY -> companyTabs
    session.cleanerType == CleanerType.EMPLOYED-> cleanerTabs
    else                                       -> cleanerTabs   // INDEPENDENT
}

// Legacy overload kept for callers that only have a role (no full session).
fun tabsForRole(role: UserRole): List<BottomTab> = when (role) {
    UserRole.CUSTOMER -> customerTabs
    UserRole.CLEANER  -> cleanerTabs
}

fun startTabForSession(session: UserSession): Screen = when {
    session.role == UserRole.CUSTOMER          -> Screen.CustomerHome
    session.cleanerType == CleanerType.COMPANY -> Screen.CompanyDashboard
    else                                       -> Screen.CleanerDashboard
}

fun startTabForRole(role: UserRole): Screen = when (role) {
    UserRole.CUSTOMER -> Screen.CustomerHome
    UserRole.CLEANER  -> Screen.CleanerDashboard
}
