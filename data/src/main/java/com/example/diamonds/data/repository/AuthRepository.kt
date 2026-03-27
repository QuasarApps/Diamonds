package com.example.diamonds.data.repository

import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Authentication repository - always requires online for write operations
 */
class AuthRepository(
    private val backendService: IBackendService,
    private val preferencesDataStore: PreferencesDataStore
) : IAuthRepository {

    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = backendService.login(email, password)
            when (result) {
                is Result.Success -> {
                    // TODO: Decode token to get user info and role
                    preferencesDataStore.saveUserSession(
                        UserSession(
                            userId = "user_123", // Extract from token
                            email = email,
                            role = UserRole.CLIENT,
                            authToken = result.data,
                            isAuthenticated = true
                        )
                    )
                    Result.Success(result.data)
                }
                else -> result
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
            val result = backendService.signup(name, email, password, phoneNumber, role.name)
            when (result) {
                is Result.Success -> {
                    // TODO: Decode token to get user info
                    preferencesDataStore.saveUserSession(
                        UserSession(
                            userId = "user_123", // Extract from token
                            email = email,
                            role = role,
                            authToken = result.data,
                            isAuthenticated = true
                        )
                    )
                    Result.Success(result.data)
                }
                else -> result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            preferencesDataStore.clearUserSession()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshToken(): Result<String> {
        // TODO: Implement token refresh with backend
        return Result.Error(Exception("Not implemented"))
    }

    override fun getCurrentUserSession(): Flow<UserSession?> {
        return preferencesDataStore.observeUserSession()
    }
}
