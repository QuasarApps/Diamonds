package com.example.diamonds.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore by preferencesDataStore("app_preferences")

/**
 * Manages user session and preferences using DataStore
 */
class PreferencesDataStore(private val context: Context) {
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_DISPLAY_NAME_KEY = stringPreferencesKey("user_display_name")
    }

    /**
     * Observe user session as Flow
     */
    fun observeUserSession(): Flow<UserSession?> =
        context.preferencesDataStore.data.map { prefs ->
            val token = prefs[AUTH_TOKEN_KEY]
            val userId = prefs[USER_ID_KEY]
            val email = prefs[USER_EMAIL_KEY]
            val roleStr = prefs[USER_ROLE_KEY]
            val displayName = prefs[USER_DISPLAY_NAME_KEY]

            if (token != null && userId != null && email != null && roleStr != null) {
                UserSession(
                    userId = userId,
                    email = email,
                    displayName = displayName,
                    role = parseRole(roleStr),
                    authToken = token,
                    isAuthenticated = true
                )
            } else {
                null
            }
        }

    /**
     * Get current session synchronously (for immediate access)
     */
    fun observeCurrentUserSession(): Flow<UserSession?> =
        observeUserSession()

    /**
     * Save user session
     */
    suspend fun saveUserSession(session: UserSession) {
        context.preferencesDataStore.edit { prefs ->
            prefs[AUTH_TOKEN_KEY] = session.authToken
            prefs[USER_ID_KEY] = session.userId
            prefs[USER_EMAIL_KEY] = session.email
            prefs[USER_ROLE_KEY] = session.role.name
            session.displayName?.let { prefs[USER_DISPLAY_NAME_KEY] = it }
        }
    }

    /**
     * Clear user session (logout)
     */
    suspend fun clearUserSession() {
        context.preferencesDataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_ROLE_KEY)
            prefs.remove(USER_DISPLAY_NAME_KEY)
        }
    }

    /**
     * Parse stored role string, handling legacy "CLIENT"/"PROVIDER" values
     * that may exist from before the rename to CUSTOMER/CLEANER.
     */
    private fun parseRole(roleStr: String): UserRole = when (roleStr) {
        "CLIENT", "CUSTOMER" -> UserRole.CUSTOMER
        "PROVIDER", "CLEANER" -> UserRole.CLEANER
        else -> UserRole.CUSTOMER
    }
}
