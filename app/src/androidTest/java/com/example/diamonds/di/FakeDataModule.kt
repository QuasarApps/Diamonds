package com.example.diamonds.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.remote.backend.BackendServiceStub
import com.example.diamonds.data.remote.backend.FirestoreBookingListener
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.sync.SyncManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces [DataModule] for instrumentation tests.
 * Uses an in-memory Room database and the stub backend so tests
 * run without a real Firebase / network connection.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class]
)
object FakeDataModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext context: Context): PreferencesDataStore =
        PreferencesDataStore(context)

    @Singleton
    @Provides
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver =
        ConnectivityObserver(context)

    @Singleton
    @Provides
    fun provideBackendService(): IBackendService = BackendServiceStub()

    @Singleton
    @Provides
    fun provideFirestoreBookingListener(): FirestoreBookingListener = FirestoreBookingListener()

    @Singleton
    @Provides
    fun provideSyncManager(
        db: AppDatabase,
        backendService: IBackendService,
        connectivityObserver: ConnectivityObserver
    ): SyncManager = SyncManager(db, backendService, connectivityObserver)

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Singleton
    @Provides
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
}
