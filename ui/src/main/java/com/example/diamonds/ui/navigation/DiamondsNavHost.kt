package com.example.diamonds.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.ui.auth.LoginScreen
import com.example.diamonds.ui.auth.SignupScreen
import com.example.diamonds.ui.home.ClientHomeScreen
import com.example.diamonds.ui.home.ProviderHomeScreen
import com.example.diamonds.ui.placeholder.PlaceholderScreen
import com.example.diamonds.ui.splash.SplashScreen

@Composable
fun DiamondsNavHost() {
    val navController = rememberNavController()

    fun homeRouteFor(role: UserRole) =
        if (role == UserRole.PROVIDER) Screen.ProviderHome.route else Screen.ClientHome.route

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateTo = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    navController.navigate(homeRouteFor(role)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = { role ->
                    navController.navigate(homeRouteFor(role)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.RoleSelection.route) {
            PlaceholderScreen(label = "Role Selection")
        }

        composable(Screen.ClientHome.route) {
            ClientHomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ProviderHome.route) {
            ProviderHomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
