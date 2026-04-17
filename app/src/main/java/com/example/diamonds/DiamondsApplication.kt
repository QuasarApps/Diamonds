package com.example.diamonds

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.sync.ConnectivitySyncTrigger
import com.example.diamonds.data.sync.SyncManager
import com.example.diamonds.data.worker.RecurringBookingWorker
import com.example.diamonds.data.worker.SyncWorker
import com.example.diamonds.fcm.DiamondsFcmService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class DiamondsApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    @Inject
    lateinit var syncManager: SyncManager
    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Create notification channels for Android 8+ (no-op on older versions)
        DiamondsFcmService.createNotificationChannels(this)

        // Schedule periodic background sync
        SyncWorker.schedulePeriodic(this)

        // Start connectivity-triggered sync
        val connectivitySyncTrigger = ConnectivitySyncTrigger(connectivityObserver, syncManager)
        connectivitySyncTrigger.start(applicationScope)

        // Schedule daily recurring booking generation
        val recurringWork = PeriodicWorkRequestBuilder<RecurringBookingWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "recurring_booking_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            recurringWork
        )
    }
}
