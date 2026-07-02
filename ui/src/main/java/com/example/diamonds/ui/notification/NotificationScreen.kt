package com.example.diamonds.ui.notification

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.common.util.DateTimeFormatUtil
import com.example.diamonds.domain.model.Notification
import com.example.diamonds.domain.model.NotificationType
import com.example.diamonds.ui.R
import com.example.diamonds.ui.components.PullToRefreshLayout

/**
 * Notification list screen — shows all in-app notifications with
 * unread highlighting, swipe-to-dismiss, and a "Mark All Read" action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNotificationClick: (Notification) -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refresh() }
    ) {
    Column(Modifier.fillMaxSize()) {
        // Header with "Mark All Read"
        if (state.notifications.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.markAllAsRead() }) {
                    Text(stringResource(R.string.mark_all_read))
                }
            }
        }

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.notifications.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔔", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_notifications),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.notifications_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = state.notifications,
                        key = { it.id }
                    ) { notification ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteNotification(notification.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                        MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.surface,
                                    label = "bg"
                                )
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        stringResource(R.string.delete_with_icon),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            },
                            content = {
                                NotificationCard(
                                    notification = notification,
                                    onClick = {
                                        viewModel.markAsRead(notification.id)
                                        onNotificationClick(notification)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
    } // end PullToRefreshLayout
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    val bgColor = if (!notification.isRead) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon based on notification type
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (notification.type) {
                            NotificationType.BOOKING_UPDATE -> MaterialTheme.colorScheme.primaryContainer
                            NotificationType.PAYMENT -> MaterialTheme.colorScheme.tertiaryContainer
                            NotificationType.REVIEW -> MaterialTheme.colorScheme.secondaryContainer
                            NotificationType.PROMOTION -> MaterialTheme.colorScheme.errorContainer
                            NotificationType.SYSTEM -> MaterialTheme.colorScheme.surfaceVariant
                            NotificationType.SUPPORT_UPDATE -> MaterialTheme.colorScheme.secondaryContainer
                            NotificationType.CLAIM_UPDATE -> MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (notification.type) {
                        NotificationType.BOOKING_UPDATE -> "📋"
                        NotificationType.PAYMENT -> "💳"
                        NotificationType.REVIEW -> "⭐"
                        NotificationType.PROMOTION -> "🎉"
                        NotificationType.SYSTEM -> "ℹ️"
                        NotificationType.SUPPORT_UPDATE -> "🎫"
                        NotificationType.CLAIM_UPDATE -> "📝"
                    },
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!notification.isRead) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = formatNotificationTime(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun formatNotificationTime(timestamp: String): String {
    val millis = timestamp.toLongOrNull() ?: return timestamp
    val minutes = (System.currentTimeMillis() - millis) / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> stringResource(R.string.time_just_now)
        minutes < 60 -> pluralStringResource(R.plurals.time_minutes_ago, minutes.toInt(), minutes.toInt())
        hours < 24 -> pluralStringResource(R.plurals.time_hours_ago, hours.toInt(), hours.toInt())
        days < 7 -> pluralStringResource(R.plurals.time_days_ago, days.toInt(), days.toInt())
        else -> DateTimeFormatUtil.formatTimestampAsDate(millis)
    }
}
