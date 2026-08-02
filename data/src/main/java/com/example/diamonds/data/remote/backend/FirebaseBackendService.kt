package com.example.diamonds.data.remote.backend

import com.example.diamonds.domain.model.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of [IBackendService].
 *
 * ## Firestore Collection Schema
 *
 * ```
 * /clients/{clientId}          → ClientDto fields
 * /providers/{providerId}      → ProviderDto fields
 * /services/{serviceId}        → ServiceDto fields  (providerId indexed)
 * /bookings/{bookingId}        → BookingDto fields   (clientId, providerId indexed)
 * /reviews/{reviewId}          → ReviewDto fields    (providerId, bookingId indexed)
 * /payments/{paymentId}        → PaymentDto fields   (clientId indexed)
 * ```
 *
 * ## How to activate
 * 1. Add `google-services.json` to `:app/`.
 * 2. Uncomment the google-services plugin in `:app/build.gradle.kts`.
 * 3. Set `BuildConfig.USE_MOCK_BACKEND = false` in `app/build.gradle.kts`.
 * 4. Create Firestore collections in the Firebase Console (or seed via script).
 */
class FirebaseBackendService : IBackendService {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Collection references ────────────────────────────────────────────────

    private val clientsCol get() = db.collection("clients")
    private val providersCol get() = db.collection("providers")
    private val servicesCol get() = db.collection("services")
    private val bookingsCol get() = db.collection("bookings")
    private val reviewsCol get() = db.collection("reviews")
    private val paymentsCol get() = db.collection("payments")
    private val fcmTokensCol get() = db.collection("fcmTokens")

    // ── Auth (delegated to IAuthService; stubs here for interface compliance) ─

    override suspend fun login(email: String, password: String): Result<String> {
        // Auth is handled by FirebaseAuthService via IAuthService, not IBackendService.
        return Result.Error(Exception("Use IAuthService for authentication"))
    }

    override suspend fun signup(
        name: String, email: String, password: String,
        phoneNumber: String, role: String
    ): Result<String> {
        return Result.Error(Exception("Use IAuthService for authentication"))
    }

    // ── Clients ──────────────────────────────────────────────────────────────

    override suspend fun getClient(clientId: String): Result<ClientDto> = firestoreCall {
        val snap = clientsCol.document(clientId).get().await()
        snap.toObject(ClientDto::class.java)
            ?: throw Exception("Client $clientId not found")
    }

    override suspend fun updateClient(client: ClientDto): Result<ClientDto> = firestoreCall {
        clientsCol.document(client.id).set(client).await()
        client
    }

    // ── Providers ────────────────────────────────────────────────────────────

    override suspend fun updateProvider(provider: ProviderDto): Result<ProviderDto> = firestoreCall {
        providersCol.document(provider.id).set(provider).await()
        provider
    }

    override suspend fun getProvider(providerId: String): Result<ProviderDto> = firestoreCall {
        val snap = providersCol.document(providerId).get().await()
        snap.toObject(ProviderDto::class.java)
            ?: throw Exception("Provider $providerId not found")
    }

    override suspend fun searchProviders(
        latitude: Double, longitude: Double, radius: Int
    ): Result<List<ProviderDto>> = firestoreCall {
        // TODO: Use GeoHash-based queries for real geo-filtering.
        // For now, return all approved providers.
        val snap = providersCol
            .whereEqualTo("verificationStatus", "APPROVED")
            .get().await()
        snap.toObjects(ProviderDto::class.java)
    }

    // ── Services ─────────────────────────────────────────────────────────────

    override suspend fun getService(serviceId: String): Result<ServiceDto> = firestoreCall {
        val snap = servicesCol.document(serviceId).get().await()
        snap.toObject(ServiceDto::class.java)
            ?: throw Exception("Service $serviceId not found")
    }

    override suspend fun getServicesForProvider(providerId: String): Result<List<ServiceDto>> =
        firestoreCall {
            val snap = servicesCol
                .whereEqualTo("providerId", providerId)
                .get().await()
            snap.toObjects(ServiceDto::class.java)
        }

    override suspend fun searchServicesByCategory(category: String): Result<List<ServiceDto>> =
        firestoreCall {
            val snap = servicesCol
                .whereEqualTo("category", category)
                .whereEqualTo("isActive", true)
                .get().await()
            snap.toObjects(ServiceDto::class.java)
        }

    override suspend fun createService(service: ServiceDto): Result<ServiceDto> = firestoreCall {
        val docRef = if (service.id.isNotBlank()) {
            servicesCol.document(service.id)
        } else {
            servicesCol.document() // auto-generated ID
        }
        val saved = service.copy(id = docRef.id)
        docRef.set(saved).await()
        saved
    }

    // ── Bookings ─────────────────────────────────────────────────────────────

    override suspend fun createBooking(booking: CreateBookingRequest): Result<BookingDto> =
        firestoreCall {
            val docRef = bookingsCol.document()
            val now = System.currentTimeMillis().toString()
            // Resolve duration and price from the service document rather than hardcoding
            // them, matching BackendServiceStub. Previously every Firebase-created booking
            // was priced at $0.00 for 60 minutes regardless of the service chosen.
            // (This read only works now that ServiceDto has a no-arg constructor — see
            // FirestoreDtoContractTest.) Falls back to the old defaults if the document
            // is missing, rather than failing the whole booking.
            val service = servicesCol.document(booking.serviceId).get().await()
                .toObject(ServiceDto::class.java)
            val dto = BookingDto(
                id = docRef.id,
                clientId = booking.clientId,
                providerId = booking.providerId,
                serviceId = booking.serviceId,
                status = "PENDING",
                scheduledDate = booking.scheduledDate,
                scheduledTime = booking.scheduledTime,
                estimatedDuration = service?.duration ?: 60,
                totalPrice = service?.basePrice ?: 0.0,
                notes = booking.notes,
                address = booking.address,
                latitude = booking.latitude,
                longitude = booking.longitude,
                createdAt = now,
                updatedAt = now
            )
            docRef.set(dto).await()
            dto
        }

    override suspend fun getBooking(bookingId: String): Result<BookingDto> = firestoreCall {
        val snap = bookingsCol.document(bookingId).get().await()
        snap.toObject(BookingDto::class.java)
            ?: throw Exception("Booking $bookingId not found")
    }

    override suspend fun getClientBookings(clientId: String): Result<List<BookingDto>> =
        firestoreCall {
            val snap = bookingsCol
                .whereEqualTo("clientId", clientId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            snap.toObjects(BookingDto::class.java)
        }

    override suspend fun getProviderBookings(providerId: String): Result<List<BookingDto>> =
        firestoreCall {
            val snap = bookingsCol
                .whereEqualTo("providerId", providerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            snap.toObjects(BookingDto::class.java)
        }

    override suspend fun updateBookingStatus(
        bookingId: String, status: String
    ): Result<BookingDto> = firestoreCall {
        val docRef = bookingsCol.document(bookingId)
        docRef.update(
            mapOf(
                "status" to status,
                "updatedAt" to System.currentTimeMillis().toString()
            )
        ).await()
        val snap = docRef.get().await()
        snap.toObject(BookingDto::class.java)
            ?: throw Exception("Booking $bookingId not found after update")
    }

    override suspend fun cancelBooking(bookingId: String): Result<BookingDto> =
        updateBookingStatus(bookingId, "CANCELLED")

    // ── Reviews ──────────────────────────────────────────────────────────────

    override suspend fun createReview(review: CreateReviewRequest): Result<ReviewDto> =
        firestoreCall {
            val docRef = reviewsCol.document()
            val now = System.currentTimeMillis().toString()
            val dto = ReviewDto(
                id = docRef.id,
                bookingId = review.bookingId,
                clientId = review.clientId,
                providerId = review.providerId,
                rating = review.rating,
                comment = review.comment,
                imageUrls = review.imageUrls,
                // Both were previously dropped, so every Firestore review defaulted to
                // CLIENT_REVIEWS_PROVIDER with no tags — breaking the reverse-review split
                // (Phase 14) and making getReviewsForClient a query that could never match.
                direction = review.direction,
                locationTags = review.locationTags,
                createdAt = now,
                updatedAt = now
            )
            docRef.set(dto).await()
            dto
        }

    override suspend fun getReviewsForProvider(providerId: String): Result<List<ReviewDto>> =
        firestoreCall {
            val snap = reviewsCol
                .whereEqualTo("providerId", providerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            snap.toObjects(ReviewDto::class.java)
        }

    override suspend fun getReviewsForBooking(bookingId: String): Result<ReviewDto?> =
        firestoreCall {
            val snap = reviewsCol
                .whereEqualTo("bookingId", bookingId)
                .whereEqualTo("direction", "CLIENT_REVIEWS_PROVIDER")
                .limit(1)
                .get().await()
            snap.toObjects(ReviewDto::class.java).firstOrNull()
        }

    override suspend fun getReviewsForClient(clientId: String): Result<List<ReviewDto>> =
        firestoreCall {
            val snap = reviewsCol
                .whereEqualTo("clientId", clientId)
                .whereEqualTo("direction", "PROVIDER_REVIEWS_CLIENT")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            snap.toObjects(ReviewDto::class.java)
        }

    override suspend fun getReviewForBookingByDirection(
        bookingId: String,
        direction: String
    ): Result<ReviewDto?> =
        firestoreCall {
            val snap = reviewsCol
                .whereEqualTo("bookingId", bookingId)
                .whereEqualTo("direction", direction)
                .limit(1)
                .get().await()
            snap.toObjects(ReviewDto::class.java).firstOrNull()
        }

    // ── Payments ─────────────────────────────────────────────────────────────

    override suspend fun createPayment(payment: CreatePaymentRequest): Result<PaymentDto> =
        firestoreCall {
            val docRef = paymentsCol.document()
            val now = System.currentTimeMillis().toString()
            val dto = PaymentDto(
                id = docRef.id,
                bookingId = payment.bookingId,
                clientId = payment.clientId,
                providerId = payment.providerId,
                amount = payment.amount,
                status = "SUCCEEDED",
                method = payment.method,
                transactionId = "txn_${docRef.id}",
                createdAt = now,
                updatedAt = now
            )
            docRef.set(dto).await()
            dto
        }

    override suspend fun getPayment(paymentId: String): Result<PaymentDto> = firestoreCall {
        val snap = paymentsCol.document(paymentId).get().await()
        snap.toObject(PaymentDto::class.java)
            ?: throw Exception("Payment $paymentId not found")
    }

    override suspend fun getPaymentsForClient(clientId: String): Result<List<PaymentDto>> =
        firestoreCall {
            val snap = paymentsCol
                .whereEqualTo("clientId", clientId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            snap.toObjects(PaymentDto::class.java)
        }

    // ── Location / Tracking ─────────────────────────────────────────────────

    private val locationsCol = db.collection("provider_locations")
    private val serviceAreasCol = db.collection("service_areas")

    override suspend fun getProviderLocation(providerId: String): Result<ProviderLocationDto> =
        firestoreCall {
            val snap = locationsCol.document(providerId).get().await()
            snap.toObject(ProviderLocationDto::class.java)
                ?: throw Exception("Provider location not found")
        }

    override suspend fun updateProviderLocation(location: ProviderLocationDto): Result<Unit> =
        firestoreCall {
            locationsCol.document(location.providerId).set(location).await()
        }

    override suspend fun getServiceArea(providerId: String): Result<ServiceAreaDto> =
        firestoreCall {
            val snap = serviceAreasCol.document(providerId).get().await()
            snap.toObject(ServiceAreaDto::class.java)
                ?: throw Exception("Service area not found")
        }

    // ── Chat / Messaging ─────────────────────────────────────────────────────

    private val conversationsCol = db.collection("conversations")
    private val messagesCol = db.collection("messages")

    override suspend fun getOrCreateConversation(request: CreateConversationRequest): Result<ConversationDto> =
        firestoreCall {
            // Check if conversation for this booking already exists
            val existing = conversationsCol
                .whereEqualTo("bookingId", request.bookingId)
                .get().await()
            if (!existing.isEmpty) {
                return@firestoreCall existing.toObjects(ConversationDto::class.java).first()
            }
            val id = conversationsCol.document().id
            val timestamp = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()
            ).format(java.util.Date())
            val dto = ConversationDto(
                id = id,
                bookingId = request.bookingId,
                clientId = request.clientId,
                clientName = request.clientName,
                providerId = request.providerId,
                providerName = request.providerName,
                lastMessage = "",
                lastMessageAt = "",
                unreadCount = 0,
                updatedAt = timestamp
            )
            conversationsCol.document(id).set(dto).await()
            dto
        }

    override suspend fun getConversationsForUser(userId: String): Result<List<ConversationDto>> =
        firestoreCall {
            val asClient = conversationsCol.whereEqualTo("clientId", userId).get().await()
            val asProvider = conversationsCol.whereEqualTo("providerId", userId).get().await()
            (asClient.toObjects(ConversationDto::class.java) +
                    asProvider.toObjects(ConversationDto::class.java))
                .distinctBy { it.id }
                .sortedByDescending { it.updatedAt }
        }

    override suspend fun getMessages(conversationId: String): Result<List<MessageDto>> =
        firestoreCall {
            val snap = messagesCol
                .whereEqualTo("conversationId", conversationId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get().await()
            snap.toObjects(MessageDto::class.java)
        }

    override suspend fun sendMessage(message: SendMessageRequest): Result<MessageDto> =
        firestoreCall {
            val dto = MessageDto(
                id = message.id,
                conversationId = message.conversationId,
                senderId = message.senderId,
                senderName = message.senderName,
                body = message.body,
                isRead = false,
                createdAt = message.createdAt
            )
            messagesCol.document(message.id).set(dto).await()
            // Update conversation last message
            conversationsCol.document(message.conversationId).update(
                mapOf(
                    "lastMessage" to message.body,
                    "lastMessageAt" to message.createdAt,
                    "updatedAt" to message.createdAt
                )
            ).await()
            dto
        }

    override suspend fun markConversationRead(
        conversationId: String,
        userId: String
    ): Result<Unit> =
        firestoreCall {
            // Reset unread count
            conversationsCol.document(conversationId).update("unreadCount", 0).await()
        }

    // ── Recurring Bookings (Firestore) ────────────────────────────────────

    private val recurringBookingsCol get() = db.collection("recurringBookings")

    override suspend fun createRecurringBooking(request: CreateRecurringBookingRequest): Result<RecurringBookingDto> =
        firestoreCall {
            val doc = recurringBookingsCol.document()
            val now = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            val dto = RecurringBookingDto(
                id = doc.id,
                clientId = request.clientId,
                providerId = request.providerId,
                providerName = request.providerName,
                serviceId = request.serviceId,
                serviceName = request.serviceName,
                frequency = request.frequency,
                preferredDay = request.preferredDay,
                preferredTime = request.preferredTime,
                address = request.address,
                latitude = request.latitude,
                longitude = request.longitude,
                totalPrice = request.totalPrice,
                status = "ACTIVE",
                nextBookingDate = now,
                createdAt = now,
                updatedAt = now
            )
            doc.set(dto).await()
            dto
        }

    override suspend fun getRecurringBooking(id: String): Result<RecurringBookingDto> =
        firestoreCall {
            recurringBookingsCol.document(id).get().await()
                .toObject(RecurringBookingDto::class.java)!!
        }

    override suspend fun getRecurringBookingsForClient(clientId: String): Result<List<RecurringBookingDto>> =
        firestoreCall {
            recurringBookingsCol.whereEqualTo("clientId", clientId).get().await()
                .toObjects(RecurringBookingDto::class.java)
        }

    override suspend fun updateRecurringBookingStatus(
        id: String,
        status: String
    ): Result<RecurringBookingDto> =
        firestoreCall {
            val now = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            recurringBookingsCol.document(id).update(mapOf("status" to status, "updatedAt" to now))
                .await()
            recurringBookingsCol.document(id).get().await()
                .toObject(RecurringBookingDto::class.java)!!
        }

    override suspend fun updateRecurringBookingSchedule(
        id: String,
        preferredDay: Int,
        preferredTime: String
    ): Result<RecurringBookingDto> =
        firestoreCall {
            val now = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            recurringBookingsCol.document(id).update(
                mapOf(
                    "preferredDay" to preferredDay,
                    "preferredTime" to preferredTime,
                    "updatedAt" to now
                )
            ).await()
            recurringBookingsCol.document(id).get().await()
                .toObject(RecurringBookingDto::class.java)!!
        }

    override suspend fun advanceRecurringBookingDate(id: String, newDate: String): Result<Unit> =
        firestoreCall {
            recurringBookingsCol.document(id).update("nextBookingDate", newDate).await()
        }

    // ── Support & Claims (Firestore stubs) ────────────────────────────────

    override suspend fun createSupportTicket(request: CreateSupportTicketRequest): Result<SupportTicketDto> =
        Result.Error(Exception("Firebase support tickets not yet implemented"))

    override suspend fun getSupportTicket(ticketId: String): Result<SupportTicketDto> =
        Result.Error(Exception("Firebase support tickets not yet implemented"))

    override suspend fun getTicketsForUser(userId: String): Result<List<SupportTicketDto>> =
        Result.Error(Exception("Firebase support tickets not yet implemented"))

    override suspend fun fileClaim(request: FileClaimRequest): Result<ClaimDto> =
        Result.Error(Exception("Firebase claims not yet implemented"))

    override suspend fun getClaim(claimId: String): Result<ClaimDto> =
        Result.Error(Exception("Firebase claims not yet implemented"))

    override suspend fun getClaimsForUser(userId: String): Result<List<ClaimDto>> =
        Result.Error(Exception("Firebase claims not yet implemented"))

    override suspend fun getClaimForBooking(bookingId: String): Result<ClaimDto?> =
        Result.Error(Exception("Firebase claims not yet implemented"))

    override suspend fun cancelBookingWithReason(request: CancelBookingWithReasonRequest): Result<BookingDto> =
        Result.Error(Exception("Firebase cancel with reason not yet implemented"))

    override suspend fun editBooking(request: EditBookingRequest): Result<BookingDto> =
        Result.Error(Exception("Firebase edit booking not yet implemented"))

    override suspend fun requestRefund(bookingId: String): Result<PaymentDto> =
        Result.Error(Exception("Firebase refund not yet implemented"))

    override suspend fun getHelpArticles(): Result<List<HelpArticleDto>> =
        Result.Error(Exception("Firebase help articles not yet implemented"))

    override suspend fun getSavedLocations(clientId: String): Result<List<SavedLocationDto>> =
        Result.Error(Exception("Firebase saved locations not yet implemented"))

    override suspend fun upsertSavedLocation(location: SavedLocationDto): Result<SavedLocationDto> =
        Result.Error(Exception("Firebase saved locations not yet implemented"))

    override suspend fun deleteSavedLocation(locationId: String): Result<Unit> =
        Result.Error(Exception("Firebase saved locations not yet implemented"))

    // ── Push notifications ───────────────────────────────────────────────────

    override suspend fun registerFcmToken(userId: String, token: String): Result<Unit> =
        firestoreCall {
            // set() rather than update(): the same device re-registering after switching accounts
            // must overwrite the row wholesale, not merge into the previous user's.
            fcmTokensCol.document(token).set(
                FcmTokenDto(
                    token = token,
                    userId = userId,
                    updatedAt = System.currentTimeMillis().toString()
                )
            ).await()
        }

    override suspend fun unregisterFcmToken(token: String): Result<Unit> = firestoreCall {
        fcmTokensCol.document(token).delete().await()
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Wraps a suspend Firestore call in a try/catch, returning [Result].
     */
    private suspend inline fun <T> firestoreCall(crossinline block: suspend () -> T): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
