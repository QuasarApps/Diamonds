package com.example.diamonds.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.diamonds.MainActivity
import com.example.diamonds.R
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.entity.NotificationEntity
import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.remote.backend.IBackendService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging service for the Diamonds app.
 *
 * Handles:
 *  - Incoming push notifications (data + notification messages)
 *  - Token refresh events (persisted to DataStore *and* registered server-side)
 *
 * Notification channels:
 *  - `diamonds_bookings`   — Booking status updates
 *  - `diamonds_payments`   — Payment confirmations
 *  - `diamonds_general`    — Promotions, system alerts
 */
@AndroidEntryPoint
class DiamondsFcmService : FirebaseMessagingService() {

    /**
     * Injected rather than constructed: `provideBackendService` picks the stub or the Firebase
     * implementation from `BuildConfig.USE_MOCK_BACKEND`, and building one here by hand would
     * quietly ignore that switch.
     */
    @Inject lateinit var backendService: IBackendService

    @Inject lateinit var prefs: PreferencesDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_BOOKINGS = "diamonds_bookings"
        const val CHANNEL_PAYMENTS = "diamonds_payments"
        const val CHANNEL_GENERAL = "diamonds_general"

        /**
         * Call from Application.onCreate() to create notification channels
         * on Android 8+.
         */
        fun createNotificationChannels(context: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)

                val bookingChannel = NotificationChannel(
                    CHANNEL_BOOKINGS,
                    "Booking Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications about booking status changes"
                }

                val paymentChannel = NotificationChannel(
                    CHANNEL_PAYMENTS,
                    "Payment Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Payment confirmation and receipt notifications"
                }

                val generalChannel = NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Promotions, tips, and system notifications"
                }

                manager.createNotificationChannels(
                    listOf(bookingChannel, paymentChannel, generalChannel)
                )
            }
        }
    }

    /**
     * Persists the new registration token and, if someone is signed in, registers it server-side.
     *
     * The session check is not optional: FCM issues tokens on its own schedule, frequently on
     * first launch before anyone has logged in, and there is no user to attribute the token to
     * then. `AuthRepository` registers the stored token on the next login to cover that ordering.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            try {
                prefs.saveFcmToken(token)
                val userId = prefs.observeUserSession().first()?.userId ?: return@launch
                backendService.registerFcmToken(userId, token)
            } catch (_: Exception) { /* best-effort — retried on the next login or refresh */
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "Diamonds"
        val body = message.notification?.body ?: data["body"] ?: ""
        val type = data["type"] ?: "SYSTEM"
        val referenceId = data["referenceId"]

        // Determine the notification channel based on type
        val channelId = when (type) {
            "BOOKING_UPDATE" -> CHANNEL_BOOKINGS
            "PAYMENT" -> CHANNEL_PAYMENTS
            else -> CHANNEL_GENERAL
        }

        // Persist to local DB so it appears in the in-app notification list
        serviceScope.launch {
            try {
                val session = prefs.observeUserSession().first()
                val userId = session?.userId ?: return@launch

                val db = AppDatabase.getInstance(applicationContext)
                db.notificationDao().insert(
                    NotificationEntity(
                        id = "notif_${System.currentTimeMillis()}",
                        userId = userId,
                        title = title,
                        body = body,
                        type = type,
                        referenceId = referenceId,
                        isRead = false,
                        createdAt = System.currentTimeMillis().toString()
                    )
                )
            } catch (_: Exception) { /* best-effort */
            }
        }

        // Build and show the system notification
        showNotification(title, body, channelId, referenceId)
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        referenceId: String?
    ) {
        // Deep-link intent — tapping opens the app at the relevant screen
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            referenceId?.let { putExtra("referenceId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
