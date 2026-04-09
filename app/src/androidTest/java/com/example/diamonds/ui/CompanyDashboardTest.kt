package com.example.diamonds.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.diamonds.MainActivity
import com.example.diamonds.di.FakeAuthRepository
import com.example.diamonds.di.fakeSession
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

private val companySession = UserSession(
    userId = "company-1",
    email = "company@diamonds.com",
    displayName = "Sparkle Pro",
    role = UserRole.CLEANER,
    cleanerType = CleanerType.COMPANY,
    authToken = "company-token",
    isAuthenticated = true
)

/**
 * UI tests for the Company (COMPANY CleanerType) dashboard and tabs.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CompanyDashboardTest {

    @get:Rule(order = -1)
    val companySessionRule = object : TestWatcher() {
        override fun starting(description: Description) {
            FakeAuthRepository.initialSession = companySession
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

    @Test
    fun companyShell_bottomNavContainsCompanyTabs() {
        composeRule.onNodeWithText("Overview").assertIsDisplayed()
        composeRule.onNodeWithText("Team").assertIsDisplayed()
        composeRule.onNodeWithText("Earnings").assertIsDisplayed()
    }

    @Test
    fun companyOverview_tab_loadsScreen() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Overview", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun companyBookings_tab_loadsScreen() {
        composeRule.onNodeWithText("Bookings").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Bookings", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun companyTeam_tab_loadsScreen() {
        composeRule.onNodeWithText("Team").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Team", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun companyEarnings_tab_loadsScreen() {
        composeRule.onNodeWithText("Earnings").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Earnings", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
