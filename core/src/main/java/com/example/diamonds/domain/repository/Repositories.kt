package com.example.diamonds.domain.repository

import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.Client
import com.example.diamonds.domain.model.Conversation
import com.example.diamonds.domain.model.GeoLocation
import com.example.diamonds.domain.model.Message
import com.example.diamonds.domain.model.Notification
import com.example.diamonds.domain.model.NotificationPreferences
import com.example.diamonds.domain.model.Payment
import com.example.diamonds.domain.model.PaymentStatus
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.ProviderLocation
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.Review
import com.example.diamonds.domain.model.Service
import com.example.diamonds.domain.model.ServiceArea
import com.example.diamonds.domain.model.ServiceCategory
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.repository.UserRole.CLEANER
import com.example.diamonds.domain.repository.UserRole.CUSTOMER
import kotlinx.coroutines.flow.Flow

/**
 * Repository for authentication operations
 */
interface IAuthRepository {
    suspend fun login(
        email: String,
        password: String,
        roleHint: UserRole? = null,
        cleanerTypeHint: com.example.diamonds.domain.model.CleanerType? = null
    ): Result<String> // returns auth token
    suspend fun signup(
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        role: UserRole,
        cleanerType: com.example.diamonds.domain.model.CleanerType =
            com.example.diamonds.domain.model.CleanerType.INDEPENDENT
    ): Result<String> // returns auth token

    suspend fun logout(): Result<Unit>
    suspend fun refreshToken(): Result<String>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun getCurrentUserSession(): Flow<UserSession?>
}

/**
 * Repository for client operations
 */
interface IClientRepository {
    suspend fun getClient(clientId: String): Result<Client>
    suspend fun updateClient(client: Client): Result<Client>
    suspend fun getCurrentClient(): Result<Client>
    fun observeCurrentClient(): Flow<Client?>
}

/**
 * Repository for provider operations
 */
interface IProviderRepository {
    suspend fun getProvider(providerId: String): Result<Provider>
    suspend fun searchProviders(
        latitude: Double,
        longitude: Double,
        radius: Int = 10
    ): Result<List<Provider>>

    suspend fun searchProvidersByCategory(
        category: ServiceCategory,
        latitude: Double,
        longitude: Double,
        radius: Int = 10
    ): Result<List<Provider>>

    suspend fun updateProvider(provider: Provider): Result<Provider>
    fun observeProviderRating(providerId: String): Flow<Pair<Float, Int>> // rating, reviewCount
}

/**
 * Repository for service operations
 */
interface IServiceRepository {
    suspend fun getService(serviceId: String): Result<Service>
    suspend fun getServicesForProvider(providerId: String): Result<List<Service>>
    suspend fun searchServicesByCategory(category: ServiceCategory): Result<List<Service>>
    suspend fun createService(service: Service): Result<Service>
    suspend fun updateService(service: Service): Result<Service>
}

/**
 * Repository for booking operations
 */
interface IBookingRepository {
    suspend fun createBooking(booking: Booking): Result<Booking>
    suspend fun getBooking(bookingId: String): Result<Booking>
    suspend fun getClientBookings(clientId: String): Result<List<Booking>>
    suspend fun getProviderBookings(providerId: String): Result<List<Booking>>
    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Result<Booking>
    suspend fun cancelBooking(bookingId: String): Result<Booking>
    fun observeBooking(bookingId: String): Flow<Booking?>
    fun observeClientBookings(clientId: String): Flow<List<Booking>>
    fun observeProviderBookings(providerId: String): Flow<List<Booking>>
}

/**
 * Repository for review operations
 */
interface IReviewRepository {
    suspend fun createReview(review: Review): Result<Review>
    suspend fun getReviewsForProvider(providerId: String): Result<List<Review>>
    suspend fun getReviewsForBooking(bookingId: String): Result<Review?>
    fun observeReviewsForProvider(providerId: String): Flow<List<Review>>
}

/**
 * Repository for payment operations
 */
interface IPaymentRepository {
    suspend fun createPayment(payment: Payment): Result<Payment>
    suspend fun getPayment(paymentId: String): Result<Payment>
    suspend fun getPaymentsForClient(clientId: String): Result<List<Payment>>
    suspend fun getPaymentsForProvider(providerId: String): Result<List<Payment>>
    suspend fun updatePaymentStatus(paymentId: String, status: PaymentStatus): Result<Payment>
}

/**
 * Repository for sync operations and queued tasks
 */
interface ISyncRepository {
    suspend fun queueOperation(operation: SyncOperation): Result<Unit>
    suspend fun getSyncQueue(): Result<List<SyncOperation>>
    suspend fun removeSyncOperation(operationId: String): Result<Unit>
    suspend fun cancelSyncOperation(operationId: String): Result<Unit>
    fun observeSyncQueue(): Flow<List<SyncOperation>>
    fun observePendingOperationCount(): Flow<Int>
    suspend fun retryFailedOperation(operationId: String): Result<Unit>
    suspend fun retryAllFailed(): Result<Unit>
    suspend fun resolveConflict(operationId: String, useLocal: Boolean): Result<Unit>
    fun observeFailedOperations(): Flow<List<SyncOperation>>
    fun observeConflictOperations(): Flow<List<SyncOperation>>

    /** Trigger an immediate sync of all queued operations. */
    suspend fun syncNow(): Result<Unit>
}

/**
 * Repository for notification operations (local persistence + preferences).
 */
interface INotificationRepository {
    suspend fun getNotifications(userId: String): Result<List<Notification>>
    suspend fun addNotification(notification: Notification): Result<Unit>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun markAllAsRead(userId: String): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    fun observeUnreadCount(userId: String): Flow<Int>
    fun observeNotifications(userId: String): Flow<List<Notification>>
    suspend fun getNotificationPreferences(): NotificationPreferences
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit>
}

/**
 * Repository for location and maps operations.
 */
interface ILocationRepository {
    /** Get the device's current location. */
    suspend fun getCurrentLocation(): Result<GeoLocation>

    /** Observe a provider's live location during a job. */
    fun observeProviderLocation(providerId: String): Flow<ProviderLocation>

    /** Update a provider's current location (cleaner-side). */
    suspend fun updateProviderLocation(providerLocation: ProviderLocation): Result<Unit>

    /** Get the service area for a provider. */
    suspend fun getServiceArea(providerId: String): Result<ServiceArea>

    /** Calculate the ETA (in minutes) between two points. */
    suspend fun calculateEta(from: GeoLocation, to: GeoLocation): Result<Double>

    /** Calculate the straight-line (Haversine) distance in km between two points. */
    fun calculateDistance(from: GeoLocation, to: GeoLocation): Double
}

/**
 * Repository for in-app chat messages and conversations.
 */
interface IMessageRepository {
    /** Get or create a conversation for a given booking between client and provider. */
    suspend fun getOrCreateConversation(
        bookingId: String,
        clientId: String,
        clientName: String,
        providerId: String,
        providerName: String
    ): Result<Conversation>

    /** List all conversations for a user (by userId). */
    suspend fun getConversationsForUser(userId: String): Result<List<Conversation>>

    /** Observe conversations for a user in real-time. */
    fun observeConversationsForUser(userId: String): Flow<List<Conversation>>

    /** Get all messages in a conversation. */
    suspend fun getMessages(conversationId: String): Result<List<Message>>

    /** Observe messages in a conversation in real-time. */
    fun observeMessages(conversationId: String): Flow<List<Message>>

    /** Send a message in a conversation. */
    suspend fun sendMessage(message: Message): Result<Message>

    /** Mark all messages in a conversation as read for a given userId. */
    suspend fun markConversationRead(conversationId: String, userId: String): Result<Unit>

    /** Total unread message count across all conversations for a user. */
    fun observeUnreadMessageCount(userId: String): Flow<Int>
}

/**
 * User role for role-based access control.
 *
 * The app has a single entry point but operates in two distinct modes:
 *  - [CUSTOMER]: books cleaning services, browses providers, leaves reviews
 *  - [CLEANER]:  manages availability, accepts bookings, views earnings
 *
 * The role is set at signup, persisted in [UserSession], and used throughout
 * the UI layer to decide which navigation graph, screens, and features are
 * available. All role checks flow through [UserSession.role].
 */
enum class UserRole {
    CUSTOMER, CLEANER
}

/**
 * Current user session information
 */
data class UserSession(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val role: UserRole,
    /**
     * Only meaningful when [role] == [UserRole.CLEANER].
     * Distinguishes a self-employed cleaner from one employed by a company,
     * and from a company account itself (represented as a special INDEPENDENT
     * provider whose name implies a business).
     *
     * The shell uses this to select the correct dashboard and tab set:
     *   INDEPENDENT → cleaner tabs
     *   EMPLOYED    → cleaner tabs (same UX, company name shown in header)
     *   COMPANY     → company tabs  ← new virtual type stored in session only
     */
    val cleanerType: com.example.diamonds.domain.model.CleanerType =
        com.example.diamonds.domain.model.CleanerType.INDEPENDENT,
    val authToken: String,
    val isAuthenticated: Boolean
)

/**
 * Represents a queued sync operation
 */
data class SyncOperation(
    val id: String,
    val operationType: SyncOperationType,
    val entityType: EntityType,
    val entityId: String,
    val payload: String, // JSON
    val status: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val createdAt: String,
    val lastAttemptAt: String? = null,
    val error: String? = null,
    val serverPayload: String? = null // Server data when CONFLICT
)

/**
 * Types of sync operations
 */
enum class SyncOperationType {
    CREATE, UPDATE, DELETE, CANCEL
}

/**
 * Entity types that can be synced
 */
enum class EntityType {
    BOOKING, REVIEW, PAYMENT, SERVICE, PROFILE
}
