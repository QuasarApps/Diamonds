package com.example.diamonds.data.sync

import com.example.diamonds.common.util.Constants
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.SyncQueueDao
import com.example.diamonds.data.local.entity.SyncQueueEntity
import com.example.diamonds.data.remote.backend.BookingDto
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.Result
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

    private fun makeEntity(
        id: String = "op1",
        operationType: String = "CREATE",
        entityType: String = "BOOKING",
        entityId: String = "b1",
        payload: String = "{}",
        status: String = "PENDING",
        retryCount: Int = 0,
        lastAttemptAt: String? = null
    ) = SyncQueueEntity(
        id = id,
        operationType = operationType,
        entityType = entityType,
        entityId = entityId,
        payload = payload,
        status = status,
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
            syncQueueDao.insert(match { it.id == "op1" && it.status == "PENDING" && it.retryCount == 0 })
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
            syncQueueDao.updateStatus("op1", "CANCELLED")
            syncQueueDao.delete("op1")
        }
    }

    // ── retry / conflict resolution ───────────────────────────────────────────

    @Test
    fun `retryFailedOperation requeues as pending`() = runTest {
        manager.retryFailedOperation("op1")
        coVerify { syncQueueDao.updateStatus("op1", "PENDING") }
    }

    @Test
    fun `retryAllFailed resets every failed operation`() = runTest {
        manager.retryAllFailed()
        coVerify { syncQueueDao.resetAllFailed() }
    }

    @Test
    fun `resolveConflict keeping local requeues without deleting`() = runTest {
        manager.resolveConflict("op1", useLocal = true)

        coVerify { syncQueueDao.updateStatus("op1", "PENDING") }
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
                listOf(makeEntity(id = "op1", status = "CANCELLED"))

        manager.processSyncQueue()

        coVerify { syncQueueDao.delete("op1") }
        coVerify(exactly = 0) { backendService.cancelBooking(any()) }
    }

    @Test
    fun `processSyncQueue dispatches a pending booking cancel then marks it synced`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(makeEntity(id = "op1", operationType = "CANCEL", status = "PENDING"))
        coEvery { backendService.cancelBooking("b1") } returns Result.Success(mockk<BookingDto>(relaxed = true))

        manager.processSyncQueue()

        coVerify { backendService.cancelBooking("b1") }
        coVerify { syncQueueDao.updateStatus("op1", "SYNCED") }
        coVerify { syncQueueDao.delete("op1") }
    }

    @Test
    fun `processSyncQueue records a failure when dispatch throws`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(makeEntity(id = "op1", operationType = "CANCEL", status = "PENDING"))
        coEvery { backendService.cancelBooking("b1") } throws RuntimeException("boom")

        manager.processSyncQueue()

        coVerify { syncQueueDao.updateAfterRetry("op1", "FAILED", any(), any()) }
    }

    @Test
    fun `processSyncQueue records a failure when the backend returns Result Error`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(makeEntity(id = "op1", operationType = "CANCEL", status = "PENDING"))
        // Backends report errors by returning Result.Error rather than throwing.
        coEvery { backendService.cancelBooking("b1") } returns Result.Error(RuntimeException("server rejected"))

        manager.processSyncQueue()

        // Must be treated as a failure — not silently marked SYNCED and dropped.
        coVerify { syncQueueDao.updateAfterRetry("op1", "FAILED", any(), any()) }
        coVerify(exactly = 0) { syncQueueDao.updateStatus("op1", "SYNCED") }
    }

    @Test
    fun `processSyncQueue marks a conflict when the backend reports one`() = runTest {
        coEvery { syncQueueDao.getQueuedOperations() } returns
                listOf(makeEntity(id = "op1", operationType = "CANCEL", status = "PENDING"))
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
                        id = "op1", operationType = "CANCEL", status = "PENDING",
                        retryCount = Constants.MAX_RETRY_ATTEMPTS
                    )
                )

        manager.processSyncQueue()

        coVerify(exactly = 0) { backendService.cancelBooking(any()) }
        coVerify { syncQueueDao.updateAfterRetry("op1", "FAILED", any(), any()) }
    }
}
