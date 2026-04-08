package com.example.diamonds.ui.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Notification preferences screen — allows the user to toggle
 * push notifications for different categories.
 */
@Composable
fun NotificationPreferencesScreen(
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val prefs = state.preferences

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Notification Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Choose which notifications you'd like to receive.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Master toggle
                PreferenceToggle(
                    icon = "🔔",
                    title = "Push Notifications",
                    subtitle = "Enable or disable all push notifications",
                    checked = prefs.pushEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences(prefs.copy(pushEnabled = it))
                    }
                )

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                // Booking updates
                PreferenceToggle(
                    icon = "📋",
                    title = "Booking Updates",
                    subtitle = "Accepted, started, completed, cancelled",
                    checked = prefs.bookingUpdates && prefs.pushEnabled,
                    enabled = prefs.pushEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences(prefs.copy(bookingUpdates = it))
                    }
                )

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                // Payment alerts
                PreferenceToggle(
                    icon = "💳",
                    title = "Payment Alerts",
                    subtitle = "Payment confirmations and receipts",
                    checked = prefs.paymentAlerts && prefs.pushEnabled,
                    enabled = prefs.pushEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences(prefs.copy(paymentAlerts = it))
                    }
                )

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                // Promotions
                PreferenceToggle(
                    icon = "🎉",
                    title = "Promotions & Offers",
                    subtitle = "Deals, discounts, and special offers",
                    checked = prefs.promotions && prefs.pushEnabled,
                    enabled = prefs.pushEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences(prefs.copy(promotions = it))
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "You can also manage notification permissions in your device's Settings app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun PreferenceToggle(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, modifier = Modifier.padding(end = 12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
