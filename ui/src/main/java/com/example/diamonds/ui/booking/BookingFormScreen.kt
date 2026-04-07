package com.example.diamonds.ui.booking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BookingFormScreen(
    providerId: String,
    serviceId: String,
    onBookingCreated: (bookingId: String) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state by viewModel.formState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(providerId, serviceId) {
        viewModel.prepareBookingForm(providerId, serviceId)
    }

    LaunchedEffect(state.bookingSuccess) {
        if (state.bookingSuccess) {
            val id = state.createdBookingId ?: return@LaunchedEffect
            viewModel.clearBookingSuccess()
            onBookingCreated(id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Service summary card
        state.service?.let { service ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Booking", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text(service.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    state.provider?.let { Text("with ${it.name}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer) }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("Price", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("$${service.basePrice.toInt()}", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text("Duration", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("${service.duration} min")
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("Schedule", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.date,
                onValueChange = viewModel::onDateChange,
                label = { Text("Date") },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                isError = state.fieldErrors.containsKey(BookingField.DATE),
                supportingText = state.fieldErrors[BookingField.DATE]?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = state.time,
                onValueChange = viewModel::onTimeChange,
                label = { Text("Time") },
                placeholder = { Text("HH:MM") },
                singleLine = true,
                isError = state.fieldErrors.containsKey(BookingField.TIME),
                supportingText = state.fieldErrors[BookingField.TIME]?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Location", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.address,
            onValueChange = viewModel::onAddressChange,
            label = { Text("Service Address") },
            singleLine = true,
            isError = state.fieldErrors.containsKey(BookingField.ADDRESS),
            supportingText = state.fieldErrors[BookingField.ADDRESS]?.let { msg -> { Text(msg) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text("Notes (optional)", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Special instructions…") },
            minLines = 3,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                Text(error ?: "", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = viewModel::submitBooking,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Review & Confirm", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
