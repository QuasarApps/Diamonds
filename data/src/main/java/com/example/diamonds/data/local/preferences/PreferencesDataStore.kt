package com.example.diamonds.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore by preferencesDataStore("app_preferences")

class PreferencesDataStore(private val context: Context) {
    companion object {
        private val AUTH_TOKEN_KEY        = stringPreferencesKey("auth_token")
        private val USER_ID_KEY           = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY        = stringPreferencesKey("user_email")
        private val USER_ROLE_KEY         = stringPreferencesKey("user_role")
        private val USER_DISPLAY_NAME_KEY = stringPreferencesKey("user_display_name")
        private val CLEANER_TYPE_KEY      = stringPreferencesKey("cleaner_type")
    }

    fun observeUserSession(): Flow<UserSession?> =
        context.preferencesDataStore.data.map { prefs ->
            val token       = prefs[AUTH_TOKEN_KEY]
            val userId      = prefs[USER_ID_KEY]
            val email       = prefs[USER_EMAIL_KEY]
            val roleStr     = prefs[USER_ROLE_KEY]
            val displayName = prefs[USER_DISPLAY_NAME_KEY]
            val cleanerTypeStr = prefs[CLEANER_TYPE_KEY]

            if (token != null && userId != null && email != null && roleStr != null) {
                UserSession(
                    userId      = userId,
                    email       = email,
                    displayName = displayName,
                    role        = parseRole(roleStr),
                    cleanerType = parseCleanerType(cleanerTypeStr),
                    authToken   = token,
                    isAuthenticated = true
                )
            } else null
        }

    fun observeCurrentUserSession(): Flow<UserSession?> = observeUserSession()

    suspend fun saveUserSession(session: UserSession) {
        context.preferencesDataStore.edit { prefs ->
            prefs[AUTH_TOKEN_KEY]   = session.authToken
            prefs[USER_ID_KEY]      = session.userId
            prefs[USER_EMAIL_KEY]   = session.email
            prefs[USER_ROLE_KEY]    = session.role.name
            prefs[CLEANER_TYPE_KEY] = session.cleanerType.name
            session.displayName?.let { prefs[USER_DISPLAY_NAME_KEY] = it }
        }
    }

    suspend fun clearUserSession() {
        context.preferencesDataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_ROLE_KEY)
            prefs.remove(USER_DISPLAY_NAME_KEY)
            prefs.remove(CLEANER_TYPE_KEY)
        }
    }

    private fun parseRole(roleStr: String): UserRole = when (roleStr) {
        "CLIENT", "CUSTOMER" -> UserRole.CUSTOMER
        "PROVIDER", "CLEANER" -> UserRole.CLEANER
        else -> UserRole.CUSTOMER
    }

    private fun parseCleanerType(str: String?): CleanerType = when (str) {
        "EMPLOYED" -> CleanerType.EMPLOYED
        "COMPANY"  -> CleanerType.COMPANY
        else       -> CleanerType.INDEPENDENT
    }
}
