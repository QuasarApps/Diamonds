package com.example.diamonds.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.diamonds.domain.repository.UserSession
import com.example.diamonds.ui.cleaner.CleanerViewModel

// ── Customer Home Tab ──────────────────────────────────────────────────────────

@Composable
fun CustomerHomeTab(displayName: String, onStartBooking: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Welcome, $displayName!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "What would you like cleaned today?",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onStartBooking),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("🔍  Find a Cleaner", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Browse cleaners near you and book a service in minutes.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Popular Services", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                listOf("🏠 House Clean", "🏢 Office Clean", "🧹 Deep Clean", "🪟 Window Clean").forEach { svc ->
                    Text(
                        svc,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onStartBooking)
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = onStartBooking, modifier = Modifier.fillMaxWidth()) {
            Text("Book a Service")
        }
    }
}

// ── Cleaner Dashboard Tab ──────────────────────────────────────────────────────
// Used by both INDEPENDENT and EMPLOYED individual cleaners.

@Composable
fun CleanerDashboardTab(
    session: UserSession,
    viewModel: CleanerViewModel = hiltViewModel()
) {
    val dashState by viewModel.dashboardState.collectAsState()
    val displayName = session.displayName
        ?: session.email.substringBefore("@").replaceFirstChar { it.uppercase() }

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(8.dp))

        Text("Hello, $displayName!", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Show employer badge for employed cleaners
        if (session.cleanerType == CleanerType.EMPLOYED) {
            Spacer(Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    "🏢  Employed cleaner",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Here's your day at a glance.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Today's Jobs", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                if (dashState.todayCount == 0) {
                    Text(
                        "No jobs scheduled for today. Check back soon!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        "${dashState.todayCount} job${if (dashState.todayCount > 1) "s" else ""} scheduled today",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("New Requests", fontWeight = FontWeight.SemiBold)
                    if (dashState.pendingCount > 0) {
                        Badge { Text("${dashState.pendingCount}") }
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (dashState.pendingCount == 0) {
                    Text(
                        "No new booking requests right now.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        "${dashState.pendingCount} request${if (dashState.pendingCount > 1) "s" else ""} awaiting your response",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("This Week's Earnings", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$${String.format("%.2f", dashState.weekEarnings)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
