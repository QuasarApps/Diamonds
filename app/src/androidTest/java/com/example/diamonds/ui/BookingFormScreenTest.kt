package com.example.diamonds.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.diamonds.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the Booking Form screen (date/time/address entry).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BookingFormScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.waitForAppShell()

        // Navigate: Home → Find a Cleaner → tap provider → tap service
        composeRule.onNodeWithText("Find a Cleaner", substring = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Clean Pro Services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Clean Pro Services", substring = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Apartment Cleaning", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Book", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Tap on the service (or the Book button)
        val bookNodes = composeRule.onAllNodes(hasText("Book", substring = true))
            .fetchSemanticsNodes()
        if (bookNodes.isNotEmpty()) {
            composeRule.onAllNodes(hasText("Book", substring = true))[0].run {}
            composeRule.onNodeWithText("Book", substring = true).performClick()
        } else {
            composeRule.onNodeWithText("Apartment Cleaning", substring = true).performClick()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Address", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Date", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Booking", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ── Form fields ───────────────────────────────────────────────────────────

    @Test
    fun bookingForm_addressField_isDisplayed() {
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasText("Address", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Address", substring = true).assertIsDisplayed()
    }

    @Test
    fun bookingForm_serviceSummaryCard_isDisplayed() {
        // The form shows a summary card with "Booking" header
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasText("Booking", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Booking", substring = true).assertIsDisplayed()
    }

    @Test
    fun bookingForm_addressInput_acceptsText() {
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasText("Address", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Address", substring = true).performTextInput("10 Test Road")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("10 Test Road", substring = true).assertIsDisplayed()
    }

    @Test
    fun bookingForm_notesField_acceptsText() {
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasText("Notes", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Notes", substring = true)
            .performTextInput("Please bring supplies")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Please bring supplies", substring = true).assertIsDisplayed()
    }
}
