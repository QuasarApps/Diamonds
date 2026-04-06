package com.example.diamonds.data.remote.auth

import com.example.diamonds.domain.model.Result

/**
 * Firebase Auth implementation of [IAuthService].
 *
 * ## How to activate
 * 1. Add the Firebase BOM + `firebase-auth-ktx` to `:data/build.gradle.kts`:
 *    ```
 *    implementation(platform("com.google.firebase:firebase-bom:33.x.x"))
 *    implementation("com.google.firebase:firebase-auth-ktx")
 *    ```
 * 2. Add `google-services.json` to `:app/`.
 * 3. Apply the `com.google.gms.google-services` plugin in `:app/build.gradle.kts`.
 * 4. In [AuthServiceModule], change the binding from [MockAuthService] to
 *    [FirebaseAuthService].
 *
 * The commented-out body below shows the full intended implementation so
 * nothing is forgotten when the time comes.
 */
class FirebaseAuthService : IAuthService {

    // Uncomment and fill in once Firebase is added:
    //
    // private val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
    //
    // override suspend fun login(email: String, password: String): Result<AuthResult> {
    //     return try {
    //         val credential = firebaseAuth
    //             .signInWithEmailAndPassword(email, password)
    //             .await()
    //         val user = credential.user ?: return Result.Error(Exception("Login failed"))
    //         val token = user.getIdToken(false).await().token
    //             ?: return Result.Error(Exception("Could not retrieve token"))
    //         Result.Success(AuthResult(uid = user.uid, email = user.email ?: email,
    //             displayName = user.displayName, token = token))
    //     } catch (e: Exception) { Result.Error(e) }
    // }
    //
    // override suspend fun signup(name, email, password, phoneNumber, role): Result<AuthResult> {
    //     return try {
    //         val credential = firebaseAuth
    //             .createUserWithEmailAndPassword(email, password)
    //             .await()
    //         val user = credential.user ?: return Result.Error(Exception("Signup failed"))
    //         user.updateProfile(userProfileChangeRequest { displayName = name }).await()
    //         val token = user.getIdToken(false).await().token
    //             ?: return Result.Error(Exception("Could not retrieve token"))
    //         Result.Success(AuthResult(uid = user.uid, email = user.email ?: email,
    //             displayName = name, token = token))
    //     } catch (e: Exception) { Result.Error(e) }
    // }
    //
    // override suspend fun logout(): Result<Unit> = try {
    //     firebaseAuth.signOut(); Result.Success(Unit)
    // } catch (e: Exception) { Result.Error(e) }
    //
    // override suspend fun refreshToken(currentToken: String): Result<String> {
    //     return try {
    //         val token = firebaseAuth.currentUser
    //             ?.getIdToken(true)?.await()?.token
    //             ?: return Result.Error(Exception("No user signed in"))
    //         Result.Success(token)
    //     } catch (e: Exception) { Result.Error(e) }
    // }
    //
    // override suspend fun sendPasswordReset(email: String): Result<Unit> = try {
    //     firebaseAuth.sendPasswordResetEmail(email).await(); Result.Success(Unit)
    // } catch (e: Exception) { Result.Error(e) }

    // -------------------------------------------------------------------------
    // Stub body — keeps this file compilable before Firebase SDK is added.
    // Remove these once the real implementation above is uncommented.
    // -------------------------------------------------------------------------

    override suspend fun login(email: String, password: String): Result<AuthResult> =
        Result.Error(Exception("FirebaseAuthService not yet configured"))

    override suspend fun signup(
        name: String, email: String, password: String,
        phoneNumber: String, role: String
    ): Result<AuthResult> =
        Result.Error(Exception("FirebaseAuthService not yet configured"))

    override suspend fun logout(): Result<Unit> =
        Result.Error(Exception("FirebaseAuthService not yet configured"))

    override suspend fun refreshToken(currentToken: String): Result<String> =
        Result.Error(Exception("FirebaseAuthService not yet configured"))

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        Result.Error(Exception("FirebaseAuthService not yet configured"))
}
