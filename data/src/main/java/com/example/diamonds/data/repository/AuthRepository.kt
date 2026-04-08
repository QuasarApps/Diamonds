package com.example.diamonds.data.repository
import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.remote.auth.IAuthService
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val authService: IAuthService,
    private val preferencesDataStore: PreferencesDataStore
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
                    preferencesDataStore.saveUserSession(
                        UserSession(
                            userId      = auth.uid,
                            email       = auth.email,
                            displayName = auth.displayName ?: name,
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
