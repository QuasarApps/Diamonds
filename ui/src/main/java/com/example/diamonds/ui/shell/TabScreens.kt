package com.example.diamonds.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.repository.UserSession
import com.example.diamonds.ui.R
import com.example.diamonds.ui.cleaner.CleanerViewModel
import com.example.diamonds.ui.components.PullToRefreshLayout

// ── Customer Home Tab ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeTab(displayName: String, onStartBooking: () -> Unit = {}) {
    var showQuickOptions by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Customer home has no remote data — refresh just dismisses itself
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) isRefreshing = false
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.welcome_user, displayName), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.what_to_clean_today),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStartBooking),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.find_a_cleaner_with_icon), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.browse_cleaners_subtitle),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier
            .fillMaxWidth()
            .clickable { showQuickOptions = !showQuickOptions }) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.quick_book_with_icon), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.quick_book_subtitle),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Inline expandable quick booking options (replaces former ModalBottomSheet)
        AnimatedVisibility(
            visible = showQuickOptions,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "🏠" to stringResource(R.string.house_cleaning),
                        "🏢" to stringResource(R.string.office_cleaning),
                        "🧹" to stringResource(R.string.deep_cleaning),
                        "🪟" to stringResource(R.string.window_cleaning),
                        "🏗️" to stringResource(R.string.post_construction),
                        "🧽" to stringResource(R.string.carpet_cleaning),
                    ).forEach { (icon, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStartBooking() }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(icon, fontSize = 22.sp)
                            Spacer(Modifier.width(14.dp))
                            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.popular_services), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                listOf(
                    stringResource(R.string.house_clean_with_icon),
                    stringResource(R.string.office_clean_with_icon),
                    stringResource(R.string.deep_clean_with_icon),
                    stringResource(R.string.window_clean_with_icon)
                ).forEach { svc ->
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
            Text(stringResource(R.string.book_a_service))
        }
    }
    } // end PullToRefreshLayout
}

// ── Cleaner Dashboard Tab ──────────────────────────────────────────────────────
// Used by both INDEPENDENT and EMPLOYED individual cleaners.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerDashboardTab(
    session: UserSession,
    onNavigateToRequests: () -> Unit = {},
    viewModel: CleanerViewModel = hiltViewModel()
) {
    val dashState by viewModel.dashboardState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val displayName = session.displayName
        ?: session.email.substringBefore("@").replaceFirstChar { it.uppercase() }

    LaunchedEffect(Unit) { viewModel.loadDashboard() }
    LaunchedEffect(dashState.isLoading) { if (!dashState.isLoading) isRefreshing = false }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshDashboard() }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(8.dp))

        Text(stringResource(R.string.hello_user, displayName), fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Show employer badge for employed cleaners
        if (session.cleanerType == CleanerType.EMPLOYED) {
            Spacer(Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    stringResource(R.string.employed_cleaner_with_icon),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.day_at_a_glance),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.todays_jobs), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                if (isRefreshing) {
                    Text(
                        "—", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (dashState.todayCount == 0) {
                    Text(
                        stringResource(R.string.no_jobs_today),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        pluralStringResource(R.plurals.jobs_scheduled_today, dashState.todayCount, dashState.todayCount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToRequests)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.new_requests), fontWeight = FontWeight.SemiBold)
                    if (!isRefreshing && dashState.pendingCount > 0) {
                        Badge { Text("${dashState.pendingCount}") }
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (isRefreshing) {
                    Text(
                        "—", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (dashState.pendingCount == 0) {
                    Text(
                        stringResource(R.string.no_new_requests),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        pluralStringResource(R.plurals.requests_awaiting_response, dashState.pendingCount, dashState.pendingCount),
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
                Text(stringResource(R.string.this_weeks_earnings), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isRefreshing) "—"
                    else "$${String.format("%.2f", dashState.weekEarnings)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    } // end PullToRefreshLayout
}
