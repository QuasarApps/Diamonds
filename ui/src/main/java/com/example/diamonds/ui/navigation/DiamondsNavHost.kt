package com.example.diamonds.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.diamonds.ui.auth.LoginScreen
import com.example.diamonds.ui.auth.SignupScreen
import com.example.diamonds.ui.shell.AppShell
import com.example.diamonds.ui.splash.SplashScreen

/**
 * Root navigation graph.
 *
 * There is one entry point: [Screen.Splash].  After the splash the user
 * is either sent to [Screen.Login] (unauthenticated) or [Screen.AppShell]
 * (authenticated).
 *
 * [Screen.AppShell] is the **single post-auth destination** for both
 * Customer and Cleaner modes.  It reads the session role internally and
 * renders the appropriate bottom-nav tabs and screens.
 */
@Composable
fun DiamondsNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // ── Splash ─────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateTo = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Auth ───────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.AppShell.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.AppShell.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Post-auth: single shell, role decides content ──────────────────
        composable(Screen.AppShell.route) {
            AppShell(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
