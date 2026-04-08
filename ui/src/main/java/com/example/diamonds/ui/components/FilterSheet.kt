package com.example.diamonds.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diamonds.domain.model.ServiceCategory

/**
 * Filter criteria selected by the user.
 */
data class FilterCriteria(
    val category: ServiceCategory? = null,
    val minRating: Float = 0f,
    val maxPrice: Float = 500f
)

/**
 * A bottom sheet with advanced filter options for provider search:
 * category selection, minimum rating, max price.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    currentFilters: FilterCriteria,
    onApply: (FilterCriteria) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCategory by remember { mutableStateOf(currentFilters.category) }
    var minRating by remember { mutableFloatStateOf(currentFilters.minRating) }
    var maxPrice by remember { mutableFloatStateOf(currentFilters.maxPrice) }

    val categoryLabels = listOf(
        null to "All",
        ServiceCategory.APARTMENT_CLEANING to "Apartment",
        ServiceCategory.HOUSE_CLEANING to "House",
        ServiceCategory.DEEP_CLEANING to "Deep Clean",
        ServiceCategory.OFFICE_CLEANING to "Office",
        ServiceCategory.CARPET_CLEANING to "Carpet",
        ServiceCategory.WINDOW_CLEANING to "Windows",
        ServiceCategory.POST_CONSTRUCTION to "Post-Build"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            // ── Category ──────────────────────────────────────────────────
            Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
            Text("Minimum Rating: ${"%.1f".format(minRating)} ★", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Slider(
                value = minRating,
                onValueChange = { minRating = it },
                valueRange = 0f..5f,
                steps = 9
            )

            // ── Max price ─────────────────────────────────────────────────
            Text("Max Price: $${maxPrice.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Slider(
                value = maxPrice,
                onValueChange = { maxPrice = it },
                valueRange = 0f..1000f,
                steps = 19
            )

            Spacer(Modifier.height(8.dp))

            // ── Actions ───────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = {
                        selectedCategory = null
                        minRating = 0f
                        maxPrice = 500f
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Reset") }

                Button(
                    onClick = {
                        onApply(FilterCriteria(selectedCategory, minRating, maxPrice))
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Apply Filters") }
            }
        }
    }
}
