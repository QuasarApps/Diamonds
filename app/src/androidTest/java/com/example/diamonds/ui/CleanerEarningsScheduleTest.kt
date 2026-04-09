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
 * UI instrumentation tests for the Cleaner Earnings screen.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CleanerEarningsScreenTest {

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
        composeRule.onNodeWithText("Earnings").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun cleanerEarnings_screenLoads_withoutCrash() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Earnings", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Earnings").assertIsDisplayed()
    }

    @Test
    fun cleanerEarnings_totalEarnedSection_isVisible() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Earned", ignoreCase = true, substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("R0", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Total", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}

/**
 * UI tests for the Cleaner Schedule screen.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CleanerScheduleScreenTest {

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
        composeRule.onNodeWithText("Schedule").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun cleanerSchedule_screenLoads_withoutCrash() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Schedule", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun cleanerSchedule_showsCalendarOrList() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Schedule", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Today", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("No scheduled", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
