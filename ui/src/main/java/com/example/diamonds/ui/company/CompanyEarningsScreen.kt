package com.example.diamonds.ui.company

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.common.util.DateTimeFormatUtil
import com.example.diamonds.ui.components.PullToRefreshLayout

/**
 * Revenue dashboard for a Company account.
 *
 * Shows:
 *  - This week's total revenue + job count
 *  - Monthly KPI row (revenue / jobs / avg)
 *  - Per-cleaner revenue breakdown
 *  - Recent completed bookings list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyEarningsScreen(
    viewModel: CompanyEarningsViewModel = hiltViewModel()
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
        // ── This week hero ────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("This Week's Revenue", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$${String.format("%.2f", state.weekTotal)}",
                        fontSize = 36.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${state.weekJobCount} job${if (state.weekJobCount != 1) "s" else ""} completed across ${state.teamSize} cleaner${if (state.teamSize != 1) "s" else ""}",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // ── Monthly KPIs ──────────────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("This Month", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CompanyKpi("Revenue",  "$${String.format("%.2f", state.monthTotal)}")
                        CompanyKpi("Jobs",     "${state.monthJobCount}")
                        CompanyKpi("Avg / Job",
                            if (state.monthJobCount > 0)
                                "$${String.format("%.0f", state.monthTotal / state.monthJobCount)}"
                            else "—"
                        )
                    }
                }
            }
        }

        // ── Per-cleaner breakdown ─────────────────────────────────────────
        if (state.cleanerBreakdown.isNotEmpty()) {
            item {
                Text("By Cleaner", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp))
            }
            items(state.cleanerBreakdown, key = { row: CleanerRevenueRow -> row.cleanerName }) { row ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(row.cleanerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${row.jobCount} job${if (row.jobCount != 1) "s" else ""} this month",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$${row.revenue.toInt()}",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // ── Recent completed bookings ─────────────────────────────────────
        if (state.recentCompleted.isNotEmpty()) {
            item {
                Text("Recent Completed Jobs", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp))
            }
            items(state.recentCompleted, key = { item: CompanyBookingItem -> item.booking.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.serviceName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                "${DateTimeFormatUtil.formatDateForDisplay(item.booking.scheduledDate)}  ·  ${item.cleanerName}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$${item.servicePrice.toInt()}",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (state.recentCompleted.isEmpty() && !state.isLoading) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💰", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No completed jobs yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }   // end LazyColumn
    }   // end PullToRefreshLayout
}       // end CompanyEarningsScreen

@Composable
private fun CompanyKpi(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
