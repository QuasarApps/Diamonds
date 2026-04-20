package com.example.diamonds.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.diamonds.domain.model.LocationDetail
import com.example.diamonds.domain.model.LocationType

private val locationTypeLabels: Map<LocationType, Pair<String, String>> = mapOf(
    LocationType.APARTMENT to ("🏢" to "Apartment"),
    LocationType.HOUSE to ("🏠" to "House"),
    LocationType.STUDIO to ("🛏️" to "Studio"),
    LocationType.OFFICE to ("💼" to "Office"),
    LocationType.RETAIL to ("🛍️" to "Retail"),
    LocationType.WAREHOUSE to ("🏭" to "Warehouse"),
    LocationType.AIRBNB to ("🔑" to "Airbnb/Short-term"),
    LocationType.OTHER to ("📍" to "Other")
)

/**
 * Composite component that allows selecting a [LocationType] and entering room/bathroom/sqft details.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationTypeSelector(
    detail: LocationDetail,
    onDetailChange: (LocationDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Location type chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LocationType.entries.forEach { type ->
                val (icon, label) = locationTypeLabels[type] ?: ("📍" to type.name)
                FilterChip(
                    selected = detail.locationType == type,
                    onClick = { onDetailChange(detail.copy(locationType = type)) },
                    label = { Text("$icon $label") }
                )
            }
        }

        // Numeric fields
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = detail.roomCount.toString(),
                onValueChange = { v ->
                    v.toIntOrNull()?.let { onDetailChange(detail.copy(roomCount = it)) }
                },
                label = { Text("Rooms") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = detail.bathroomCount.toString(),
                onValueChange = { v ->
                    v.toIntOrNull()?.let { onDetailChange(detail.copy(bathroomCount = it)) }
                },
                label = { Text("Bathrooms") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = detail.sqFootage?.toString() ?: "",
                onValueChange = { v ->
                    onDetailChange(detail.copy(sqFootage = if (v.isBlank()) null else v.toIntOrNull()))
                },
                label = { Text("Sq Ft (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
