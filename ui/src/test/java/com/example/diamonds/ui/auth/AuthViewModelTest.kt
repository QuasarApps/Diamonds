package com.example.diamonds.ui.auth

import com.example.diamonds.common.util.ConnectivityState
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: IAuthRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        authRepository = mockk(relaxed = true)
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns flowOf(ConnectivityState.ONLINE)
            every { isOnline() } returns true
        }
        viewModel = AuthViewModel(authRepository, connectivityObserver)
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    @Test
    fun `onEmailChange updates email state`() {
        viewModel.onEmailChange("test@example.com")
        assertEquals("test@example.com", viewModel.email.value)
    }

    @Test
    fun `onPasswordChange updates password state`() {
        viewModel.onPasswordChange("secret123")
        assertEquals("secret123", viewModel.password.value)
    }

    @Test
    fun `onNameChange updates name state`() {
        viewModel.onNameChange("Alice")
        assertEquals("Alice", viewModel.name.value)
    }

    @Test
    fun `onPhoneChange updates phone state`() {
        viewModel.onPhoneChange("+15551234567")
        assertEquals("+15551234567", viewModel.phone.value)
    }

    @Test
    fun `onRoleChange updates selectedRole state`() {
        viewModel.onRoleChange(UserRole.CLEANER)
        assertEquals(UserRole.CLEANER, viewModel.selectedRole.value)
    }

    @Test
    fun `onCleanerTypeChange updates cleanerType state`() {
        viewModel.onCleanerTypeChange(CleanerType.COMPANY)
        assertEquals(CleanerType.COMPANY, viewModel.cleanerType.value)
    }

    // ── Login validation ──────────────────────────────────────────────────────

    @Test
    fun `login with empty email sets EMAIL field error`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password123")
        viewModel.login()

        val errors = viewModel.uiState.value.fieldErrors
        assertTrue(errors.containsKey(AuthField.EMAIL))
    }

    @Test
    fun `login with short password sets PASSWORD field error`() = runTest {
        viewModel.onEmailChange("valid@test.com")
        viewModel.onPasswordChange("abc")
        viewModel.login()

        val errors = viewModel.uiState.value.fieldErrors
        assertTrue(errors.containsKey(AuthField.PASSWORD))
    }

    @Test
    fun `login success sets loginSuccess true in uiState`() = runTest {
        coEvery { authRepository.login(any(), any(), any(), any()) } returns Result.Success("token")

        viewModel.onEmailChange("valid@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.login()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loginSuccess)
    }

    @Test
    fun `login error sets error message`() = runTest {
        coEvery { authRepository.login(any(), any(), any(), any()) } returns
            Result.Error(Exception("Invalid credentials"))

        viewModel.onEmailChange("valid@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Invalid credentials", viewModel.error.value)
        assertFalse(viewModel.uiState.value.loginSuccess)
    }

    // ── Signup validation ─────────────────────────────────────────────────────

    @Test
    fun `signup with blank name sets NAME field error`() = runTest {
        viewModel.onNameChange("")
        viewModel.onEmailChange("valid@test.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.onPhoneChange("+15551234567")
        viewModel.signup()

        assertTrue(viewModel.uiState.value.fieldErrors.containsKey(AuthField.NAME))
    }

    @Test
    fun `signup with mismatched passwords sets CONFIRM_PASSWORD error`() = runTest {
        viewModel.onNameChange("Alice")
        viewModel.onEmailChange("alice@test.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("different")
        viewModel.onPhoneChange("+15551234567")
        viewModel.signup()

        assertTrue(viewModel.uiState.value.fieldErrors.containsKey(AuthField.CONFIRM_PASSWORD))
    }

    @Test
    fun `signup success sets signupSuccess true`() = runTest {
        coEvery { authRepository.signup(any(), any(), any(), any(), any(), any()) } returns
            Result.Success("token")

        viewModel.onNameChange("Alice")
        viewModel.onEmailChange("alice@test.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.onPhoneChange("+15551234567")
        viewModel.signup()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.signupSuccess)
    }

    // ── Password reset ────────────────────────────────────────────────────────

    @Test
    fun `sendPasswordReset with invalid email sets field error`() = runTest {
        viewModel.onEmailChange("not-an-email")
        viewModel.sendPasswordReset()

        assertTrue(viewModel.uiState.value.fieldErrors.containsKey(AuthField.EMAIL))
    }

    @Test
    fun `sendPasswordReset success sets passwordResetSent true`() = runTest {
        coEvery { authRepository.sendPasswordReset(any()) } returns Result.Success(Unit)

        viewModel.onEmailChange("user@example.com")
        viewModel.sendPasswordReset()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.passwordResetSent)
    }

    // ── clearAuthSuccess ──────────────────────────────────────────────────────

    @Test
    fun `clearAuthSuccess resets all success flags`() = runTest {
        coEvery { authRepository.login(any(), any(), any(), any()) } returns Result.Success("t")

        viewModel.onEmailChange("valid@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.login()
        advanceUntilIdle()

        viewModel.clearAuthSuccess()

        assertFalse(viewModel.uiState.value.loginSuccess)
        assertFalse(viewModel.uiState.value.signupSuccess)
        assertFalse(viewModel.uiState.value.passwordResetSent)
    }

    // ── Field error clearing ──────────────────────────────────────────────────

    @Test
    fun `typing in email field clears EMAIL error`() = runTest {
        viewModel.onEmailChange("")
        viewModel.login() // triggers EMAIL error

        viewModel.onEmailChange("good@test.com")

        assertFalse(viewModel.uiState.value.fieldErrors.containsKey(AuthField.EMAIL))
    }
}
