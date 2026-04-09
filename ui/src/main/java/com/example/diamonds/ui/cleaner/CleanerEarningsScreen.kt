package com.example.diamonds.ui.cleaner

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.common.util.DateTimeFormatUtil
import com.example.diamonds.ui.components.PullToRefreshLayout

/**
 * Earnings dashboard for an individual cleaner.
 *
 * Shows:
 *  - Weekly totals bar chart (last 7 days)
 *  - Monthly summary card
 *  - Per-booking earnings history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerEarningsScreen(
    viewModel: CleanerEarningsViewModel = hiltViewModel()
) {
    val state by viewModel.earningsState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadEarnings() }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    if (state.isLoading && !isRefreshing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshEarnings() }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
        // ── This week's total ─────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("This Week", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$${String.format("%.2f", state.weekTotal)}",
                        fontSize = 36.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${state.weekCompletedCount} job${if (state.weekCompletedCount != 1) "s" else ""} completed",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // ── Daily bar chart ───────────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Daily Earnings — Last 7 Days", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    DailyBarChart(
                        dailyTotals = state.dailyTotals,
                        modifier    = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        state.dailyTotals.forEach { (day, _) ->
                            Text(
                                day, fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // ── Monthly summary ───────────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("This Month", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EarningsStat("Revenue",  "$${String.format("%.2f", state.monthTotal)}")
                        EarningsStat("Jobs Done","${state.monthCompletedCount}")
                        EarningsStat("Avg / Job",
                            if (state.monthCompletedCount > 0)
                                "$${String.format("%.0f", state.monthTotal / state.monthCompletedCount)}"
                            else "—"
                        )
                    }
                }
            }
        }

        // ── Completed jobs list header ────────────────────────────────────
        if (state.completedBookings.isNotEmpty()) {
            item {
                Text(
                    "Completed Jobs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // ── Per-booking rows ──────────────────────────────────────────────
        items(state.completedBookings, key = { it.booking.id }) { item ->
            EarningsBookingRow(item)
        }

        if (state.completedBookings.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💰", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No completed jobs yet.\nComplete your first job to see earnings here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        } // end LazyColumn
    } // end PullToRefreshLayout
}

// ── Bar chart (Canvas only – no labels inside to avoid key collision) ─────────

@Composable
private fun DailyBarChart(
    dailyTotals: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val maxVal = remember(dailyTotals) {
        dailyTotals.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    }

    Canvas(modifier = modifier) {
        val barCount    = dailyTotals.size.coerceAtLeast(1)
        val barWidth    = size.width / (barCount * 2f)
        val chartHeight = size.height
        val spacing     = barWidth

        dailyTotals.forEachIndexed { idx, (_, amount) ->
            val x    = spacing + idx * (barWidth + spacing)
            val barH = ((amount / maxVal) * chartHeight).toFloat().coerceAtLeast(if (amount > 0) 4f else 0f)
            val y    = chartHeight - barH

            drawRoundRect(
                color        = surfaceColor,
                topLeft      = Offset(x, 0f),
                size         = Size(barWidth, chartHeight),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            if (amount > 0) {
                drawRoundRect(
                    color        = primaryColor,
                    topLeft      = Offset(x, y),
                    size         = Size(barWidth, barH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun EarningsStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EarningsBookingRow(item: CleanerBookingItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.serviceName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "${DateTimeFormatUtil.formatDateForDisplay(item.booking.scheduledDate)}  ·  ${item.clientName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "$${item.servicePrice.toInt()}",
                fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
