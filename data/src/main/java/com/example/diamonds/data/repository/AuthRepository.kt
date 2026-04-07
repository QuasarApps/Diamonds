package com.example.diamonds.data.repository
import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.remote.auth.IAuthService
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
/**
 * Authentication repository.
 *
 * Delegates all network calls to [IAuthService] (swap MockAuthService /
 * FirebaseAuthService via Hilt without touching this class).
 * Session state is persisted in [PreferencesDataStore].
 */
class AuthRepository(
    private val authService: IAuthService,
    private val preferencesDataStore: PreferencesDataStore
) : IAuthRepository {
    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            when (val result = authService.login(email, password)) {
                is Result.Success -> {
                    val auth = result.data
                    // On login we reload the previously-stored role.
                    // If the user has never signed up on this device the
                    // default is CUSTOMER (the most common path).
                    val existingSession = preferencesDataStore.observeUserSession().first()
                    val role = existingSession?.role ?: UserRole.CUSTOMER
                    preferencesDataStore.saveUserSession(
                        UserSession(
                            userId = auth.uid,
                            email = auth.email,
                            displayName = auth.displayName,
                            role = role,
                            authToken = auth.token,
                            isAuthenticated = true
                        )
                    )
                    Result.Success(auth.token)
                }
                is Result.Error -> result
                is Result.Loading -> result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    override suspend fun signup(
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        role: UserRole
    ): Result<String> {
        return try {
            when (val result = authService.signup(name, email, password, phoneNumber, role.name)) {
                is Result.Success -> {
                    val auth = result.data
                    preferencesDataStore.saveUserSession(
                        UserSession(
                            userId = auth.uid,
                            email = auth.email,
                            displayName = auth.displayName ?: name,
                            role = role,
                            authToken = auth.token,
                            isAuthenticated = true
                        )
                    )
                    Result.Success(auth.token)
                }
                is Result.Error -> result
                is Result.Loading -> result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    override suspend fun logout(): Result<Unit> {
        return try {
            authService.logout()
            preferencesDataStore.clearUserSession()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    override suspend fun refreshToken(): Result<String> {
        return try {
            val session = preferencesDataStore.observeUserSession().first()
                ?: return Result.Error(Exception("No active session"))
            authService.refreshToken(session.authToken)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            authService.sendPasswordReset(email)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    override fun getCurrentUserSession(): Flow<UserSession?> =
        preferencesDataStore.observeUserSession()
}
