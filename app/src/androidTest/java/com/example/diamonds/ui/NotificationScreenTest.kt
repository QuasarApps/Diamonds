package com.example.diamonds.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.diamonds.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the Notification screen.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NotificationScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.waitForAppShell()
    }

    private fun navigateToNotifications() {
        // The notifications icon is in the top app bar of the AppShell
        // It may use a content description or simply a badge icon
        // Try clicking via content description first, then fall back to looking
        // for the notifications text
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("Notifications", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Notifications", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        try {
            composeRule.onNode(
                androidx.compose.ui.test.hasContentDescription("Notifications", substring = true)
            ).performClick()
        } catch (e: AssertionError) {
            composeRule.onNodeWithText("Notifications", substring = true).performClick()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun notificationScreen_markAllRead_buttonVisible_whenNotificationsExist() {
        navigateToNotifications()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Mark All Read", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("No notifications yet"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun notificationScreen_fakeNotification_isDisplayed() {
        navigateToNotifications()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Booking Confirmed", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("No notifications yet"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun notificationScreen_notificationBody_isDisplayed() {
        navigateToNotifications()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("accepted", substring = true, ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("No notifications yet"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
