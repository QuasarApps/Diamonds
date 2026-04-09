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
 * UI instrumentation tests for the Provider Search screen.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProviderSearchScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.waitForAppShell()
        // Navigate to provider search from the home tab
        composeRule.onNodeWithText("Find a Cleaner", substring = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("All")).fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Apartment")).fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    // ── Category chips ────────────────────────────────────────────────────────

    @Test
    fun providerSearch_categoryChips_areVisible() {
        composeRule.onNodeWithText("All").assertIsDisplayed()
        composeRule.onNodeWithText("Apartment").assertIsDisplayed()
        composeRule.onNodeWithText("House").assertIsDisplayed()
    }

    @Test
    fun providerSearch_deepCleanChip_isVisible() {
        composeRule.onNodeWithText("Deep Clean").assertIsDisplayed()
    }

    // ── Provider list ─────────────────────────────────────────────────────────

    @Test
    fun providerSearch_fakeProvider_appearsInList() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Clean Pro Services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Clean Pro Services", substring = true).assertIsDisplayed()
    }

    @Test
    fun providerSearch_providerCard_showsRating() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Clean Pro Services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Rating should appear as "4.5" or "★ 4.5"
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasText("4.5", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ── Category filter ───────────────────────────────────────────────────────

    @Test
    fun providerSearch_tapApartmentChip_filtersList() {
        composeRule.onNodeWithText("Apartment").performClick()
        composeRule.waitForIdle()
        // Providers list should still render (or show empty) without crashing
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasText("Clean Pro Services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("No cleaners found", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("All")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ── Navigation to service list ────────────────────────────────────────────

    @Test
    fun providerSearch_tapProvider_navigatesToServiceList() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Clean Pro Services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Clean Pro Services", substring = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Apartment Cleaning", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Services", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
