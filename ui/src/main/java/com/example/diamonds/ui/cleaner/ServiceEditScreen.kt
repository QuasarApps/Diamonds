package com.example.diamonds.ui.cleaner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.Service
import com.example.diamonds.domain.model.ServiceCategory

/**
 * Full-screen form to add or edit a service.
 *
 * @param serviceId The ID of the service to edit, or "new" to create a new one.
 * @param onDone    Called after a successful save or when the user cancels – the caller pops the back-stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceEditScreen(
    serviceId: String,
    onDone: () -> Unit,
    viewModel: CleanerProfileViewModel = hiltViewModel()
) {
    val state by viewModel.serviceManageState.collectAsState()
    val isNew = serviceId == "new"

    // Load services so we can find the one being edited
    LaunchedEffect(Unit) { viewModel.loadServices() }

    // Resolve the existing service once services are loaded
    val existing: Service? = if (isNew) null else state.services.find { it.id == serviceId }

    // Wait for services to load before showing the form (edit mode only)
    if (!isNew && state.isLoading) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Listen for save-success to navigate back
    LaunchedEffect(state.savedServiceId) {
        if (state.savedServiceId != null) {
            viewModel.clearSavedServiceId()
            onDone()
        }
    }

    var title    by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var desc     by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var price    by remember(existing) { mutableStateOf(existing?.basePrice?.toInt()?.toString() ?: "") }
    var duration by remember(existing) { mutableStateOf(existing?.duration?.toString() ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: ServiceCategory.APARTMENT_CLEANING) }
    var catExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (isNew) "New Service" else "Edit Service",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Service Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Price ($)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it.filter { c -> c.isDigit() } },
                label = { Text("Duration (min)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        // Category dropdown
        ExposedDropdownMenuBox(
            expanded = catExpanded,
            onExpandedChange = { catExpanded = !catExpanded }
        ) {
            OutlinedTextField(
                value = category.name.replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = catExpanded,
                onDismissRequest = { catExpanded = false }
            ) {
                ServiceCategory.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = {
                            Text(cat.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() })
                        },
                        onClick = { category = cat; catExpanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    val priceVal    = price.toDoubleOrNull()    ?: return@Button
                    val durationVal = duration.toIntOrNull()    ?: return@Button
                    if (title.isBlank()) return@Button
                    viewModel.saveService(
                        title, desc, priceVal, durationVal, category,
                        serviceId = if (isNew) null else serviceId
                    )
                },
                enabled = !state.isSaving && title.isNotBlank()
                        && price.isNotBlank() && duration.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isNew) "Add Service" else "Save Changes")
                }
            }
        }
    }
}
