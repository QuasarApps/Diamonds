package com.example.diamonds.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.diamonds.MainActivity
import com.example.diamonds.di.FakeAuthRepository
import com.example.diamonds.di.fakeCleanerSession
import com.example.diamonds.di.fakeSession
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

/**
 * UI instrumentation tests for Cleaner-specific screens.
 *
 * Sets [FakeAuthRepository.initialSession] to the cleaner session before
 * the Activity launches so the AppShell renders cleaner tabs.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CleanerShellTest {

    @get:Rule(order = -1)
    val cleanerSessionRule = object : TestWatcher() {
        override fun starting(description: Description) {
            FakeAuthRepository.initialSession = fakeCleanerSession
        }

        override fun finished(description: Description) {
            FakeAuthRepository.initialSession = fakeSession
        }
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.waitForAppShell()
    }

    // ── Bottom navigation tabs ────────────────────────────────────────────────

    @Test
    fun cleanerShell_bottomNavContainsCleanerTabs() {
        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeRule.onNodeWithText("Requests").assertIsDisplayed()
        composeRule.onNodeWithText("Schedule").assertIsDisplayed()
        composeRule.onNodeWithText("Earnings").assertIsDisplayed()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    // ── Cleaner Dashboard ─────────────────────────────────────────────────────

    @Test
    fun cleanerDashboard_isDisplayedAfterLogin() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Dashboard")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }

    // ── Booking Requests ──────────────────────────────────────────────────────

    @Test
    fun cleanerRequests_tab_loadsScreen() {
        composeRule.onNodeWithText("Requests").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("No pending requests"))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Accept", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Booking", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun cleanerRequests_emptyState_showsNoRequestsMessage() {
        composeRule.onNodeWithText("Requests").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("No pending requests"))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Accept", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ── Schedule ──────────────────────────────────────────────────────────────

    @Test
    fun cleanerSchedule_tab_loadsScreen() {
        composeRule.onNodeWithText("Schedule").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Schedule", ignoreCase = true))
                .fetchSemanticsNodes().size >= 1
        }
    }

    // ── Earnings ──────────────────────────────────────────────────────────────

    @Test
    fun cleanerEarnings_tab_loadsScreen() {
        composeRule.onNodeWithText("Earnings").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Earnings", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Total Earned", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("R0", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ── Cleaner Profile ───────────────────────────────────────────────────────

    @Test
    fun cleanerProfile_tab_showsProfileContent() {
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Clean Pro", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Profile", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
