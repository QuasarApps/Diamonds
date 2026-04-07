package com.example.diamonds.data.remote.auth

import com.example.diamonds.domain.model.Result
import kotlinx.coroutines.delay

/**
 * Development / testing auth service that always succeeds immediately.
 *
 * Use this implementation to work on the UI without a real Firebase project.
 * Swap it out for [FirebaseAuthService] in [AuthServiceModule] when ready.
 *
 * Credentials accepted:
 *   - Any well-formed email + password of ≥ 6 chars succeeds.
 *   - Email "fail@test.com" always returns an error (handy for testing error states).
 *
 * Demo shortcut accounts:
 *   - "cleaner@demo.com" / any password → signs in as provider p1 (Maria Garcia)
 *   - "customer@demo.com" / any password → signs in as demo_customer
 */
class MockAuthService : IAuthService {

    override suspend fun login(email: String, password: String): Result<AuthResult> {
        delay(600) // simulate network latency

        if (email == "fail@test.com") {
            return Result.Error(Exception("Invalid email or password"))
        }

        // Demo shortcut accounts with predictable UIDs that match seed data
        val uid = when (email.lowercase()) {
            "cleaner@demo.com"  -> "p1"
            "customer@demo.com" -> "demo_customer"
            else                -> "mock_uid_${email.hashCode()}"
        }
        val displayName = when (email.lowercase()) {
            "cleaner@demo.com"  -> "Maria Garcia"
            "customer@demo.com" -> "Demo Customer"
            else                -> email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }

        return Result.Success(
            AuthResult(
                uid         = uid,
                email       = email,
                displayName = displayName,
                token       = "mock_token_${System.currentTimeMillis()}"
            )
        )
    }

    override suspend fun signup(
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        role: String
    ): Result<AuthResult> {
        delay(800) // simulate network latency

        if (email == "fail@test.com") {
            return Result.Error(Exception("Email already in use"))
        }

        return Result.Success(
            AuthResult(
                uid         = "mock_uid_${email.hashCode()}",
                email       = email,
                displayName = name,
                token       = "mock_token_${System.currentTimeMillis()}"
            )
        )
    }

    override suspend fun logout(): Result<Unit> {
        delay(200)
        return Result.Success(Unit)
    }

    override suspend fun refreshToken(currentToken: String): Result<String> {
        delay(300)
        return Result.Success("mock_token_refreshed_${System.currentTimeMillis()}")
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        delay(500)
        return Result.Success(Unit)
    }
}
