package com.example.diamonds.di

import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.Client
import com.example.diamonds.domain.model.Conversation
import com.example.diamonds.domain.model.GeoLocation
import com.example.diamonds.domain.model.Message
import com.example.diamonds.domain.model.Notification
import com.example.diamonds.domain.model.NotificationPreferences
import com.example.diamonds.domain.model.NotificationType
import com.example.diamonds.domain.model.Payment
import com.example.diamonds.domain.model.PaymentMethod
import com.example.diamonds.domain.model.PaymentStatus
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.ProviderLocation
import com.example.diamonds.domain.model.RecurringBooking
import com.example.diamonds.domain.model.RecurringBookingStatus
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.Review
import com.example.diamonds.domain.model.Service
import com.example.diamonds.domain.model.ServiceArea
import com.example.diamonds.domain.model.ServiceCategory
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.model.VerificationStatus
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.IClientRepository
import com.example.diamonds.domain.repository.ILocationRepository
import com.example.diamonds.domain.repository.IMessageRepository
import com.example.diamonds.domain.repository.INotificationRepository
import com.example.diamonds.domain.repository.IPaymentRepository
import com.example.diamonds.domain.repository.IProviderRepository
import com.example.diamonds.domain.repository.IReviewRepository
import com.example.diamonds.domain.repository.IServiceRepository
import com.example.diamonds.domain.repository.ISubscriptionRepository
import com.example.diamonds.domain.repository.ISyncRepository
import com.example.diamonds.domain.repository.SyncOperation
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

// ── Shared test data ──────────────────────────────────────────────────────────

val fakeSession = UserSession(
    userId = "test-user-1",
    email = "test@diamonds.com",
    displayName = "Test User",
    role = UserRole.CUSTOMER,
    cleanerType = CleanerType.INDEPENDENT,
    authToken = "fake-token",
    isAuthenticated = true
)

val fakeCleanerSession = UserSession(
    userId = "cleaner-1",
    email = "cleaner@diamonds.com",
    displayName = "Clean Pro",
    role = UserRole.CLEANER,
    cleanerType = CleanerType.INDEPENDENT,
    authToken = "fake-token-cleaner",
    isAuthenticated = true
)

val fakeProvider = Provider(
    id = "provider-1",
    name = "Clean Pro Services",
    email = "pro@cleaning.com",
    phoneNumber = "+27821234567",
    bio = "Professional cleaner with 5 years experience.",
    rating = 4.5f,
    reviewCount = 12,
    verificationStatus = VerificationStatus.APPROVED,
    serviceRadius = 15,
    cleanerType = CleanerType.INDEPENDENT,
    createdAt = "2024-01-01T00:00:00Z",
    updatedAt = "2024-01-01T00:00:00Z"
)

val fakeService = Service(
    id = "service-1",
    providerId = "provider-1",
    title = "Apartment Cleaning",
    description = "Full apartment deep clean",
    basePrice = 350.0,
    duration = 120,
    category = ServiceCategory.APARTMENT_CLEANING,
    isActive = true,
    createdAt = "2024-01-01T00:00:00Z",
    updatedAt = "2024-01-01T00:00:00Z"
)

val fakeBooking = Booking(
    id = "booking-1",
    clientId = "test-user-1",
    providerId = "provider-1",
    serviceId = "service-1",
    status = BookingStatus.PENDING,
    scheduledDate = "2026-05-01",
    scheduledTime = "10:00",
    estimatedDuration = 120,
    totalPrice = 350.0,
    address = "123 Main St, Cape Town",
    syncStatus = SyncStatus.SYNCED,
    createdAt = "2024-01-01T00:00:00Z",
    updatedAt = "2024-01-01T00:00:00Z"
)

val fakePayment = Payment(
    id = "payment-1",
    bookingId = "booking-1",
    clientId = "test-user-1",
    providerId = "provider-1",
    amount = 350.0,
    status = PaymentStatus.SUCCEEDED,
    method = PaymentMethod.CARD,
    transactionId = "txn-001",
    syncStatus = SyncStatus.SYNCED,
    createdAt = "2024-01-01T00:00:00Z",
    updatedAt = "2024-01-01T00:00:00Z"
)

val fakeNotification = Notification(
    id = "notif-1",
    userId = "test-user-1",
    title = "Booking Confirmed",
    body = "Your booking has been accepted.",
    type = NotificationType.BOOKING_UPDATE,
    referenceId = "booking-1",
    isRead = false,
    createdAt = "2024-01-01T00:00:00Z"
)

val fakeReview = Review(
    id = "review-1",
    bookingId = "booking-1",
    clientId = "test-user-1",
    providerId = "provider-1",
    rating = 5,
    comment = "Excellent work!",
    syncStatus = SyncStatus.SYNCED,
    createdAt = "2024-01-01T00:00:00Z",
    updatedAt = "2024-01-01T00:00:00Z"
)

val fakeClient = Client(
    id = "test-user-1",
    name = "Test User",
    email = "test@diamonds.com",
    phoneNumber = "+27821234567",
    createdAt = "2024-01-01T00:00:00Z",
    updatedAt = "2024-01-01T00:00:00Z"
)

// ── Fake implementations ──────────────────────────────────────────────────────

class FakeAuthRepository : IAuthRepository {
    var shouldFail = false
    private val _session = MutableStateFlow<UserSession?>(initialSession)

    companion object {
        /**
         * Set this BEFORE the Activity launches (e.g., in a @BeforeClass or via
         * a Rule with order = -1) to control the initial auth state.
         *
         * Default = [fakeSession] (authenticated customer) so app-shell tests work.
         * Set to null in login/signup tests so the splash routes to the Login screen.
         */
        var initialSession: UserSession? = fakeSession
    }

    override suspend fun login(
        email: String, password: String,
        roleHint: UserRole?,
        cleanerTypeHint: com.example.diamonds.domain.model.CleanerType?
    ): Result<String> = if (shouldFail) Result.Error(Exception("Login failed"))
    else Result.Success("fake-token")

    override suspend fun signup(
        name: String, email: String, password: String, phoneNumber: String,
        role: UserRole, cleanerType: com.example.diamonds.domain.model.CleanerType
    ): Result<String> = if (shouldFail) Result.Error(Exception("Signup failed"))
    else Result.Success("fake-token")

    override suspend fun logout(): Result<Unit> {
        _session.value = null
        return Result.Success(Unit)
    }

    override suspend fun refreshToken(): Result<String> = Result.Success("fake-token")
    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        if (shouldFail) Result.Error(Exception("Reset failed")) else Result.Success(Unit)

    override fun getCurrentUserSession(): Flow<UserSession?> = _session

    fun setSession(session: UserSession?) {
        _session.value = session
    }
}

class FakeBookingRepository : IBookingRepository {
    var bookings = mutableListOf(fakeBooking)
    private val _bookingsFlow = MutableStateFlow(bookings.toList())

    override suspend fun createBooking(booking: Booking): Result<Booking> {
        bookings.add(booking)
        _bookingsFlow.value = bookings.toList()
        return Result.Success(booking.copy(syncStatus = SyncStatus.SYNCED))
    }

    override suspend fun getBooking(bookingId: String): Result<Booking> =
        bookings.find { it.id == bookingId }?.let { Result.Success(it) }
            ?: Result.Error(Exception("Not found"))

    override suspend fun getClientBookings(clientId: String): Result<List<Booking>> =
        Result.Success(bookings.filter { it.clientId == clientId })

    override suspend fun getProviderBookings(providerId: String): Result<List<Booking>> =
        Result.Success(bookings.filter { it.providerId == providerId })

    override suspend fun updateBookingStatus(
        bookingId: String, status: BookingStatus
    ): Result<Booking> {
        val idx = bookings.indexOfFirst { it.id == bookingId }
        return if (idx >= 0) {
            bookings[idx] = bookings[idx].copy(status = status)
            _bookingsFlow.value = bookings.toList()
            Result.Success(bookings[idx])
        } else Result.Error(Exception("Not found"))
    }

    override suspend fun cancelBooking(bookingId: String): Result<Booking> =
        updateBookingStatus(bookingId, BookingStatus.CANCELLED)

    override fun observeBooking(bookingId: String): Flow<Booking?> =
        flowOf(bookings.find { it.id == bookingId })

    override fun observeClientBookings(clientId: String): Flow<List<Booking>> = _bookingsFlow

    override fun observeProviderBookings(providerId: String): Flow<List<Booking>> =
        flowOf(bookings.filter { it.providerId == providerId })
}

class FakeProviderRepository : IProviderRepository {
    var providers = mutableListOf(fakeProvider)

    override suspend fun getProvider(providerId: String): Result<Provider> =
        providers.find { it.id == providerId }?.let { Result.Success(it) }
            ?: Result.Error(Exception("Not found"))

    override suspend fun searchProviders(
        latitude: Double, longitude: Double, radius: Int
    ): Result<List<Provider>> = Result.Success(providers)

    override suspend fun searchProvidersByCategory(
        category: ServiceCategory, latitude: Double, longitude: Double, radius: Int
    ): Result<List<Provider>> = Result.Success(providers.filter { true })

    override suspend fun updateProvider(provider: Provider): Result<Provider> {
        val idx = providers.indexOfFirst { it.id == provider.id }
        return if (idx >= 0) {
            providers[idx] = provider
            Result.Success(provider)
        } else Result.Error(Exception("Not found"))
    }

    override fun observeProviderRating(providerId: String): Flow<Pair<Float, Int>> =
        flowOf(4.5f to 12)
}

class FakeServiceRepository : IServiceRepository {
    var services = mutableListOf(fakeService)

    override suspend fun getService(serviceId: String): Result<Service> =
        services.find { it.id == serviceId }?.let { Result.Success(it) }
            ?: Result.Error(Exception("Not found"))

    override suspend fun getServicesForProvider(providerId: String): Result<List<Service>> =
        Result.Success(services.filter { it.providerId == providerId })

    override suspend fun searchServicesByCategory(category: ServiceCategory): Result<List<Service>> =
        Result.Success(services.filter { it.category == category })

    override suspend fun createService(service: Service): Result<Service> {
        services.add(service)
        return Result.Success(service)
    }

    override suspend fun updateService(service: Service): Result<Service> {
        val idx = services.indexOfFirst { it.id == service.id }
        return if (idx >= 0) {
            services[idx] = service
            Result.Success(service)
        } else Result.Error(Exception("Not found"))
    }
}

class FakeClientRepository : IClientRepository {
    private val client = fakeClient

    override suspend fun getClient(clientId: String): Result<Client> = Result.Success(client)
    override suspend fun updateClient(client: Client): Result<Client> = Result.Success(client)
    override suspend fun getCurrentClient(): Result<Client> = Result.Success(client)
    override fun observeCurrentClient(): Flow<Client?> = flowOf(client)
}

class FakePaymentRepository : IPaymentRepository {
    var payments = mutableListOf(fakePayment)

    override suspend fun createPayment(payment: Payment): Result<Payment> {
        payments.add(payment)
        return Result.Success(payment.copy(status = PaymentStatus.SUCCEEDED))
    }

    override suspend fun getPayment(paymentId: String): Result<Payment> =
        payments.find { it.id == paymentId }?.let { Result.Success(it) }
            ?: Result.Error(Exception("Not found"))

    override suspend fun getPaymentsForClient(clientId: String): Result<List<Payment>> =
        Result.Success(payments.filter { it.clientId == clientId })

    override suspend fun getPaymentsForProvider(providerId: String): Result<List<Payment>> =
        Result.Success(payments.filter { it.providerId == providerId })

    override suspend fun updatePaymentStatus(
        paymentId: String, status: PaymentStatus
    ): Result<Payment> {
        val idx = payments.indexOfFirst { it.id == paymentId }
        return if (idx >= 0) {
            payments[idx] = payments[idx].copy(status = status)
            Result.Success(payments[idx])
        } else Result.Error(Exception("Not found"))
    }
}

class FakeReviewRepository : IReviewRepository {
    var reviews = mutableListOf(fakeReview)

    override suspend fun createReview(review: Review): Result<Review> {
        reviews.add(review)
        return Result.Success(review)
    }

    override suspend fun getReviewsForProvider(providerId: String): Result<List<Review>> =
        Result.Success(reviews.filter { it.providerId == providerId })

    override suspend fun getReviewsForBooking(bookingId: String): Result<Review?> =
        Result.Success(reviews.find { it.bookingId == bookingId })

    override fun observeReviewsForProvider(providerId: String): Flow<List<Review>> =
        flowOf(reviews.filter { it.providerId == providerId })
}

class FakeNotificationRepository : INotificationRepository {
    var notifications = mutableListOf(fakeNotification)
    private val _flow = MutableStateFlow(notifications.toList())

    override suspend fun getNotifications(userId: String): Result<List<Notification>> =
        Result.Success(notifications.filter { it.userId == userId })

    override suspend fun addNotification(notification: Notification): Result<Unit> {
        notifications.add(notification)
        _flow.value = notifications.toList()
        return Result.Success(Unit)
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        val idx = notifications.indexOfFirst { it.id == notificationId }
        if (idx >= 0) notifications[idx] = notifications[idx].copy(isRead = true)
        _flow.value = notifications.toList()
        return Result.Success(Unit)
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        notifications = notifications.map { it.copy(isRead = true) }.toMutableList()
        _flow.value = notifications.toList()
        return Result.Success(Unit)
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        notifications.removeIf { it.id == notificationId }
        _flow.value = notifications.toList()
        return Result.Success(Unit)
    }

    override fun observeUnreadCount(userId: String): Flow<Int> =
        flowOf(notifications.count { !it.isRead && it.userId == userId })

    override fun observeNotifications(userId: String): Flow<List<Notification>> = _flow

    override suspend fun getNotificationPreferences(): NotificationPreferences =
        NotificationPreferences()

    override suspend fun updateNotificationPreferences(
        preferences: NotificationPreferences
    ): Result<Unit> = Result.Success(Unit)
}

class FakeLocationRepository : ILocationRepository {
    override suspend fun getCurrentLocation(): Result<GeoLocation> =
        Result.Success(GeoLocation(-33.9249, 18.4241))

    override fun observeProviderLocation(providerId: String): Flow<ProviderLocation> =
        flowOf(
            ProviderLocation(
                providerId = providerId,
                location = GeoLocation(-33.9249, 18.4241),
                updatedAt = "2024-01-01T00:00:00Z"
            )
        )

    override suspend fun updateProviderLocation(providerLocation: ProviderLocation): Result<Unit> =
        Result.Success(Unit)

    override suspend fun getServiceArea(providerId: String): Result<ServiceArea> =
        Result.Success(
            ServiceArea(
                providerId = providerId,
                center = GeoLocation(-33.9249, 18.4241),
                radiusKm = 15.0
            )
        )

    override suspend fun calculateEta(from: GeoLocation, to: GeoLocation): Result<Double> =
        Result.Success(15.0)

    override fun calculateDistance(from: GeoLocation, to: GeoLocation): Double = 5.0
}

class FakeSyncRepository : ISyncRepository {
    override suspend fun queueOperation(operation: SyncOperation): Result<Unit> =
        Result.Success(Unit)

    override suspend fun getSyncQueue(): Result<List<SyncOperation>> = Result.Success(emptyList())
    override suspend fun removeSyncOperation(operationId: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun cancelSyncOperation(operationId: String): Result<Unit> =
        Result.Success(Unit)

    override fun observeSyncQueue(): Flow<List<SyncOperation>> = flowOf(emptyList())
    override fun observePendingOperationCount(): Flow<Int> = flowOf(0)
    override fun observeConflictOperations(): Flow<List<SyncOperation>> = flowOf(emptyList())
    override fun observeFailedOperations(): Flow<List<SyncOperation>> = flowOf(emptyList())
    override suspend fun retryFailedOperation(operationId: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun retryAllFailed(): Result<Unit> = Result.Success(Unit)

    override suspend fun resolveConflict(operationId: String, useLocal: Boolean): Result<Unit> =
        Result.Success(Unit)

    override suspend fun syncNow(): Result<Unit> = Result.Success(Unit)
}

class FakeMessageRepository : IMessageRepository {
    private val conversations = mutableListOf<Conversation>()
    private val messages = mutableListOf<Message>()

    override suspend fun getOrCreateConversation(
        bookingId: String,
        clientId: String,
        clientName: String,
        providerId: String,
        providerName: String
    ): Result<Conversation> {
        val existing = conversations.find { it.bookingId == bookingId }
        if (existing != null) return Result.Success(existing)

        val conv = Conversation(
            id = "conv-$bookingId",
            bookingId = bookingId,
            clientId = clientId,
            clientName = clientName,
            providerId = providerId,
            providerName = providerName,
            lastMessage = "",
            lastMessageAt = "",
            unreadCount = 0,
            updatedAt = System.currentTimeMillis().toString()
        )
        conversations.add(conv)
        return Result.Success(conv)
    }

    override suspend fun getConversationsForUser(userId: String): Result<List<Conversation>> {
        return Result.Success(
            conversations.filter { it.clientId == userId || it.providerId == userId }
        )
    }

    override fun observeConversationsForUser(userId: String): Flow<List<Conversation>> {
        return flowOf(
            conversations.filter { it.clientId == userId || it.providerId == userId }
        )
    }

    override suspend fun getMessages(conversationId: String): Result<List<Message>> {
        return Result.Success(messages.filter { it.conversationId == conversationId })
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> {
        return flowOf(messages.filter { it.conversationId == conversationId })
    }

    override suspend fun sendMessage(message: Message): Result<Message> {
        messages.add(message)
        // Update conversation
        val convIdx = conversations.indexOfFirst { it.id == message.conversationId }
        if (convIdx >= 0) {
            val conv = conversations[convIdx]
            conversations[convIdx] = conv.copy(
                lastMessage = message.body,
                lastMessageAt = message.createdAt,
                updatedAt = message.createdAt
            )
        }
        return Result.Success(message)
    }

    override suspend fun markConversationRead(
        conversationId: String,
        userId: String
    ): Result<Unit> {
        val convIdx = conversations.indexOfFirst { it.id == conversationId }
        if (convIdx >= 0) {
            conversations[convIdx] = conversations[convIdx].copy(unreadCount = 0)
        }
        return Result.Success(Unit)
    }

    override fun observeUnreadMessageCount(userId: String): Flow<Int> {
        return flowOf(conversations.filter {
            it.clientId == userId || it.providerId == userId
        }.sumOf { it.unreadCount })
    }
}

class FakeSubscriptionRepository : ISubscriptionRepository {
    private val store = mutableListOf<RecurringBooking>()

    override suspend fun createRecurringBooking(recurringBooking: RecurringBooking): Result<RecurringBooking> {
        store.add(recurringBooking)
        return Result.Success(recurringBooking)
    }

    override suspend fun getRecurringBooking(id: String): Result<RecurringBooking> {
        val rb = store.find { it.id == id }
            ?: return Result.Error(Exception("Not found"))
        return Result.Success(rb)
    }

    override suspend fun getRecurringBookingsForClient(clientId: String): Result<List<RecurringBooking>> {
        return Result.Success(store.filter { it.clientId == clientId })
    }

    override suspend fun updateRecurringBookingStatus(
        id: String,
        status: RecurringBookingStatus
    ): Result<RecurringBooking> {
        val idx = store.indexOfFirst { it.id == id }
        if (idx < 0) return Result.Error(Exception("Not found"))
        store[idx] = store[idx].copy(status = status)
        return Result.Success(store[idx])
    }

    override suspend fun updateSchedule(
        id: String,
        preferredDay: Int,
        preferredTime: String
    ): Result<RecurringBooking> {
        val idx = store.indexOfFirst { it.id == id }
        if (idx < 0) return Result.Error(Exception("Not found"))
        store[idx] = store[idx].copy(preferredDay = preferredDay, preferredTime = preferredTime)
        return Result.Success(store[idx])
    }

    override fun observeRecurringBookingsForClient(clientId: String): Flow<List<RecurringBooking>> {
        return flowOf(store.filter { it.clientId == clientId })
    }

    override suspend fun getActiveRecurringBookingsDue(todayIso: String): Result<List<RecurringBooking>> {
        return Result.Success(
            store.filter { it.status == RecurringBookingStatus.ACTIVE && it.nextBookingDate <= todayIso }
        )
    }

    override suspend fun advanceNextBookingDate(id: String, newDate: String): Result<Unit> {
        val idx = store.indexOfFirst { it.id == id }
        if (idx >= 0) store[idx] = store[idx].copy(nextBookingDate = newDate)
        return Result.Success(Unit)
    }
}
