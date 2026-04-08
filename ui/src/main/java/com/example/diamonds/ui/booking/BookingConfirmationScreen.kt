package com.example.diamonds.ui.booking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.BookingStatus

@Composable
fun BookingConfirmationScreen(
    bookingId: String,
    onViewBookings: () -> Unit,
    onBookAnother: () -> Unit,
    onPayNow: (bookingId: String) -> Unit = {},
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state by viewModel.detailState.collectAsState()

    LaunchedEffect(bookingId) { viewModel.loadBookingDetail(bookingId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.isLoading) {
            Spacer(Modifier.height(80.dp))
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
        } else {

        Spacer(Modifier.height(32.dp))

        // Success icon
        Text("✅", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("Booking Confirmed!", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your booking request has been sent. The cleaner will confirm shortly.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        state.booking?.let { booking ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Booking Details", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    DetailRow("Service",  state.service?.title ?: "—")
                    DetailRow("Cleaner",  state.provider?.name ?: "—")
                    DetailRow("Date",     booking.scheduledDate)
                    DetailRow("Time",     booking.scheduledTime)
                    DetailRow("Address",  booking.address)
                    booking.notes?.let { DetailRow("Notes", it) }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Total", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            "$${booking.totalPrice.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    StatusChip(booking.status)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = { onPayNow(bookingId) },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("💳  Pay Now", fontSize = 16.sp)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onViewBookings,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("View My Bookings", fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBookAnother,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Book Another Service", fontSize = 16.sp)
        }

        } // end else
    }
}

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f), fontSize = 14.sp)
        Text(value, modifier = Modifier.weight(0.6f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun StatusChip(status: BookingStatus) {
    val (label, color) = when (status) {
        BookingStatus.PENDING     -> "Pending"     to MaterialTheme.colorScheme.tertiary
        BookingStatus.ACCEPTED    -> "Accepted"    to MaterialTheme.colorScheme.primary
        BookingStatus.IN_PROGRESS -> "In Progress" to MaterialTheme.colorScheme.secondary
        BookingStatus.COMPLETED   -> "Completed"   to MaterialTheme.colorScheme.primary
        BookingStatus.CANCELLED   -> "Cancelled"   to MaterialTheme.colorScheme.error
        BookingStatus.NO_SHOW     -> "No Show"     to MaterialTheme.colorScheme.error
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = Modifier
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
