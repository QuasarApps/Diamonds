package com.example.diamonds.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Payment screen — card entry form for a booking.
 *
 * Shows:
 *  - Order summary card (service, provider, amount)
 *  - Stylised card input: number (auto-formatted), holder name, expiry, CVV
 *  - Pay button with processing spinner
 *  - Accepted cards row (decorative)
 */
@Composable
fun PaymentScreen(
    bookingId: String,
    onPaymentSuccess: (paymentId: String) -> Unit,
    onSkip: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val state by viewModel.paymentState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(bookingId) { viewModel.loadForBooking(bookingId) }
    LaunchedEffect(state.paymentSuccess) {
        if (state.paymentSuccess) {
            viewModel.clearPaymentSuccess()
            onPaymentSuccess(state.paymentId ?: "")
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Order summary ─────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(state.serviceName.ifBlank { "Service" },
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (state.providerName.isNotBlank()) {
                        Text("by ${state.providerName}", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text(
                    "$${state.amount.toInt()}",
                    fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── Card visual ───────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("💳", fontSize = 28.sp)
                    Text("DIAMONDS PAY", fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        letterSpacing = 2.sp)
                }
                Text(
                    formatCardDisplay(state.cardNumber),
                    fontSize = 18.sp, fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("CARD HOLDER", fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            letterSpacing = 1.sp)
                        Text(
                            state.cardHolder.ifBlank { "YOUR NAME" },
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EXPIRES", fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            letterSpacing = 1.sp)
                        Text(
                            state.expiry.ifBlank { "MM/YY" },
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Card number ───────────────────────────────────────────────────
        OutlinedTextField(
            value         = state.cardNumber,
            onValueChange = viewModel::onCardNumberChange,
            label         = { Text("Card Number") },
            placeholder   = { Text("1234 5678 9012 3456") },
            singleLine    = true,
            isError       = state.cardNumberError != null,
            supportingText = state.cardNumberError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction    = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Card holder ───────────────────────────────────────────────────
        OutlinedTextField(
            value         = state.cardHolder,
            onValueChange = viewModel::onCardHolderChange,
            label         = { Text("Cardholder Name") },
            placeholder   = { Text("NAME AS ON CARD") },
            singleLine    = true,
            isError       = state.cardHolderError != null,
            supportingText = state.cardHolderError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction      = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Expiry + CVV ──────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value         = state.expiry,
                onValueChange = viewModel::onExpiryChange,
                label         = { Text("Expiry") },
                placeholder   = { Text("MM/YY") },
                singleLine    = true,
                isError       = state.expiryError != null,
                supportingText = state.expiryError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value         = state.cvv,
                onValueChange = viewModel::onCvvChange,
                label         = { Text("CVV") },
                placeholder   = { Text("123") },
                singleLine    = true,
                isError       = state.cvvError != null,
                supportingText = state.cvvError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // ── Declined banner ───────────────────────────────────────────────
        if (state.paymentDeclined) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "❌  Card declined. Please check your details or use a different card.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp
                )
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }

        // ── Pay button ────────────────────────────────────────────────────
        Button(
            onClick  = { viewModel.processPayment(bookingId) },
            enabled  = !state.isProcessing,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color       = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(12.dp))
                Text("Processing…", fontSize = 16.sp)
            } else {
                Text("Pay $${state.amount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Skip for demo
        androidx.compose.material3.TextButton(
            onClick  = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip Payment (Demo)", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // ── Accepted cards ────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔒 Secure payment  ·  ", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("VISA  MC  AMEX  DISCOVER", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatCardDisplay(input: String): String {
    val digits = input.filter { it.isDigit() }
    val padded = digits.padEnd(16, '•')
    return padded.chunked(4).joinToString("  ")
}
