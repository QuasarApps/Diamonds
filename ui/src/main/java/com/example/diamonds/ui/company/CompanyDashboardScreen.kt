package com.example.diamonds.ui.company

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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

/**
 * Dashboard for a Company account — shows a business-level overview:
 * team size, pending requests across all cleaners, today's activity,
 * and weekly revenue.
 */
@Composable
fun CompanyDashboardScreen(
    viewModel: CompanyViewModel = hiltViewModel()
) {
    val state by viewModel.dashboardState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(8.dp))
        Text(state.companyName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Company overview",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))

        // ── KPI row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                emoji = "👥",
                label = "Team",
                value = "${state.teamSize}"
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                emoji = "⚡",
                label = "Active",
                value = "${state.activeJobCount}"
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                emoji = "📅",
                label = "Today",
                value = "${state.todayJobCount}"
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Pending requests banner ────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = if (state.pendingCount > 0)
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            else CardDefaults.cardColors()
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🔔", fontSize = 28.sp)
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Pending Requests", fontWeight = FontWeight.SemiBold)
                        if (state.pendingCount > 0) {
                            Badge { Text("${state.pendingCount}") }
                        }
                    }
                    Text(
                        if (state.pendingCount == 0) "No requests awaiting approval."
                        else "${state.pendingCount} request${if (state.pendingCount > 1) "s" else ""} across your team need${if (state.pendingCount == 1) "s" else ""} action.",
                        fontSize = 13.sp,
                        color = if (state.pendingCount > 0)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Weekly earnings ────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("This Week's Revenue", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "$${String.format("%.2f", state.weekEarnings)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Completed jobs",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KpiCard(modifier: Modifier = Modifier, emoji: String, label: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
