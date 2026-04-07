package com.example.diamonds.data.remote.auth

import com.example.diamonds.domain.model.Result
import kotlinx.coroutines.delay

/**
 * Development / testing auth service that always succeeds immediately.
 *
 * Use this implementation to work on the UI without a real Firebase project.
 * Swap it out for [FirebaseAuthService] when ready.
 *
 * Credentials accepted:
 *   - Any well-formed email + password of ≥ 6 chars succeeds.
 *   - Email "fail@test.com" always returns an error (handy for testing error states).
 *
 * Demo shortcut accounts (any password):
 *   | Email                  | Role    | UID | Notes                          |
 *   |------------------------|---------|-----|--------------------------------|
 *   | customer@demo.com      | CUSTOMER| demo_customer | Standard customer   |
 *   | cleaner@demo.com       | CLEANER | p1  | Independent cleaner (Maria)    |
 *   | employed@demo.com      | CLEANER | p2  | Employed by Sparkle Pro (James)|
 *   | company@demo.com       | CLEANER | p5  | Sparkle Pro Cleaning Co.       |
 */
class MockAuthService : IAuthService {

    override suspend fun login(email: String, password: String): Result<AuthResult> {
        delay(600) // simulate network latency

        if (email == "fail@test.com") {
            return Result.Error(Exception("Invalid email or password"))
        }

        // Demo shortcut accounts with predictable UIDs that match seed data
        val (uid, displayName) = when (email.lowercase()) {
            "cleaner@demo.com"  -> "p1" to "Maria Garcia"
            "customer@demo.com" -> "demo_customer" to "Demo Customer"
            "employed@demo.com" -> "p2" to "James Okafor"
            "company@demo.com"  -> "p5" to "Sparkle Pro Cleaning Co."
            else                -> "mock_uid_${email.hashCode()}" to
                    email.substringBefore("@").replaceFirstChar { it.uppercase() }
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
