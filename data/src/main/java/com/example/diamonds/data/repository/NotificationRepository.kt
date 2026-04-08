package com.example.diamonds.data.repository

import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.preferences.PreferencesDataStore
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.domain.model.Notification
import com.example.diamonds.domain.model.NotificationPreferences
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Notification repository backed by Room (local) and DataStore (preferences).
 *
 * Notifications are stored locally — they originate from FCM push messages
 * or in-app events, and are persisted so the user can review them later.
 */
class NotificationRepository(
    db: AppDatabase,
    private val preferencesDataStore: PreferencesDataStore
) : INotificationRepository {

    private val dao = db.notificationDao()

    override suspend fun getNotifications(userId: String): Result<List<Notification>> {
        return try {
            val notifications = dao.getAll(userId).map { it.toDomain() }
            Result.Success(notifications)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addNotification(notification: Notification): Result<Unit> {
        return try {
            dao.insert(notification.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            dao.markAsRead(notificationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            dao.markAllAsRead(userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            dao.delete(notificationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeUnreadCount(userId: String): Flow<Int> =
        dao.observeUnreadCount(userId)

    override fun observeNotifications(userId: String): Flow<List<Notification>> =
        dao.observeAll(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getNotificationPreferences(): NotificationPreferences =
        preferencesDataStore.getNotificationPreferences()

    override suspend fun updateNotificationPreferences(
        preferences: NotificationPreferences
    ): Result<Unit> {
        return try {
            preferencesDataStore.saveNotificationPreferences(preferences)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
