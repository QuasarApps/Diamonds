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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.diamonds.domain.model.LocationDetail
import com.example.diamonds.domain.model.LocationType
import com.example.diamonds.ui.R

private val locationTypeIcons: Map<LocationType, String> = mapOf(
    LocationType.APARTMENT to "🏢",
    LocationType.HOUSE to "🏠",
    LocationType.STUDIO to "🛏️",
    LocationType.OFFICE to "💼",
    LocationType.RETAIL to "🛍️",
    LocationType.WAREHOUSE to "🏭",
    LocationType.AIRBNB to "🔑",
    LocationType.OTHER to "📍"
)

@Composable
private fun locationTypeLabel(type: LocationType): String = when (type) {
    LocationType.APARTMENT -> stringResource(R.string.category_apartment)
    LocationType.HOUSE -> stringResource(R.string.category_house)
    LocationType.STUDIO -> stringResource(R.string.location_type_studio)
    LocationType.OFFICE -> stringResource(R.string.category_office)
    LocationType.RETAIL -> stringResource(R.string.location_type_retail)
    LocationType.WAREHOUSE -> stringResource(R.string.location_type_warehouse)
    LocationType.AIRBNB -> stringResource(R.string.location_type_airbnb)
    LocationType.OTHER -> stringResource(R.string.location_type_other)
}

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
                val icon = locationTypeIcons[type] ?: "📍"
                val label = locationTypeLabel(type)
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
                label = { Text(stringResource(R.string.rooms)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = detail.bathroomCount.toString(),
                onValueChange = { v ->
                    v.toIntOrNull()?.let { onDetailChange(detail.copy(bathroomCount = it)) }
                },
                label = { Text(stringResource(R.string.bathrooms)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = detail.sqFootage?.toString() ?: "",
                onValueChange = { v ->
                    onDetailChange(detail.copy(sqFootage = if (v.isBlank()) null else v.toIntOrNull()))
                },
                label = { Text(stringResource(R.string.sq_ft_optional)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
