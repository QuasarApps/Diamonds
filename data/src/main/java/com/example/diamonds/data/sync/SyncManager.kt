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
    ): Unit = when (entType) {
        EntityType.BOOKING -> dispatchBooking(opType, entityId, payload)
        EntityType.REVIEW -> dispatchReview(opType, payload)
        EntityType.PAYMENT -> dispatchPayment(opType, payload)
        EntityType.SERVICE -> dispatchService(opType, payload)
        EntityType.PROFILE -> dispatchProfile(opType, payload)
        EntityType.PROVIDER_PROFILE -> dispatchProviderProfile(opType, payload)

        // These entity types are synced by their own repositories and must never
        // reach this queue. Fail loudly rather than returning normally — a silent
        // no-op here would be marked SYNCED and dropped, the exact data-loss shape
        // the Result.Error handling above guards against.
        EntityType.RECURRING_BOOKING,
        EntityType.SUPPORT_TICKET,
        EntityType.CLAIM ->
            throw IllegalStateException(
                "$entType operations are handled by their own repository and must not be queued in SyncManager"
            )
    }

    private suspend fun dispatchBooking(
        opType: SyncOperationType,
        entityId: String,
        payload: String
    ): Unit = when (opType) {
        SyncOperationType.CREATE -> {
            val req =
                json.decodeFromString<com.example.diamonds.data.remote.backend.CreateBookingRequest>(
                    payload
                )
            backendService.createBooking(req).orThrow()
        }

        SyncOperationType.UPDATE -> {
            val dto =
                json.decodeFromString<com.example.diamonds.data.remote.backend.BookingDto>(
                    payload
                )
            backendService.updateBookingStatus(entityId, dto.status).orThrow()
        }

        SyncOperationType.CANCEL -> backendService.cancelBooking(entityId).orThrow()
        SyncOperationType.DELETE -> backendService.cancelBooking(entityId).orThrow()
    }

    private suspend fun dispatchReview(opType: SyncOperationType, payload: String): Unit = when (opType) {
        SyncOperationType.CREATE, SyncOperationType.UPDATE -> {
            val req =
                json.decodeFromString<com.example.diamonds.data.remote.backend.CreateReviewRequest>(
                    payload
                )
            backendService.createReview(req).orThrow()
        }

        // Reviews can't be deleted — no representable write.
        SyncOperationType.DELETE, SyncOperationType.CANCEL ->
            unsupportedOp(EntityType.REVIEW, opType)
    }

    private suspend fun dispatchPayment(opType: SyncOperationType, payload: String): Unit = when (opType) {
        SyncOperationType.CREATE -> {
            val req =
                json.decodeFromString<com.example.diamonds.data.remote.backend.CreatePaymentRequest>(
                    payload
                )
            backendService.createPayment(req).orThrow()
        }

        // A payment status change (e.g. refund via updatePaymentStatus) is a real write,
        // but it isn't wired for queued sync yet — fail loudly rather than dropping it.
        SyncOperationType.UPDATE, SyncOperationType.DELETE, SyncOperationType.CANCEL ->
            unsupportedOp(EntityType.PAYMENT, opType)
    }

    private suspend fun dispatchService(opType: SyncOperationType, payload: String): Unit = when (opType) {
        SyncOperationType.CREATE -> {
            val dto =
                json.decodeFromString<com.example.diamonds.data.remote.backend.ServiceDto>(
                    payload
                )
            backendService.createService(dto).orThrow()
        }

        // UPDATE used to share the CREATE branch and replay through createService, which appends
        // a second service instead of editing the existing one — a queued price change would have
        // duplicated the listing. IBackendService.updateService now exists precisely so the two
        // are distinguishable here.
        SyncOperationType.UPDATE -> {
            val dto =
                json.decodeFromString<com.example.diamonds.data.remote.backend.ServiceDto>(
                    payload
                )
            backendService.updateService(dto).orThrow()
        }

        // Service deletion is not supported — no representable write.
        SyncOperationType.DELETE, SyncOperationType.CANCEL ->
            unsupportedOp(EntityType.SERVICE, opType)
    }

    private suspend fun dispatchProfile(opType: SyncOperationType, payload: String): Unit = when (opType) {
        SyncOperationType.UPDATE -> {
            val dto = json.decodeFromString<com.example.diamonds.data.remote.backend.ClientDto>(
                payload
            )
            backendService.updateClient(dto).orThrow()
        }

        // Profile create/delete are handled by the auth flow, not this queue.
        SyncOperationType.CREATE, SyncOperationType.DELETE, SyncOperationType.CANCEL ->
            unsupportedOp(EntityType.PROFILE, opType)
    }

    /**
     * Provider profiles route here rather than through [dispatchProfile].
     *
     * `json` is configured with `ignoreUnknownKeys = true`, so a `ProviderDto` payload decodes
     * *successfully* as a `ClientDto` — silently discarding `bio`, `rating`, `serviceRadius`,
     * `specializations` and the employer fields, then writing that truncated record to the
     * `clients` collection. Keeping the entity types distinct is what prevents that.
     */
    private suspend fun dispatchProviderProfile(opType: SyncOperationType, payload: String): Unit = when (opType) {
        SyncOperationType.UPDATE -> {
            val dto = json.decodeFromString<com.example.diamonds.data.remote.backend.ProviderDto>(
                payload
            )
            backendService.updateProvider(dto).orThrow()
        }

        SyncOperationType.CREATE, SyncOperationType.DELETE, SyncOperationType.CANCEL ->
            unsupportedOp(EntityType.PROVIDER_PROFILE, opType)
    }

    /**
     * Throw when a backend [Result] is a failure so [processOperation]'s catch marks the
     * operation FAILED (or CONFLICT). Backends report errors by returning [Result.Error]
     * rather than throwing, so without this a rejected write would be silently treated as
     * SYNCED and dropped. Returns [Unit] — dispatch discards the success payload.
     */
    private fun Result<*>.orThrow(): Unit = when (this) {
        is Result.Success -> Unit
        is Result.Error -> throw exception
        is Result.Loading -> throw IllegalStateException("Backend returned Loading during sync dispatch")
    }

    /**
     * A queue entry whose (entity, operation) pair maps to no representable backend write.
     * These are never enqueued today; failing loudly (surfaced as FAILED via [processOperation]'s
     * catch) keeps a future routing/serialization mistake from being silently marked SYNCED and
     * dropped — the same guarantee the entity-type guard in [dispatchToBackend] provides.
     */
    private fun unsupportedOp(entType: EntityType, opType: SyncOperationType): Nothing =
        throw IllegalStateException(
            "$opType is not a representable sync operation for $entType and must not be queued"
        )

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
