package com.example.diamonds.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.LocationDetail
import com.example.diamonds.domain.model.SavedLocation
import com.example.diamonds.ui.R
import com.example.diamonds.ui.components.LocationTypeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLocationsScreen(
    onBack: () -> Unit,
    onLocationSelected: ((SavedLocation) -> Unit)? = null,
    viewModel: SavedLocationsViewModel = hiltViewModel()
) {
    val state by viewModel.savedLocationsState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) viewModel.clearSaveSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_saved_locations)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_location))
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (state.locations.isEmpty()) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📍", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.no_saved_locations), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.tap_plus_add_location),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.locations, key = { it.id }) { loc ->
                        SavedLocationCard(
                            location = loc,
                            onDelete = { viewModel.deleteLocation(loc.id) },
                            onSelect = onLocationSelected?.let { cb -> { cb(loc) } }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLocationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { label, address, detail ->
                viewModel.addLocation(
                    label = label,
                    address = address,
                    locationType = detail.locationType,
                    roomCount = detail.roomCount,
                    bathroomCount = detail.bathroomCount,
                    sqFootage = detail.sqFootage
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SavedLocationCard(
    location: SavedLocation,
    onDelete: () -> Unit,
    onSelect: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect ?: {}
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📍", fontSize = 28.sp)
            Column(Modifier.weight(1f)) {
                Text(location.label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    location.address,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                val detail = buildString {
                    append(
                        location.locationType.name.lowercase().replaceFirstChar { it.uppercase() })
                    append(" · ${location.roomCount} rooms · ${location.bathroomCount} baths")
                    location.sqFootage?.let { append(" · $it sq ft") }
                }
                Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddLocationDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, address: String, detail: LocationDetail) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf(LocationDetail()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_location)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.location_label_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.address)) },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                Text(stringResource(R.string.property_details), fontWeight = FontWeight.SemiBold)
                LocationTypeSelector(detail = detail, onDetailChange = { detail = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (label.isNotBlank() && address.isNotBlank()) onConfirm(
                        label,
                        address,
                        detail
                    )
                },
                enabled = label.isNotBlank() && address.isNotBlank()
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
