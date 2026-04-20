package com.example.diamonds.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diamonds.domain.model.CleaningType

/** Human-readable labels and icons for each cleaning type. */
val cleaningTypeLabels: Map<CleaningType, Pair<String, String>> = mapOf(
    CleaningType.STANDARD to ("🏠" to "Standard"),
    CleaningType.DEEP_CLEAN to ("🧹" to "Deep Clean"),
    CleaningType.END_OF_TENANCY to ("🔑" to "End of Tenancy"),
    CleaningType.POST_CONSTRUCTION to ("🏗️" to "Post-Construction"),
    CleaningType.CARPET_AND_UPHOLSTERY to ("🛋️" to "Carpet & Upholstery"),
    CleaningType.WINDOW_CLEANING to ("🪟" to "Window Cleaning"),
    CleaningType.OVEN_AND_APPLIANCE to ("🍳" to "Oven & Appliance"),
    CleaningType.MOVE_IN_MOVE_OUT to ("📦" to "Move In/Out"),
    CleaningType.OFFICE_COMMERCIAL to ("🏢" to "Office/Commercial")
)

/**
 * Single-select chip grid for picking a [CleaningType].
 * Pass [selected] = null to show no selection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CleaningTypeSelector(
    selected: CleaningType?,
    onSelect: (CleaningType?) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CleaningType.entries.forEach { type ->
            val (icon, label) = cleaningTypeLabels[type] ?: ("🔷" to type.name)
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(if (selected == type) null else type) },
                label = { Text("$icon $label") }
            )
        }
    }
}

/**
 * Multi-select variant for cleaners setting specializations.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CleaningTypeMultiSelector(
    selected: List<CleaningType>,
    onToggle: (CleaningType) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CleaningType.entries.forEach { type ->
            val (icon, label) = cleaningTypeLabels[type] ?: ("🔷" to type.name)
            FilterChip(
                selected = type in selected,
                onClick = { onToggle(type) },
                label = { Text("$icon $label") }
            )
        }
    }
}
