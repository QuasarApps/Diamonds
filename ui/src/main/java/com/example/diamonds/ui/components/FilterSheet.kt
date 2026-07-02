package com.example.diamonds.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diamonds.domain.model.CleaningType
import com.example.diamonds.domain.model.ServiceCategory
import com.example.diamonds.ui.R

/**
 * Filter criteria selected by the user.
 */
data class FilterCriteria(
    val category: ServiceCategory? = null,
    val minRating: Float = 0f,
    val maxPrice: Float = 500f,
    val specialization: CleaningType? = null
)

/**
 * An inline, expandable/collapsible filter section for provider search.
 *
 * Replaces the former [ModalBottomSheet]-based implementation to keep filters
 * as part of the normal screen flow rather than an overlay interruption.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSection(
    expanded: Boolean,
    currentFilters: FilterCriteria,
    onApply: (FilterCriteria) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember(currentFilters) { mutableStateOf(currentFilters.category) }
    var minRating by remember(currentFilters) { mutableFloatStateOf(currentFilters.minRating) }
    var maxPrice by remember(currentFilters) { mutableFloatStateOf(currentFilters.maxPrice) }
    var selectedSpecialization by remember(currentFilters) { mutableStateOf(currentFilters.specialization) }

    val categoryLabels = listOf(
        null to stringResource(R.string.category_all),
        ServiceCategory.APARTMENT_CLEANING to stringResource(R.string.category_apartment),
        ServiceCategory.HOUSE_CLEANING to stringResource(R.string.category_house),
        ServiceCategory.DEEP_CLEANING to stringResource(R.string.category_deep_clean),
        ServiceCategory.OFFICE_CLEANING to stringResource(R.string.category_office),
        ServiceCategory.CARPET_CLEANING to stringResource(R.string.category_carpet),
        ServiceCategory.WINDOW_CLEANING to stringResource(R.string.category_windows),
        ServiceCategory.POST_CONSTRUCTION to stringResource(R.string.category_post_build)
    )

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider()

            Text(stringResource(R.string.filters), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // ── Category ──────────────────────────────────────────────────
            Text(stringResource(R.string.category), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryLabels.forEach { (cat, label) ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick  = { selectedCategory = cat },
                        label    = { Text(label) }
                    )
                }
            }

            // ── Min rating ────────────────────────────────────────────────
            Text(stringResource(R.string.minimum_rating, "%.1f".format(minRating)), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Slider(
                value = minRating,
                onValueChange = { minRating = it },
                valueRange = 0f..5f,
                steps = 9
            )

            // ── Max price ─────────────────────────────────────────────────
            Text(stringResource(R.string.max_price, maxPrice.toInt()), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Slider(
                value = maxPrice,
                onValueChange = { maxPrice = it },
                valueRange = 0f..1000f,
                steps = 19
            )

            // ── Specialization ────────────────────────────────────────────
            Text(stringResource(R.string.specialization), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            CleaningTypeSelector(
                selected = selectedSpecialization,
                onSelect = { selectedSpecialization = it }
            )

            // ── Actions ───────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = {
                        selectedCategory = null
                        minRating = 0f
                        maxPrice = 500f
                        selectedSpecialization = null
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.reset)) }

                Button(
                    onClick = {
                        onApply(
                            FilterCriteria(
                                selectedCategory,
                                minRating,
                                maxPrice,
                                selectedSpecialization
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.apply_filters)) }
            }

            HorizontalDivider()
        }
    }
}
