package com.example.diamonds.domain.repository

import com.example.diamonds.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for authentication operations
 */
interface IAuthRepository {
    suspend fun login(email: String, password: String): Result<String> // returns auth token
    suspend fun signup(
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        role: UserRole
    ): Result<String> // returns auth token

    suspend fun logout(): Result<Unit>
    suspend fun refreshToken(): Result<String>
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
}

/**
 * User role for role-based access control
 */
enum class UserRole {
    CLIENT, PROVIDER
}

/**
 * Current user session information
 */
data class UserSession(
    val userId: String,
    val email: String,
    val role: UserRole,
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
    val error: String? = null
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
