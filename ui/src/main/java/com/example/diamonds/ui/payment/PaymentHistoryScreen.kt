package com.example.diamonds.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.common.util.DateTimeFormatUtil
import com.example.diamonds.domain.model.PaymentStatus
import com.example.diamonds.ui.R
import com.example.diamonds.ui.components.PullToRefreshLayout

/**
 * Full payment history for the current customer.
 * Reachable from the customer profile tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val state by viewModel.historyState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadHistory() }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    if (state.isLoading && !isRefreshing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshHistory() }
    ) {
    if (state.items.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("💳", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.no_payments_yet), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.payment_receipts_appear_here),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    pluralStringResource(R.plurals.transaction_count, state.items.size, state.items.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.items, key = { it.payment.id }) { item ->
                PaymentHistoryCard(item)
            }
        }
    }
    } // end PullToRefreshLayout
}

@Composable
private fun PaymentHistoryCard(item: PaymentHistoryItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.serviceName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (item.providerName.isNotBlank()) {
                    Text(stringResource(R.string.by_provider, item.providerName), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    item.payment.createdAt.toLongOrNull()
                        ?.let { DateTimeFormatUtil.formatTimestampAsDate(it) }
                        ?: item.payment.createdAt.take(10),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$${item.payment.amount.toInt()}",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary)
                StatusBadge(item.payment.status)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: PaymentStatus) {
    val (emoji, color) = when (status) {
        PaymentStatus.SUCCEEDED  -> "✅" to MaterialTheme.colorScheme.primary
        PaymentStatus.FAILED     -> "❌" to MaterialTheme.colorScheme.error
        PaymentStatus.REFUNDED   -> "↩️" to MaterialTheme.colorScheme.secondary
        PaymentStatus.PROCESSING -> "⏳" to MaterialTheme.colorScheme.onSurfaceVariant
        PaymentStatus.PENDING    -> "🕐" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text("$emoji ${status.name.lowercase().replaceFirstChar { it.uppercase() }}",
        fontSize = 11.sp, color = color)
}
