package com.example.diamonds.di

import android.content.Context
import androidx.work.WorkManager
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.remote.auth.IAuthService
import com.example.diamonds.data.remote.backend.BackendServiceStub
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.repository.*
import com.example.diamonds.data.sync.SyncManager
import com.example.diamonds.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext context: Context): PreferencesDataStore {
        return PreferencesDataStore(context)
    }

    @Singleton
    @Provides
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return ConnectivityObserver(context)
    }

    @Singleton
    @Provides
    fun provideBackendService(): IBackendService {
        // TODO: Replace with real Firebase or REST implementation
        return BackendServiceStub()
    }

    @Singleton
    @Provides
    fun provideSyncManager(
        db: AppDatabase,
        backendService: IBackendService,
        connectivityObserver: ConnectivityObserver
    ): SyncManager {
        return SyncManager(db, backendService, connectivityObserver)
    }

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideAuthRepository(
        authService: IAuthService,
        preferencesDataStore: PreferencesDataStore
    ): IAuthRepository {
        return AuthRepository(authService, preferencesDataStore)
    }

    @Singleton
    @Provides
    fun provideClientRepository(
        db: AppDatabase,
        backendService: IBackendService,
        connectivityObserver: ConnectivityObserver
    ): IClientRepository {
        return ClientRepository(db, backendService, connectivityObserver)
    }

    @Singleton
    @Provides
    fun provideProviderRepository(
        db: AppDatabase,
        backendService: IBackendService,
        connectivityObserver: ConnectivityObserver
    ): IProviderRepository {
        return ProviderRepository(db, backendService, connectivityObserver)
    }

    @Singleton
    @Provides
    fun provideServiceRepository(
        db: AppDatabase,
        backendService: IBackendService,
        connectivityObserver: ConnectivityObserver
    ): IServiceRepository {
        return ServiceRepository(db, backendService, connectivityObserver)
    }

    @Singleton
    @Provides
    fun provideBookingRepository(
        db: AppDatabase,
        backendService: IBackendService,
        connectivityObserver: ConnectivityObserver
    ): IBookingRepository {
        return BookingRepository(db, backendService, connectivityObserver)
    }

    @Singleton
    @Provides
    fun provideReviewRepository(
        db: AppDatabase,
        backendService: IBackendService,
        connectivityObserver: ConnectivityObserver
    ): IReviewRepository {
        return ReviewRepository(db, backendService, connectivityObserver)
    }

    @Singleton
    @Provides
    fun providePaymentRepository(
        db: AppDatabase,
        backendService: IBackendService,
        connectivityObserver: ConnectivityObserver
    ): IPaymentRepository {
        return PaymentRepository(db, backendService, connectivityObserver)
    }

    @Singleton
    @Provides
    fun provideSyncRepository(syncManager: SyncManager): ISyncRepository {
        return syncManager
    }
}
