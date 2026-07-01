package com.example.diamonds.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualHelpScreen(
    bookingId: String,
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    viewModel: ContextualHelpViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_and_support)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            stringResource(R.string.cd_back)
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
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.booking?.let { b ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.booking_number, b.id.takeLast(6)),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.status_label, b.status.name),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Text(stringResource(R.string.what_help_with), style = MaterialTheme.typography.titleMedium)

                state.actions.forEach { action ->
                    val isEmergency = action.route == "EMERGENCY"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isEmergency) {
                                    // Launch phone dialer
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                                    context.startActivity(intent)
                                } else if (action.route.isNotEmpty()) {
                                    onNavigate(action.route)
                                }
                            },
                        colors = if (isEmergency) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ) else CardDefaults.cardColors()
                    ) {
                        Row(
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(action.icon, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                action.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isEmergency) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
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
