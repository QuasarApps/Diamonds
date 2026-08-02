package com.example.diamonds.data.remote.auth

import com.example.diamonds.domain.model.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Firebase Auth implementation of [IAuthService].
 *
 * ## How to activate
 * 1. Add `google-services.json` to `:app/`.
 * 2. Uncomment `alias(libs.plugins.google.services)` in `:app/build.gradle.kts`.
 * 3. Set `BuildConfig.USE_MOCK_AUTH = false` in `app/build.gradle.kts`.
 *
 * Firebase Auth methods:
 *  - Email/Password sign-in & registration
 *  - ID token refresh (force = true to bypass cache)
 *  - Password reset email
 */
class FirebaseAuthService : IAuthService {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override suspend fun login(email: String, password: String): Result<AuthResult> {
        return try {
            val credential = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()
            val user = credential.user
                ?: return Result.Error(Exception("Login failed — no user returned"))
            val token = user.getIdToken(false).await().token
                ?: return Result.Error(Exception("Could not retrieve ID token"))
            Result.Success(
                AuthResult(
                    uid = user.uid,
                    email = user.email ?: email,
                    displayName = user.displayName,
                    token = token
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Creates the Firebase Auth credential and sets its display name.
     *
     * [phoneNumber] and [role] are intentionally unused here: Firebase Auth has nowhere to put
     * them. They are persisted by `AuthRepository.signup`, which writes the matching
     * `clients`/`providers` profile document immediately after this call returns.
     */
    override suspend fun signup(
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        role: String
    ): Result<AuthResult> {
        return try {
            val credential = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()
            val user = credential.user
                ?: return Result.Error(Exception("Signup failed — no user returned"))

            // Set display name on the Firebase user profile
            user.updateProfile(userProfileChangeRequest { displayName = name }).await()

            val token = user.getIdToken(false).await().token
                ?: return Result.Error(Exception("Could not retrieve ID token"))
            Result.Success(
                AuthResult(
                    uid = user.uid,
                    email = user.email ?: email,
                    displayName = name,
                    token = token
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshToken(currentToken: String): Result<String> {
        return try {
            val token = firebaseAuth.currentUser
                ?.getIdToken(true)?.await()?.token
                ?: return Result.Error(Exception("No user signed in"))
            Result.Success(token)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
