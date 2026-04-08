package com.example.diamonds.data.repository

import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.remote.auth.AuthResult
import com.example.diamonds.data.remote.auth.IAuthService
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var authService: IAuthService
    private lateinit var preferencesDataStore: PreferencesDataStore
    private lateinit var repository: AuthRepository

    private val stubAuthResult = AuthResult(
        uid = "uid_123",
        email = "test@example.com",
        displayName = "Test User",
        token = "token_abc"
    )

    @Before
    fun setUp() {
        authService = mockk()
        preferencesDataStore = mockk(relaxed = true) {
            every { observeUserSession() } returns flowOf(null)
        }
        repository = AuthRepository(authService, preferencesDataStore)
    }

    @Test
    fun `login success saves session and returns token`() = runTest {
        coEvery { authService.login(any(), any()) } returns Result.Success(stubAuthResult)

        val result = repository.login("test@example.com", "password")

        assertTrue(result is Result.Success)
        assertEquals("token_abc", (result as Result.Success).data)
        coVerify { preferencesDataStore.saveUserSession(any()) }
    }

    @Test
    fun `login with cleanerTypeHint persists the hint in the session`() = runTest {
        coEvery { authService.login(any(), any()) } returns Result.Success(stubAuthResult)

        repository.login("test@example.com", "password", UserRole.CLEANER, CleanerType.EMPLOYED)

        coVerify {
            preferencesDataStore.saveUserSession(
                match { session ->
                    session.role == UserRole.CLEANER && session.cleanerType == CleanerType.EMPLOYED
                }
            )
        }
    }

    @Test
    fun `login error propagates error result`() = runTest {
        val error = Exception("Invalid credentials")
        coEvery { authService.login(any(), any()) } returns Result.Error(error)

        val result = repository.login("bad@email.com", "wrong")

        assertTrue(result is Result.Error)
        assertEquals("Invalid credentials", (result as Result.Error).exception.message)
    }

    @Test
    fun `login exception is wrapped in Result Error`() = runTest {
        coEvery { authService.login(any(), any()) } throws RuntimeException("network failure")

        val result = repository.login("t@t.com", "p")

        assertTrue(result is Result.Error)
        assertEquals("network failure", (result as Result.Error).exception.message)
    }

    @Test
    fun `signup success saves session with correct role`() = runTest {
        coEvery { authService.signup(any(), any(), any(), any(), any()) } returns Result.Success(stubAuthResult)

        val result = repository.signup(
            name        = "Test User",
            email       = "test@example.com",
            password    = "password",
            phoneNumber = "+1555",
            role        = UserRole.CLEANER,
            cleanerType = CleanerType.INDEPENDENT
        )

        assertTrue(result is Result.Success)
        coVerify {
            preferencesDataStore.saveUserSession(
                match { it.role == UserRole.CLEANER && it.cleanerType == CleanerType.INDEPENDENT }
            )
        }
    }

    @Test
    fun `signup error propagates without saving session`() = runTest {
        coEvery { authService.signup(any(), any(), any(), any(), any()) } returns Result.Error(Exception("Email taken"))

        val result = repository.signup("N","e@t.com","p","+1", UserRole.CUSTOMER, CleanerType.INDEPENDENT)

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { preferencesDataStore.saveUserSession(any()) }
    }

    @Test
    fun `logout calls clearUserSession`() = runTest {
        coEvery { authService.logout() } returns Result.Success(Unit)

        val result = repository.logout()

        assertTrue(result is Result.Success)
        coVerify { preferencesDataStore.clearUserSession() }
    }

    @Test
    fun `sendPasswordReset delegates to authService`() = runTest {
        coEvery { authService.sendPasswordReset(any()) } returns Result.Success(Unit)

        val result = repository.sendPasswordReset("user@example.com")

        assertTrue(result is Result.Success)
        coVerify { authService.sendPasswordReset("user@example.com") }
    }

    @Test
    fun `sendPasswordReset propagates error`() = runTest {
        coEvery { authService.sendPasswordReset(any()) } returns Result.Error(Exception("no account"))

        val result = repository.sendPasswordReset("ghost@example.com")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `getCurrentUserSession returns flow from preferencesDataStore`() = runTest {
        val mockSession = UserSession(
            userId = "uid_1", email = "t@t.com", role = UserRole.CUSTOMER,
            authToken = "tok", isAuthenticated = true
        )
        every { preferencesDataStore.observeUserSession() } returns flowOf(mockSession)

        val flow = repository.getCurrentUserSession()
        var received: UserSession? = null
        flow.collect { received = it }

        assertEquals(mockSession, received)
    }
}
