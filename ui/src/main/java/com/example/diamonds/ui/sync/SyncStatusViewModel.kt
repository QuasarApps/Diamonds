package com.example.diamonds.ui.sync

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.repository.ISyncRepository
import com.example.diamonds.domain.repository.SyncOperation
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Sync Status screen.
 */
data class SyncStatusUiState(
    val pendingOps: List<SyncOperation> = emptyList(),
    val failedOps: List<SyncOperation> = emptyList(),
    val conflictOps: List<SyncOperation> = emptyList(),
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String? = null
) {
    val totalCount: Int get() = pendingOps.size + failedOps.size + conflictOps.size
    val allSynced: Boolean get() = totalCount == 0
}

@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    private val syncRepository: ISyncRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<SyncStatusUiState>(connectivityObserver, SyncStatusUiState()) {

    val syncState: StateFlow<SyncStatusUiState> = combine(
        syncRepository.observeSyncQueue(),
        syncRepository.observeFailedOperations(),
        syncRepository.observeConflictOperations(),
        isOnline
    ) { queued, failed, conflicts, online ->
        val pending = queued.filter { it.status == SyncStatus.PENDING }
        SyncStatusUiState(
            pendingOps = pending,
            failedOps = failed,
            conflictOps = conflicts,
            isOnline = online
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatusUiState())

    fun retryFailed(operationId: String) {
        viewModelScope.launch {
            syncRepository.retryFailedOperation(operationId)
        }
    }

    fun retryAllFailed() {
        viewModelScope.launch {
            syncRepository.retryAllFailed()
        }
    }

    fun cancelOperation(operationId: String) {
        viewModelScope.launch {
            syncRepository.cancelSyncOperation(operationId)
        }
    }

    fun resolveConflict(operationId: String, useLocal: Boolean) {
        viewModelScope.launch {
            syncRepository.resolveConflict(operationId, useLocal)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            updateState { it.copy(isSyncing = true) }
            val result = syncRepository.syncNow()
            result.onSuccess {
                updateState { it.copy(isSyncing = false, lastSyncMessage = "Sync complete") }
            }.onError { e ->
                updateState {
                    it.copy(
                        isSyncing = false,
                        lastSyncMessage = "Sync failed: ${e.message}"
                    )
                }
            }
        }
    }
}
