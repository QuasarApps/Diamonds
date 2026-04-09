package com.example.diamonds.ui

import androidx.compose.ui.test.assertIsDisplayed
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
 * UI tests for the Customer Profile screen.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CustomerProfileScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.waitForAppShell()
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.waitForIdle()
    }

    // ── Profile content ───────────────────────────────────────────────────────

    @Test
    fun customerProfile_displaysUserInfo() {
        // FakeSession displayName = "Test User"
        composeRule.onNodeWithText("Test User", substring = true).assertIsDisplayed()
    }

    @Test
    fun customerProfile_displaysEmail() {
        composeRule.onNodeWithText("test@diamonds.com", substring = true).assertIsDisplayed()
    }

    @Test
    fun customerProfile_showsCustomerRoleBadge() {
        composeRule.onNodeWithText("Customer", substring = true).assertIsDisplayed()
    }

    @Test
    fun customerProfile_myBookingsButtonIsVisible() {
        composeRule.onNodeWithText("My Bookings", substring = true).assertIsDisplayed()
    }

    @Test
    fun customerProfile_paymentHistoryButtonIsVisible() {
        composeRule.onNodeWithText("Payment History", substring = true).assertIsDisplayed()
    }

    @Test
    fun customerProfile_signOutButtonIsVisible() {
        composeRule.onNodeWithText("Sign Out", substring = true).assertIsDisplayed()
    }

    // ── Sign out flow ─────────────────────────────────────────────────────────

    @Test
    fun customerProfile_signOut_navigatesToLogin() {
        composeRule.onNodeWithText("Sign Out", substring = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Sign in to continue"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Sign in to continue").assertIsDisplayed()
    }

    // ── Navigation from profile ───────────────────────────────────────────────

    @Test
    fun customerProfile_tapMyBookings_navigatesToBookings() {
        composeRule.onNodeWithText("My Bookings", substring = true).performClick()
        composeRule.waitUntil(timeoutMillis = 4_000) {
            composeRule.onAllNodes(hasText("No bookings yet"))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Find a Cleaner"))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("123 Main St", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
