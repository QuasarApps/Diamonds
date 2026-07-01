package com.example.diamonds.ui.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(
    onBack: () -> Unit = {},
    viewModel: HelpCenterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_center)) },
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
        if (state.isLoading && state.articles.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // FAQ section
                item {
                    Text(stringResource(R.string.faq_title), style = MaterialTheme.typography.titleLarge)
                }

                items(state.articles) { article ->
                    var expanded by remember { mutableStateOf(false) }
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .clickable { expanded = !expanded }
                                .padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    article.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    "Toggle"
                                )
                            }
                            AnimatedVisibility(visible = expanded) {
                                Text(
                                    article.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                // My Tickets section
                if (state.tickets.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.my_tickets), style = MaterialTheme.typography.titleLarge)
                    }
                    items(state.tickets) { ticket ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        ticket.subject,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    AssistChip(onClick = {}, label = { Text(ticket.status.name) })
                                }
                                Text(
                                    ticket.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                // Contact Support
                item {
                    Spacer(Modifier.height(8.dp))
                    if (state.ticketCreated) {
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(stringResource(R.string.support_ticket_created), Modifier.padding(16.dp))
                        }
                    }

                    Button(
                        onClick = viewModel::toggleContactForm,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.showContactForm) stringResource(R.string.close) else stringResource(R.string.contact_support))
                    }

                    AnimatedVisibility(visible = state.showContactForm) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.contactSubject,
                                onValueChange = viewModel::onSubjectChanged,
                                label = { Text(stringResource(R.string.subject)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = state.contactDescription,
                                onValueChange = viewModel::onDescriptionChanged,
                                label = { Text(stringResource(R.string.describe_your_issue)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                            Button(
                                onClick = viewModel::submitContactTicket,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.contactSubject.isNotBlank() && state.contactDescription.isNotBlank()
                            ) {
                                Text(stringResource(R.string.submit_ticket))
                            }
                        }
                    }

                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
