package com.example.diamonds.data.sync

import com.example.diamonds.common.util.ConnectivityState
import com.example.diamonds.data.connectivity.ConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Triggers an immediate sync when network connectivity is restored.
 *
 * Collects [ConnectivityObserver.observeConnectivityState] and, when the state
 * transitions from OFFLINE to ONLINE/METERED, calls [SyncManager.processSyncQueue]
 * after a brief debounce to avoid flapping.
 */
class ConnectivitySyncTrigger(
    private val connectivityObserver: ConnectivityObserver,
    private val syncManager: SyncManager
) {
    private var previouslyOnline = true

    /**
     * Start observing connectivity changes in the given [scope].
     * Call once from Application.onCreate or a similar long-lived scope.
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            connectivityObserver.observeConnectivityState()
                .map { it != ConnectivityState.OFFLINE }
                .distinctUntilChanged()
                .collect { isOnline ->
                    if (isOnline && !previouslyOnline) {
                        // Debounce to avoid sync flapping on unstable connections
                        delay(2_000)
                        if (connectivityObserver.isOnline()) {
                            syncManager.processSyncQueue()
                        }
                    }
                    previouslyOnline = isOnline
                }
        }
    }
}
