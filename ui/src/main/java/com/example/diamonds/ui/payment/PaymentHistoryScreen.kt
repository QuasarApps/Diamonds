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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.common.util.DateTimeFormatUtil
import com.example.diamonds.domain.model.PaymentStatus

/**
 * Full payment history for the current customer.
 * Reachable from the customer profile tab.
 */
@Composable
fun PaymentHistoryScreen(
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val state by viewModel.historyState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.items.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("💳", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("No payments yet", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("Your payment receipts will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "${state.items.size} transaction${if (state.items.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(state.items, key = { it.payment.id }) { item ->
            PaymentHistoryCard(item)
        }
    }
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
                    Text("by ${item.providerName}", fontSize = 12.sp,
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
