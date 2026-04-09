package com.example.diamonds.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.example.diamonds.MainActivity

/**
 * Extension helpers shared across all UI instrumentation tests.
 */

/** Wait until the login screen is visible (unauthenticated start). */
fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForLoginScreen(
    timeoutMs: Long = 5_000
) {
    waitUntil(timeoutMillis = timeoutMs) {
        onAllNodes(hasText("Sign in to continue")).fetchSemanticsNodes().isNotEmpty()
    }
}

/** Wait until the app shell (post-login) is visible. */
fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForAppShell(
    timeoutMs: Long = 5_000
) {
    waitUntil(timeoutMillis = timeoutMs) {
        onAllNodes(hasText("Home")).fetchSemanticsNodes().isNotEmpty()
                || onAllNodes(hasText("Bookings")).fetchSemanticsNodes().isNotEmpty()
                || onAllNodes(hasText("Dashboard")).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Logs in using the customer demo credentials via the Login screen.
 * Call after [waitForLoginScreen].
 */
fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.loginAsCustomer() {
    onNodeWithText("Email").performTextInput("customer@demo.com")
    onNodeWithText("Password").performTextInput("demo1234")
    onNodeWithText("Sign In").performClick()
    waitForAppShell()
}

/**
 * Logs in as a cleaner via the Login screen.
 * Call after [waitForLoginScreen].
 */
fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.loginAsCleaner() {
    onNodeWithText("Email").performTextInput("cleaner@demo.com")
    onNodeWithText("Password").performTextInput("demo1234")
    onNodeWithText("Sign In").performClick()
    waitForAppShell()
}
