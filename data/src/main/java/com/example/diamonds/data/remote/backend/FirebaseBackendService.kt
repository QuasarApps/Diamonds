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
            val dto = BookingDto(
                id = docRef.id,
                clientId = booking.clientId,
                providerId = booking.providerId,
                serviceId = booking.serviceId,
                status = "PENDING",
                scheduledDate = booking.scheduledDate,
                scheduledTime = booking.scheduledTime,
                estimatedDuration = 60, // default; could lookup service duration
                totalPrice = 0.0,       // default; could lookup service price
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
