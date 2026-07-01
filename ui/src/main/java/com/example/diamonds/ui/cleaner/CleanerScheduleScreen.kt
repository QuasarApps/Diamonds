package com.example.diamonds.ui.cleaner

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScheduleScreen(
    onReviewClient: ((bookingId: String, clientId: String) -> Unit)? = null,
    onViewClientRatings: ((clientId: String) -> Unit)? = null,
    onFileClaim: ((bookingId: String) -> Unit)? = null,
    viewModel: CleanerViewModel = hiltViewModel()
) {
    val state by viewModel.scheduleState.collectAsState()
    val error by viewModel.error.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadSchedule() }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    if (state.isLoading && !isRefreshing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshSchedule() }
    ) {
    val hasAnything = state.todayJobs.isNotEmpty() || state.upcomingJobs.isNotEmpty()
        when {
            error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp), contentAlignment = Alignment.Center
            ) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            !hasAnything -> Column(
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
            item {
                SectionHeader(stringResource(R.string.today))
            }
            items(state.todayJobs, key = { "today-${it.booking.id}" }) { item ->
                ScheduledJobCard(
                    item = item,
                    viewModel = viewModel,
                    onReviewClient = onReviewClient,
                    onViewClientRatings = onViewClientRatings,
                    onFileClaim = onFileClaim
                )
            }
        }

        if (state.upcomingJobs.isNotEmpty()) {
            item {
                if (state.todayJobs.isNotEmpty()) Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.upcoming))
            }
            items(state.upcomingJobs, key = { "upcoming-${it.booking.id}" }) { item ->
                ScheduledJobCard(
                    item = item,
                    viewModel = viewModel,
                    onReviewClient = onReviewClient,
                    onViewClientRatings = onViewClientRatings,
                    onFileClaim = onFileClaim
                )
            }
        }
            } // end else -> LazyColumn
        } // end when
    } // end PullToRefreshLayout
}

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
private fun ScheduledJobCard(
    item: CleanerBookingItem,
    viewModel: CleanerViewModel,
    onReviewClient: ((bookingId: String, clientId: String) -> Unit)? = null,
    onViewClientRatings: ((clientId: String) -> Unit)? = null,
    onFileClaim: ((bookingId: String) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
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
                                " ⭐",
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { onViewClientRatings(item.booking.clientId) }
                            )
                        }
                    }
                }
                JobStatusPill(item.booking.status)
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth()) {
                Text(
                    "📅 ${DateTimeFormatUtil.formatDateForDisplay(item.booking.scheduledDate)}  🕐 ${
                        DateTimeFormatUtil.formatTimeForDisplay(
                            item.booking.scheduledTime
                        )
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

            // Quick action button based on current status
            when (item.booking.status) {
                BookingStatus.ACCEPTED -> {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick  = { viewModel.startJob(item.booking.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.start_job))
                    }
                }
                BookingStatus.IN_PROGRESS -> {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick  = { viewModel.completeJob(item.booking.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.mark_complete))
                    }
                }
                BookingStatus.COMPLETED -> {
                    if (onReviewClient != null) {
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { onReviewClient(item.booking.id, item.booking.clientId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.review_client))
                        }
                    }
                    if (onFileClaim != null) {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { onFileClaim(item.booking.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.report_issue_with_client))
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun JobStatusPill(status: BookingStatus) {
    val (label, color) = when (status) {
        BookingStatus.ACCEPTED    -> stringResource(R.string.accepted)    to MaterialTheme.colorScheme.primary
        BookingStatus.IN_PROGRESS -> stringResource(R.string.in_progress) to MaterialTheme.colorScheme.secondary
        BookingStatus.COMPLETED   -> stringResource(R.string.completed)   to MaterialTheme.colorScheme.primary
        else                      -> status.name   to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize  = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = color
        )
    }
}
