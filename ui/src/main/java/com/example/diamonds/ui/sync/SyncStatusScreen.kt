package com.example.diamonds.ui.sync

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.repository.EntityType
import com.example.diamonds.domain.repository.SyncOperation
import com.example.diamonds.ui.R
import kotlinx.coroutines.launch

/**
 * Full-screen Sync Status page showing pending, failed, and conflicting operations.
 * Users can retry failed operations, cancel pending ones, and resolve conflicts.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SyncStatusScreen(
    viewModel: SyncStatusViewModel = hiltViewModel()
) {
    val state by viewModel.syncState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // ── Header summary ──────────────────────────────────────────────────
        SyncSummaryHeader(
            state = state,
            isSyncing = uiState.isSyncing,
            onSyncNow = { viewModel.syncNow() }
        )

        if (uiState.lastSyncMessage != null) {
            Text(
                text = uiState.lastSyncMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        if (state.allSynced) {
            // ── All synced ──────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.all_synced),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.no_pending_operations),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // ── Tabbed pager ────────────────────────────────────────────────
            val pendingLabel = stringResource(R.string.sync_tab_pending, state.pendingOps.size)
            val failedLabel = stringResource(R.string.sync_tab_failed, state.failedOps.size)
            val conflictsLabel = stringResource(R.string.sync_tab_conflicts, state.conflictOps.size)
            val tabs = buildList {
                if (state.pendingOps.isNotEmpty()) add(pendingLabel)
                if (state.failedOps.isNotEmpty()) add(failedLabel)
                if (state.conflictOps.isNotEmpty()) add(conflictsLabel)
            }
            val tabTypes = buildList {
                if (state.pendingOps.isNotEmpty()) add(TabType.PENDING)
                if (state.failedOps.isNotEmpty()) add(TabType.FAILED)
                if (state.conflictOps.isNotEmpty()) add(TabType.CONFLICT)
            }

            val pagerState = rememberPagerState(pageCount = { tabs.size })

            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            // ── Retry All button for Failed tab ─────────────────────────────
            if (tabTypes.getOrNull(pagerState.currentPage) == TabType.FAILED && state.failedOps.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(onClick = { viewModel.retryAllFailed() }) {
                        Text(stringResource(R.string.retry_all_with_icon))
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val type = tabTypes.getOrNull(page) ?: return@HorizontalPager
                when (type) {
                    TabType.PENDING -> SyncOperationList(
                        operations = state.pendingOps,
                        onCancel = { viewModel.cancelOperation(it) }
                    )

                    TabType.FAILED -> SyncOperationList(
                        operations = state.failedOps,
                        onRetry = { viewModel.retryFailed(it) },
                        onCancel = { viewModel.cancelOperation(it) }
                    )

                    TabType.CONFLICT -> ConflictOperationList(
                        operations = state.conflictOps,
                        onUseLocal = { viewModel.resolveConflict(it, useLocal = true) },
                        onUseServer = { viewModel.resolveConflict(it, useLocal = false) }
                    )
                }
            }
        }
    }
}

private enum class TabType { PENDING, FAILED, CONFLICT }

// ── Summary header ──────────────────────────────────────────────────────────

@Composable
private fun SyncSummaryHeader(
    state: SyncStatusUiState,
    isSyncing: Boolean,
    onSyncNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                state.conflictOps.isNotEmpty() -> MaterialTheme.colorScheme.errorContainer
                state.failedOps.isNotEmpty() -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                state.pendingOps.isNotEmpty() -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        state.allSynced -> stringResource(R.string.all_synced_with_icon)
                        state.conflictOps.isNotEmpty() -> pluralStringResource(
                            R.plurals.sync_conflicts_summary,
                            state.conflictOps.size,
                            state.conflictOps.size
                        )
                        state.failedOps.isNotEmpty() -> stringResource(R.string.sync_failed_summary, state.failedOps.size)
                        else -> stringResource(R.string.sync_pending_summary, state.pendingOps.size)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (state.isOnline) stringResource(R.string.online_with_icon) else stringResource(R.string.offline_with_icon),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!state.allSynced && state.isOnline) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Button(onClick = onSyncNow) {
                        Text(stringResource(R.string.sync_now))
                    }
                }
            }
        }
    }
}

// ── Pending / Failed list ───────────────────────────────────────────────────

@Composable
private fun SyncOperationList(
    operations: List<SyncOperation>,
    onRetry: ((String) -> Unit)? = null,
    onCancel: ((String) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(operations, key = { it.id }) { op ->
            SyncOperationCard(
                operation = op,
                onRetry = onRetry,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun SyncOperationCard(
    operation: SyncOperation,
    onRetry: ((String) -> Unit)? = null,
    onCancel: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = when (operation.status) {
                SyncStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                SyncStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = iconForEntityType(operation.entityType),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${operation.operationType.name} ${operation.entityType.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.sync_entity_id, operation.entityId.take(12)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = statusBadge(operation.status),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (operation.error != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sync_error, operation.error.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (operation.retryCount > 0) {
                Text(
                    text = stringResource(R.string.sync_retries, operation.retryCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action buttons
            if (onRetry != null || onCancel != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onRetry != null && operation.status == SyncStatus.FAILED) {
                        FilledTonalButton(onClick = { onRetry(operation.id) }) {
                            Text(stringResource(R.string.retry_with_icon))
                        }
                    }
                    if (onCancel != null) {
                        OutlinedButton(
                            onClick = { onCancel(operation.id) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

// ── Conflict list ───────────────────────────────────────────────────────────

@Composable
private fun ConflictOperationList(
    operations: List<SyncOperation>,
    onUseLocal: (String) -> Unit,
    onUseServer: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(operations, key = { it.id }) { op ->
            ConflictCard(
                operation = op,
                onUseLocal = { onUseLocal(op.id) },
                onUseServer = { onUseServer(op.id) }
            )
        }
    }
}

@Composable
private fun ConflictCard(
    operation: SyncOperation,
    onUseLocal: () -> Unit,
    onUseServer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.sync_conflict_label,
                            operation.operationType.name,
                            operation.entityType.name
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.sync_entity_id, operation.entityId.take(12)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (operation.error != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = operation.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.conflict_choose_which),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onUseLocal,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.keep_local_with_icon))
                }
                OutlinedButton(
                    onClick = onUseServer,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.use_server_with_icon))
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun iconForEntityType(type: EntityType): String = when (type) {
    EntityType.BOOKING -> "📅"
    EntityType.REVIEW -> "⭐"
    EntityType.PAYMENT -> "💳"
    EntityType.SERVICE -> "🧹"
    EntityType.PROFILE -> "👤"
    EntityType.RECURRING_BOOKING -> "🔄"
    EntityType.SUPPORT_TICKET -> "🎫"
    EntityType.CLAIM -> "📝"
}

private fun statusBadge(status: SyncStatus): String = when (status) {
    SyncStatus.PENDING -> "⏳ Pending"
    SyncStatus.FAILED -> "❌ Failed"
    SyncStatus.CONFLICT -> "⚠️ Conflict"
    SyncStatus.SYNCED -> "✅ Synced"
    SyncStatus.CANCELLED -> "🚫 Cancelled"
    SyncStatus.READ_ONLY -> "📖 Read-only"
}
