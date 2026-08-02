package com.example.diamonds.data.repository
import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.remote.auth.IAuthService
import com.example.diamonds.data.remote.backend.ClientDto
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.ProviderDto
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.VerificationStatus
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val authService: IAuthService,
    private val preferencesDataStore: PreferencesDataStore,
    private val backendService: IBackendService
) : IAuthRepository {

    override suspend fun login(
        email: String,
        password: String,
        roleHint: UserRole?,
        cleanerTypeHint: CleanerType?
    ): Result<String> {
        return try {
            when (val result = authService.login(email, password)) {
                is Result.Success -> {
                    val auth = result.data
                    val existingSession = preferencesDataStore.observeUserSession().first()
                    // Hint wins > existing session > default CUSTOMER
                    val role = roleHint
                        ?: existingSession?.role
                        ?: UserRole.CUSTOMER
                    // Hint wins > existing session > default INDEPENDENT
                    val cleanerType = cleanerTypeHint
                        ?: existingSession?.cleanerType
                        ?: CleanerType.INDEPENDENT
                    preferencesDataStore.saveUserSession(
                        UserSession(
                            userId      = auth.uid,
                            email       = auth.email,
                            displayName = auth.displayName,
                            role        = role,
                            cleanerType = cleanerType,
                            authToken   = auth.token,
                            isAuthenticated = true
                        )
                    )
                    Result.Success(auth.token)
                }
                is Result.Error   -> result
                is Result.Loading -> result
            }
        } catch (e: Exception) { Result.Error(e) }
    }

    /**
     * Creates the auth account **and** its backing profile document.
     *
     * [IAuthService.signup] only creates the credential; without a `clients`/`providers` document
     * every later profile read resolves to nothing, which is why this is done here rather than
     * left to the first profile-edit screen.
     *
     * The two writes are not atomic — only a server-side transaction could make them so — and the
     * profile write is deliberately ordered *before* the session is persisted. A failure therefore
     * leaves an auth account with no profile and no session, and is reported as an error rather
     * than being swallowed: a saved session pointing at a missing profile would instead surface as
     * unexplained "not found" errors on every screen the user visits afterwards.
     */
    override suspend fun signup(
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        role: UserRole,
        cleanerType: CleanerType
    ): Result<String> {
        return try {
            when (val result = authService.signup(name, email, password, phoneNumber, role.name)) {
                is Result.Success -> {
                    val auth = result.data
                    val displayName = auth.displayName ?: name

                    when (val profile = createProfile(auth.uid, displayName, auth.email, phoneNumber, role, cleanerType)) {
                        is Result.Success -> Unit
                        is Result.Loading -> return Result.Loading
                        is Result.Error -> return Result.Error(
                            Exception(
                                "Your account was created but its profile could not be saved. " +
                                    "Please check your connection and sign in to finish setting up.",
                                profile.exception
                            )
                        )
                    }

                    preferencesDataStore.saveUserSession(
                        UserSession(
                            userId      = auth.uid,
                            email       = auth.email,
                            displayName = displayName,
                            role        = role,
                            cleanerType = cleanerType,
                            authToken   = auth.token,
                            isAuthenticated = true
                        )
                    )
                    Result.Success(auth.token)
                }
                is Result.Error   -> result
                is Result.Loading -> result
            }
        } catch (e: Exception) { Result.Error(e) }
    }

    /**
     * Writes the profile document that matches [role]: a `ClientDto` for [UserRole.CUSTOMER],
     * a `ProviderDto` for [UserRole.CLEANER]. Both backend calls are upserts keyed on the auth
     * uid, so retrying a signup that got this far is idempotent.
     */
    private suspend fun createProfile(
        uid: String,
        name: String,
        email: String,
        phoneNumber: String,
        role: UserRole,
        cleanerType: CleanerType
    ): Result<Unit> {
        val now = System.currentTimeMillis().toString()
        return when (role) {
            UserRole.CUSTOMER -> backendService.updateClient(
                ClientDto(
                    id          = uid,
                    name        = name,
                    email       = email,
                    phoneNumber = phoneNumber,
                    createdAt   = now,
                    updatedAt   = now
                )
            ).map { }

            UserRole.CLEANER -> backendService.updateProvider(
                ProviderDto(
                    id          = uid,
                    name        = name,
                    email       = email,
                    phoneNumber = phoneNumber,
                    // Explicit rather than defaulted: ProviderDto's default is "", and
                    // ProviderDto.toDomain() maps this field with an unguarded
                    // VerificationStatus.valueOf(), which would throw on an empty string.
                    verificationStatus = VerificationStatus.PENDING.name,
                    cleanerType = cleanerType.name,
                    createdAt   = now,
                    updatedAt   = now
                )
            ).map { }
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            authService.logout()
            preferencesDataStore.clearUserSession()
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e) }
    }

    override suspend fun refreshToken(): Result<String> {
        return try {
            val session = preferencesDataStore.observeUserSession().first()
                ?: return Result.Error(Exception("No active session"))
            authService.refreshToken(session.authToken)
        } catch (e: Exception) { Result.Error(e) }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            authService.sendPasswordReset(email)
        } catch (e: Exception) { Result.Error(e) }
    }

    override fun getCurrentUserSession(): Flow<UserSession?> =
        preferencesDataStore.observeUserSession()
}
