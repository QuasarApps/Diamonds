package com.example.diamonds.data.sync

import com.example.diamonds.common.util.Constants
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.SyncQueueDao
import com.example.diamonds.data.local.entity.SyncQueueEntity
import com.example.diamonds.data.remote.backend.BookingDto
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.ProviderDto
import com.example.diamonds.data.remote.backend.ServiceDto
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.repository.EntityType
import com.example.diamonds.domain.repository.SyncOperation
import com.example.diamonds.domain.repository.SyncOperationType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SyncManager]'s queue management and dispatch orchestration.
 *
 * The DAO / backend / connectivity collaborators are mocked; the tests exercise the
 * real queue CRUD, conflict resolution, and [SyncManager.processSyncQueue] control flow.
 * The `BOOKING`/`CANCEL` dispatch path is used for the processing cases because it calls
 * the backend directly without JSON payload decoding, keeping the operation-processing
 * assertions focused on the retry/success/failure/conflict transitions.
 */
class SyncManagerTest {

    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var backendService: IBackendService
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var manager: SyncManager

    @Before
    fun setUp() {
        syncQueueDao = mockk(relaxed = true)
        val db = mockk<AppDatabase> { every { syncQueueDao() } returns syncQueueDao }
        backendService = mockk(relaxed = true)
        connectivityObserver = mockk { every { isOnline() } returns true }
        manager = SyncManager(db, backendService, connectivityObserver)
    }

    private fun makeOperation(
        id: String = "op1",
        operationType: SyncOperationType = SyncOperationType.CREATE,
        entityType: EntityType = EntityType.BOOKING,
        entityId: String = "b1",
        payload: String = "{}"
    ) = SyncOperation(
        id = id,
        operationType = operationType,
        entityType = entityType,
        entityId = entityId,
        payload = payload,
        createdAt = "2026-04-01T10:00:00"
    )

    // The entity stores enum names as strings; take typed enums and convert via `.name`
    // so the tests don't hardcode raw enum names that could silently drift.
    private fun makeEntity(
        id: String = "op1",
        operationType: SyncOperationType = SyncOperationType.CREATE,
        entityType: EntityType = EntityType.BOOKING,
        entityId: String = "b1",
        payload: String = "{}",
        status: SyncStatus = SyncStatus.PENDING,
        retryCount: Int = 0,
        lastAttemptAt: String? = null
    ) = SyncQueueEntity(
        id = id,
        operationType = operationType.name,
        entityType = entityType.name,
        entityId = entityId,
        payload = payload,
        status = status.name,
        retryCount = retryCount,
        createdAt = "2026-04-01T10:00:00",
        lastAttemptAt = lastAttemptAt,
        error = null,
        serverPayload = null
    )

    // ── queueOperation ────────────────────────────────────────────────────────

    @Test
    fun `queueOperation inserts a fresh PENDING entity`() = runTest {
        val result = manager.queueOperation(makeOperation(id = "op1"))

        assertTrue(result is Result.Success)
        coVerify {
            syncQueueDao.insert(
                match { it.id == "op1" && it.status == SyncStatus.PENDING.name && it.retryCount == 0 }
            )
        }
    }

    @Test
    fun `queueOperation returns error when the dao throws`() = runTest {
        coEvery { syncQueueDao.insert(any()) } throws RuntimeException("db fail")

        val result = manager.queueOperation(makeOperation())

        assertTrue(result is Result.Error)
    }

    // ── read / remove / cancel ────────────────────────────────────────────────

    @Test
    fun `getSyncQueue maps entities to domain operations`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns listOf(makeEntity(id = "op1"))

        val result = manager.getSyncQueue()

        assertTrue(result is Result.Success)
        val ops = (result as Result.Success).data
        assertEquals(1, ops.size)
        assertEquals("op1", ops.first().id)
        assertEquals(SyncOperationType.CREATE, ops.first().operationType)
    }

    @Test
    fun `removeSyncOperation deletes by id`() = runTest {
        val result = manager.removeSyncOperation("op1")

        assertTrue(result is Result.Success)
        coVerify { syncQueueDao.delete("op1") }
    }

    @Test
    fun `cancelSyncOperation marks cancelled then deletes`() = runTest {
        val result = manager.cancelSyncOperation("op1")

        assertTrue(result is Result.Success)
        coVerifyOrder {
            syncQueueDao.updateStatus("op1", SyncStatus.CANCELLED.name)
            syncQueueDao.delete("op1")
        }
    }

    // ── retry / conflict resolution ───────────────────────────────────────────

    @Test
    fun `retryFailedOperation requeues as pending`() = runTest {
        manager.retryFailedOperation("op1")
        coVerify { syncQueueDao.updateStatus("op1", SyncStatus.PENDING.name) }
    }

    @Test
    fun `retryAllFailed resets every failed operation`() = runTest {
        manager.retryAllFailed()
        coVerify { syncQueueDao.resetAllFailed() }
    }

    @Test
    fun `resolveConflict keeping local requeues without deleting`() = runTest {
        manager.resolveConflict("op1", useLocal = true)

        coVerify { syncQueueDao.updateStatus("op1", SyncStatus.PENDING.name) }
        coVerify(exactly = 0) { syncQueueDao.delete("op1") }
    }

    @Test
    fun `resolveConflict using server drops the operation`() = runTest {
        manager.resolveConflict("op1", useLocal = false)

        coVerify { syncQueueDao.delete("op1") }
        coVerify(exactly = 0) { syncQueueDao.updateStatus("op1", any()) }
    }

    // ── observers ─────────────────────────────────────────────────────────────

    @Test
    fun `observePendingOperationCount emits the dao count`() = runTest {
        every { syncQueueDao.observePendingCount() } returns flowOf(4)
        assertEquals(4, manager.observePendingOperationCount().first())
    }

    @Test
    fun `observeSyncQueue maps entities to domain operations`() = runTest {
        every { syncQueueDao.observeQueuedOperations() } returns flowOf(listOf(makeEntity(id = "op1")))

        val ops = manager.observeSyncQueue().first()

        assertEquals(1, ops.size)
        assertEquals("op1", ops.first().id)
    }

    // ── processSyncQueue ──────────────────────────────────────────────────────

    @Test
    fun `processSyncQueue does nothing when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        manager.processSyncQueue()

        coVerify(exactly = 0) { syncQueueDao.getQueuedOperations() }
    }

    @Test
    fun `syncNow processes the queue and purges cancelled operations`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns emptyList()

        val result = manager.syncNow()

        assertTrue(result is Result.Success)
        coVerify { syncQueueDao.getQueuedOperations() }
        coVerify { syncQueueDao.purgeCancelledOperations() }
    }

    @Test
    fun `processSyncQueue deletes cancelled operations without dispatching`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(makeEntity(id = "op1", status = SyncStatus.CANCELLED))

        manager.processSyncQueue()

        coVerify { syncQueueDao.delete("op1") }
        coVerify(exactly = 0) { backendService.cancelBooking(any()) }
    }

    @Test
    fun `processSyncQueue dispatches a pending booking cancel then marks it synced`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(makeEntity(id = "op1", operationType = SyncOperationType.CANCEL))
        coEvery { backendService.cancelBooking("b1") } returns Result.Success(mockk<BookingDto>(relaxed = true))

        manager.processSyncQueue()

        coVerify { backendService.cancelBooking("b1") }
        coVerify { syncQueueDao.updateStatus("op1", SyncStatus.SYNCED.name) }
        coVerify { syncQueueDao.delete("op1") }
    }

    @Test
    fun `processSyncQueue records a failure when dispatch throws`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(makeEntity(id = "op1", operationType = SyncOperationType.CANCEL))
        coEvery { backendService.cancelBooking("b1") } throws RuntimeException("boom")

        manager.processSyncQueue()

        coVerify { syncQueueDao.updateAfterRetry("op1", SyncStatus.FAILED.name, any(), any()) }
    }

    @Test
    fun `processSyncQueue records a failure when the backend returns Result Error`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(makeEntity(id = "op1", operationType = SyncOperationType.CANCEL))
        // Backends report errors by returning Result.Error rather than throwing.
        coEvery { backendService.cancelBooking("b1") } returns Result.Error(RuntimeException("server rejected"))

        manager.processSyncQueue()

        // Must be treated as a failure — not silently marked SYNCED and dropped.
        coVerify { syncQueueDao.updateAfterRetry("op1", SyncStatus.FAILED.name, any(), any()) }
        coVerify(exactly = 0) { syncQueueDao.updateStatus("op1", SyncStatus.SYNCED.name) }
    }

    @Test
    fun `processSyncQueue marks a conflict when the backend reports one`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(makeEntity(id = "op1", operationType = SyncOperationType.CANCEL))
        coEvery { backendService.cancelBooking("b1") } throws
                ConflictException("version mismatch", "{\"server\":true}")

        manager.processSyncQueue()

        coVerify { syncQueueDao.markConflict("op1", "{\"server\":true}", any(), any()) }
    }

    @Test
    fun `processSyncQueue fails operations past the retry limit without dispatching`() = runTest {
        // retryCount == Constants.MAX_RETRY_ATTEMPTS; lastAttemptAt null so the backoff wait is skipped.
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(
                    makeEntity(
                        id = "op1", operationType = SyncOperationType.CANCEL,
                        retryCount = Constants.MAX_RETRY_ATTEMPTS
                    )
                )

        manager.processSyncQueue()

        coVerify(exactly = 0) { backendService.cancelBooking(any()) }
        coVerify { syncQueueDao.updateAfterRetry("op1", SyncStatus.FAILED.name, any(), any()) }
    }

    @Test
    fun `processSyncQueue fails an unroutable entity type instead of marking it synced`() = runTest {
        // CLAIM (like RECURRING_BOOKING / SUPPORT_TICKET) is synced by its own repository and
        // must never reach this queue — if one does, it must surface as FAILED, not be silently
        // marked SYNCED and dropped.
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(makeEntity(id = "op1", operationType = SyncOperationType.CREATE, entityType = EntityType.CLAIM))

        manager.processSyncQueue()

        coVerify { syncQueueDao.updateAfterRetry("op1", SyncStatus.FAILED.name, any(), any()) }
        coVerify(exactly = 0) { syncQueueDao.updateStatus("op1", SyncStatus.SYNCED.name) }
    }

    @Test
    fun `processSyncQueue fails non-representable operations instead of marking them synced`() = runTest {
        // (entity, op) pairs that map to no backend write. None are enqueued today, but each must
        // fail loudly rather than being silently marked SYNCED and dropped — notably PAYMENT/UPDATE,
        // which becomes a real write (refund via updatePaymentStatus) once payment sync is wired.
        val nonRepresentable = listOf(
            EntityType.PAYMENT to SyncOperationType.UPDATE,
            EntityType.REVIEW to SyncOperationType.DELETE,
            EntityType.SERVICE to SyncOperationType.DELETE,
            EntityType.PROFILE to SyncOperationType.CREATE,
            EntityType.PROVIDER_PROFILE to SyncOperationType.CREATE
        )

        nonRepresentable.forEachIndexed { index, (entity, op) ->
            val id = "op$index"
            coEvery { syncQueueDao.getQueuedOperations() } returns
                    listOf(makeEntity(id = id, operationType = op, entityType = entity))

            manager.processSyncQueue()

            coVerify(exactly = 1) { syncQueueDao.updateAfterRetry(id, SyncStatus.FAILED.name, any(), any()) }
            coVerify(exactly = 0) { syncQueueDao.updateStatus(id, SyncStatus.SYNCED.name) }
        }
    }

    // ── Dispatch routing ──────────────────────────────────────────────────────

    @Test
    fun `a queued service update replays as an update, not a second create`() = runTest {
        // SERVICE/UPDATE used to share the CREATE branch and replay through createService, which
        // appends rather than edits — a queued price change would have duplicated the listing.
        val dto = ServiceDto(
            id = "s1", providerId = "p1", title = "Deep Clean", basePrice = 149.0,
            duration = 240, category = "DEEP_CLEANING",
            createdAt = "2026-04-01", updatedAt = "2026-04-07"
        )
        coEvery { backendService.updateService(any()) } returns Result.Success(dto)
        coEvery { syncQueueDao.getQueuedOperations() } returns listOf(
            makeEntity(
                operationType = SyncOperationType.UPDATE,
                entityType = EntityType.SERVICE,
                entityId = "s1",
                payload = Json.encodeToString(dto)
            )
        )

        manager.processSyncQueue()

        coVerify(exactly = 1) { backendService.updateService(match { it.id == "s1" }) }
        coVerify(exactly = 0) { backendService.createService(any()) }
        coVerify { syncQueueDao.updateStatus("op1", SyncStatus.SYNCED.name) }
    }

    @Test
    fun `a queued provider profile update keeps the provider-only fields`() = runTest {
        // Routed through PROFILE it would decode as a ClientDto — `ignoreUnknownKeys = true` makes
        // that succeed — and write a truncated record to the clients collection.
        val dto = ProviderDto(
            id = "p1", name = "Maria", email = "m@c.com", phoneNumber = "+1",
            bio = "Ten years of deep cleans", rating = 4.9f, reviewCount = 143,
            verificationStatus = "APPROVED", serviceRadius = 15, cleanerType = "EMPLOYED",
            employerId = "co1", specializations = listOf("DEEP_CLEAN"),
            createdAt = "2023-01-01", updatedAt = "2026-01-01"
        )
        coEvery { backendService.updateProvider(any()) } returns Result.Success(dto)
        coEvery { syncQueueDao.getQueuedOperations() } returns listOf(
            makeEntity(
                operationType = SyncOperationType.UPDATE,
                entityType = EntityType.PROVIDER_PROFILE,
                entityId = "p1",
                payload = Json.encodeToString(dto)
            )
        )

        manager.processSyncQueue()

        coVerify(exactly = 1) {
            backendService.updateProvider(
                match {
                    it.id == "p1" && it.bio == "Ten years of deep cleans" &&
                        it.serviceRadius == 15 && it.cleanerType == "EMPLOYED" &&
                        it.employerId == "co1" && it.specializations == listOf("DEEP_CLEAN")
                }
            )
        }
        coVerify(exactly = 0) { backendService.updateClient(any()) }
    }
}
