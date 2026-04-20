@file:Suppress("NewApi") // java.time APIs are handled by core library desugaring (configured in build.gradle.kts)

package com.example.diamonds.data.sync

import com.example.diamonds.common.util.Constants
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.entity.SyncQueueEntity
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.EntityType
import com.example.diamonds.domain.repository.ISyncRepository
import com.example.diamonds.domain.repository.SyncOperation
import com.example.diamonds.domain.repository.SyncOperationType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import kotlin.math.min
import com.example.diamonds.domain.model.SyncStatus as ModelSyncStatus

/**
 * Manages sync queue and orchestrates retries with exponential backoff.
 * Dispatches operations to [IBackendService] based on operation and entity type.
 * Respects user cancellations; automatically retries failures.
 */
class SyncManager(
    db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : ISyncRepository {

    private val syncQueueDao = db.syncQueueDao()
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

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
                error = null,
                serverPayload = null
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

    override suspend fun retryAllFailed(): Result<Unit> {
        return try {
            syncQueueDao.resetAllFailed()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun resolveConflict(operationId: String, useLocal: Boolean): Result<Unit> {
        return try {
            if (useLocal) {
                syncQueueDao.updateStatus(operationId, ModelSyncStatus.PENDING.name)
            } else {
                syncQueueDao.delete(operationId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeFailedOperations(): Flow<List<SyncOperation>> =
        syncQueueDao.observeFailedOperations().map { ops -> ops.map { it.toDomain() } }

    override fun observeConflictOperations(): Flow<List<SyncOperation>> =
        syncQueueDao.observeConflictOperations().map { ops -> ops.map { it.toDomain() } }

    override suspend fun syncNow(): Result<Unit> {
        return try {
            processSyncQueue()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Process all queued operations. Called periodically or on connectivity change.
     */
    suspend fun processSyncQueue() {
        if (!connectivityObserver.isOnline()) return

        val operations = syncQueueDao.getQueuedOperations()
        for (operation in operations) {
            if (operation.status == ModelSyncStatus.CANCELLED.name) {
                syncQueueDao.delete(operation.id)
                continue
            }
            if (operation.status == ModelSyncStatus.PENDING.name ||
                operation.status == ModelSyncStatus.FAILED.name
            ) {
                processOperation(operation)
            }
        }
        syncQueueDao.purgeCancelledOperations()
    }

    /**
     * Process a single operation with exponential backoff.
     */
    private suspend fun processOperation(operation: SyncQueueEntity) {
        if (operation.retryCount > 0 && operation.lastAttemptAt != null) {
            val backoffMinutes = calculateBackoff(operation.retryCount)
            val lastAttempt = LocalDateTime.parse(operation.lastAttemptAt)
            val nextRetry = lastAttempt.plusMinutes(backoffMinutes.toLong())
            if (LocalDateTime.now() < nextRetry) return
        }

        if (operation.retryCount >= Constants.MAX_RETRY_ATTEMPTS) {
            syncQueueDao.updateAfterRetry(
                operation.id,
                ModelSyncStatus.FAILED.name,
                LocalDateTime.now().toString(),
                "Max retry attempts (${Constants.MAX_RETRY_ATTEMPTS}) exceeded"
            )
            return
        }

        try {
            val opType = SyncOperationType.valueOf(operation.operationType)
            val entType = EntityType.valueOf(operation.entityType)
            dispatchToBackend(opType, entType, operation.entityId, operation.payload)

            syncQueueDao.updateStatus(operation.id, ModelSyncStatus.SYNCED.name)
            delay(50)
            syncQueueDao.delete(operation.id)
        } catch (e: ConflictException) {
            syncQueueDao.markConflict(
                operation.id,
                e.serverPayload,
                LocalDateTime.now().toString(),
                "Conflict: ${e.message}"
            )
        } catch (e: Exception) {
            syncQueueDao.updateAfterRetry(
                operation.id,
                ModelSyncStatus.FAILED.name,
                LocalDateTime.now().toString(),
                e.message
            )
        }
    }

    // ── Backend dispatch ────────────────────────────────────────────────────

    private suspend fun dispatchToBackend(
        opType: SyncOperationType,
        entType: EntityType,
        entityId: String,
        payload: String
    ) {
        when (entType) {
            EntityType.BOOKING -> dispatchBooking(opType, entityId, payload)
            EntityType.REVIEW -> dispatchReview(opType, payload)
            EntityType.PAYMENT -> dispatchPayment(opType, payload)
            EntityType.SERVICE -> dispatchService(opType, payload)
            EntityType.PROFILE -> dispatchProfile(opType, payload)
            EntityType.RECURRING_BOOKING -> { /* handled by SubscriptionRepository directly */
            }
            EntityType.SUPPORT_TICKET -> { /* handled by SupportRepository directly */
            }

            EntityType.CLAIM -> { /* handled by SupportRepository directly */
            }
        }
    }

    private suspend fun dispatchBooking(
        opType: SyncOperationType,
        entityId: String,
        payload: String
    ) {
        when (opType) {
            SyncOperationType.CREATE -> {
                val req =
                    json.decodeFromString<com.example.diamonds.data.remote.backend.CreateBookingRequest>(
                        payload
                    )
                backendService.createBooking(req)
            }

            SyncOperationType.UPDATE -> {
                val dto =
                    json.decodeFromString<com.example.diamonds.data.remote.backend.BookingDto>(
                        payload
                    )
                backendService.updateBookingStatus(entityId, dto.status)
            }

            SyncOperationType.CANCEL -> backendService.cancelBooking(entityId)
            SyncOperationType.DELETE -> backendService.cancelBooking(entityId)
        }
    }

    private suspend fun dispatchReview(opType: SyncOperationType, payload: String) {
        when (opType) {
            SyncOperationType.CREATE, SyncOperationType.UPDATE -> {
                val req =
                    json.decodeFromString<com.example.diamonds.data.remote.backend.CreateReviewRequest>(
                        payload
                    )
                backendService.createReview(req)
            }

            else -> { /* Reviews can't be deleted */
            }
        }
    }

    private suspend fun dispatchPayment(opType: SyncOperationType, payload: String) {
        when (opType) {
            SyncOperationType.CREATE -> {
                val req =
                    json.decodeFromString<com.example.diamonds.data.remote.backend.CreatePaymentRequest>(
                        payload
                    )
                backendService.createPayment(req)
            }

            else -> { /* Payments are immutable */
            }
        }
    }

    private suspend fun dispatchService(opType: SyncOperationType, payload: String) {
        when (opType) {
            SyncOperationType.CREATE, SyncOperationType.UPDATE -> {
                val dto =
                    json.decodeFromString<com.example.diamonds.data.remote.backend.ServiceDto>(
                        payload
                    )
                backendService.createService(dto)
            }

            else -> { /* Service deletion not supported */
            }
        }
    }

    private suspend fun dispatchProfile(opType: SyncOperationType, payload: String) {
        when (opType) {
            SyncOperationType.UPDATE -> {
                val dto = json.decodeFromString<com.example.diamonds.data.remote.backend.ClientDto>(
                    payload
                )
                backendService.updateClient(dto)
            }

            else -> { /* Profile create/delete handled by auth flow */
            }
        }
    }

    // ── Backoff ─────────────────────────────────────────────────────────────

    private fun calculateBackoff(retryCount: Int): Int {
        val exponentialBackoff = Constants.INITIAL_BACKOFF_MINUTES * (1 shl retryCount)
        return min(exponentialBackoff, Constants.MAX_BACKOFF_MINUTES)
    }
}

/**
 * Exception thrown when the server returns a conflict (e.g., version mismatch).
 */
class ConflictException(
    message: String,
    val serverPayload: String
) : Exception(message)
