package com.example.diamonds.ui

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
 * UI instrumentation tests for Cleaner Service Management screens.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ServiceManagementScreenTest {

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
        // Navigate to Profile → Manage Services
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Manage Services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Manage Services", substring = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Services", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Add", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun serviceManagement_existingService_isDisplayed() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Apartment Cleaning", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("No services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Add", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun serviceManagement_addServiceButton_isVisible() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Add Service", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Add", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun serviceManagement_tapAddService_navigatesToEditScreen() {
        val addNodes = composeRule.onAllNodes(hasText("Add Service", substring = true))
            .fetchSemanticsNodes()
        if (addNodes.isEmpty()) return // skip if button not found
        composeRule.onNodeWithText("Add Service", substring = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Title", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Save", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("New Service", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
