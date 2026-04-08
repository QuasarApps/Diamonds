package com.example.diamonds.ui.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.common.util.DateTimeFormatUtil

/**
 * Full ratings & reviews screen for a single provider.
 *
 * Shows:
 *  - Large average rating + total count
 *  - Star breakdown bar chart (5 → 1)
 *  - Scrollable list of individual reviews with star display and comment
 */
@Composable
fun ProviderRatingsScreen(
    providerId: String,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.ratingsState.collectAsState()

    LaunchedEffect(providerId) { viewModel.loadProviderRatings(providerId) }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.reviews.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("⭐", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("No reviews yet", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("Be the first to review ${state.provider?.name ?: "this provider"}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Summary header ────────────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    state.provider?.let {
                        Text(it.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(12.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                String.format("%.1f", state.averageRating),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            StarPicker(
                                selected   = state.averageRating.toInt(),
                                onSelect   = {},
                                enabled    = false,
                                starSizeSp = 18
                            )
                            Text("${state.totalCount} reviews",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            (5 downTo 1).forEach { star ->
                                val count = state.starCounts[star - 1]
                                val frac  = if (state.totalCount > 0)
                                    count.toFloat() / state.totalCount else 0f
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$star★", fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(28.dp))
                                    LinearProgressIndicator(
                                        progress = { frac },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp),
                                        color    = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Text(" $count", fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("${state.totalCount} Review${if (state.totalCount != 1) "s" else ""}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp))
        }

        // ── Individual review cards ───────────────────────────────────────
        items(state.reviews, key = { it.review.id }) { item ->
            ReviewCard(item)
        }
    }
}

@Composable
private fun ReviewCard(item: ReviewWithClientName) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initials avatar
                Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Text(
                        item.clientName.firstOrNull()?.uppercase() ?: "?",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(item.clientName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(item.review.createdAt.toLongOrNull()
                        ?.let { DateTimeFormatUtil.formatTimestampAsDate(it) }
                        ?: item.review.createdAt.take(10),
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StarPicker(
                    selected   = item.review.rating,
                    onSelect   = {},
                    enabled    = false,
                    starSizeSp = 14
                )
            }
            item.review.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(comment, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
