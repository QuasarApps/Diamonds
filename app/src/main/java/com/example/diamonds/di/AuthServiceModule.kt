package com.example.diamonds.di

import com.example.diamonds.BuildConfig
import com.example.diamonds.data.remote.auth.FirebaseAuthService
import com.example.diamonds.data.remote.auth.IAuthService
import com.example.diamonds.data.remote.auth.MockAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the active [IAuthService] implementation.
 *
 * Switch between mock and Firebase:
 *  - **Debug**   → [BuildConfig.USE_MOCK_AUTH] = true  → [MockAuthService]
 *  - **Release** → [BuildConfig.USE_MOCK_AUTH] = false → [FirebaseAuthService]
 *
 * To switch in debug without a code change, flip the flag in `app/build.gradle.kts`:
 * ```
 * debug { buildConfigField("boolean", "USE_MOCK_AUTH", "false") }
 * ```
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthServiceModule {

    @Singleton
    @Provides
    fun provideAuthService(): IAuthService =
        if (BuildConfig.USE_MOCK_AUTH) MockAuthService() else FirebaseAuthService()
}
