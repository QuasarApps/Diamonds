package com.example.diamonds.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.diamonds.MainActivity
import com.example.diamonds.di.FakeAuthRepository
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
 * UI instrumentation tests for the Sign-Up / Registration screen.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SignupScreenTest {

    @get:Rule(order = -1)
    val unauthRule = object : TestWatcher() {
        override fun starting(description: Description) {
            FakeAuthRepository.initialSession = null
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
        composeRule.waitForLoginScreen()
        // Navigate to signup from login
        composeRule.onNodeWithText("Sign Up").performClick()
        composeRule.waitForIdle()
    }

    // ── Screen content ────────────────────────────────────────────────────────

    @Test
    fun signupScreen_displaysRequiredElements() {
        composeRule.onNodeWithText("Create Account").assertIsDisplayed()
        composeRule.onNodeWithText("Full Name").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Phone Number").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm Password").assertIsDisplayed()
        composeRule.onNodeWithText("Customer").assertIsDisplayed()
        composeRule.onNodeWithText("Cleaner").assertIsDisplayed()
    }

    @Test
    fun signupScreen_hasBothRoleOptions() {
        composeRule.onNodeWithText("Book cleaners").assertIsDisplayed()
        composeRule.onNodeWithText("Get hired").assertIsDisplayed()
    }

    // ── Field validation ──────────────────────────────────────────────────────

    @Test
    fun signupScreen_emptyName_showsError() {
        composeRule.onNodeWithText("Email").performTextInput("new@test.com")
        composeRule.onNodeWithText("Phone Number").performTextInput("+27821234567")
        composeRule.onNodeWithText("Password").performTextInput("password123")
        composeRule.onNodeWithText("Confirm Password").performTextInput("password123")
        composeRule.onNodeWithText("Create Account").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Name is required").assertIsDisplayed()
    }

    @Test
    fun signupScreen_passwordMismatch_showsError() {
        composeRule.onNodeWithText("Full Name").performTextInput("Test User")
        composeRule.onNodeWithText("Email").performTextInput("new@test.com")
        composeRule.onNodeWithText("Phone Number").performTextInput("+27821234567")
        composeRule.onNodeWithText("Password").performTextInput("password123")
        composeRule.onNodeWithText("Confirm Password").performTextInput("different456")
        composeRule.onNodeWithText("Create Account").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Passwords don't match").assertIsDisplayed()
    }

    @Test
    fun signupScreen_shortPassword_showsError() {
        composeRule.onNodeWithText("Full Name").performTextInput("Test User")
        composeRule.onNodeWithText("Email").performTextInput("new@test.com")
        composeRule.onNodeWithText("Phone Number").performTextInput("+27821234567")
        composeRule.onNodeWithText("Password").performTextInput("123")
        composeRule.onNodeWithText("Confirm Password").performTextInput("123")
        composeRule.onNodeWithText("Create Account").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Password must be at least 6 characters").assertIsDisplayed()
    }

    @Test
    fun signupScreen_invalidEmail_showsError() {
        composeRule.onNodeWithText("Full Name").performTextInput("Test User")
        composeRule.onNodeWithText("Email").performTextInput("not-an-email")
        composeRule.onNodeWithText("Phone Number").performTextInput("+27821234567")
        composeRule.onNodeWithText("Password").performTextInput("password123")
        composeRule.onNodeWithText("Confirm Password").performTextInput("password123")
        composeRule.onNodeWithText("Create Account").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter a valid email address").assertIsDisplayed()
    }

    // ── Role selection ────────────────────────────────────────────────────────

    @Test
    fun signupScreen_selectCleanerRole_showsCleanerTypeOptions() {
        composeRule.onNodeWithText("Get hired").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Work type").assertIsDisplayed()
        composeRule.onNodeWithText("Independent").assertIsDisplayed()
        composeRule.onNodeWithText("Employed").assertIsDisplayed()
    }

    @Test
    fun signupScreen_selectEmployedCleaner_showsInfoMessage() {
        composeRule.onNodeWithText("Get hired").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Employed").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            "Your employer will link you", substring = true
        ).assertIsDisplayed()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Test
    fun signupScreen_backToLogin_navigatesToLoginScreen() {
        composeRule.onNodeWithText("Sign In").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sign in to continue").assertIsDisplayed()
    }

    // ── Successful signup ─────────────────────────────────────────────────────

    @Test
    fun signupScreen_validCustomerDetails_signupSucceeds() {
        composeRule.onNodeWithText("Full Name").performTextInput("Jane Doe")
        composeRule.onNodeWithText("Email").performTextInput("jane@example.com")
        composeRule.onNodeWithText("Phone Number").performTextInput("+27821234567")
        composeRule.onNodeWithText("Password").performTextInput("password123")
        composeRule.onNodeWithText("Confirm Password").performTextInput("password123")
        composeRule.onNodeWithText("Create Account").performClick()
        // After successful signup the app should navigate to the AppShell
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Home")).fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Bookings")).fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
