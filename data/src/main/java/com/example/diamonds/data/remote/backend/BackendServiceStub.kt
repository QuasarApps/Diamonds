package com.example.diamonds.data.remote.backend

/**
 * Stub implementation of backend service for development/testing
 * Replace with real Firebase or REST implementation later
 */
class BackendServiceStub : IBackendService {
    override suspend fun login(email: String, password: String): Result<String> {
        // TODO: Implement with real backend
        return Result.Success("stub_token_${System.currentTimeMillis()}")
    }

    override suspend fun signup(
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        role: String
    ): Result<String> {
        // TODO: Implement with real backend
        return Result.Success("stub_token_${System.currentTimeMillis()}")
    }

    override suspend fun getClient(clientId: String): Result<ClientDto> {
        // TODO: Implement with real backend
        return Result.Success(ClientDto(
            id = clientId,
            name = "Stub Client",
            email = "stub@example.com",
            phoneNumber = "+1234567890",
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        ))
    }

    override suspend fun updateClient(client: ClientDto): Result<ClientDto> {
        // TODO: Implement with real backend
        return Result.Success(client)
    }

    override suspend fun getProvider(providerId: String): Result<ProviderDto> {
        // TODO: Implement with real backend
        return Result.Success(ProviderDto(
            id = providerId,
            name = "Stub Provider",
            email = "provider@example.com",
            phoneNumber = "+1234567890",
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        ))
    }

    override suspend fun searchProviders(
        latitude: Double,
        longitude: Double,
        radius: Int
    ): Result<List<ProviderDto>> {
        // TODO: Implement with real backend
        return Result.Success(emptyList())
    }

    override suspend fun getService(serviceId: String): Result<ServiceDto> {
        // TODO: Implement with real backend
        return Result.Success(ServiceDto(
            id = serviceId,
            providerId = "stub_provider",
            title = "Stub Service",
            description = "Stub Description",
            basePrice = 50.0,
            duration = 60,
            category = "APARTMENT_CLEANING",
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        ))
    }

    override suspend fun getServicesForProvider(providerId: String): Result<List<ServiceDto>> {
        // TODO: Implement with real backend
        return Result.Success(emptyList())
    }

    override suspend fun searchServicesByCategory(category: String): Result<List<ServiceDto>> {
        // TODO: Implement with real backend
        return Result.Success(emptyList())
    }

    override suspend fun createService(service: ServiceDto): Result<ServiceDto> {
        // TODO: Implement with real backend
        return Result.Success(service)
    }

    override suspend fun createBooking(booking: CreateBookingRequest): Result<BookingDto> {
        // TODO: Implement with real backend
        return Result.Success(BookingDto(
            id = "stub_booking_${System.currentTimeMillis()}",
            clientId = booking.clientId,
            providerId = booking.providerId,
            serviceId = booking.serviceId,
            status = "PENDING",
            scheduledDate = booking.scheduledDate,
            scheduledTime = booking.scheduledTime,
            estimatedDuration = 60,
            totalPrice = 50.0,
            notes = booking.notes,
            address = booking.address,
            latitude = booking.latitude,
            longitude = booking.longitude,
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        ))
    }

    override suspend fun getBooking(bookingId: String): Result<BookingDto> {
        // TODO: Implement with real backend
        return Result.Success(BookingDto(
            id = bookingId,
            clientId = "stub_client",
            providerId = "stub_provider",
            serviceId = "stub_service",
            status = "PENDING",
            scheduledDate = "2026-03-28",
            scheduledTime = "10:00",
            estimatedDuration = 60,
            totalPrice = 50.0,
            address = "123 Main St",
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        ))
    }

    override suspend fun getClientBookings(clientId: String): Result<List<BookingDto>> {
        // TODO: Implement with real backend
        return Result.Success(emptyList())
    }

    override suspend fun getProviderBookings(providerId: String): Result<List<BookingDto>> {
        // TODO: Implement with real backend
        return Result.Success(emptyList())
    }

    override suspend fun updateBookingStatus(bookingId: String, status: String): Result<BookingDto> {
        // TODO: Implement with real backend
        return Result.Success(BookingDto(
            id = bookingId,
            clientId = "stub_client",
            providerId = "stub_provider",
            serviceId = "stub_service",
            status = status,
            scheduledDate = "2026-03-28",
            scheduledTime = "10:00",
            estimatedDuration = 60,
            totalPrice = 50.0,
            address = "123 Main St",
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        ))
    }

    override suspend fun cancelBooking(bookingId: String): Result<BookingDto> {
        // TODO: Implement with real backend
        return updateBookingStatus(bookingId, "CANCELLED")
    }

    override suspend fun createReview(review: CreateReviewRequest): Result<ReviewDto> {
        // TODO: Implement with real backend
        return Result.Success(ReviewDto(
            id = "stub_review_${System.currentTimeMillis()}",
            bookingId = review.bookingId,
            clientId = review.clientId,
            providerId = review.providerId,
            rating = review.rating,
            comment = review.comment,
            imageUrls = review.imageUrls,
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        ))
    }

    override suspend fun getReviewsForProvider(providerId: String): Result<List<ReviewDto>> {
        // TODO: Implement with real backend
        return Result.Success(emptyList())
    }

    override suspend fun createPayment(payment: CreatePaymentRequest): Result<PaymentDto> {
        // TODO: Implement with real backend
        return Result.Success(PaymentDto(
            id = "stub_payment_${System.currentTimeMillis()}",
            bookingId = payment.bookingId,
            clientId = payment.clientId,
            providerId = payment.providerId,
            amount = payment.amount,
            status = "PENDING",
            method = payment.method,
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        ))
    }

    override suspend fun getPayment(paymentId: String): Result<PaymentDto> {
        // TODO: Implement with real backend
        return Result.Success(PaymentDto(
            id = paymentId,
            bookingId = "stub_booking",
            clientId = "stub_client",
            providerId = "stub_provider",
            amount = 50.0,
            status = "PENDING",
            method = "CARD",
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        ))
    }
}
