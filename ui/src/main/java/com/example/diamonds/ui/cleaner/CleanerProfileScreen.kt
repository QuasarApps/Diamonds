package com.example.diamonds.ui.cleaner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.ui.R
import com.example.diamonds.domain.repository.UserSession
import com.example.diamonds.ui.components.CleaningTypeMultiSelector
import com.example.diamonds.ui.components.PullToRefreshLayout
import com.example.diamonds.ui.components.cleaningTypeLabels

/**
 * Profile screen for individual cleaners (INDEPENDENT or EMPLOYED).
 *
 * Shows:
 *  - Avatar + name + star rating + review count
 *  - Employment type badge
 *  - Editable bio and phone fields
 *  - Service summary with a "Manage Services" button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerProfileScreen(
    session: UserSession,
    onManageServices: () -> Unit,
    onLanguage: () -> Unit = {},
    onHelpCenter: () -> Unit = {},
    viewModel: CleanerProfileViewModel = hiltViewModel()
) {
    val state       by viewModel.profileState.collectAsState()
    val editName    by viewModel.editName.collectAsState()
    val editBio     by viewModel.editBio.collectAsState()
    val editPhone   by viewModel.editPhone.collectAsState()
    val editSpecializations by viewModel.editSpecializations.collectAsState()
    val error       by viewModel.error.collectAsState()
    val snackbar    = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    val profileSavedMessage = stringResource(R.string.profile_saved)

    LaunchedEffect(Unit) { viewModel.loadProfile() }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    LaunchedEffect(state.savedSuccess) {
        if (state.savedSuccess) {
            snackbar.showSnackbar(profileSavedMessage)
            viewModel.clearSavedSuccess()
        }
    }

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it) }
    }

    if (state.isLoading && !isRefreshing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refreshProfile() }
    ) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Avatar + headline ─────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    modifier = Modifier.size(72.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            editName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 30.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    val yourNamePlaceholder = stringResource(R.string.your_name_placeholder)
                    Text(editName.ifBlank { yourNamePlaceholder },
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐ ${state.averageRating}", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.provider_review_count, state.reviewCount), fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    // Employment badge
                    val (badgeText, badgeColor) = when (session.cleanerType) {
                        CleanerType.EMPLOYED -> (stringResource(R.string.badge_employed) to MaterialTheme.colorScheme.secondary)
                        CleanerType.COMPANY  -> (stringResource(R.string.badge_company_emoji) to MaterialTheme.colorScheme.tertiary)
                        else                 -> (stringResource(R.string.badge_independent_emoji) to MaterialTheme.colorScheme.primary)
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = badgeColor.copy(alpha = 0.12f)
                        )
                    ) {
                        Text(badgeText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = badgeColor)
                    }
                }
            }

            HorizontalDivider()

            // ── Specializations ───────────────────────────────────────────
            val providerSpecializations = state.provider?.specializations ?: emptyList()
            if (providerSpecializations.isNotEmpty()) {
                Text(stringResource(R.string.specializations), style = MaterialTheme.typography.titleMedium)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    providerSpecializations.forEach { spec ->
                        val (icon, label) = cleaningTypeLabels[spec] ?: ("🔷" to spec.name)
                        AssistChip(onClick = {}, label = { Text("$icon $label", fontSize = 12.sp) })
                    }
                }
                HorizontalDivider()
            }

            // ── Editable fields ───────────────────────────────────────────
            Text(stringResource(R.string.edit_profile), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = editName,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.display_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = editPhone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text(stringResource(R.string.phone_number)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = editBio,
                onValueChange = viewModel::onBioChange,
                label = { Text(stringResource(R.string.bio_about_you)) },
                minLines = 3,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.my_specializations), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            CleaningTypeMultiSelector(
                selected = editSpecializations,
                onToggle = viewModel::onToggleSpecialization
            )

            Button(
                onClick = viewModel::saveProfile,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.save_changes))
                }
            }

            HorizontalDivider()

            // ── Language ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onLanguage
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌐", fontSize = 24.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.language), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            stringResource(R.string.change_app_language), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Help & Support ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onHelpCenter
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("❓", fontSize = 24.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.help_and_support), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            stringResource(R.string.help_support_subtitle), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            // ── Services summary ──────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.my_services), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.services_active_total,
                            state.services.count { it.isActive },
                            state.services.size
                        ),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onManageServices) { Text(stringResource(R.string.manage)) }
            }

            if (state.services.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.no_services_yet_hint),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                state.services.take(3).forEach { svc ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(svc.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(svc.category.name.replace("_", " ").lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$${svc.basePrice.toInt()}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                            if (!svc.isActive) {
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.service_off), fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (state.services.size > 3) {
                    Text(
                        stringResource(R.string.more_count, state.services.size - 3),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    } // end PullToRefreshLayout
}
