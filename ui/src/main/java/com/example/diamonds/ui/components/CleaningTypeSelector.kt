package com.example.diamonds.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.diamonds.domain.model.CleaningType
import com.example.diamonds.ui.R

/** Emoji icon for each cleaning type. */
private val cleaningTypeIcons: Map<CleaningType, String> = mapOf(
    CleaningType.STANDARD to "🏠",
    CleaningType.DEEP_CLEAN to "🧹",
    CleaningType.END_OF_TENANCY to "🔑",
    CleaningType.POST_CONSTRUCTION to "🏗️",
    CleaningType.CARPET_AND_UPHOLSTERY to "🛋️",
    CleaningType.WINDOW_CLEANING to "🪟",
    CleaningType.OVEN_AND_APPLIANCE to "🍳",
    CleaningType.MOVE_IN_MOVE_OUT to "📦",
    CleaningType.OFFICE_COMMERCIAL to "🏢"
)

/** Localized human-readable label for a [CleaningType]. */
@Composable
fun cleaningTypeLabel(type: CleaningType): String = when (type) {
    CleaningType.STANDARD -> stringResource(R.string.cleaning_type_standard)
    CleaningType.DEEP_CLEAN -> stringResource(R.string.cleaning_type_deep_clean)
    CleaningType.END_OF_TENANCY -> stringResource(R.string.cleaning_type_end_of_tenancy)
    CleaningType.POST_CONSTRUCTION -> stringResource(R.string.cleaning_type_post_construction)
    CleaningType.CARPET_AND_UPHOLSTERY -> stringResource(R.string.cleaning_type_carpet)
    CleaningType.WINDOW_CLEANING -> stringResource(R.string.cleaning_type_window)
    CleaningType.OVEN_AND_APPLIANCE -> stringResource(R.string.cleaning_type_oven)
    CleaningType.MOVE_IN_MOVE_OUT -> stringResource(R.string.cleaning_type_move)
    CleaningType.OFFICE_COMMERCIAL -> stringResource(R.string.cleaning_type_office)
}

/** Emoji + localized label for a [CleaningType], e.g. "🏠 Standard". */
@Composable
fun cleaningTypeIconLabel(type: CleaningType): String =
    "${cleaningTypeIcons[type] ?: "🔷"} ${cleaningTypeLabel(type)}"

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
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(if (selected == type) null else type) },
                label = { Text(cleaningTypeIconLabel(type)) }
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
            FilterChip(
                selected = type in selected,
                onClick = { onToggle(type) },
                label = { Text(cleaningTypeIconLabel(type)) }
            )
        }
    }
}
