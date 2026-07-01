package com.example.diamonds.ui.cleaner

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.Service
import com.example.diamonds.ui.R
import com.example.diamonds.ui.components.PullToRefreshLayout

/**
 * Lets a cleaner or company manage their listed services:
 * toggle active/inactive, and navigate to add/edit screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceManagementScreen(
    onAddService: () -> Unit = {},
    onEditService: (serviceId: String) -> Unit = {},
    viewModel: CleanerProfileViewModel = hiltViewModel()
) {
    val state by viewModel.serviceManageState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadServices() }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshServices() }
    ) {
    Box(Modifier.fillMaxSize()) {
        if (state.isLoading && !isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (state.services.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🧹", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.no_services_yet), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.add_first_service_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onAddService) { Text(stringResource(R.string.add_service)) }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        stringResource(
                            R.string.services_active_count,
                            state.services.count { it.isActive },
                            state.services.size
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(state.services, key = { it.id }) { svc ->
                    ServiceCard(
                        service = svc,
                        onToggle = { viewModel.toggleServiceActive(svc) },
                        onEdit   = { onEditService(svc.id) }
                    )
                }
            }
        }

        if (!state.isLoading) {
            ExtendedFloatingActionButton(
                onClick = onAddService,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                icon = { Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.add_service)) }
            )
        }
    }
    } // end PullToRefreshLayout
}

@Composable
private fun ServiceCard(service: Service, onToggle: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = if (service.isActive) CardDefaults.cardColors()
        else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(service.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        service.category.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = service.isActive, onCheckedChange = { onToggle() })
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("$${service.basePrice.toInt()}", fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.service_duration_minutes, service.duration), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
            }
            service.description.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(4.dp))
                Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2)
            }
        }
    }
}

