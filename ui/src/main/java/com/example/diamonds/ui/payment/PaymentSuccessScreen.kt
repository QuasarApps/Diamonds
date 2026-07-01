package com.example.diamonds.ui.payment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.diamonds.ui.R

/**
 * Shown after a successful payment — receipt-style summary.
 */
@Composable
fun PaymentSuccessScreen(
    paymentId: String,
    onViewBookings: () -> Unit,
    onDone: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val state by viewModel.paymentState.collectAsState()

    // paymentState already holds the booking context if we just came from PaymentScreen;
    // if navigated fresh (e.g. deep link) we can re-load from paymentId.
    // For now we use whatever is in state.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✅", fontSize = 72.sp)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.payment_successful_exclaim), fontSize = 26.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.booking_confirmed_payment_received),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, fontSize = 14.sp)

        Spacer(Modifier.height(32.dp))

        // Receipt card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                ReceiptRow(stringResource(R.string.service),  state.serviceName.ifBlank { "—" })
                ReceiptRow(stringResource(R.string.provider), state.providerName.ifBlank { "—" })
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ReceiptRow(stringResource(R.string.amount_paid), "$${state.amount.toInt()}", bold = true)
                ReceiptRow(stringResource(R.string.payment_id),  paymentId.takeLast(12), small = true)
                ReceiptRow(stringResource(R.string.status),      stringResource(R.string.status_succeeded))
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(onClick = onViewBookings,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)) {
            Text(stringResource(R.string.view_my_bookings))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)) {
            Text(stringResource(R.string.back_to_home))
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    bold: Boolean = false,
    small: Boolean = false
) {
    androidx.compose.foundation.layout.Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label,
            fontSize = if (small) 11.sp else 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(value,
            fontSize = if (small) 11.sp else 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface)
    }
}
