package com.example.diamonds.di

import com.example.diamonds.data.remote.auth.IAuthService
import com.example.diamonds.data.remote.auth.MockAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces [AuthServiceModule] for instrumentation tests.
 * Always uses [MockAuthService] so login/signup work without Firebase.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AuthServiceModule::class]
)
object FakeAuthServiceModule {

    @Singleton
    @Provides
    fun provideMockAuthService(): IAuthService = MockAuthService()
}
