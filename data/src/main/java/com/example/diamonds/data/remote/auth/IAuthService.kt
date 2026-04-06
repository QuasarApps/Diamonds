package com.example.diamonds.data.remote.auth

import com.example.diamonds.domain.model.Result

/**
 * Narrow auth-only service interface, decoupled from the broader [IBackendService].
 *
 * Concrete implementations:
 *  - [MockAuthService]   – instant-success stub for development / UI work
 *  - FirebaseAuthService – real Firebase Auth (wired when firebase-auth SDK is added)
 *
 * Swap via the Hilt [AuthServiceModule] binding.
 */
interface IAuthService {
    /**
     * Sign in with email + password.
     * Returns an opaque auth token string on success.
     */
    suspend fun login(email: String, password: String): Result<AuthResult>

    /**
     * Create a new account.
     * Returns an opaque auth token string on success.
     */
    suspend fun signup(
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        role: String
    ): Result<AuthResult>

    /** Sign out the current user. */
    suspend fun logout(): Result<Unit>

    /**
     * Exchange a potentially-expired token for a fresh one.
     * Returns the new token on success.
     */
    suspend fun refreshToken(currentToken: String): Result<String>

    /** Send a password-reset email. */
    suspend fun sendPasswordReset(email: String): Result<Unit>
}

/** Returned by a successful login / signup. */
data class AuthResult(
    val uid: String,
    val email: String,
    val displayName: String?,
    /** Raw token (JWT / custom token / Firebase ID token). */
    val token: String
)
