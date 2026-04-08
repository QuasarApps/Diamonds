@file:Suppress("NewApi") // java.time APIs are handled by core library desugaring (configured in build.gradle.kts)

package com.example.diamonds.data.sync

import com.example.diamonds.common.util.Constants
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.entity.SyncQueueEntity
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.ISyncRepository
import com.example.diamonds.domain.repository.SyncOperation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import kotlin.math.min
import com.example.diamonds.domain.model.SyncStatus as ModelSyncStatus

/**
 * Manages sync queue and orchestrates retries with exponential backoff
 * Respects user cancellations; automatically retries failures
 */
class SyncManager(
    db: AppDatabase,
    @Suppress("unused") private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : ISyncRepository {

    private val syncQueueDao = db.syncQueueDao()

    override suspend fun queueOperation(operation: SyncOperation): Result<Unit> {
        return try {
            val entity = SyncQueueEntity(
                id = operation.id,
                operationType = operation.operationType.name,
                entityType = operation.entityType.name,
                entityId = operation.entityId,
                payload = operation.payload,
                status = ModelSyncStatus.PENDING.name,
                retryCount = 0,
                createdAt = LocalDateTime.now().toString(),
                lastAttemptAt = null,
                error = null
            )
            syncQueueDao.insert(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getSyncQueue(): Result<List<SyncOperation>> {
        return try {
            val operations = syncQueueDao.getQueuedOperations()
            Result.Success(operations.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeSyncOperation(operationId: String): Result<Unit> {
        return try {
            syncQueueDao.delete(operationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun cancelSyncOperation(operationId: String): Result<Unit> {
        return try {
            syncQueueDao.updateStatus(operationId, ModelSyncStatus.CANCELLED.name)
            // Immediately remove cancelled operations (don't retry)
            delay(100)
            syncQueueDao.delete(operationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeSyncQueue(): Flow<List<SyncOperation>> =
        syncQueueDao.observeQueuedOperations().map { operations ->
            operations.map { it.toDomain() }
        }

    override fun observePendingOperationCount(): Flow<Int> =
        syncQueueDao.observePendingCount()

    override suspend fun retryFailedOperation(operationId: String): Result<Unit> {
        return try {
            syncQueueDao.updateStatus(operationId, ModelSyncStatus.PENDING.name)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Process all queued operations. Called periodically or on connectivity change
     */
    suspend fun processSyncQueue() {
        if (!connectivityObserver.isOnline()) {
            return // Don't process if offline
        }

        val operations = syncQueueDao.getQueuedOperations()
        for (operation in operations) {
            if (operation.status == ModelSyncStatus.CANCELLED.name) {
                syncQueueDao.delete(operation.id)
                continue
            }

            if (operation.status == ModelSyncStatus.PENDING.name || operation.status == ModelSyncStatus.FAILED.name) {
                processOperation(operation)
            }
        }

        // Cleanup old cancelled operations
        syncQueueDao.purgeCancelledOperations()
    }

    /**
     * Process a single operation with exponential backoff
     */
    private suspend fun processOperation(operation: SyncQueueEntity) {
        // Check if we should retry based on backoff
        if (operation.retryCount > 0 && operation.lastAttemptAt != null) {
            val backoffMinutes = calculateBackoff(operation.retryCount)
            val lastAttempt = LocalDateTime.parse(operation.lastAttemptAt)
            val nextRetry = lastAttempt.plusMinutes(backoffMinutes.toLong())
            if (LocalDateTime.now() < nextRetry) {
                return // Not time to retry yet
            }
        }

        try {
            // TODO: Send operation to backend based on operationType and entityType
            // For now, this is a placeholder - actual implementation depends on
            // specific operation type and backend service methods

            // On success:
            syncQueueDao.updateStatus(operation.id, ModelSyncStatus.SYNCED.name)
            delay(100)
            syncQueueDao.delete(operation.id)
        } catch (e: Exception) {
            // Mark for retry (or permanently failed if max retries exceeded)
            syncQueueDao.updateAfterRetry(
                operation.id,
                ModelSyncStatus.FAILED.name,
                LocalDateTime.now().toString(),
                e.message
            )
        }
    }

    /**
     * Calculate exponential backoff: 1 min, 2 min, 4 min, 8 min, etc. (capped at 60 min)
     */
    private fun calculateBackoff(retryCount: Int): Int {
        val exponentialBackoff = Constants.INITIAL_BACKOFF_MINUTES * (1 shl retryCount)
        return min(exponentialBackoff, Constants.MAX_BACKOFF_MINUTES)
    }
}
