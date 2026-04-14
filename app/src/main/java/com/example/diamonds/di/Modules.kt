package com.example.diamonds.di

import android.content.Context
import androidx.work.WorkManager
import com.example.diamonds.BuildConfig
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.remote.auth.IAuthService
import com.example.diamonds.data.remote.backend.BackendServiceStub
import com.example.diamonds.data.remote.backend.FirebaseBackendService
import com.example.diamonds.data.remote.backend.FirestoreBookingListener
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.repository.AuthRepository
import com.example.diamonds.data.repository.BookingRepository
import com.example.diamonds.data.repository.ClientRepository
import com.example.diamonds.data.repository.LocationRepository
import com.example.diamonds.data.repository.MessageRepository
import com.example.diamonds.data.repository.NotificationRepository
import com.example.diamonds.data.repository.PaymentRepository
import com.example.diamonds.data.repository.ProviderRepository
import com.example.diamonds.data.repository.ReviewRepository
import com.example.diamonds.data.repository.ServiceRepository
import com.example.diamonds.data.sync.ConnectivitySyncTrigger
import com.example.diamonds.data.sync.SyncManager
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
        return if (BuildConfig.USE_MOCK_BACKEND) {
            BackendServiceStub()
        } else {
            FirebaseBackendService()
        }
    }

    @Singleton
    @Provides
    fun provideFirestoreBookingListener(): FirestoreBookingListener {
        return FirestoreBookingListener()
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

    @Singleton
    @Provides
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Singleton
    @Provides
    fun provideConnectivitySyncTrigger(
        connectivityObserver: ConnectivityObserver,
        syncManager: SyncManager
    ): ConnectivitySyncTrigger {
        return ConnectivitySyncTrigger(connectivityObserver, syncManager)
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

    @Singleton
    @Provides
    fun provideNotificationRepository(
        db: AppDatabase,
        preferencesDataStore: PreferencesDataStore
    ): INotificationRepository {
        return NotificationRepository(db, preferencesDataStore)
    }

    @Singleton
    @Provides
    fun provideLocationRepository(
        db: AppDatabase,
        backendService: IBackendService,
        connectivityObserver: ConnectivityObserver,
        fusedLocationClient: FusedLocationProviderClient
    ): ILocationRepository {
        return LocationRepository(db, backendService, connectivityObserver, fusedLocationClient)
    }

    @Singleton
    @Provides
    fun provideMessageRepository(
        db: AppDatabase,
        backendService: IBackendService,
        connectivityObserver: ConnectivityObserver
    ): IMessageRepository {
        return MessageRepository(db, backendService, connectivityObserver)
    }
}
