package com.example.diamonds.ui.navigation

/**
 * Sealed class representing every navigable destination in the app.
 * The [route] string is used as the NavHost composable route key.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object RoleSelection : Screen("role_selection")
    data object ClientHome : Screen("client_home")
    data object ProviderHome : Screen("provider_home")
}
