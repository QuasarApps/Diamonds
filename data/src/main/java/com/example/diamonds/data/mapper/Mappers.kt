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
import com.example.diamonds.domain.model.CleaningType
import com.example.diamonds.domain.model.Client
import com.example.diamonds.domain.model.Conversation
import com.example.diamonds.domain.model.GeoLocation
import com.example.diamonds.domain.model.HelpArticle
import com.example.diamonds.domain.model.LocationType
import com.example.diamonds.domain.model.MalformedDtoException
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
    specializations = try {
        if (specializations.isBlank() || specializations == "[]") emptyList()
        else kotlinx.serialization.json.Json.decodeFromString<List<String>>(specializations)
            .mapNotNull { runCatching { com.example.diamonds.domain.model.CleaningType.valueOf(it) }.getOrNull() }
    } catch (_: Exception) {
        emptyList()
    },
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
    cleaningType = cleaningType?.let {
        runCatching {
            com.example.diamonds.domain.model.CleaningType.valueOf(
                it
            )
        }.getOrNull()
    },
    locationType = locationType?.let {
        runCatching {
            com.example.diamonds.domain.model.LocationType.valueOf(
                it
            )
        }.getOrNull()
    },
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
    specializations = kotlinx.serialization.json.Json.encodeToString(
        kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()),
        specializations.map { it.name }
    ),
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
    cleaningType = cleaningType?.name,
    locationType = locationType?.name,
    syncStatus = syncStatus.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// DTO to Domain
//
// These run at the untrusted boundary: the values come from the backend, not from our own writes.
// Since every DTO parameter has a default (required by Firestore's object mapper — see
// FirestoreDtoContractTest), a partial or malformed document deserializes happily into blanks and
// zeroes. The helpers below put the check back where it can name what is wrong.

/**
 * Parses [value] as a [T], or throws naming the field and what was actually received.
 *
 * Enum fields are not defaulted on failure. An unrecognised value means the backend sent something
 * this build does not understand, and quietly substituting a fallback misrepresents the record —
 * a `CANCELLED` booking shown as `PENDING`, a provider-to-client review attributed the other way
 * round. Failing is recoverable; silently wrong data is not.
 */
private inline fun <reified T : Enum<T>> enumField(value: String, dto: String, field: String): T =
    enumValues<T>().firstOrNull { it.name == value }
        ?: throw MalformedDtoException(
            "$dto.$field: expected one of [${enumValues<T>().joinToString { it.name }}] " +
                "but got \"$value\""
        )

/** Nullable variant: absent stays absent, but a *present* unrecognised value is still an error. */
private inline fun <reified T : Enum<T>> enumFieldOrNull(value: String?, dto: String, field: String): T? =
    value?.let { enumField<T>(it, dto, field) }

/**
 * Returns [value], or throws if it is blank.
 *
 * Applied to identity fields specifically. A domain object with a blank `id` is not a harmless
 * partial read: repositories cache by primary key, so every malformed record of a given type
 * collides on the same `""` row and overwrites the previous one.
 */
private fun requiredId(value: String, dto: String, field: String): String =
    value.ifBlank {
        throw MalformedDtoException("$dto.$field is blank — the document has no usable identity")
    }

fun ClientDto.toDomain(): Client = Client(
    id = requiredId(id, "ClientDto", "id"),
    name = name,
    email = email,
    phoneNumber = phoneNumber,
    profileImageUrl = profileImageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProviderDto.toDomain(): Provider = Provider(
    id = requiredId(id, "ProviderDto", "id"),
    name = name,
    email = email,
    phoneNumber = phoneNumber,
    profileImageUrl = profileImageUrl,
    bio = bio,
    rating = rating,
    reviewCount = reviewCount,
    // Never defaulted: guessing this field either locks out a verified provider or, worse,
    // presents an unverified one as APPROVED.
    verificationStatus = enumField(verificationStatus, "ProviderDto", "verificationStatus"),
    serviceRadius = serviceRadius,
    cleanerType = enumField(cleanerType, "ProviderDto", "cleanerType"),
    employerId = employerId,
    employerName = employerName,
    // Was mapNotNull { getOrNull() }: a provider listing five specialities, two of them
    // unrecognised, silently showed three and nothing anywhere said so.
    specializations = specializations.map {
        enumField<CleaningType>(it, "ProviderDto", "specializations")
    },
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Inverse of [ProviderDto.toDomain], for the write path.
 *
 * Enum-backed fields go over the wire as their `name`, matching what `toDomain` parses back.
 */
fun Provider.toDto(): ProviderDto = ProviderDto(
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
    specializations = specializations.map { it.name },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ServiceDto.toDomain(): Service = Service(
    id = requiredId(id, "ServiceDto", "id"),
    providerId = providerId,
    title = title,
    description = description,
    basePrice = basePrice,
    duration = duration,
    category = enumField(category, "ServiceDto", "category"),
    imageUrl = imageUrl,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BookingDto.toDomain(): Booking = Booking(
    id = requiredId(id, "BookingDto", "id"),
    clientId = clientId,
    providerId = providerId,
    serviceId = serviceId,
    status = enumField(status, "BookingDto", "status"),
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    estimatedDuration = estimatedDuration,
    totalPrice = totalPrice,
    notes = notes,
    address = address,
    latitude = latitude,
    longitude = longitude,
    // Genuinely optional, so absent stays null — but a value we cannot parse is not the same
    // thing as "unspecified", and it used to be flattened into one.
    cleaningType = enumFieldOrNull<CleaningType>(cleaningType, "BookingDto", "cleaningType"),
    locationType = enumFieldOrNull<LocationType>(locationType, "BookingDto", "locationType"),
    syncStatus = SyncStatus.SYNCED,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ReviewDto.toDomain(): Review = Review(
    id = requiredId(id, "ReviewDto", "id"),
    bookingId = bookingId,
    clientId = clientId,
    providerId = providerId,
    rating = rating,
    comment = comment,
    imageUrls = imageUrls,
    // Was defaulted to CLIENT_REVIEWS_PROVIDER, which silently attributes a cleaner's review of a
    // customer to the customer instead — the review shows up on the wrong profile.
    direction = enumField(direction, "ReviewDto", "direction"),
    locationTags = locationTags,
    syncStatus = SyncStatus.SYNCED,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PaymentDto.toDomain(): Payment = Payment(
    id = requiredId(id, "PaymentDto", "id"),
    bookingId = bookingId,
    clientId = clientId,
    providerId = providerId,
    amount = amount,
    status = enumField(status, "PaymentDto", "status"),
    method = enumField(method, "PaymentDto", "method"),
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
    id = requiredId(id, "ConversationDto", "id"),
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
    id = requiredId(id, "MessageDto", "id"),
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
    id = requiredId(id, "RecurringBookingDto", "id"),
    clientId = clientId,
    providerId = providerId,
    providerName = providerName,
    serviceId = serviceId,
    serviceName = serviceName,
    frequency = enumField(frequency, "RecurringBookingDto", "frequency"),
    preferredDay = preferredDay,
    preferredTime = preferredTime,
    address = address,
    latitude = latitude,
    longitude = longitude,
    totalPrice = totalPrice,
    status = enumField(status, "RecurringBookingDto", "status"),
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
    id = requiredId(id, "SupportTicketDto", "id"),
    userId = userId, userRole = userRole, bookingId = bookingId,
    type = enumField(type, "SupportTicketDto", "type"),
    status = enumField(status, "SupportTicketDto", "status"),
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
    id = requiredId(id, "ClaimDto", "id"),
    bookingId = bookingId, filedByUserId = filedByUserId, filedByRole = filedByRole,
    claimType = enumField(claimType, "ClaimDto", "claimType"),
    status = enumField(status, "ClaimDto", "status"),
    description = description, evidenceImageUrls = evidenceImageUrls,
    resolutionNotes = resolutionNotes, refundAmount = refundAmount,
    createdAt = createdAt, updatedAt = updatedAt
)

// ── HelpArticle mappers ──────────────────────────────────────────────────────

fun HelpArticleDto.toDomain(): HelpArticle = HelpArticle(
    id = requiredId(id, "HelpArticleDto", "id"),
    title = title, body = body, category = category, tags = tags
)

// ── SavedLocation mappers ────────────────────────────────────────────────────

fun com.example.diamonds.data.local.entity.SavedLocationEntity.toDomain(): com.example.diamonds.domain.model.SavedLocation =
    com.example.diamonds.domain.model.SavedLocation(
        id = id, clientId = clientId, label = label, address = address,
        latitude = latitude, longitude = longitude,
        locationType = runCatching {
            com.example.diamonds.domain.model.LocationType.valueOf(
                locationType
            )
        }.getOrDefault(com.example.diamonds.domain.model.LocationType.HOUSE),
        roomCount = roomCount, bathroomCount = bathroomCount, sqFootage = sqFootage,
        createdAt = createdAt, updatedAt = updatedAt
    )

fun com.example.diamonds.domain.model.SavedLocation.toEntity(): com.example.diamonds.data.local.entity.SavedLocationEntity =
    com.example.diamonds.data.local.entity.SavedLocationEntity(
        id = id, clientId = clientId, label = label, address = address,
        latitude = latitude, longitude = longitude,
        locationType = locationType.name,
        roomCount = roomCount, bathroomCount = bathroomCount, sqFootage = sqFootage,
        createdAt = createdAt, updatedAt = updatedAt
    )

fun com.example.diamonds.data.remote.backend.SavedLocationDto.toDomain(): com.example.diamonds.domain.model.SavedLocation =
    com.example.diamonds.domain.model.SavedLocation(
        id = requiredId(id, "SavedLocationDto", "id"),
        clientId = clientId, label = label, address = address,
        latitude = latitude, longitude = longitude,
        locationType = enumField(locationType, "SavedLocationDto", "locationType"),
        roomCount = roomCount, bathroomCount = bathroomCount, sqFootage = sqFootage,
        createdAt = createdAt, updatedAt = updatedAt
    )

fun com.example.diamonds.domain.model.SavedLocation.toDto(): com.example.diamonds.data.remote.backend.SavedLocationDto =
    com.example.diamonds.data.remote.backend.SavedLocationDto(
        id = id, clientId = clientId, label = label, address = address,
        latitude = latitude, longitude = longitude,
        locationType = locationType.name,
        roomCount = roomCount, bathroomCount = bathroomCount, sqFootage = sqFootage,
        createdAt = createdAt, updatedAt = updatedAt
    )

