package com.example.diamonds.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.diamonds.common.util.Constants
import com.example.diamonds.data.sync.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager task for syncing queued operations with exponential backoff.
 * Uses @HiltWorker for dependency injection.
 */
@HiltWorker
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
        const val IMMEDIATE_SYNC_WORK_NAME = "immediate_sync_work"

        /**
         * Schedule periodic background sync.
         */
        fun schedulePeriodic(context: Context) {
            val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
                Constants.DEFAULT_SYNC_INTERVAL_MINUTES.toLong(),
                TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
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

        /**
         * Enqueue an immediate one-shot sync (e.g., after connectivity restoration).
         */
        fun enqueueImmediate(context: Context) {
            val immediateWork = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateWork
            )
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_SYNC_WORK_NAME)
        }
    }
}
