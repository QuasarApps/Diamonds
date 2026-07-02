package com.example.diamonds.ui.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.ServiceCategory
import com.example.diamonds.ui.R
import com.example.diamonds.ui.components.FilterCriteria
import com.example.diamonds.ui.components.FilterSection
import com.example.diamonds.ui.components.PullToRefreshLayout
import com.example.diamonds.ui.components.cleaningTypeIconLabel

private val categories = listOf(
    null to R.string.category_all,
    ServiceCategory.APARTMENT_CLEANING to R.string.category_apartment,
    ServiceCategory.HOUSE_CLEANING to R.string.category_house,
    ServiceCategory.DEEP_CLEANING to R.string.category_deep_clean,
    ServiceCategory.OFFICE_CLEANING to R.string.category_office,
    ServiceCategory.CARPET_CLEANING to R.string.category_carpet,
    ServiceCategory.WINDOW_CLEANING to R.string.category_windows,
    ServiceCategory.POST_CONSTRUCTION to R.string.category_post_build
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSearchScreen(
    onProviderSelected: (String) -> Unit,
    onViewRatings: (String) -> Unit = {},
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state by viewModel.searchState.collectAsState()
    val error by viewModel.error.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(FilterCriteria()) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadProviders() }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshProviders() }
    ) {
    Column(modifier = Modifier.fillMaxSize()) {

        // Category filter chips + advanced filter button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categories) { (cat, labelRes) ->
                    FilterChip(
                        selected = state.selectedCategory == cat,
                        onClick  = { viewModel.selectCategory(cat) },
                        label    = { Text(stringResource(labelRes)) }
                    )
                }
            }
            IconButton(onClick = { showFilterSheet = !showFilterSheet }) {
                Text("⚙️", fontSize = 20.sp)
            }
        }

        // Inline expandable filter section (replaces former ModalBottomSheet)
        FilterSection(
            expanded = showFilterSheet,
            currentFilters = filters,
            onApply = { newFilters ->
                filters = newFilters
                showFilterSheet = false
                viewModel.selectCategory(newFilters.category)
                viewModel.selectSpecialization(newFilters.specialization)
            },
            onDismiss = { showFilterSheet = false }
        )

        // Use when/else instead of return@Column so the composition tree is
        // always structurally identical.  Early-returning from a Column lambda
        // removes child groups mid-recomposition → IndexOutOfBoundsException.
        when {
            state.isLoading -> {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.finding_cleaners), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            state.providers.isEmpty() -> {
                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("😕", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.no_cleaners_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            else -> {
                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.providers, key = { it.id }) { provider ->
                        ProviderCard(
                            provider     = provider,
                            onClick      = { onProviderSelected(provider.id) },
                            onViewRatings = { onViewRatings(provider.id) }
                        )
                    }
                }
            }
        }
    }
    } // end PullToRefreshLayout
}

@Composable
private fun ProviderCard(provider: Provider, onClick: () -> Unit, onViewRatings: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar placeholder
            Card(
                modifier = Modifier.size(52.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(provider.name.first().uppercase(), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(provider.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onViewRatings)
                ) {
                    Text("⭐ ${provider.rating}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    Text(pluralStringResource(R.plurals.review_count_inline, provider.reviewCount, provider.reviewCount), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.chevron_link), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                CleanerTypeBadge(provider)
                // Specialization badges (up to 3)
                if (provider.specializations.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        provider.specializations.take(3).forEach { spec ->
                            AssistChip(
                                onClick = {},
                                label = { Text(cleaningTypeIconLabel(spec), fontSize = 11.sp) }
                            )
                        }
                    }
                }
                provider.bio?.let { bio ->
                    Spacer(Modifier.height(4.dp))
                    Text(bio, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
        }
    }
}

/**
 * Small pill showing whether this provider is an independent cleaner,
 * employed by a company, or is a cleaning company itself.
 *
 * The heuristic for "company" (vs independent) is simply checking for
 * [CleanerType.INDEPENDENT] + a non-null [Provider.employerName] absence
 * and whether the bio implies a team. In production this would be a first-
 * class field (e.g. ProviderKind.COMPANY).
 */
@Composable
private fun CleanerTypeBadge(provider: Provider) {
    // A company is an INDEPENDENT entry that employs others.
    // We detect it by the absence of an employer + a company-style name.
    // In a real backend this would be a proper ProviderKind enum.
    val isCompany = provider.cleanerType == CleanerType.INDEPENDENT
            && (provider.name.contains("Co.", ignoreCase = true)
                || provider.name.contains("Company", ignoreCase = true)
                || provider.name.contains("Ltd", ignoreCase = true)
                || provider.name.contains("Services", ignoreCase = true))

    val (emoji, label, color) = when {
        isCompany ->
            Triple("🏢", stringResource(R.string.badge_company), MaterialTheme.colorScheme.tertiary)
        provider.cleanerType == CleanerType.EMPLOYED ->
            Triple("🏢", stringResource(R.string.badge_via_company, provider.employerName ?: stringResource(R.string.a_company)), MaterialTheme.colorScheme.secondary)
        else ->
            Triple("🧑‍💼", stringResource(R.string.badge_independent), MaterialTheme.colorScheme.primary)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 11.sp)
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}
