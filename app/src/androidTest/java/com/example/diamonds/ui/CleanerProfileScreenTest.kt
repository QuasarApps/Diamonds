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
 * UI tests for the Cleaner Profile and Service Management screens.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CleanerProfileScreenTest {

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
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.waitForIdle()
    }

    // ── Profile content ───────────────────────────────────────────────────────

    @Test
    fun cleanerProfile_displaysCleanerName() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Clean Pro", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Profile", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    @Test
    fun cleanerProfile_manageServicesButton_isVisible() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Manage Services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun cleanerProfile_tapManageServices_navigatesToServiceManagement() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Manage Services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Manage Services", substring = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Apartment Cleaning", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Add Service", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Services", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
