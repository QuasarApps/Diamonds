package com.example.diamonds.ui.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.domain.model.ClaimType
import com.example.diamonds.ui.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileClaimScreen(
    bookingId: String,
    currentUserId: String,
    currentUserRole: String,
    onBack: () -> Unit = {},
    onClaimFiled: (claimId: String) -> Unit = {},
    viewModel: FileClaimViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted) {
            state.submittedClaim?.let { onClaimFiled(it.id) }
        }
    }

    val claimTypes = if (currentUserRole == "CLEANER") {
        listOf(
            ClaimType.DANGEROUS_PROPERTY,
            ClaimType.EXCEEDINGLY_DIRTY,
            ClaimType.INAPPROPRIATE_BEHAVIOR_CLIENT
        )
    } else {
        listOf(
            ClaimType.INCOMPLETE_SERVICE,
            ClaimType.UNSATISFACTORY_SERVICE,
            ClaimType.PROPERTY_DAMAGE,
            ClaimType.INAPPROPRIATE_BEHAVIOR_PROVIDER
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.file_a_claim)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.claim_what_happened), style = MaterialTheme.typography.titleMedium)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                claimTypes.forEach { type ->
                    FilterChip(
                        selected = state.selectedClaimType == type,
                        onClick = { viewModel.onClaimTypeSelected(type) },
                        label = {
                            Text(
                                type.name.replace("_", " ").lowercase()
                                    .replaceFirstChar { it.uppercase() })
                        }
                    )
                }
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text(stringResource(R.string.claim_describe_issue)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            // TODO: Add image evidence picker (PickMultipleVisualMedia) in future

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.submitClaim(currentUserId, currentUserRole) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) CircularProgressIndicator(
                    Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                else Text(stringResource(R.string.submit_claim))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimDetailScreen(
    claimId: String,
    onBack: () -> Unit = {},
    viewModel: ClaimDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.claim_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.claim != null) {
            val claim = state.claim!!
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.status), style = MaterialTheme.typography.labelMedium)
                            AssistChip(onClick = {}, label = { Text(claim.status.name) })
                        }
                        Text(stringResource(R.string.claim_type_label, claim.claimType.name.replace("_", " ")))
                        Text(stringResource(R.string.claim_filed_label, claim.createdAt))
                        HorizontalDivider()
                        Text(claim.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                claim.resolutionNotes?.let {
                    Card(
                        Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.claim_resolution), style = MaterialTheme.typography.titleSmall)
                            Text(it)
                            claim.refundAmount?.let { amt ->
                                Text(
                                    "Refund: $${String.format("%.2f", amt)}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: stringResource(R.string.claim_not_found))
            }
        }
    }
}
