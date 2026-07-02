package com.example.diamonds.ui.subscription

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.RecurringBooking
import com.example.diamonds.domain.model.RecurringBookingStatus
import com.example.diamonds.ui.R

@Composable
fun SubscriptionManagementScreen(
    viewModel: RecurringBookingViewModel = hiltViewModel()
) {
    val state by viewModel.mgmtState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadRecurringBookings() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.my_recurring_bookings),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.recurringBookings.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_recurring_bookings),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            stringResource(R.string.setup_recurring_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.recurringBookings, key = { it.id }) { rb ->
                        RecurringBookingCard(
                            recurringBooking = rb,
                            onPause = { viewModel.pauseRecurringBooking(rb.id) },
                            onResume = { viewModel.resumeRecurringBooking(rb.id) },
                            onCancel = { viewModel.cancelRecurringBooking(rb.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringBookingCard(
    recurringBooking: RecurringBooking,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val statusColor = when (recurringBooking.status) {
        RecurringBookingStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        RecurringBookingStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
        RecurringBookingStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        recurringBooking.serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.by_provider, recurringBooking.providerName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    recurringBooking.status.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "📅 ${
                            recurringBooking.frequency.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                        }", style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "🕐 ${recurringBooking.preferredTime}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "📍 ${recurringBooking.address}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                Text(
                    "$${String.format("%.2f", recurringBooking.totalPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.next_booking_date, recurringBooking.nextBookingDate),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (recurringBooking.status != RecurringBookingStatus.CANCELLED) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recurringBooking.status == RecurringBookingStatus.ACTIVE) {
                        FilledTonalButton(onClick = onPause) {
                            Text(stringResource(R.string.pause_with_icon), style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (recurringBooking.status == RecurringBookingStatus.PAUSED) {
                        FilledTonalButton(onClick = onResume) {
                            Text(stringResource(R.string.resume_with_icon), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_cancel),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
