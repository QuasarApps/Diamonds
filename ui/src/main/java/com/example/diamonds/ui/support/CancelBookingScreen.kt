package com.example.diamonds.ui.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.CancellationReason
import com.example.diamonds.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelBookingScreen(
    bookingId: String,
    onBack: () -> Unit = {},
    onCancelled: () -> Unit = {},
    viewModel: CancelBookingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isCancelled) {
        if (state.isCancelled) onCancelled()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cancel_booking)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading && state.booking == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.booking?.let { b ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.booking_number, b.id.takeLast(6)),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(stringResource(R.string.booking_date_at_time, b.scheduledDate, b.scheduledTime))
                            Text(
                                "$${String.format("%.2f", b.totalPrice)}",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

                Text(stringResource(R.string.why_cancelling), style = MaterialTheme.typography.titleMedium)

                CancellationReason.entries.forEach { reason ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = state.selectedReason == reason,
                                onClick = { viewModel.onReasonSelected(reason) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = state.selectedReason == reason, onClick = null)
                        Text(reason.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() })
                    }
                }

                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChanged,
                    label = { Text(stringResource(R.string.additional_notes_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = viewModel::confirmCancellation,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && state.selectedReason != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (state.isLoading) CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    else Text(stringResource(R.string.confirm_cancellation))
                }

                Text(
                    stringResource(R.string.full_refund_if_applicable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
