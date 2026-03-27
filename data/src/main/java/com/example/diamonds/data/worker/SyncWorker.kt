package com.example.diamonds.data.worker

import android.content.Context
import androidx.work.*
import com.example.diamonds.common.util.Constants
import com.example.diamonds.data.sync.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager task for syncing queued operations with exponential backoff
 */
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            syncManager.processSyncQueue()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < Constants.MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val SYNC_WORK_NAME = "sync_operations_work"

        fun scheduleSyncWork(context: Context) {
            val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
                Constants.DEFAULT_SYNC_INTERVAL_MINUTES.toLong(),
                TimeUnit.MINUTES
            )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    Constants.INITIAL_BACKOFF_MINUTES.toLong(),
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWork
            )
        }

        fun cancelSyncWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        }
    }
}

/**
 * Factory for creating SyncWorker with Hilt injection
 */
class SyncWorkerFactory @AssistedInject constructor(
    private val syncManager: SyncManager
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return if (workerClassName == SyncWorker::class.java.name) {
            SyncWorker(appContext, workerParameters, syncManager)
        } else {
            null
        }
    }
}
