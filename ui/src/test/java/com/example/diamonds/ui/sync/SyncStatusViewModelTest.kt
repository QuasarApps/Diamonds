package com.example.diamonds.ui.sync

import com.example.diamonds.common.util.ConnectivityState
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.repository.EntityType
import com.example.diamonds.domain.repository.ISyncRepository
import com.example.diamonds.domain.repository.SyncOperation
import com.example.diamonds.domain.repository.SyncOperationType
import com.example.diamonds.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncStatusViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var syncRepository: ISyncRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var viewModel: SyncStatusViewModel

    private fun makePendingOp(id: String = "op1") = SyncOperation(
        id = id,
        entityType = EntityType.BOOKING,
        operationType = SyncOperationType.CREATE,
        entityId = "b1",
        payload = "{}",
        status = SyncStatus.PENDING,
        retryCount = 0,
        createdAt = "2026-04-01T10:00:00"
    )

    private fun makeFailedOp(id: String = "op2") =
        makePendingOp(id).copy(status = SyncStatus.FAILED)

    private fun makeConflictOp(id: String = "op3") =
        makePendingOp(id).copy(status = SyncStatus.CONFLICT)

    @Before
    fun setUp() {
        syncRepository = mockk(relaxed = true)
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns flowOf(ConnectivityState.ONLINE)
            every { isOnline() } returns true
        }
        // Default stubs for the combine() flow in SyncStatusViewModel
        every { syncRepository.observeSyncQueue() } returns flowOf(emptyList())
        every { syncRepository.observeFailedOperations() } returns flowOf(emptyList())
        every { syncRepository.observeConflictOperations() } returns flowOf(emptyList())

        viewModel = SyncStatusViewModel(syncRepository, connectivityObserver)
    }

    // ── syncState derivation ──────────────────────────────────────────────────

    @Test
    fun `syncState has empty queues when no operations`() = runTest {
        advanceUntilIdle()
        val state = viewModel.syncState.value
        assertTrue(state.pendingOps.isEmpty())
        assertTrue(state.failedOps.isEmpty())
        assertTrue(state.conflictOps.isEmpty())
        assertTrue(state.allSynced)
    }

    @Test
    fun `syncState reflects pending operations`() = runTest {
        every { syncRepository.observeSyncQueue() } returns flowOf(listOf(makePendingOp()))
        viewModel = SyncStatusViewModel(syncRepository, connectivityObserver)

        // Start collecting so WhileSubscribed activates
        val job = launch { viewModel.syncState.collect {} }
        advanceUntilIdle()

        val state = viewModel.syncState.value
        assertEquals(1, state.pendingOps.size)
        assertEquals("op1", state.pendingOps.first().id)
        assertFalse(state.allSynced)
        assertEquals(1, state.totalCount)
        job.cancel()
    }

    @Test
    fun `syncState reflects failed operations`() = runTest {
        every { syncRepository.observeFailedOperations() } returns flowOf(listOf(makeFailedOp()))
        viewModel = SyncStatusViewModel(syncRepository, connectivityObserver)

        val job = launch { viewModel.syncState.collect {} }
        advanceUntilIdle()

        val state = viewModel.syncState.value
        assertEquals(1, state.failedOps.size)
        assertEquals("op2", state.failedOps.first().id)
        job.cancel()
    }

    @Test
    fun `syncState reflects conflict operations`() = runTest {
        every { syncRepository.observeConflictOperations() } returns flowOf(listOf(makeConflictOp()))
        viewModel = SyncStatusViewModel(syncRepository, connectivityObserver)

        val job = launch { viewModel.syncState.collect {} }
        advanceUntilIdle()

        val state = viewModel.syncState.value
        assertEquals(1, state.conflictOps.size)
        assertEquals("op3", state.conflictOps.first().id)
        job.cancel()
    }

    @Test
    fun `totalCount sums pending, failed and conflict operations`() = runTest {
        every { syncRepository.observeSyncQueue() } returns flowOf(listOf(makePendingOp()))
        every { syncRepository.observeFailedOperations() } returns flowOf(listOf(makeFailedOp()))
        every { syncRepository.observeConflictOperations() } returns flowOf(listOf(makeConflictOp()))
        viewModel = SyncStatusViewModel(syncRepository, connectivityObserver)

        val job = launch { viewModel.syncState.collect {} }
        advanceUntilIdle()

        assertEquals(3, viewModel.syncState.value.totalCount)
        assertFalse(viewModel.syncState.value.allSynced)
        job.cancel()
    }

    @Test
    fun `syncState isOnline reflects connectivity`() = runTest {
        every { connectivityObserver.observeConnectivityState() } returns
                flowOf(ConnectivityState.OFFLINE)
        viewModel = SyncStatusViewModel(syncRepository, connectivityObserver)

        val job = launch { viewModel.syncState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.syncState.value.isOnline)
        job.cancel()
    }

    // ── retryFailed ───────────────────────────────────────────────────────────

    @Test
    fun `retryFailed delegates to syncRepository`() = runTest {
        coEvery { syncRepository.retryFailedOperation("op2") } returns Result.Success(Unit)

        viewModel.retryFailed("op2")
        advanceUntilIdle()

        coVerify { syncRepository.retryFailedOperation("op2") }
    }

    // ── retryAllFailed ────────────────────────────────────────────────────────

    @Test
    fun `retryAllFailed delegates to syncRepository`() = runTest {
        coEvery { syncRepository.retryAllFailed() } returns Result.Success(Unit)

        viewModel.retryAllFailed()
        advanceUntilIdle()

        coVerify { syncRepository.retryAllFailed() }
    }

    // ── cancelOperation ───────────────────────────────────────────────────────

    @Test
    fun `cancelOperation delegates to syncRepository`() = runTest {
        coEvery { syncRepository.cancelSyncOperation("op1") } returns Result.Success(Unit)

        viewModel.cancelOperation("op1")
        advanceUntilIdle()

        coVerify { syncRepository.cancelSyncOperation("op1") }
    }

    // ── resolveConflict ───────────────────────────────────────────────────────

    @Test
    fun `resolveConflict with useLocal=true delegates to syncRepository`() = runTest {
        coEvery { syncRepository.resolveConflict("op3", true) } returns Result.Success(Unit)

        viewModel.resolveConflict("op3", useLocal = true)
        advanceUntilIdle()

        coVerify { syncRepository.resolveConflict("op3", true) }
    }

    @Test
    fun `resolveConflict with useLocal=false delegates to syncRepository`() = runTest {
        coEvery { syncRepository.resolveConflict("op3", false) } returns Result.Success(Unit)

        viewModel.resolveConflict("op3", useLocal = false)
        advanceUntilIdle()

        coVerify { syncRepository.resolveConflict("op3", false) }
    }

    // ── syncNow ───────────────────────────────────────────────────────────────

    @Test
    fun `syncNow sets isSyncing true then false on success`() = runTest {
        coEvery { syncRepository.syncNow() } returns Result.Success(Unit)

        viewModel.syncNow()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSyncing)
        assertEquals("Sync complete", viewModel.uiState.value.lastSyncMessage)
    }

    @Test
    fun `syncNow sets error message on failure`() = runTest {
        coEvery { syncRepository.syncNow() } returns Result.Error(Exception("timeout"))

        viewModel.syncNow()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSyncing)
        assertTrue(viewModel.uiState.value.lastSyncMessage!!.contains("timeout"))
    }
}
