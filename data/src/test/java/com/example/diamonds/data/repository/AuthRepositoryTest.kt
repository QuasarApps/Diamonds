package com.example.diamonds.data.repository

import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.remote.auth.AuthResult
import com.example.diamonds.data.remote.auth.IAuthService
import com.example.diamonds.data.remote.backend.ClientDto
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.ProviderDto
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.VerificationStatus
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var authService: IAuthService
    private lateinit var preferencesDataStore: PreferencesDataStore
    private lateinit var backendService: IBackendService
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
        backendService = mockk()
        coEvery { backendService.updateClient(any()) } answers { Result.Success(firstArg<ClientDto>()) }
        coEvery { backendService.updateProvider(any()) } answers { Result.Success(firstArg<ProviderDto>()) }
        repository = AuthRepository(authService, preferencesDataStore, backendService)
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
    fun `signup as customer creates the client profile document`() = runTest {
        coEvery { authService.signup(any(), any(), any(), any(), any()) } returns Result.Success(stubAuthResult)
        val dto = slot<ClientDto>()

        val result = repository.signup(
            name        = "Test User",
            email       = "test@example.com",
            password    = "password",
            phoneNumber = "+15550001",
            role        = UserRole.CUSTOMER,
            cleanerType = CleanerType.INDEPENDENT
        )

        assertTrue(result is Result.Success)
        coVerify { backendService.updateClient(capture(dto)) }
        // Keyed on the auth uid, so the profile is reachable from the session that was just saved.
        assertEquals("uid_123", dto.captured.id)
        assertEquals("Test User", dto.captured.name)
        assertEquals("test@example.com", dto.captured.email)
        assertEquals("+15550001", dto.captured.phoneNumber)
        coVerify(exactly = 0) { backendService.updateProvider(any()) }
    }

    @Test
    fun `signup as cleaner creates the provider profile document`() = runTest {
        coEvery { authService.signup(any(), any(), any(), any(), any()) } returns Result.Success(stubAuthResult)
        val dto = slot<ProviderDto>()

        val result = repository.signup(
            name        = "Test User",
            email       = "test@example.com",
            password    = "password",
            phoneNumber = "+15550002",
            role        = UserRole.CLEANER,
            cleanerType = CleanerType.EMPLOYED
        )

        assertTrue(result is Result.Success)
        coVerify { backendService.updateProvider(capture(dto)) }
        assertEquals("uid_123", dto.captured.id)
        assertEquals(CleanerType.EMPLOYED.name, dto.captured.cleanerType)
        coVerify(exactly = 0) { backendService.updateClient(any()) }
    }

    @Test
    fun `new provider profile carries a verification status the mapper can parse`() = runTest {
        // ProviderDto.verificationStatus defaults to "", and ProviderDto.toDomain() runs it
        // through an unguarded VerificationStatus.valueOf() — leaving it defaulted would make
        // every read of a freshly signed-up provider throw.
        coEvery { authService.signup(any(), any(), any(), any(), any()) } returns Result.Success(stubAuthResult)
        val dto = slot<ProviderDto>()

        repository.signup("N", "e@t.com", "p", "+1", UserRole.CLEANER, CleanerType.INDEPENDENT)

        coVerify { backendService.updateProvider(capture(dto)) }
        assertEquals(VerificationStatus.PENDING.name, dto.captured.verificationStatus)
        assertEquals(VerificationStatus.PENDING, VerificationStatus.valueOf(dto.captured.verificationStatus))
    }

    @Test
    fun `signup fails without saving a session when the profile write fails`() = runTest {
        coEvery { authService.signup(any(), any(), any(), any(), any()) } returns Result.Success(stubAuthResult)
        coEvery { backendService.updateClient(any()) } returns Result.Error(Exception("firestore unavailable"))

        val result = repository.signup("N", "e@t.com", "p", "+1", UserRole.CUSTOMER, CleanerType.INDEPENDENT)

        assertTrue(result is Result.Error)
        // A session pointing at a profile that does not exist is worse than no session at all.
        coVerify(exactly = 0) { preferencesDataStore.saveUserSession(any()) }
        assertEquals("firestore unavailable", (result as Result.Error).exception.cause?.message)
    }

    @Test
    fun `signup profile failure is reported to the user rather than swallowed`() = runTest {
        coEvery { authService.signup(any(), any(), any(), any(), any()) } returns Result.Success(stubAuthResult)
        coEvery { backendService.updateProvider(any()) } returns Result.Error(Exception("boom"))

        val result = repository.signup("N", "e@t.com", "p", "+1", UserRole.CLEANER, CleanerType.INDEPENDENT)

        // AuthViewModel surfaces exception.message verbatim, so it has to read as user-facing text.
        val message = (result as Result.Error).exception.message.orEmpty()
        assertTrue("message was: $message", message.contains("profile could not be saved"))
    }

    @Test
    fun `signup saves the session only after the profile write succeeds`() = runTest {
        coEvery { authService.signup(any(), any(), any(), any(), any()) } returns Result.Success(stubAuthResult)

        repository.signup("N", "e@t.com", "p", "+1", UserRole.CUSTOMER, CleanerType.INDEPENDENT)

        coVerify(ordering = Ordering.ORDERED) {
            backendService.updateClient(any())
            preferencesDataStore.saveUserSession(any())
        }
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
