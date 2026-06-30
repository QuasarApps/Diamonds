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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.common.util.DateTimeFormatUtil
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.ui.R

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
        Text(stringResource(R.string.booking_confirmed_exclaim), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.booking_request_sent),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        state.booking?.let { booking ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.booking_details), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    DetailRow(stringResource(R.string.service),  state.service?.title ?: "—")
                    DetailRow(stringResource(R.string.cleaner),  state.provider?.name ?: "—")
                    DetailRow(
                        stringResource(R.string.date),
                        DateTimeFormatUtil.formatDateForDisplay(booking.scheduledDate)
                    )
                    DetailRow(
                        stringResource(R.string.time),
                        DateTimeFormatUtil.formatTimeForDisplay(booking.scheduledTime)
                    )
                    DetailRow(stringResource(R.string.address),  booking.address)
                    booking.notes?.let { DetailRow(stringResource(R.string.notes), it) }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.total), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
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
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.pay_now_with_icon), fontSize = 16.sp)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onViewBookings,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.view_my_bookings), fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBookAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.book_another_service), fontSize = 16.sp)
        }

        } // end else
    }
}

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f), fontSize = 14.sp)
        Text(value, modifier = Modifier.weight(0.6f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun StatusChip(status: BookingStatus) {
    val (label, color) = when (status) {
        BookingStatus.PENDING     -> stringResource(R.string.pending)     to MaterialTheme.colorScheme.tertiary
        BookingStatus.ACCEPTED    -> stringResource(R.string.accepted)    to MaterialTheme.colorScheme.primary
        BookingStatus.IN_PROGRESS -> stringResource(R.string.in_progress) to MaterialTheme.colorScheme.secondary
        BookingStatus.COMPLETED   -> stringResource(R.string.completed)   to MaterialTheme.colorScheme.primary
        BookingStatus.CANCELLED   -> stringResource(R.string.cancelled)   to MaterialTheme.colorScheme.error
        BookingStatus.NO_SHOW     -> stringResource(R.string.status_no_show)     to MaterialTheme.colorScheme.error
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
