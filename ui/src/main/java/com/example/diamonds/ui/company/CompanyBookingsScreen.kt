package com.example.diamonds.ui.company

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.common.util.DateTimeFormatUtil
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.ui.R
import com.example.diamonds.ui.components.PullToRefreshLayout

/**
 * Company-wide bookings screen: tabbed view of Pending, Active, and Completed
 * jobs across all employed cleaners. Company can accept/decline pending requests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyBookingsScreen(
    viewModel: CompanyViewModel = hiltViewModel()
) {
    val state by viewModel.bookingsState.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    // Show '—' counts while refreshing so stale numbers don't persist
    val tabs = if (isRefreshing) listOf(
        stringResource(R.string.company_tab_pending, "—"),
        stringResource(R.string.company_tab_active, "—"),
        stringResource(R.string.company_tab_completed, "—")
    )
    else listOf(
        stringResource(R.string.company_tab_pending, state.pending.size.toString()),
        stringResource(R.string.company_tab_active, state.active.size.toString()),
        stringResource(R.string.company_tab_completed, state.completed.size.toString())
    )

    LaunchedEffect(Unit) { viewModel.loadBookings() }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    if (state.isLoading && !isRefreshing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshBookings() }
    ) {
    if (error != null) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp), contentAlignment = Alignment.Center) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    } else {
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick  = { selectedTab = i },
                    text     = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        val items = if (isRefreshing) emptyList()
        else when (selectedTab) {
            0    -> state.pending
            1    -> state.active
            else -> state.completed
        }

        if (items.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when (selectedTab) {
                            0 -> stringResource(R.string.company_no_pending_requests)
                            1 -> stringResource(R.string.company_no_active_jobs)
                            else -> stringResource(R.string.company_no_completed_jobs)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.booking.id }) { item ->
                    CompanyBookingCard(
                        item       = item,
                        showActions = selectedTab == 0,
                        onAccept   = { viewModel.acceptRequest(item.booking.id) },
                        onDecline  = { viewModel.declineRequest(item.booking.id) }
                    )
                }
            }
        }
    }
    } // end else
    } // end PullToRefreshLayout
}

@Composable
private fun CompanyBookingCard(
    item: CompanyBookingItem,
    showActions: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var showDeclineDialog by remember { mutableStateOf(false) }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title   = { Text(stringResource(R.string.decline_request_question)) },
            text    = { Text(stringResource(R.string.company_decline_customer_notified)) },
            confirmButton = {
                TextButton(onClick = { showDeclineDialog = false; onDecline() }) {
                    Text(stringResource(R.string.decline), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) { Text(stringResource(R.string.keep)) }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.serviceName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(stringResource(R.string.client_name, item.clientName), fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("$${item.servicePrice.toInt()}", fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "📅 ${DateTimeFormatUtil.formatDateForDisplay(item.booking.scheduledDate)}  🕐 ${
                            DateTimeFormatUtil.formatTimeForDisplay(
                                item.booking.scheduledTime
                            )
                        }",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.company_cleaner_name, item.cleanerName), fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp))
                }
                BookingStatusPill(item.booking.status)
            }

            if (showActions) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showDeclineDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text(stringResource(R.string.decline)) }
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.accept))
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingStatusPill(status: BookingStatus) {
    val (label, color) = when (status) {
        BookingStatus.PENDING     -> stringResource(R.string.pending)     to MaterialTheme.colorScheme.tertiary
        BookingStatus.ACCEPTED    -> stringResource(R.string.accepted)    to MaterialTheme.colorScheme.primary
        BookingStatus.IN_PROGRESS -> stringResource(R.string.in_progress) to MaterialTheme.colorScheme.secondary
        BookingStatus.COMPLETED   -> stringResource(R.string.completed)   to MaterialTheme.colorScheme.primary
        BookingStatus.CANCELLED   -> stringResource(R.string.cancelled)   to MaterialTheme.colorScheme.error
        else                      -> status.name   to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Text(label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
