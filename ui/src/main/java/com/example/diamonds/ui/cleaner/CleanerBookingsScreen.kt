package com.example.diamonds.ui.cleaner

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

/**
 * Unified Bookings screen for providers — three sub-tabs:
 *  1. Requests  – pending booking requests (accept / decline)
 *  2. Upcoming  – today's and future accepted/in-progress jobs
 *  3. History   – completed past bookings
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CleanerBookingsScreen(
    onReviewClient: ((bookingId: String, clientId: String) -> Unit)? = null,
    onViewClientRatings: ((clientId: String) -> Unit)? = null,
    onFileClaim: ((bookingId: String) -> Unit)? = null,
    viewModel: CleanerViewModel = hiltViewModel()
) {
    val tabTitles = listOf(
        stringResource(R.string.requests),
        stringResource(R.string.upcoming),
        stringResource(R.string.history)
    )
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }

        // Eagerly load data when each tab becomes current
        LaunchedEffect(pagerState.currentPage) {
            when (pagerState.currentPage) {
                0 -> viewModel.loadRequests()
                1 -> viewModel.loadSchedule()
                2 -> viewModel.loadHistory()
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> RequestsTab(viewModel)
                1 -> UpcomingTab(viewModel, onReviewClient, onViewClientRatings, onFileClaim)
                2 -> HistoryTab(viewModel, onReviewClient, onViewClientRatings, onFileClaim)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Tab 0 – Requests
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestsTab(viewModel: CleanerViewModel) {
    val state by viewModel.requestsState.collectAsState()
    val error by viewModel.error.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    if (!state.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.isLoading) CircularProgressIndicator()
        }
        return
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshRequests() }
    ) {
        when {
            error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            state.requests.isEmpty() -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🎉", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.no_pending_requests), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.new_requests_appear_here),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.requests, key = { it.booking.id }) { item ->
                    RequestCard(
                        item = item,
                        onAccept = { viewModel.acceptRequest(item.booking.id) },
                        onDecline = { viewModel.declineRequest(item.booking.id) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Tab 1 – Upcoming
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingTab(
    viewModel: CleanerViewModel,
    onReviewClient: ((bookingId: String, clientId: String) -> Unit)?,
    onViewClientRatings: ((clientId: String) -> Unit)?,
    onFileClaim: ((bookingId: String) -> Unit)?
) {
    val state by viewModel.scheduleState.collectAsState()
    val error by viewModel.error.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    if (!state.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.isLoading) CircularProgressIndicator()
        }
        return
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshSchedule() }
    ) {
        when {
            error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            state.todayJobs.isEmpty() && state.upcomingJobs.isEmpty() -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📅", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.no_upcoming_jobs), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.accepted_bookings_appear_here),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.todayJobs.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.today)) }
                    items(state.todayJobs, key = { "today-${it.booking.id}" }) { item ->
                        ScheduleJobCard(
                            item,
                            viewModel,
                            onReviewClient,
                            onViewClientRatings,
                            onFileClaim
                        )
                    }
                }
                if (state.upcomingJobs.isNotEmpty()) {
                    item {
                        if (state.todayJobs.isNotEmpty()) Spacer(Modifier.height(4.dp))
                        SectionHeader(stringResource(R.string.upcoming))
                    }
                    items(state.upcomingJobs, key = { "upcoming-${it.booking.id}" }) { item ->
                        ScheduleJobCard(
                            item,
                            viewModel,
                            onReviewClient,
                            onViewClientRatings,
                            onFileClaim
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Tab 2 – History
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTab(
    viewModel: CleanerViewModel,
    onReviewClient: ((bookingId: String, clientId: String) -> Unit)?,
    onViewClientRatings: ((clientId: String) -> Unit)?,
    onFileClaim: ((bookingId: String) -> Unit)?
) {
    val state by viewModel.historyState.collectAsState()
    val error by viewModel.error.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    if (!state.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.isLoading) CircularProgressIndicator()
        }
        return
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshHistory() }
    ) {
        when {
            error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            state.completedJobs.isEmpty() -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📜", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.no_completed_jobs_yet), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.completed_bookings_appear_here),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.completedJobs, key = { it.booking.id }) { item ->
                    HistoryJobCard(item, onReviewClient, onViewClientRatings, onFileClaim)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Shared card composables
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RequestCard(
    item: CleanerBookingItem,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var showDeclineDialog by remember { mutableStateOf(false) }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = { Text(stringResource(R.string.decline_request_question)) },
            text = { Text(stringResource(R.string.decline_request_message)) },
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.serviceName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        stringResource(R.string.client_name, item.clientName),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "$${item.servicePrice.toInt()}",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            DetailRow(stringResource(R.string.detail_date), DateTimeFormatUtil.formatDateForDisplay(item.booking.scheduledDate))
            DetailRow(stringResource(R.string.detail_time), DateTimeFormatUtil.formatTimeForDisplay(item.booking.scheduledTime))
            DetailRow(stringResource(R.string.detail_address), item.booking.address)
            item.booking.notes?.let { DetailRow(stringResource(R.string.detail_notes), it) }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showDeclineDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.decline)) }
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.accept)) }
            }
        }
    }
}

@Composable
private fun ScheduleJobCard(
    item: CleanerBookingItem,
    viewModel: CleanerViewModel,
    onReviewClient: ((bookingId: String, clientId: String) -> Unit)?,
    onViewClientRatings: ((clientId: String) -> Unit)?,
    onFileClaim: ((bookingId: String) -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.serviceName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.client_name, item.clientName),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onViewClientRatings != null) {
                            Text(
                                " ⭐", fontSize = 13.sp,
                                modifier = Modifier.clickable { onViewClientRatings(item.booking.clientId) }
                            )
                        }
                    }
                }
                StatusPill(item.booking.status)
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth()) {
                Text(
                    "📅 ${DateTimeFormatUtil.formatDateForDisplay(item.booking.scheduledDate)}  🕐 ${
                        DateTimeFormatUtil.formatTimeForDisplay(item.booking.scheduledTime)
                    }",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$${item.servicePrice.toInt()}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                "📍 ${item.booking.address}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            when (item.booking.status) {
                BookingStatus.ACCEPTED -> {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.startJob(item.booking.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.start_job))
                    }
                }

                BookingStatus.IN_PROGRESS -> {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.completeJob(item.booking.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.mark_complete))
                    }
                }

                BookingStatus.COMPLETED -> {
                    if (onReviewClient != null) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onReviewClient(item.booking.id, item.booking.clientId) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.review_client)) }
                    }
                    if (onFileClaim != null) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { onFileClaim(item.booking.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.report_issue_with_client)) }
                    }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun HistoryJobCard(
    item: CleanerBookingItem,
    onReviewClient: ((bookingId: String, clientId: String) -> Unit)?,
    onViewClientRatings: ((clientId: String) -> Unit)?,
    onFileClaim: ((bookingId: String) -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.serviceName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.client_name, item.clientName),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onViewClientRatings != null) {
                            Text(
                                " ⭐", fontSize = 13.sp,
                                modifier = Modifier.clickable { onViewClientRatings(item.booking.clientId) }
                            )
                        }
                    }
                }
                Text(
                    "$${item.servicePrice.toInt()}",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth()) {
                Text(
                    "📅 ${DateTimeFormatUtil.formatDateForDisplay(item.booking.scheduledDate)}  🕐 ${
                        DateTimeFormatUtil.formatTimeForDisplay(item.booking.scheduledTime)
                    }",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                "📍 ${item.booking.address}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (onReviewClient != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onReviewClient(item.booking.id, item.booking.clientId) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.review_client)) }
            }
            if (onFileClaim != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onFileClaim(item.booking.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.report_issue_with_client)) }
            }
        }
    }
}

// ── Small shared composables ──────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusPill(status: BookingStatus) {
    val (label, color) = when (status) {
        BookingStatus.ACCEPTED -> stringResource(R.string.accepted) to MaterialTheme.colorScheme.primary
        BookingStatus.IN_PROGRESS -> stringResource(R.string.in_progress) to MaterialTheme.colorScheme.secondary
        BookingStatus.COMPLETED -> stringResource(R.string.completed) to MaterialTheme.colorScheme.primary
        else -> status.name to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color
        )
    }
}
