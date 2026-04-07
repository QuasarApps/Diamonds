package com.example.diamonds.ui.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.ServiceCategory

private val categories = listOf(
    null to "All",
    ServiceCategory.APARTMENT_CLEANING to "Apartment",
    ServiceCategory.HOUSE_CLEANING to "House",
    ServiceCategory.DEEP_CLEANING to "Deep Clean",
    ServiceCategory.OFFICE_CLEANING to "Office",
    ServiceCategory.CARPET_CLEANING to "Carpet",
    ServiceCategory.WINDOW_CLEANING to "Windows",
    ServiceCategory.POST_CONSTRUCTION to "Post-Build"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSearchScreen(
    onProviderSelected: (String) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state by viewModel.searchState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadProviders() }

    Column(modifier = Modifier.fillMaxSize()) {

        // Category filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { (cat, label) ->
                FilterChip(
                    selected = state.selectedCategory == cat,
                    onClick  = { viewModel.selectCategory(cat) },
                    label    = { Text(label) }
                )
            }
        }

        if (state.isLoading) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Finding cleaners near you…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        if (error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (state.providers.isEmpty() && !state.isLoading) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("😕", fontSize = 40.sp)
                Spacer(Modifier.height(12.dp))
                Text("No cleaners found in your area.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.providers, key = { it.id }) { provider ->
                ProviderCard(provider = provider, onClick = { onProviderSelected(provider.id) })
            }
        }
    }
}

@Composable
private fun ProviderCard(provider: Provider, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐ ${provider.rating}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    Text("  ·  ${provider.reviewCount} reviews", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                // Cleaner type badge
                CleanerTypeBadge(provider)
                provider.bio?.let { bio ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        bio,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
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
            Triple("🏢", "Company", MaterialTheme.colorScheme.tertiary)
        provider.cleanerType == CleanerType.EMPLOYED ->
            Triple("🏢", "Via ${provider.employerName ?: "a company"}", MaterialTheme.colorScheme.secondary)
        else ->
            Triple("🧑‍💼", "Independent", MaterialTheme.colorScheme.primary)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 11.sp)
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}
