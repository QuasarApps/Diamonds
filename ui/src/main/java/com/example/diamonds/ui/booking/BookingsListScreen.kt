package com.example.diamonds.ui.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.diamonds.ui.components.ConfirmationDialog
import com.example.diamonds.ui.components.NotFoundScreen
import com.example.diamonds.ui.components.PullToRefreshLayout
import com.example.diamonds.ui.map.BookingLocationMapCard

// ── Bookings List ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsListScreen(
    onBookingSelected: (String) -> Unit,
    onStartBooking: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state by viewModel.bookingsListState.collectAsState()
    val error by viewModel.error.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadMyBookings() }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshBookingsList() }
    ) {
        if (state.isLoading && !isRefreshing) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null && state.bookings.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⚠️", fontSize = 40.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        } else if (state.bookings.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📋", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.no_bookings_yet), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.book_first_service),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onStartBooking) { Text(stringResource(R.string.find_a_cleaner)) }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.bookings, key = { it.booking.id }) { item ->
                    BookingSummaryCard(
                        item = item,
                        onClick = { onBookingSelected(item.booking.id) })
                }
            }
        }
    }
}

@Composable
private fun BookingSummaryCard(item: BookingWithDetails, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.serviceName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(item.providerName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusChip(item.booking.status)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Row {
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
                Text("$${item.servicePrice.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ── Booking Detail ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
data class OpenChatParams(
    val bookingId: String,
    val clientId: String,
    val clientName: String,
    val providerId: String,
    val providerName: String
)

@Composable
fun BookingDetailScreen(
    bookingId: String,
    onCancelled: () -> Unit,
    onLeaveReview: (bookingId: String, providerId: String) -> Unit = { _, _ -> },
    onTrackCleaner: (bookingId: String) -> Unit = {},
    onOpenChat: (OpenChatParams) -> Unit = {},
    onEditBooking: (bookingId: String) -> Unit = {},
    onFileClaim: (bookingId: String) -> Unit = {},
    onGetHelp: (bookingId: String) -> Unit = {},
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state by viewModel.detailState.collectAsState()
    val error by viewModel.error.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(bookingId) { viewModel.loadBookingDetail(bookingId) }
    LaunchedEffect(state.cancelSuccess) {
        if (state.cancelSuccess) onCancelled()
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) isRefreshing = false
    }

    if (showCancelDialog) {
        ConfirmationDialog(
            title        = stringResource(R.string.cancel_booking_question),
            message      = stringResource(R.string.cancel_booking_message),
            confirmLabel = stringResource(R.string.cancel_booking),
            dismissLabel = stringResource(R.string.keep),
            isDestructive = true,
            onConfirm    = { showCancelDialog = false; viewModel.cancelBooking(bookingId) },
            onDismiss    = { showCancelDialog = false }
        )
    }

    if (state.isLoading && !isRefreshing) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val booking = state.booking ?: run {
        NotFoundScreen(
            message = error ?: stringResource(R.string.booking_not_found),
            onGoBack = onCancelled
        )
        return
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshBookingDetail(bookingId) }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status header
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(booking.status)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.booking_number, booking.id.takeLast(6)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionLabel(stringResource(R.string.service))
                Text(state.service?.title ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                state.provider?.let { Text(stringResource(R.string.by_provider, it.name), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(stringResource(R.string.schedule_and_location))
                DetailRow(stringResource(R.string.date), DateTimeFormatUtil.formatDateForDisplay(booking.scheduledDate))
                DetailRow(stringResource(R.string.time), DateTimeFormatUtil.formatTimeForDisplay(booking.scheduledTime))
                DetailRow(stringResource(R.string.address), booking.address)
                booking.notes?.let { DetailRow(stringResource(R.string.notes), it) }

                // Mini-map showing the booking location
                if (booking.latitude != null && booking.longitude != null) {
                    Spacer(Modifier.height(8.dp))
                    BookingLocationMapCard(
                        latitude = booking.latitude!!,
                        longitude = booking.longitude!!,
                        address = booking.address,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionLabel(stringResource(R.string.payment))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.total), modifier = Modifier.weight(1f))
                    Text("$${booking.totalPrice.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.est_duration, state.service?.duration ?: "—"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        val canCancel = booking.status in listOf(BookingStatus.PENDING, BookingStatus.ACCEPTED)
        if (canCancel) {
            OutlinedButton(
                onClick = { showCancelDialog = true },
                enabled = !state.isLoading,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.cancel_this_booking), fontSize = 15.sp)
            }
        }

        // Track Cleaner button for accepted / in-progress bookings
        val canTrack = booking.status in listOf(BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS)
        if (canTrack) {
            Button(
                onClick = { onTrackCleaner(bookingId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(stringResource(R.string.track_cleaner_with_icon), fontSize = 15.sp) }
        }

        // Chat button — available for all non-cancelled bookings
        val canChat = booking.status != BookingStatus.CANCELLED
        if (canChat && state.provider != null) {
            // Hoisted out of the onClick lambda: stringResource is @Composable and
            // can't be called from a (non-composable) click handler.
            val youLabel = stringResource(R.string.you_label)
            OutlinedButton(
                onClick = {
                    onOpenChat(
                        OpenChatParams(
                            bookingId = bookingId,
                            clientId = booking.clientId,
                            clientName = youLabel,  // Session name would be better, but not available here
                            providerId = booking.providerId,
                            providerName = state.provider!!.name
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(stringResource(R.string.chat_with_cleaner_with_icon), fontSize = 15.sp) }
        }

        if (booking.status == BookingStatus.COMPLETED && state.provider != null) {
            // Show existing review if present
            val review = state.review
            if (review != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SectionLabel(stringResource(R.string.your_review))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "⭐".repeat(review.rating) + "☆".repeat(5 - review.rating),
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.rating_out_of_five, review.rating),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        val comment = review.comment
                        if (!comment.isNullOrBlank()) {
                            Text(
                                comment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            DateTimeFormatUtil.formatDateForDisplay(review.createdAt),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onLeaveReview(bookingId, booking.providerId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) { Text(stringResource(R.string.leave_a_review_with_icon), fontSize = 15.sp) }
            }

            OutlinedButton(
                onClick = { onFileClaim(bookingId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(stringResource(R.string.report_an_issue_with_icon), fontSize = 15.sp) }
        }

        // Edit / Reschedule for PENDING bookings
        if (booking.status == BookingStatus.PENDING) {
            OutlinedButton(
                onClick = { onEditBooking(bookingId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(stringResource(R.string.edit_reschedule_with_icon), fontSize = 15.sp) }
        }

        // Help & Support button for all active bookings
        if (booking.status !in listOf(BookingStatus.CANCELLED)) {
            OutlinedButton(
                onClick = { onGetHelp(bookingId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(stringResource(R.string.help_and_support_with_icon), fontSize = 15.sp) }
        }
    }
    } // end PullToRefreshLayout
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
    Spacer(Modifier.height(4.dp))
}
