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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

/**
 * End-to-end UI instrumentation test for the Login screen.
 *
 * Sets [FakeAuthRepository.initialSession] = null (order = -1) so the
 * splash routes to the Login screen before the Activity is even created.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    /** Runs before all other rules — clears the auth session. */
    @get:Rule(order = -1)
    val unauthRule = object : TestWatcher() {
        override fun starting(description: Description) {
            FakeAuthRepository.initialSession = null
        }

        override fun finished(description: Description) {
            // Restore the default authenticated session for subsequent tests
            FakeAuthRepository.initialSession = com.example.diamonds.di.fakeSession
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
    }

    // ── Screen content ────────────────────────────────────────────────────────

    @Test
    fun loginScreen_displaysRequiredElements() {
        // Wait for splash to pass through (mock auth reports unauthenticated)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sign in to continue").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeRule.onNodeWithText("Sign Up").assertIsDisplayed()
    }

    // ── Field validation ──────────────────────────────────────────────────────

    @Test
    fun loginScreen_emptyEmail_showsValidationError() {
        composeRule.waitForIdle()
        // Leave email empty, provide password, then click Sign In
        composeRule.onNodeWithText("Password").performTextInput("password123")
        composeRule.onNodeWithText("Sign In").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter a valid email address").assertIsDisplayed()
    }

    @Test
    fun loginScreen_shortPassword_showsValidationError() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Email").performTextInput("user@test.com")
        composeRule.onNodeWithText("Password").performTextInput("123")
        composeRule.onNodeWithText("Sign In").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Password must be at least 6 characters").assertIsDisplayed()
    }

    @Test
    fun loginScreen_invalidEmailFormat_showsValidationError() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Email").performTextInput("not-an-email")
        composeRule.onNodeWithText("Password").performTextInput("password123")
        composeRule.onNodeWithText("Sign In").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter a valid email address").assertIsDisplayed()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Test
    fun loginScreen_clickSignUp_navigatesToSignupScreen() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sign Up").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create Account").assertIsDisplayed()
    }

    @Test
    fun loginScreen_forgotPassword_opensDialog() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Forgot password?").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Reset Password").assertIsDisplayed()
        composeRule.onNodeWithText("Send").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun loginScreen_forgotPasswordDialog_cancel_closesDialog() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Forgot password?").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        // Dialog should be gone; main screen text should be visible again
        composeRule.onNodeWithText("Sign in to continue").assertIsDisplayed()
    }

    // ── Successful login ──────────────────────────────────────────────────────

    @Test
    fun loginScreen_validCredentials_navigatesToAppShell() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Email").performTextInput("customer@demo.com")
        composeRule.onNodeWithText("Password").performTextInput("demo1234")
        composeRule.onNodeWithText("Sign In").performClick()
        // After login, we expect the shell; the customer home tab text is "Home" or similar
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Home")).fetchSemanticsNodes().isNotEmpty()
                    || composeRule.onAllNodes(hasText("Bookings")).fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    // ── Demo accounts ─────────────────────────────────────────────────────────

    @Test
    fun loginScreen_demoPanelIsVisible() {
        composeRule.waitForIdle()
        // Resolve the expected text from the string resource rather than hardcoding it: the
        // literal drifted once already when Track B externalized it (the old assertion expected
        // two spaces, the resource ships one), and a hardcoded English literal would also fail
        // under any non-default locale.
        val demoHint =
            composeRule.activity.getString(com.example.diamonds.ui.R.string.demo_accounts_hint)
        composeRule.onNodeWithText(demoHint, substring = true)
            .assertIsDisplayed()
    }
}
