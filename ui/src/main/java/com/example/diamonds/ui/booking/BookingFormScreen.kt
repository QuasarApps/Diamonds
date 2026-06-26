package com.example.diamonds.ui.booking

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.common.util.DateTimeFormatUtil
import com.example.diamonds.domain.model.LocationDetail
import com.example.diamonds.ui.R
import com.example.diamonds.ui.components.CleaningTypeSelector
import com.example.diamonds.ui.components.LocationTypeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(
    providerId: String,
    serviceId: String,
    onBookingCreated: (bookingId: String) -> Unit,
    onPickOnMap: () -> Unit = {},
    mapLat: Double? = null,
    mapLng: Double? = null,
    mapAddress: String? = null,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state by viewModel.formState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(providerId, serviceId) {
        viewModel.prepareBookingForm(providerId, serviceId)
    }

    // Apply location selected from the map screen
    LaunchedEffect(mapLat, mapLng, mapAddress) {
        if (mapLat != null && mapLng != null) {
            viewModel.onLocationSelected(mapLat, mapLng, mapAddress ?: "")
        }
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
                    Text(stringResource(R.string.booking), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text(service.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    state.provider?.let { Text(stringResource(R.string.with_provider, it.name), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer) }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text(stringResource(R.string.price), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("$${service.basePrice.toInt()}", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text(stringResource(R.string.duration), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(stringResource(R.string.duration_minutes, service.duration))
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.cleaning_type), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        CleaningTypeSelector(
            selected = state.cleaningType,
            onSelect = viewModel::onCleaningTypeSelected
        )

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.property_details), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        LocationTypeSelector(
            detail = state.locationDetail ?: LocationDetail(),
            onDetailChange = viewModel::onLocationDetailChanged
        )

        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.schedule), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        // ── Date & Time pickers ────────────────────────────────────────
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }

        val datePickerState = rememberDatePickerState()
        val timePickerState = rememberTimePickerState()

        // Date picker dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onDateChange(DateTimeFormatUtil.millisToIsoDate(millis))
                        }
                        showDatePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Time picker dialog
        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text(stringResource(R.string.select_time_title)) },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onTimeChange(
                            DateTimeFormatUtil.toIsoTime(
                                timePickerState.hour,
                                timePickerState.minute
                            )
                        )
                        showTimePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // Read-only date field that opens DatePickerDialog on click
            val dateInteractionSource = remember { MutableInteractionSource() }
            LaunchedEffect(dateInteractionSource) {
                dateInteractionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        showDatePicker = true
                    }
                }
            }
            OutlinedTextField(
                value = if (state.date.isNotBlank()) DateTimeFormatUtil.formatDateForDisplay(state.date) else "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.date)) },
                placeholder = { Text(stringResource(R.string.select_date)) },
                singleLine = true,
                isError = state.fieldErrors.containsKey(BookingField.DATE),
                supportingText = state.fieldErrors[BookingField.DATE]?.let { msg -> { Text(msg) } },
                interactionSource = dateInteractionSource,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            // Read-only time field that opens TimePickerDialog on click
            val timeInteractionSource = remember { MutableInteractionSource() }
            LaunchedEffect(timeInteractionSource) {
                timeInteractionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        showTimePicker = true
                    }
                }
            }
            OutlinedTextField(
                value = if (state.time.isNotBlank()) DateTimeFormatUtil.formatTimeForDisplay(state.time) else "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.time)) },
                placeholder = { Text(stringResource(R.string.select_time)) },
                singleLine = true,
                isError = state.fieldErrors.containsKey(BookingField.TIME),
                supportingText = state.fieldErrors[BookingField.TIME]?.let { msg -> { Text(msg) } },
                interactionSource = timeInteractionSource,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.location), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.address,
            onValueChange = viewModel::onAddressChange,
            label = { Text(stringResource(R.string.service_address)) },
            singleLine = true,
            isError = state.fieldErrors.containsKey(BookingField.ADDRESS),
            supportingText = state.fieldErrors[BookingField.ADDRESS]?.let { msg -> { Text(msg) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Show coordinates if selected via map
        if (state.latitude != null && state.longitude != null) {
            Text(
                "📍 ${String.format("%.5f", state.latitude)}, ${
                    String.format(
                        "%.5f",
                        state.longitude
                    )
                }",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.height(4.dp))
        }

        OutlinedButton(
            onClick = onPickOnMap,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.pick_on_map))
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.notes_optional), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text(stringResource(R.string.special_instructions)) },
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
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.review_and_confirm), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
