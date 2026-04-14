package com.example.diamonds.domain.model

import com.example.diamonds.domain.model.CleanerType.COMPANY
import com.example.diamonds.domain.model.CleanerType.EMPLOYED
import com.example.diamonds.domain.model.CleanerType.INDEPENDENT
import kotlinx.serialization.Serializable

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
 * Verification status for providers
 */
@Serializable
enum class VerificationStatus {
    PENDING, APPROVED, REJECTED, SUSPENDED
}

/**
 * Whether a cleaner works independently, is employed by a company, or IS a company.
 *
 * - [INDEPENDENT]: self-employed, owns their own provider profile and listings.
 * - [EMPLOYED]: works under a cleaning company; profile linked to company via [Provider.employerId].
 * - [COMPANY]: this account represents a cleaning business that employs other cleaners.
 */
@Serializable
enum class CleanerType {
    INDEPENDENT, EMPLOYED, COMPANY
}

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
    /** Whether this cleaner is independent or employed by a company. */
    val cleanerType: CleanerType = CleanerType.INDEPENDENT,
    /**
     * For [CleanerType.EMPLOYED] cleaners, the ID of the company [Provider]
     * that employs them. Null for [CleanerType.INDEPENDENT] cleaners.
     */
    val employerId: String? = null,
    /** Display name of the employing company (denormalised for easy display). */
    val employerName: String? = null,
    val createdAt: String,
    val updatedAt: String
)

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
 * CONFLICT: Server has a different version; needs manual resolution
 */
@Serializable
enum class SyncStatus {
    READ_ONLY,
    PENDING,
    SYNCED,
    FAILED,
    CANCELLED,
    CONFLICT
}

/**
 * Types of push / in-app notifications.
 */
@Serializable
enum class NotificationType {
    BOOKING_UPDATE,
    PAYMENT,
    REVIEW,
    PROMOTION,
    SYSTEM
}

/**
 * Represents an in-app notification (persisted locally).
 */
@Serializable
data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: NotificationType = NotificationType.SYSTEM,
    /** Optional ID of the related entity (bookingId, paymentId, etc.) */
    val referenceId: String? = null,
    val isRead: Boolean = false,
    val createdAt: String
)

/**
 * User-configurable notification preferences.
 */
data class NotificationPreferences(
    val pushEnabled: Boolean = true,
    val bookingUpdates: Boolean = true,
    val paymentAlerts: Boolean = true,
    val promotions: Boolean = true
)

// ── Chat & Messaging ──────────────────────────────────────────────────────────

/**
 * A single chat message sent within a [Conversation].
 */
@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val body: String,
    val isRead: Boolean = false,
    val createdAt: String
)

/**
 * A conversation thread between a client and a provider,
 * linked to a specific booking.
 */
@Serializable
data class Conversation(
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

// ── Location & Maps ────────────────────────────────────────────────────────────

/**
 * A geographic coordinate pair. Pure Kotlin — no Android dependency.
 */
@Serializable
data class GeoLocation(
    val latitude: Double,
    val longitude: Double
)

/**
 * The service area a provider covers, centred on their base location.
 */
@Serializable
data class ServiceArea(
    val providerId: String,
    val center: GeoLocation,
    val radiusKm: Double
)

/**
 * A snapshot of a provider's live position (used for real-time tracking).
 */
@Serializable
data class ProviderLocation(
    val providerId: String,
    val location: GeoLocation,
    val heading: Float = 0f,
    val updatedAt: String
)

/**
 * Aggregate tracking state for the customer tracking screen.
 */
data class TrackingState(
    val providerLocation: ProviderLocation? = null,
    val jobLocation: GeoLocation? = null,
    val etaMinutes: Double? = null,
    val distanceKm: Double? = null,
    val isActive: Boolean = false
)

