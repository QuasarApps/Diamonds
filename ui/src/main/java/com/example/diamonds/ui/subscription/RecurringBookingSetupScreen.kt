package com.example.diamonds.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.RecurringFrequency
import com.example.diamonds.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBookingSetupScreen(
    providerId: String,
    serviceId: String,
    providerName: String = "",
    serviceName: String = "",
    price: Double = 0.0,
    onSuccess: () -> Unit,
    viewModel: RecurringBookingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(providerId, serviceId) {
        viewModel.initSetup(providerId, serviceId, providerName, serviceName, price)
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.setup_recurring),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Provider & Service info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.provider_label, state.providerName.ifEmpty { providerId }),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    stringResource(R.string.service_label, state.serviceName.ifEmpty { serviceId }),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state.totalPrice > 0) {
                    Text(
                        stringResource(R.string.price_per_visit, String.format("%.2f", state.totalPrice)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Frequency picker
        Text(stringResource(R.string.frequency), style = MaterialTheme.typography.titleMedium)
        var freqExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = freqExpanded, onExpandedChange = { freqExpanded = it }) {
            OutlinedTextField(
                value = state.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = freqExpanded,
                onDismissRequest = { freqExpanded = false }) {
                RecurringFrequency.entries.forEach { freq ->
                    DropdownMenuItem(
                        text = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            viewModel.onFrequencyChanged(freq)
                            freqExpanded = false
                        }
                    )
                }
            }
        }

        // Day picker
        val dayLabel = when (state.frequency) {
            RecurringFrequency.DAILY -> null
            RecurringFrequency.WEEKLY, RecurringFrequency.FORTNIGHTLY -> stringResource(R.string.day_of_week)
            RecurringFrequency.MONTHLY -> stringResource(R.string.day_of_month)
        }
        if (dayLabel != null) {
            Text(dayLabel, style = MaterialTheme.typography.titleMedium)
            if (state.frequency == RecurringFrequency.MONTHLY) {
                var dayExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = it }) {
                    OutlinedTextField(
                        value = "${state.preferredDay}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }) {
                        (1..28).forEach { d ->
                            DropdownMenuItem(
                                text = { Text("$d") },
                                onClick = { viewModel.onDayChanged(d); dayExpanded = false }
                            )
                        }
                    }
                }
            } else {
                val days = listOf(
                    stringResource(R.string.monday),
                    stringResource(R.string.tuesday),
                    stringResource(R.string.wednesday),
                    stringResource(R.string.thursday),
                    stringResource(R.string.friday),
                    stringResource(R.string.saturday),
                    stringResource(R.string.sunday)
                )
                var dayExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = it }) {
                    OutlinedTextField(
                        value = days.getOrElse(state.preferredDay - 1) { days.first() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }) {
                        days.forEachIndexed { idx, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { viewModel.onDayChanged(idx + 1); dayExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        // Time picker (simplified text field)
        Text(stringResource(R.string.preferred_time), style = MaterialTheme.typography.titleMedium)
        var timeExpanded by remember { mutableStateOf(false) }
        val times = listOf(
            "07:00",
            "08:00",
            "09:00",
            "10:00",
            "11:00",
            "12:00",
            "13:00",
            "14:00",
            "15:00",
            "16:00",
            "17:00",
            "18:00"
        )
        ExposedDropdownMenuBox(expanded = timeExpanded, onExpandedChange = { timeExpanded = it }) {
            OutlinedTextField(
                value = state.preferredTime,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = timeExpanded,
                onDismissRequest = { timeExpanded = false }) {
                times.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t) },
                        onClick = { viewModel.onTimeChanged(t); timeExpanded = false }
                    )
                }
            }
        }

        // Address
        Text(stringResource(R.string.address), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.address,
            onValueChange = { viewModel.onAddressChanged(it) },
            placeholder = { Text(stringResource(R.string.enter_your_address)) },
            modifier = Modifier.fillMaxWidth()
        )

        // Error
        if (state.errorMessage != null) {
            Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))

        // Submit button
        FilledTonalButton(
            onClick = { viewModel.submitRecurringBooking() },
            enabled = !state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Text(stringResource(R.string.create_recurring_booking), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
