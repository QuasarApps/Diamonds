package com.example.diamonds.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.diamonds.data.local.entity.BookingEntity
import com.example.diamonds.data.local.entity.ClaimEntity
import com.example.diamonds.data.local.entity.ClientEntity
import com.example.diamonds.data.local.entity.ConversationEntity
import com.example.diamonds.data.local.entity.MessageEntity
import com.example.diamonds.data.local.entity.NotificationEntity
import com.example.diamonds.data.local.entity.PaymentEntity
import com.example.diamonds.data.local.entity.ProviderEntity
import com.example.diamonds.data.local.entity.ProviderLocationEntity
import com.example.diamonds.data.local.entity.RecurringBookingEntity
import com.example.diamonds.data.local.entity.ReviewEntity
import com.example.diamonds.data.local.entity.SavedLocationEntity
import com.example.diamonds.data.local.entity.ServiceEntity
import com.example.diamonds.data.local.entity.SupportTicketEntity
import com.example.diamonds.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(client: ClientEntity)

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE id = :id")
    fun observeById(id: String): Flow<ClientEntity?>

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ProviderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(provider: ProviderEntity)

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getById(id: String): ProviderEntity?

    @Query("SELECT * FROM providers WHERE id = :id")
    fun observeById(id: String): Flow<ProviderEntity?>

    @Query("SELECT * FROM providers LIMIT :limit")
    suspend fun getNearby(limit: Int): List<ProviderEntity>


    @Delete
    suspend fun delete(provider: ProviderEntity)
}

@Dao
interface ServiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(service: ServiceEntity)

    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun getById(id: String): ServiceEntity?

    @Query("SELECT * FROM services WHERE providerId = :providerId")
    suspend fun getForProvider(providerId: String): List<ServiceEntity>

    @Query("SELECT * FROM services WHERE category = :category")
    suspend fun getByCategory(category: String): List<ServiceEntity>

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface BookingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(booking: BookingEntity)

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getById(id: String): BookingEntity?

    @Query("SELECT * FROM bookings WHERE id = :id")
    fun observeById(id: String): Flow<BookingEntity?>

    @Query("SELECT * FROM bookings WHERE clientId = :clientId ORDER BY createdAt DESC")
    suspend fun getForClient(clientId: String): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun observeForClient(clientId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE providerId = :providerId ORDER BY createdAt DESC")
    suspend fun getForProvider(providerId: String): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun observeForProvider(providerId: String): Flow<List<BookingEntity>>

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: ReviewEntity)

    @Query("SELECT * FROM reviews WHERE id = :id")
    suspend fun getById(id: String): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE providerId = :providerId AND reviewDirection = 'CLIENT_REVIEWS_PROVIDER' ORDER BY createdAt DESC")
    suspend fun getForProvider(providerId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE providerId = :providerId AND reviewDirection = 'CLIENT_REVIEWS_PROVIDER' ORDER BY createdAt DESC")
    fun observeForProvider(providerId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getForBooking(bookingId: String): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE bookingId = :bookingId AND reviewDirection = :direction LIMIT 1")
    suspend fun getForBookingByDirection(bookingId: String, direction: String): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE clientId = :clientId AND reviewDirection = 'PROVIDER_REVIEWS_CLIENT' ORDER BY createdAt DESC")
    suspend fun getForClient(clientId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE clientId = :clientId AND reviewDirection = 'PROVIDER_REVIEWS_CLIENT' ORDER BY createdAt DESC")
    fun observeForClient(clientId: String): Flow<List<ReviewEntity>>

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY createdAt DESC")
    suspend fun getForClient(clientId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE providerId = :providerId ORDER BY createdAt DESC")
    suspend fun getForProvider(providerId: String): List<PaymentEntity>

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' ORDER BY lastAttemptAt ASC")
    suspend fun getFailedOperations(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getQueuedOperations(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    fun observeQueuedOperations(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'FAILED')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' ORDER BY lastAttemptAt ASC")
    fun observeFailedOperations(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'CONFLICT' ORDER BY lastAttemptAt ASC")
    fun observeConflictOperations(): Flow<List<SyncQueueEntity>>

    @Query("UPDATE sync_queue SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String)

    @Query("UPDATE sync_queue SET status = :newStatus, retryCount = retryCount + 1, lastAttemptAt = :timestamp, error = :error WHERE id = :id")
    suspend fun updateAfterRetry(id: String, newStatus: String, timestamp: String, error: String?)

    @Query("UPDATE sync_queue SET status = 'CONFLICT', serverPayload = :serverPayload, lastAttemptAt = :timestamp, error = :error WHERE id = :id")
    suspend fun markConflict(id: String, serverPayload: String, timestamp: String, error: String?)

    @Query("UPDATE sync_queue SET status = 'PENDING' WHERE status = 'FAILED'")
    suspend fun resetAllFailed()

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sync_queue WHERE status = 'CANCELLED'")
    suspend fun purgeCancelledOperations()
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAll(userId: String): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}

@Dao
interface ProviderLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(location: ProviderLocationEntity)

    @Query("SELECT * FROM provider_locations WHERE providerId = :providerId")
    suspend fun getByProviderId(providerId: String): ProviderLocationEntity?

    @Query("SELECT * FROM provider_locations WHERE providerId = :providerId")
    fun observeByProviderId(providerId: String): Flow<ProviderLocationEntity?>

    @Query("DELETE FROM provider_locations WHERE providerId = :providerId")
    suspend fun delete(providerId: String)
}

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getByBookingId(bookingId: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE clientId = :userId OR providerId = :userId ORDER BY updatedAt DESC")
    suspend fun getForUser(userId: String): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE clientId = :userId OR providerId = :userId ORDER BY updatedAt DESC")
    fun observeForUser(userId: String): Flow<List<ConversationEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM messages
        WHERE isRead = 0
          AND senderId != :userId
          AND conversationId IN (
              SELECT id FROM conversations
              WHERE clientId = :userId OR providerId = :userId
          )
    """
    )
    fun observeTotalUnreadForUser(userId: String): Flow<Int?>

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun resetUnreadCount(conversationId: String)

    @Query(
        """
        SELECT COUNT(*) FROM messages
        WHERE conversationId = :conversationId
          AND isRead = 0
          AND senderId != :userId
    """
    )
    fun observeUnreadCountForUser(conversationId: String, userId: String): Flow<Int>

    @Query("UPDATE conversations SET lastMessage = :lastMessage, lastMessageAt = :lastMessageAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLastMessage(
        id: String,
        lastMessage: String,
        lastMessageAt: String,
        updatedAt: String
    )

    @Query("UPDATE conversations SET unreadCount = unreadCount + 1, lastMessage = :lastMessage, lastMessageAt = :lastMessageAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun incrementUnreadAndUpdateLastMessage(
        id: String,
        lastMessage: String,
        lastMessageAt: String,
        updatedAt: String
    )

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getForConversation(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :conversationId AND senderId != :userId")
    suspend fun markAllReadInConversation(conversationId: String, userId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: String)
}

@Dao
interface RecurringBookingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recurringBooking: RecurringBookingEntity)

    @Query("SELECT * FROM recurring_bookings WHERE id = :id")
    suspend fun getById(id: String): RecurringBookingEntity?

    @Query("SELECT * FROM recurring_bookings WHERE clientId = :clientId ORDER BY createdAt DESC")
    suspend fun getForClient(clientId: String): List<RecurringBookingEntity>

    @Query("SELECT * FROM recurring_bookings WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun observeForClient(clientId: String): Flow<List<RecurringBookingEntity>>

    @Query("UPDATE recurring_bookings SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: String)

    @Query("UPDATE recurring_bookings SET preferredDay = :day, preferredTime = :time, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSchedule(id: String, day: Int, time: String, updatedAt: String)

    @Query("UPDATE recurring_bookings SET nextBookingDate = :newDate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun advanceNextBookingDate(id: String, newDate: String, updatedAt: String)

    @Query("SELECT * FROM recurring_bookings WHERE status = 'ACTIVE' AND nextBookingDate <= :todayIso")
    suspend fun getActiveDue(todayIso: String): List<RecurringBookingEntity>

    @Query("DELETE FROM recurring_bookings WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SupportTicketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ticket: SupportTicketEntity)

    @Query("SELECT * FROM support_tickets WHERE id = :id")
    suspend fun getById(id: String): SupportTicketEntity?

    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getForUser(userId: String): List<SupportTicketEntity>

    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeForUser(userId: String): Flow<List<SupportTicketEntity>>

    @Query("UPDATE support_tickets SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: String)

    @Query("DELETE FROM support_tickets WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ClaimDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(claim: ClaimEntity)

    @Query("SELECT * FROM claims WHERE id = :id")
    suspend fun getById(id: String): ClaimEntity?

    @Query("SELECT * FROM claims WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getByBookingId(bookingId: String): ClaimEntity?

    @Query("SELECT * FROM claims WHERE filedByUserId = :userId ORDER BY createdAt DESC")
    suspend fun getForUser(userId: String): List<ClaimEntity>

    @Query("SELECT * FROM claims WHERE filedByUserId = :userId ORDER BY createdAt DESC")
    fun observeForUser(userId: String): Flow<List<ClaimEntity>>

    @Query("UPDATE claims SET status = :status, resolutionNotes = :notes, refundAmount = :refundAmount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(
        id: String,
        status: String,
        notes: String?,
        refundAmount: Double?,
        updatedAt: String
    )

    @Query("DELETE FROM claims WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SavedLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(location: SavedLocationEntity)

    @Query("SELECT * FROM saved_locations WHERE clientId = :clientId ORDER BY label ASC")
    suspend fun getForClient(clientId: String): List<SavedLocationEntity>

    @Query("SELECT * FROM saved_locations WHERE clientId = :clientId ORDER BY label ASC")
    fun observeForClient(clientId: String): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun getById(id: String): SavedLocationEntity?

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun delete(id: String)
}

