package com.example.diamonds.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diamonds.domain.repository.UserSession
import com.example.diamonds.ui.R

/**
 * Customer profile tab.
 */
@Composable
fun CustomerProfileScreen(
    session: UserSession,
    onPaymentHistory: () -> Unit,
    onMyBookings: () -> Unit,
    onSubscriptions: () -> Unit = {},
    onSavedLocations: () -> Unit = {},
    onLanguage: () -> Unit = {},
    onHelpCenter: () -> Unit = {},
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Avatar + name ─────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Card(
                modifier = Modifier.size(72.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        (session.displayName?.firstOrNull() ?: session.email.firstOrNull() ?: '?')
                            .uppercase(),
                        fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    session.displayName ?: stringResource(R.string.customer),
                    fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
                Text(session.email,
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(stringResource(R.string.badge_customer_emoji),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        HorizontalDivider()

        // ── Quick links ───────────────────────────────────────────────────
        Text(stringResource(R.string.my_account), style = MaterialTheme.typography.titleMedium)

        ProfileLinkCard(
            icon    = "📋",
            title   = stringResource(R.string.my_bookings),
            subtitle = stringResource(R.string.view_all_bookings),
            onClick  = onMyBookings
        )

        ProfileLinkCard(
            icon    = "💳",
            title   = stringResource(R.string.payment_history),
            subtitle = stringResource(R.string.view_receipts),
            onClick  = onPaymentHistory
        )

        ProfileLinkCard(
            icon = "🔄",
            title = stringResource(R.string.recurring_bookings),
            subtitle = stringResource(R.string.manage_scheduled_plans),
            onClick = onSubscriptions
        )

        ProfileLinkCard(
            icon = "📍",
            title = stringResource(R.string.my_locations),
            subtitle = stringResource(R.string.save_manage_locations),
            onClick = onSavedLocations
        )

        ProfileLinkCard(
            icon = "🌐",
            title = stringResource(R.string.language),
            subtitle = stringResource(R.string.change_app_language),
            onClick = onLanguage
        )

        ProfileLinkCard(
            icon = "❓",
            title = stringResource(R.string.help_and_support),
            subtitle = stringResource(R.string.help_support_subtitle),
            onClick = onHelpCenter
        )

        HorizontalDivider()

        // ── Account info ──────────────────────────────────────────────────
        Text(stringResource(R.string.account), style = MaterialTheme.typography.titleMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountRow(stringResource(R.string.email),   session.email)
                AccountRow(stringResource(R.string.member_since), "2025")   // placeholder — would come from Client model
                AccountRow(stringResource(R.string.account_type), stringResource(R.string.customer))
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Sign out ──────────────────────────────────────────────────────
        OutlinedButton(
            onClick  = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors   = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(stringResource(R.string.sign_out), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ProfileLinkCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick  = onClick
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
