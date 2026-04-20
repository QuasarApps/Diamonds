package com.example.diamonds.data.mapper

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
import com.example.diamonds.data.local.entity.ServiceEntity
import com.example.diamonds.data.local.entity.SupportTicketEntity
import com.example.diamonds.data.local.entity.SyncQueueEntity
import com.example.diamonds.data.remote.backend.BookingDto
import com.example.diamonds.data.remote.backend.ClaimDto
import com.example.diamonds.data.remote.backend.ClientDto
import com.example.diamonds.data.remote.backend.ConversationDto
import com.example.diamonds.data.remote.backend.HelpArticleDto
import com.example.diamonds.data.remote.backend.MessageDto
import com.example.diamonds.data.remote.backend.PaymentDto
import com.example.diamonds.data.remote.backend.ProviderDto
import com.example.diamonds.data.remote.backend.RecurringBookingDto
import com.example.diamonds.data.remote.backend.ReviewDto
import com.example.diamonds.data.remote.backend.ServiceDto
import com.example.diamonds.data.remote.backend.SupportTicketDto
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.Claim
import com.example.diamonds.domain.model.ClaimStatus
import com.example.diamonds.domain.model.ClaimType
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.Client
import com.example.diamonds.domain.model.Conversation
import com.example.diamonds.domain.model.GeoLocation
import com.example.diamonds.domain.model.HelpArticle
import com.example.diamonds.domain.model.Message
import com.example.diamonds.domain.model.Notification
import com.example.diamonds.domain.model.NotificationType
import com.example.diamonds.domain.model.Payment
import com.example.diamonds.domain.model.PaymentMethod
import com.example.diamonds.domain.model.PaymentStatus
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.ProviderLocation
import com.example.diamonds.domain.model.RecurringBooking
import com.example.diamonds.domain.model.RecurringBookingStatus
import com.example.diamonds.domain.model.RecurringFrequency
import com.example.diamonds.domain.model.Review
import com.example.diamonds.domain.model.ReviewDirection
import com.example.diamonds.domain.model.Service
import com.example.diamonds.domain.model.ServiceCategory
import com.example.diamonds.domain.model.SupportTicket
import com.example.diamonds.domain.model.SupportTicketStatus
import com.example.diamonds.domain.model.SupportTicketType
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.model.VerificationStatus
import com.example.diamonds.domain.repository.EntityType
import com.example.diamonds.domain.repository.SyncOperation
import com.example.diamonds.domain.repository.SyncOperationType

/**
 * Maps between Domain Models, DTOs, and Entities
 */

// Entity to Domain

fun ClientEntity.toDomain(): Client = Client(
    id = id,
    name = name,
    email = email,
    phoneNumber = phoneNumber,
    profileImageUrl = profileImageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProviderEntity.toDomain(): Provider = Provider(
    id = id,
    name = name,
    email = email,
    phoneNumber = phoneNumber,
    profileImageUrl = profileImageUrl,
    bio = bio,
    rating = rating,
    reviewCount = reviewCount,
    verificationStatus = VerificationStatus.valueOf(verificationStatus),
    serviceRadius = serviceRadius,
    cleanerType = try { CleanerType.valueOf(cleanerType) } catch (_: Exception) { CleanerType.INDEPENDENT },
    employerId = employerId,
    employerName = employerName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ServiceEntity.toDomain(): Service = Service(
    id = id,
    providerId = providerId,
    title = title,
    description = description,
    basePrice = basePrice,
    duration = duration,
    category = ServiceCategory.valueOf(category),
    imageUrl = imageUrl,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BookingEntity.toDomain(): Booking = Booking(
    id = id,
    clientId = clientId,
    providerId = providerId,
    serviceId = serviceId,
    status = BookingStatus.valueOf(status),
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    estimatedDuration = estimatedDuration,
    totalPrice = totalPrice,
    notes = notes,
    address = address,
    latitude = latitude,
    longitude = longitude,
    syncStatus = SyncStatus.valueOf(syncStatus),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ReviewEntity.toDomain(): Review = Review(
    id = id,
    bookingId = bookingId,
    clientId = clientId,
    providerId = providerId,
    rating = rating,
    comment = comment,
    imageUrls = try {
        kotlinx.serialization.json.Json.decodeFromString(imageUrls)
    } catch (e: Exception) {
        emptyList()
    },
    direction = try {
        ReviewDirection.valueOf(reviewDirection)
    } catch (_: Exception) {
        ReviewDirection.CLIENT_REVIEWS_PROVIDER
    },
    locationTags = try {
        if (locationTags.isBlank()) emptyList()
        else kotlinx.serialization.json.Json.decodeFromString(locationTags)
    } catch (_: Exception) {
        emptyList()
    },
    syncStatus = SyncStatus.valueOf(syncStatus),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PaymentEntity.toDomain(): Payment = Payment(
    id = id,
    bookingId = bookingId,
    clientId = clientId,
    providerId = providerId,
    amount = amount,
    status = PaymentStatus.valueOf(status),
    method = PaymentMethod.valueOf(method),
    transactionId = transactionId,
    syncStatus = SyncStatus.valueOf(syncStatus),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun SyncQueueEntity.toDomain(): SyncOperation = SyncOperation(
    id = id,
    operationType = SyncOperationType.valueOf(operationType),
    entityType = EntityType.valueOf(entityType),
    entityId = entityId,
    payload = payload,
    status = SyncStatus.valueOf(status),
    retryCount = retryCount,
    createdAt = createdAt,
    lastAttemptAt = lastAttemptAt,
    error = error,
    serverPayload = serverPayload
)

// Domain to Entity

fun Client.toEntity(): ClientEntity = ClientEntity(
    id = id,
    name = name,
    email = email,
    phoneNumber = phoneNumber,
    profileImageUrl = profileImageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Provider.toEntity(): ProviderEntity = ProviderEntity(
    id = id,
    name = name,
    email = email,
    phoneNumber = phoneNumber,
    profileImageUrl = profileImageUrl,
    bio = bio,
    rating = rating,
    reviewCount = reviewCount,
    verificationStatus = verificationStatus.name,
    serviceRadius = serviceRadius,
    cleanerType = cleanerType.name,
    employerId = employerId,
    employerName = employerName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Service.toEntity(): ServiceEntity = ServiceEntity(
    id = id,
    providerId = providerId,
    title = title,
    description = description,
    basePrice = basePrice,
    duration = duration,
    category = category.name,
    imageUrl = imageUrl,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Booking.toEntity(): BookingEntity = BookingEntity(
    id = id,
    clientId = clientId,
    providerId = providerId,
    serviceId = serviceId,
    status = status.name,
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    estimatedDuration = estimatedDuration,
    totalPrice = totalPrice,
    notes = notes,
    address = address,
    latitude = latitude,
    longitude = longitude,
    syncStatus = syncStatus.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// DTO to Domain

fun ClientDto.toDomain(): Client = Client(
    id = id,
    name = name,
    email = email,
    phoneNumber = phoneNumber,
    profileImageUrl = profileImageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProviderDto.toDomain(): Provider = Provider(
    id = id,
    name = name,
    email = email,
    phoneNumber = phoneNumber,
    profileImageUrl = profileImageUrl,
    bio = bio,
    rating = rating,
    reviewCount = reviewCount,
    verificationStatus = VerificationStatus.valueOf(verificationStatus),
    serviceRadius = serviceRadius,
    cleanerType = try { CleanerType.valueOf(cleanerType) } catch (_: Exception) { CleanerType.INDEPENDENT },
    employerId = employerId,
    employerName = employerName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ServiceDto.toDomain(): Service = Service(
    id = id,
    providerId = providerId,
    title = title,
    description = description,
    basePrice = basePrice,
    duration = duration,
    category = ServiceCategory.valueOf(category),
    imageUrl = imageUrl,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BookingDto.toDomain(): Booking = Booking(
    id = id,
    clientId = clientId,
    providerId = providerId,
    serviceId = serviceId,
    status = BookingStatus.valueOf(status),
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    estimatedDuration = estimatedDuration,
    totalPrice = totalPrice,
    notes = notes,
    address = address,
    latitude = latitude,
    longitude = longitude,
    syncStatus = SyncStatus.SYNCED,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ReviewDto.toDomain(): Review = Review(
    id = id,
    bookingId = bookingId,
    clientId = clientId,
    providerId = providerId,
    rating = rating,
    comment = comment,
    imageUrls = imageUrls,
    direction = try {
        ReviewDirection.valueOf(direction)
    } catch (_: Exception) {
        ReviewDirection.CLIENT_REVIEWS_PROVIDER
    },
    locationTags = locationTags,
    syncStatus = SyncStatus.SYNCED,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PaymentDto.toDomain(): Payment = Payment(
    id = id,
    bookingId = bookingId,
    clientId = clientId,
    providerId = providerId,
    amount = amount,
    status = PaymentStatus.valueOf(status),
    method = PaymentMethod.valueOf(method),
    transactionId = transactionId,
    syncStatus = SyncStatus.SYNCED,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ── Notification mappers ─────────────────────────────────────────────────────

fun NotificationEntity.toDomain(): Notification = Notification(
    id = id,
    userId = userId,
    title = title,
    body = body,
    type = try {
        NotificationType.valueOf(type)
    } catch (_: Exception) {
        NotificationType.SYSTEM
    },
    referenceId = referenceId,
    isRead = isRead,
    createdAt = createdAt
)

fun Notification.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    userId = userId,
    title = title,
    body = body,
    type = type.name,
    referenceId = referenceId,
    isRead = isRead,
    createdAt = createdAt
)

// ── ProviderLocation ─────────────────────────────────────────────────────────

fun ProviderLocationEntity.toDomain(): ProviderLocation = ProviderLocation(
    providerId = providerId,
    location = GeoLocation(latitude = latitude, longitude = longitude),
    heading = heading,
    updatedAt = updatedAt
)

fun ProviderLocation.toEntity(): ProviderLocationEntity = ProviderLocationEntity(
    providerId = providerId,
    latitude = location.latitude,
    longitude = location.longitude,
    heading = heading,
    updatedAt = updatedAt
)

// ── Conversation & Message ────────────────────────────────────────────────────

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    bookingId = bookingId,
    clientId = clientId,
    clientName = clientName,
    providerId = providerId,
    providerName = providerName,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    unreadCount = unreadCount,
    updatedAt = updatedAt
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    bookingId = bookingId,
    clientId = clientId,
    clientName = clientName,
    providerId = providerId,
    providerName = providerName,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    unreadCount = unreadCount,
    updatedAt = updatedAt
)

fun ConversationDto.toDomain(): Conversation = Conversation(
    id = id,
    bookingId = bookingId,
    clientId = clientId,
    clientName = clientName,
    providerId = providerId,
    providerName = providerName,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    unreadCount = unreadCount,
    updatedAt = updatedAt
)

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    body = body,
    isRead = isRead,
    createdAt = createdAt
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    body = body,
    isRead = isRead,
    createdAt = createdAt
)

fun MessageDto.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    body = body,
    isRead = isRead,
    createdAt = createdAt
)

// ── Recurring Booking mappers ──────────────────────────────────────────────

fun RecurringBookingEntity.toDomain(): RecurringBooking = RecurringBooking(
    id = id,
    clientId = clientId,
    providerId = providerId,
    providerName = providerName,
    serviceId = serviceId,
    serviceName = serviceName,
    frequency = RecurringFrequency.valueOf(frequency),
    preferredDay = preferredDay,
    preferredTime = preferredTime,
    address = address,
    latitude = latitude,
    longitude = longitude,
    totalPrice = totalPrice,
    status = RecurringBookingStatus.valueOf(status),
    nextBookingDate = nextBookingDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RecurringBooking.toEntity(): RecurringBookingEntity = RecurringBookingEntity(
    id = id,
    clientId = clientId,
    providerId = providerId,
    providerName = providerName,
    serviceId = serviceId,
    serviceName = serviceName,
    frequency = frequency.name,
    preferredDay = preferredDay,
    preferredTime = preferredTime,
    address = address,
    latitude = latitude,
    longitude = longitude,
    totalPrice = totalPrice,
    status = status.name,
    nextBookingDate = nextBookingDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RecurringBookingDto.toDomain(): RecurringBooking = RecurringBooking(
    id = id,
    clientId = clientId,
    providerId = providerId,
    providerName = providerName,
    serviceId = serviceId,
    serviceName = serviceName,
    frequency = RecurringFrequency.valueOf(frequency),
    preferredDay = preferredDay,
    preferredTime = preferredTime,
    address = address,
    latitude = latitude,
    longitude = longitude,
    totalPrice = totalPrice,
    status = RecurringBookingStatus.valueOf(status),
    nextBookingDate = nextBookingDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ── Support Ticket mappers ──────────────────────────────────────────────────

fun SupportTicketEntity.toDomain(): SupportTicket = SupportTicket(
    id = id, userId = userId, userRole = userRole, bookingId = bookingId,
    type = SupportTicketType.valueOf(type), status = SupportTicketStatus.valueOf(status),
    subject = subject, description = description, conversationId = conversationId,
    createdAt = createdAt, updatedAt = updatedAt
)

fun SupportTicket.toEntity(): SupportTicketEntity = SupportTicketEntity(
    id = id, userId = userId, userRole = userRole, bookingId = bookingId,
    type = type.name, status = status.name,
    subject = subject, description = description, conversationId = conversationId,
    createdAt = createdAt, updatedAt = updatedAt
)

fun SupportTicketDto.toDomain(): SupportTicket = SupportTicket(
    id = id, userId = userId, userRole = userRole, bookingId = bookingId,
    type = SupportTicketType.valueOf(type), status = SupportTicketStatus.valueOf(status),
    subject = subject, description = description, conversationId = conversationId,
    createdAt = createdAt, updatedAt = updatedAt
)

// ── Claim mappers ────────────────────────────────────────────────────────────

fun ClaimEntity.toDomain(): Claim = Claim(
    id = id, bookingId = bookingId, filedByUserId = filedByUserId, filedByRole = filedByRole,
    claimType = ClaimType.valueOf(claimType), status = ClaimStatus.valueOf(status),
    description = description,
    evidenceImageUrls = try {
        if (evidenceImageUrls.isBlank()) emptyList()
        else kotlinx.serialization.json.Json.decodeFromString<List<String>>(evidenceImageUrls)
    } catch (_: Exception) {
        emptyList()
    },
    resolutionNotes = resolutionNotes, refundAmount = refundAmount,
    createdAt = createdAt, updatedAt = updatedAt
)

fun Claim.toEntity(): ClaimEntity = ClaimEntity(
    id = id, bookingId = bookingId, filedByUserId = filedByUserId, filedByRole = filedByRole,
    claimType = claimType.name, status = status.name, description = description,
    evidenceImageUrls = kotlinx.serialization.json.Json.encodeToString(
        kotlinx.serialization.builtins.ListSerializer(
            kotlinx.serialization.serializer<String>()
        ), evidenceImageUrls
    ),
    resolutionNotes = resolutionNotes, refundAmount = refundAmount,
    createdAt = createdAt, updatedAt = updatedAt
)

fun ClaimDto.toDomain(): Claim = Claim(
    id = id, bookingId = bookingId, filedByUserId = filedByUserId, filedByRole = filedByRole,
    claimType = ClaimType.valueOf(claimType), status = ClaimStatus.valueOf(status),
    description = description, evidenceImageUrls = evidenceImageUrls,
    resolutionNotes = resolutionNotes, refundAmount = refundAmount,
    createdAt = createdAt, updatedAt = updatedAt
)

// ── HelpArticle mappers ──────────────────────────────────────────────────────

fun HelpArticleDto.toDomain(): HelpArticle = HelpArticle(
    id = id, title = title, body = body, category = category, tags = tags
)
