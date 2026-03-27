package com.example.diamonds.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * Represents a client (person booking cleaning services)
 */
@Serializable
data class Client(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val profileImageUrl: String? = null,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Represents a provider (person offering cleaning services)
 */
@Serializable
data class Provider(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val serviceRadius: Int = 10, // km
    val createdAt: String,
    val updatedAt: String
)

/**
 * Verification status for providers
 */
@Serializable
enum class VerificationStatus {
    PENDING, APPROVED, REJECTED, SUSPENDED
}

/**
 * Represents a cleaning service offered by a provider
 */
@Serializable
data class Service(
    val id: String,
    val providerId: String,
    val title: String,
    val description: String,
    val basePrice: Double,
    val duration: Int, // minutes
    val category: ServiceCategory,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Service categories
 */
@Serializable
enum class ServiceCategory {
    APARTMENT_CLEANING,
    HOUSE_CLEANING,
    OFFICE_CLEANING,
    DEEP_CLEANING,
    POST_CONSTRUCTION,
    CARPET_CLEANING,
    WINDOW_CLEANING,
    OTHER
}

/**
 * Represents a booking request from a client to a provider
 */
@Serializable
data class Booking(
    val id: String,
    val clientId: String,
    val providerId: String,
    val serviceId: String,
    val status: BookingStatus = BookingStatus.PENDING,
    val scheduledDate: String,
    val scheduledTime: String,
    val estimatedDuration: Int, // minutes
    val totalPrice: Double,
    val notes: String? = null,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val syncStatus: SyncStatus = SyncStatus.READ_ONLY,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Booking status throughout its lifecycle
 */
@Serializable
enum class BookingStatus {
    PENDING,           // Awaiting provider acceptance
    ACCEPTED,          // Provider accepted
    IN_PROGRESS,       // Service in progress
    COMPLETED,         // Service completed
    CANCELLED,         // Cancelled by either party
    NO_SHOW            // Provider didn't show
}

/**
 * Represents a review/rating left by a client for a provider
 */
@Serializable
data class Review(
    val id: String,
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val rating: Int, // 1-5
    val comment: String? = null,
    val imageUrls: List<String> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.READ_ONLY,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Represents a payment transaction
 */
@Serializable
data class Payment(
    val id: String,
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val amount: Double,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val method: PaymentMethod = PaymentMethod.CARD,
    val transactionId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.READ_ONLY,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Payment status
 */
@Serializable
enum class PaymentStatus {
    PENDING, PROCESSING, SUCCEEDED, FAILED, REFUNDED
}

/**
 * Payment method
 */
@Serializable
enum class PaymentMethod {
    CARD, BANK_TRANSFER, WALLET, OTHER
}

/**
 * Sync status for offline-first operations
 * READ_ONLY: Cached data, no local modifications
 * PENDING: Operation queued, waiting for sync
 * SYNCED: Confirmed on server
 * FAILED: Sync failed, retry pending
 * CANCELLED: User cancelled operation
 */
@Serializable
enum class SyncStatus {
    READ_ONLY,
    PENDING,
    SYNCED,
    FAILED,
    CANCELLED
}
