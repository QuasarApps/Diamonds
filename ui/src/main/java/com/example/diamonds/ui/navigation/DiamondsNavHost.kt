package com.example.diamonds.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.diamonds.ui.placeholder.PlaceholderScreen
import com.example.diamonds.ui.splash.SplashScreen

/**
 * Root navigation graph for the Diamonds app.
 *
 * Starts on the [Screen.Splash] destination which decides whether to route
 * the user to [Screen.Login] or their home screen depending on auth state.
 *
 * Replace each [PlaceholderScreen] call with the real composable as screens
 * are built in later phases.
 */
@Composable
fun DiamondsNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateTo = { destination ->
                    navController.navigate(destination.route) {
                        // Remove splash from the back stack so back-press doesn't return to it
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            // TODO Phase 2: replace with real LoginScreen
            PlaceholderScreen(label = "Login – coming in Phase 2")
        }

        composable(Screen.Signup.route) {
            // TODO Phase 2: replace with real SignupScreen
            PlaceholderScreen(label = "Sign Up – coming in Phase 2")
        }

        composable(Screen.RoleSelection.route) {
            // TODO Phase 2: replace with real RoleSelectionScreen
            PlaceholderScreen(label = "Role Selection – coming in Phase 2")
        }

        composable(Screen.ClientHome.route) {
            // TODO Phase 3: replace with real ClientHomeScreen
            PlaceholderScreen(label = "Client Home – coming in Phase 3")
        }

        composable(Screen.ProviderHome.route) {
            // TODO Phase 4: replace with real ProviderHomeScreen
            PlaceholderScreen(label = "Provider Home – coming in Phase 4")
        }
    }
}
