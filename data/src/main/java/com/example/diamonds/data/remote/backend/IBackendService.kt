package com.example.diamonds.data.remote.backend

import com.example.diamonds.domain.model.*

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

    // Payments
    suspend fun createPayment(payment: CreatePaymentRequest): Result<PaymentDto>
    suspend fun getPayment(paymentId: String): Result<PaymentDto>
}

// DTO classes for API communication (separate from domain models)

data class ClientDto(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val profileImageUrl: String? = null,
    val createdAt: String,
    val updatedAt: String
)

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
    val createdAt: String,
    val updatedAt: String
)

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

data class ReviewDto(
    val id: String,
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val rating: Int,
    val comment: String? = null,
    val imageUrls: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String
)

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

data class CreateReviewRequest(
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val rating: Int,
    val comment: String? = null,
    val imageUrls: List<String> = emptyList()
)

data class CreatePaymentRequest(
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val amount: Double,
    val method: String
)
