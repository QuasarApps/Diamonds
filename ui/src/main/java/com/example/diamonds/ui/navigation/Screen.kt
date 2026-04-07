package com.example.diamonds.ui.navigation

/**
 * Every navigable destination in the app, organised into three groups:
 *
 *  1. **Top-level** – Splash, Auth flow
 *  2. **Customer tabs** – the bottom-nav destinations a *customer* sees
 *  3. **Cleaner tabs**  – the bottom-nav destinations a *cleaner* sees
 *
 * Both customer and cleaner enter through the same [Splash] → [Login] flow;
 * after auth the [AppShell] reads the session role and renders the correct
 * set of tabs.
 */
sealed class Screen(val route: String) {

    // ── Top-level (role-agnostic) ──────────────────────────────────────────
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Signup : Screen("signup")

    /** Single post-auth entry point – role drives the content inside. */
    data object AppShell : Screen("app_shell")

    // ── Customer tab destinations ──────────────────────────────────────────
    data object CustomerHome : Screen("customer/home")
    data object CustomerBookings : Screen("customer/bookings")
    data object CustomerProfile : Screen("customer/profile")

    // ── Cleaner tab destinations ───────────────────────────────────────────
    data object CleanerDashboard : Screen("cleaner/dashboard")
    data object CleanerSchedule : Screen("cleaner/schedule")
    data object CleanerEarnings : Screen("cleaner/earnings")
    data object CleanerProfile : Screen("cleaner/profile")
}
