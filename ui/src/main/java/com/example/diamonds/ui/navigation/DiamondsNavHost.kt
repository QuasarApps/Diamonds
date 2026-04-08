package com.example.diamonds.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.diamonds.ui.auth.LoginScreen
import com.example.diamonds.ui.auth.SignupScreen
import com.example.diamonds.ui.shell.AppShell
import com.example.diamonds.ui.splash.SplashScreen

/** Transition duration for the root nav graph (ms). */
private const val ROOT_ANIM_DURATION = 400

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
        startDestination = Screen.Splash.route,
        enterTransition  = { fadeIn(tween(ROOT_ANIM_DURATION)) },
        exitTransition   = { fadeOut(tween(ROOT_ANIM_DURATION)) },
        popEnterTransition = { fadeIn(tween(ROOT_ANIM_DURATION)) },
        popExitTransition  = { fadeOut(tween(ROOT_ANIM_DURATION)) }
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
                        // Clear the entire back stack including the shell
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
    }
}
