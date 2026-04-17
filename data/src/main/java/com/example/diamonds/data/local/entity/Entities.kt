package com.example.diamonds.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val profileImageUrl: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null // Last successful server sync timestamp
)

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val verificationStatus: String, // VerificationStatus enum
    val serviceRadius: Int = 10,
    /** "INDEPENDENT" or "EMPLOYED" */
    val cleanerType: String = "INDEPENDENT",
    /** Non-null when cleanerType == "EMPLOYED" */
    val employerId: String? = null,
    val employerName: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val title: String,
    val description: String,
    val basePrice: Double,
    val duration: Int,
    val category: String, // ServiceCategory enum
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val providerId: String,
    val serviceId: String,
    val status: String, // BookingStatus enum
    val scheduledDate: String,
    val scheduledTime: String,
    val estimatedDuration: Int,
    val totalPrice: Double,
    val notes: String? = null,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val syncStatus: String, // SyncStatus enum
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val rating: Int,
    val comment: String? = null,
    val imageUrls: String = "", // JSON array as string
    val syncStatus: String, // SyncStatus enum
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val bookingId: String,
    val clientId: String,
    val providerId: String,
    val amount: Double,
    val status: String, // PaymentStatus enum
    val method: String, // PaymentMethod enum
    val transactionId: String? = null,
    val syncStatus: String, // SyncStatus enum
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val operationType: String, // SyncOperationType enum
    val entityType: String, // EntityType enum
    val entityId: String,
    val payload: String, // JSON
    val status: String, // SyncStatus enum
    val retryCount: Int = 0,
    val createdAt: String,
    val lastAttemptAt: String? = null,
    val error: String? = null,
    val serverPayload: String? = null // Server data when CONFLICT
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String, // NotificationType enum name
    val referenceId: String? = null,
    val isRead: Boolean = false,
    val createdAt: String
)

@Entity(tableName = "provider_locations")
data class ProviderLocationEntity(
    @PrimaryKey val providerId: String,
    val latitude: Double,
    val longitude: Double,
    val heading: Float = 0f,
    val updatedAt: String
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val body: String,
    val isRead: Boolean = false,
    val createdAt: String
)

@Entity(tableName = "recurring_bookings")
data class RecurringBookingEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val providerId: String,
    val providerName: String,
    val serviceId: String,
    val serviceName: String,
    val frequency: String, // RecurringFrequency enum
    val preferredDay: Int,
    val preferredTime: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val totalPrice: Double,
    val status: String, // RecurringBookingStatus enum
    val nextBookingDate: String,
    val createdAt: String,
    val updatedAt: String
)

