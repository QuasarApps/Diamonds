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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.ui.components.ConfirmationDialog
import com.example.diamonds.ui.components.NotFoundScreen
import com.example.diamonds.ui.map.BookingLocationMapCard

// ── Bookings List ─────────────────────────────────────────────────────────────

@Composable
fun BookingsListScreen(
    onBookingSelected: (String) -> Unit,
    onStartBooking: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state by viewModel.bookingsListState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadMyBookings() }

    if (state.isLoading) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("⚠️", fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text(error ?: "", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        return
    }

    if (state.bookings.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("📋", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("No bookings yet", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("Book your first cleaning service to get started.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onStartBooking) { Text("Find a Cleaner") }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.bookings, key = { it.booking.id }) { item ->
            BookingSummaryCard(item = item, onClick = { onBookingSelected(item.booking.id) })
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
                Text("📅 ${item.booking.scheduledDate}  🕐 ${item.booking.scheduledTime}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("$${item.servicePrice.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ── Booking Detail ────────────────────────────────────────────────────────────

@Composable
fun BookingDetailScreen(
    bookingId: String,
    onCancelled: () -> Unit,
    onLeaveReview: (bookingId: String, providerId: String) -> Unit = { _, _ -> },
    onTrackCleaner: (bookingId: String) -> Unit = {},
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state by viewModel.detailState.collectAsState()
    val error by viewModel.error.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bookingId) { viewModel.loadBookingDetail(bookingId) }
    LaunchedEffect(state.cancelSuccess) {
        if (state.cancelSuccess) onCancelled()
    }

    if (showCancelDialog) {
        ConfirmationDialog(
            title        = "Cancel Booking?",
            message      = "This action cannot be undone. The cleaner will be notified.",
            confirmLabel = "Cancel Booking",
            dismissLabel = "Keep",
            isDestructive = true,
            onConfirm    = { showCancelDialog = false; viewModel.cancelBooking(bookingId) },
            onDismiss    = { showCancelDialog = false }
        )
    }

    if (state.isLoading) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val booking = state.booking ?: run {
        NotFoundScreen(
            message = error ?: "This booking could not be found.",
            onGoBack = onCancelled
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status header
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(booking.status)
            Spacer(Modifier.width(12.dp))
            Text("Booking #${booking.id.takeLast(6)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionLabel("Service")
                Text(state.service?.title ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                state.provider?.let { Text("by ${it.name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Schedule & Location")
                DetailRow("Date",    booking.scheduledDate)
                DetailRow("Time",    booking.scheduledTime)
                DetailRow("Address", booking.address)
                booking.notes?.let { DetailRow("Notes", it) }

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
                SectionLabel("Payment")
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Total", modifier = Modifier.weight(1f))
                    Text("$${booking.totalPrice.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                Text("Est. duration: ${state.service?.duration ?: "—"} min", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.weight(1f))

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
                else Text("Cancel This Booking", fontSize = 15.sp)
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
            ) { Text("🗺️  Track Cleaner", fontSize = 15.sp) }
        }

        if (booking.status == BookingStatus.COMPLETED && state.provider != null) {
            Button(
                onClick  = { onLeaveReview(bookingId, booking.providerId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("⭐  Leave a Review", fontSize = 15.sp) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
    Spacer(Modifier.height(4.dp))
}
