package com.example.diamonds.data.remote.backend

import com.example.diamonds.domain.model.Result
import kotlinx.serialization.Serializable

/**
 * Abstract backend service interface - agnostic to Firebase/REST/GraphQL
 * Implementations provided later based on chosen backend
 */
interface IBackendService {
    // Auth
    suspend fun login(email: String, password: String): Result<String>
    suspend fun signup(
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        role: String
    ): Result<String>

    // Clients
    suspend fun getClient(clientId: String): Result<ClientDto>
    suspend fun updateClient(client: ClientDto): Result<ClientDto>

    // Providers
    suspend fun getProvider(providerId: String): Result<ProviderDto>
    suspend fun searchProviders(
        latitude: Double,
        longitude: Double,
        radius: Int
    ): Result<List<ProviderDto>>

    // Services
    suspend fun getService(serviceId: String): Result<ServiceDto>
    suspend fun getServicesForProvider(providerId: String): Result<List<ServiceDto>>
    suspend fun searchServicesByCategory(category: String): Result<List<ServiceDto>>
    suspend fun createService(service: ServiceDto): Result<ServiceDto>

    // Bookings
    suspend fun createBooking(booking: CreateBookingRequest): Result<BookingDto>
    suspend fun getBooking(bookingId: String): Result<BookingDto>
    suspend fun getClientBookings(clientId: String): Result<List<BookingDto>>
    suspend fun getProviderBookings(providerId: String): Result<List<BookingDto>>
    suspend fun updateBookingStatus(bookingId: String, status: String): Result<BookingDto>
    suspend fun cancelBooking(bookingId: String): Result<BookingDto>

    // Reviews
    suspend fun createReview(review: CreateReviewRequest): Result<ReviewDto>
    suspend fun getReviewsForProvider(providerId: String): Result<List<ReviewDto>>
    suspend fun getReviewsForBooking(bookingId: String): Result<ReviewDto?>
    suspend fun getReviewsForClient(clientId: String): Result<List<ReviewDto>>
    suspend fun getReviewForBookingByDirection(
        bookingId: String,
        direction: String
    ): Result<ReviewDto?>

    // Payments
    suspend fun createPayment(payment: CreatePaymentRequest): Result<PaymentDto>
    suspend fun getPayment(paymentId: String): Result<PaymentDto>
    suspend fun getPaymentsForClient(clientId: String): Result<List<PaymentDto>>

    // Location / Tracking
    suspend fun getProviderLocation(providerId: String): Result<ProviderLocationDto>
    suspend fun updateProviderLocation(location: ProviderLocationDto): Result<Unit>
    suspend fun getServiceArea(providerId: String): Result<ServiceAreaDto>

    // Chat / Messaging
    suspend fun getOrCreateConversation(request: CreateConversationRequest): Result<ConversationDto>
    suspend fun getConversationsForUser(userId: String): Result<List<ConversationDto>>
    suspend fun getMessages(conversationId: String): Result<List<MessageDto>>
    suspend fun sendMessage(message: SendMessageRequest): Result<MessageDto>
    suspend fun markConversationRead(conversationId: String, userId: String): Result<Unit>

    // Recurring Bookings
    suspend fun createRecurringBooking(request: CreateRecurringBookingRequest): Result<RecurringBookingDto>
    suspend fun getRecurringBooking(id: String): Result<RecurringBookingDto>
    suspend fun getRecurringBookingsForClient(clientId: String): Result<List<RecurringBookingDto>>
    suspend fun updateRecurringBookingStatus(
        id: String,
        status: String
    ): Result<RecurringBookingDto>

    suspend fun updateRecurringBookingSchedule(
        id: String,
        preferredDay: Int,
        preferredTime: String
    ): Result<RecurringBookingDto>

    suspend fun advanceRecurringBookingDate(id: String, newDate: String): Result<Unit>
}

// DTO classes for API communication (separate from domain models)

@Serializable
data class ClientDto(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val profileImageUrl: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ProviderDto(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val verificationStatus: String,
    val serviceRadius: Int = 10,
    /** "INDEPENDENT" or "EMPLOYED" */
    val cleanerType: String = "INDEPENDENT",
    /** Non-null for employed cleaners */
    val employerId: String? = null,
    val employerName: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ServiceDto(
    val id: String,
    val providerId: String,
    val title: String,
    val description: String,
    val basePrice: Double,
    val duration: Int,
    val category: String,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class BookingDto(
    val id: String,
    val clientId: String,
    val providerId: String,
    val serviceId: String,
    val status: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val estimatedDuration: Int,
    val totalPrice: Double,
    val notes: String? = null,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ReviewDto(
    val id: String,
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val rating: Int,
    val comment: String? = null,
    val imageUrls: List<String> = emptyList(),
    val direction: String = "CLIENT_REVIEWS_PROVIDER",
    val locationTags: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PaymentDto(
    val id: String,
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val amount: Double,
    val status: String,
    val method: String,
    val transactionId: String? = null,
    val createdAt: String,
    val updatedAt: String
)

// Request payloads

@Serializable
data class CreateBookingRequest(
    val clientId: String,
    val providerId: String,
    val serviceId: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null
)

@Serializable
data class CreateReviewRequest(
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val rating: Int,
    val comment: String? = null,
    val imageUrls: List<String> = emptyList(),
    val direction: String = "CLIENT_REVIEWS_PROVIDER",
    val locationTags: List<String> = emptyList()
)

@Serializable
data class CreatePaymentRequest(
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val amount: Double,
    val method: String
)

@Serializable
data class ProviderLocationDto(
    val providerId: String,
    val latitude: Double,
    val longitude: Double,
    val heading: Float = 0f,
    val updatedAt: String
)

@Serializable
data class ServiceAreaDto(
    val providerId: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusKm: Double
)

@Serializable
data class ConversationDto(
    val id: String,
    val bookingId: String,
    val clientId: String,
    val clientName: String,
    val providerId: String,
    val providerName: String,
    val lastMessage: String = "",
    val lastMessageAt: String = "",
    val unreadCount: Int = 0,
    val updatedAt: String
)

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val body: String,
    val isRead: Boolean = false,
    val createdAt: String
)

@Serializable
data class CreateConversationRequest(
    val bookingId: String,
    val clientId: String,
    val clientName: String,
    val providerId: String,
    val providerName: String
)

@Serializable
data class SendMessageRequest(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val body: String,
    val createdAt: String
)

@Serializable
data class RecurringBookingDto(
    val id: String,
    val clientId: String,
    val providerId: String,
    val providerName: String,
    val serviceId: String,
    val serviceName: String,
    val frequency: String,
    val preferredDay: Int,
    val preferredTime: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val totalPrice: Double,
    val status: String,
    val nextBookingDate: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateRecurringBookingRequest(
    val clientId: String,
    val providerId: String,
    val providerName: String,
    val serviceId: String,
    val serviceName: String,
    val frequency: String,
    val preferredDay: Int,
    val preferredTime: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val totalPrice: Double
)

