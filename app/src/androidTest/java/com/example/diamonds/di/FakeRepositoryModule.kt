package com.example.diamonds.di

import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.IClientRepository
import com.example.diamonds.domain.repository.ILocationRepository
import com.example.diamonds.domain.repository.IMessageRepository
import com.example.diamonds.domain.repository.INotificationRepository
import com.example.diamonds.domain.repository.IPaymentRepository
import com.example.diamonds.domain.repository.IProviderRepository
import com.example.diamonds.domain.repository.IReviewRepository
import com.example.diamonds.domain.repository.IServiceRepository
import com.example.diamonds.domain.repository.ISyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces [RepositoryModule] for all instrumentation tests.
 * Provides lightweight, in-memory fake repositories so tests run
 * fully offline without real network/database calls.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object FakeRepositoryModule {

    @Singleton
    @Provides
    fun provideFakeAuthRepository(): IAuthRepository = FakeAuthRepository()

    @Singleton
    @Provides
    fun provideFakeBookingRepository(): IBookingRepository = FakeBookingRepository()

    @Singleton
    @Provides
    fun provideFakeProviderRepository(): IProviderRepository = FakeProviderRepository()

    @Singleton
    @Provides
    fun provideFakeServiceRepository(): IServiceRepository = FakeServiceRepository()

    @Singleton
    @Provides
    fun provideFakeClientRepository(): IClientRepository = FakeClientRepository()

    @Singleton
    @Provides
    fun provideFakePaymentRepository(): IPaymentRepository = FakePaymentRepository()

    @Singleton
    @Provides
    fun provideFakeReviewRepository(): IReviewRepository = FakeReviewRepository()

    @Singleton
    @Provides
    fun provideFakeNotificationRepository(): INotificationRepository = FakeNotificationRepository()

    @Singleton
    @Provides
    fun provideFakeLocationRepository(): ILocationRepository = FakeLocationRepository()

    @Singleton
    @Provides
    fun provideFakeSyncRepository(): ISyncRepository = FakeSyncRepository()

    @Singleton
    @Provides
    fun provideFakeMessageRepository(): IMessageRepository = FakeMessageRepository()
}
