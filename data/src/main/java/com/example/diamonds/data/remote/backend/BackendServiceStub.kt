package com.example.diamonds.data.remote.backend

import com.example.diamonds.domain.model.Result
import kotlinx.coroutines.delay

/**
 * Stub backend with realistic seed data so every screen has something
 * meaningful to render during development.
 *
 * Simulates ~400 ms network latency on all calls.
 */
class BackendServiceStub : IBackendService {

    // ── Seed data ────────────────────────────────────────────────────────────

    private val seedProviders = listOf(
        // ── Independent cleaners ──────────────────────────────────────────
        ProviderDto(
            id = "p1", name = "Maria Garcia", email = "maria@clean.com",
            phoneNumber = "+1 555-0101", bio = "10 years experience, specialising in deep cleans and post-construction.",
            rating = 4.9f, reviewCount = 143, verificationStatus = "APPROVED",
            serviceRadius = 15, cleanerType = "INDEPENDENT",
            createdAt = "2023-01-10", updatedAt = "2026-01-01"
        ),
        ProviderDto(
            id = "p4", name = "Daniel Choi", email = "daniel@clean.com",
            phoneNumber = "+1 555-0404", bio = "Carpet and upholstery expert. Steam cleaning available.",
            rating = 4.6f, reviewCount = 57, verificationStatus = "APPROVED",
            serviceRadius = 12, cleanerType = "INDEPENDENT",
            createdAt = "2024-06-20", updatedAt = "2026-03-01"
        ),
        // ── Cleaning company ──────────────────────────────────────────────
        ProviderDto(
            id = "p5", name = "Sparkle Pro Cleaning Co.", email = "hello@sparklepro.com",
            phoneNumber = "+1 555-0500", bio = "Fully insured commercial & residential cleaning company. Vetted team of 12 cleaners.",
            rating = 4.8f, reviewCount = 304, verificationStatus = "APPROVED",
            serviceRadius = 25, cleanerType = "INDEPENDENT", // company itself has no employer
            createdAt = "2021-06-01", updatedAt = "2026-03-15"
        ),
        // ── Employed cleaners (work for Sparkle Pro) ──────────────────────
        ProviderDto(
            id = "p2", name = "James Okafor", email = "james@sparklepro.com",
            phoneNumber = "+1 555-0202", bio = "Fast, reliable, eco-friendly products only.",
            rating = 4.7f, reviewCount = 89, verificationStatus = "APPROVED",
            serviceRadius = 10, cleanerType = "EMPLOYED",
            employerId = "p5", employerName = "Sparkle Pro Cleaning Co.",
            createdAt = "2023-03-15", updatedAt = "2025-12-01"
        ),
        ProviderDto(
            id = "p3", name = "Sofia Petrov", email = "sofia@sparklepro.com",
            phoneNumber = "+1 555-0303", bio = "Office and commercial specialist. Flexible evening slots.",
            rating = 4.8f, reviewCount = 212, verificationStatus = "APPROVED",
            serviceRadius = 20, cleanerType = "EMPLOYED",
            employerId = "p5", employerName = "Sparkle Pro Cleaning Co.",
            createdAt = "2022-11-05", updatedAt = "2026-02-14"
        )
    )

    private val seedServices = mapOf(
        "p1" to listOf(
            ServiceDto("s1-1","p1","Standard Apartment Clean","Full clean of up to 2BR apartment",79.0,120,"APARTMENT_CLEANING",isActive=true, createdAt="2023-01-10",updatedAt="2026-01-01"),
            ServiceDto("s1-2","p1","Deep Clean","Thorough deep clean including inside appliances",149.0,240,"DEEP_CLEANING",isActive=true, createdAt="2023-01-10",updatedAt="2026-01-01"),
            ServiceDto("s1-3","p1","Post-Construction Tidy","Dust, debris and surface clean after building work",199.0,300,"POST_CONSTRUCTION",isActive=true, createdAt="2023-01-10",updatedAt="2026-01-01")
        ),
        "p2" to listOf(
            ServiceDto("s2-1","p2","House Clean (3BR)","Complete house clean using eco-friendly products",119.0,180,"HOUSE_CLEANING",isActive=true, createdAt="2023-03-15",updatedAt="2025-12-01"),
            ServiceDto("s2-2","p2","Express Tidy","Quick 1-hour surface clean and hoover",49.0,60,"APARTMENT_CLEANING",isActive=true, createdAt="2023-03-15",updatedAt="2025-12-01")
        ),
        "p3" to listOf(
            ServiceDto("s3-1","p3","Office Clean (small)","Up to 500 sq ft office, daily or weekly",89.0,90,"OFFICE_CLEANING",isActive=true, createdAt="2022-11-05",updatedAt="2026-02-14"),
            ServiceDto("s3-2","p3","Office Clean (large)","500-2000 sq ft, includes kitchen and bathrooms",189.0,210,"OFFICE_CLEANING",isActive=true, createdAt="2022-11-05",updatedAt="2026-02-14"),
            ServiceDto("s3-3","p3","Window Clean","Interior and exterior window cleaning",69.0,90,"WINDOW_CLEANING",isActive=true, createdAt="2022-11-05",updatedAt="2026-02-14")
        ),
        "p4" to listOf(
            ServiceDto("s4-1","p4","Carpet Steam Clean (1 room)","Professional steam clean, one room",65.0,60,"CARPET_CLEANING",isActive=true, createdAt="2024-06-20",updatedAt="2026-03-01"),
            ServiceDto("s4-2","p4","Carpet Steam Clean (whole home)","Full home carpet steam treatment",220.0,240,"CARPET_CLEANING",isActive=true, createdAt="2024-06-20",updatedAt="2026-03-01")
        ),
        // Sparkle Pro company-level services (booked directly; dispatched to employed cleaners)
        "p5" to listOf(
            ServiceDto("s5-1","p5","Residential Deep Clean","Professional whole-home deep clean by our vetted team",169.0,300,"DEEP_CLEANING",isActive=true, createdAt="2021-06-01",updatedAt="2026-03-15"),
            ServiceDto("s5-2","p5","Regular House Clean","Weekly or fortnightly maintenance clean",99.0,150,"HOUSE_CLEANING",isActive=true, createdAt="2021-06-01",updatedAt="2026-03-15"),
            ServiceDto("s5-3","p5","Commercial Office Clean","Daily office cleaning contract from 500 sq ft",149.0,180,"OFFICE_CLEANING",isActive=true, createdAt="2021-06-01",updatedAt="2026-03-15"),
            ServiceDto("s5-4","p5","Move-In / Move-Out Clean","Full property clean for tenancy handovers",219.0,360,"HOUSE_CLEANING",isActive=true, createdAt="2021-06-01",updatedAt="2026-03-15")
        )
    )

    // Mutable list so createBooking can append and getClientBookings can retrieve them
    private val bookingStore = mutableListOf(
        // ── Customer-side demo bookings ───────────────────────────────────
        BookingDto(id="b1", clientId="demo_customer", providerId="p1", serviceId="s1-1", status="COMPLETED",
            scheduledDate="2026-03-10", scheduledTime="09:00", estimatedDuration=120, totalPrice=79.0,
            notes=null, address="45 Oak Street", createdAt="2026-03-10", updatedAt="2026-03-10"),
        BookingDto(id="b2", clientId="demo_customer", providerId="p2", serviceId="s2-1", status="ACCEPTED",
            scheduledDate="2026-04-15", scheduledTime="14:00", estimatedDuration=180, totalPrice=119.0,
            notes="Move-in clean please", address="12 Birch Ave", createdAt="2026-04-01", updatedAt="2026-04-01"),

        // ── Cleaner (p1) demo bookings ─────────────────────────────────────
        // Pending requests waiting for acceptance
        BookingDto(id="r1", clientId="client_alice", providerId="p1", serviceId="s1-2", status="PENDING",
            scheduledDate="2026-04-12", scheduledTime="10:00", estimatedDuration=240, totalPrice=149.0,
            notes="Please bring eco-friendly products", address="88 Maple Drive",
            createdAt="2026-04-07", updatedAt="2026-04-07"),
        BookingDto(id="r2", clientId="client_bob", providerId="p1", serviceId="s1-1", status="PENDING",
            scheduledDate="2026-04-14", scheduledTime="13:00", estimatedDuration=120, totalPrice=79.0,
            notes=null, address="22 Elm Street",
            createdAt="2026-04-07", updatedAt="2026-04-07"),

        // Accepted – scheduled for today (2026-04-07) and upcoming
        BookingDto(id="j1", clientId="client_carol", providerId="p1", serviceId="s1-1", status="ACCEPTED",
            scheduledDate="2026-04-07", scheduledTime="09:00", estimatedDuration=120, totalPrice=79.0,
            notes=null, address="5 Pine Avenue",
            createdAt="2026-04-03", updatedAt="2026-04-05"),
        BookingDto(id="j2", clientId="client_dave", providerId="p1", serviceId="s1-3", status="IN_PROGRESS",
            scheduledDate="2026-04-07", scheduledTime="14:00", estimatedDuration=300, totalPrice=199.0,
            notes="Post-construction – lots of dust", address="101 Cedar Road",
            createdAt="2026-04-02", updatedAt="2026-04-07"),
        BookingDto(id="j3", clientId="client_eve", providerId="p1", serviceId="s1-2", status="ACCEPTED",
            scheduledDate="2026-04-09", scheduledTime="11:00", estimatedDuration=240, totalPrice=149.0,
            notes=null, address="34 Willow Lane",
            createdAt="2026-04-04", updatedAt="2026-04-04"),

        // Completed this week – contributes to earnings
        BookingDto(id="e1", clientId="client_frank", providerId="p1", serviceId="s1-1", status="COMPLETED",
            scheduledDate="2026-04-06", scheduledTime="09:00", estimatedDuration=120, totalPrice=79.0,
            notes=null, address="9 Birch Court",
            createdAt="2026-03-31", updatedAt="2026-04-06"),
        BookingDto(id="e2", clientId="client_grace", providerId="p1", serviceId="s1-2", status="COMPLETED",
            scheduledDate="2026-04-05", scheduledTime="10:00", estimatedDuration=240, totalPrice=149.0,
            notes=null, address="77 Oak Terrace",
            createdAt="2026-03-30", updatedAt="2026-04-05")
    )

    // ── Auth ─────────────────────────────────────────────────────────────────

    override suspend fun login(email: String, password: String): Result<String> {
        delay(400)
        return Result.Success("stub_token_${System.currentTimeMillis()}")
    }

    override suspend fun signup(name: String, email: String, password: String, phoneNumber: String, role: String): Result<String> {
        delay(400)
        return Result.Success("stub_token_${System.currentTimeMillis()}")
    }

    // ── Clients ───────────────────────────────────────────────────────────────

    private val seedClients = mapOf(
        "demo_customer" to ClientDto(id="demo_customer", name="Demo Customer",   email="customer@demo.com", phoneNumber="+1 555-9999", createdAt="2025-01-01", updatedAt="2026-01-01"),
        "client_alice"  to ClientDto(id="client_alice",  name="Alice Johnson",   email="alice@example.com", phoneNumber="+1 555-1001", createdAt="2025-02-01", updatedAt="2026-01-01"),
        "client_bob"    to ClientDto(id="client_bob",    name="Bob Williams",    email="bob@example.com",   phoneNumber="+1 555-1002", createdAt="2025-03-01", updatedAt="2026-01-01"),
        "client_carol"  to ClientDto(id="client_carol",  name="Carol Martinez",  email="carol@example.com", phoneNumber="+1 555-1003", createdAt="2025-04-01", updatedAt="2026-01-01"),
        "client_dave"   to ClientDto(id="client_dave",   name="Dave Thompson",   email="dave@example.com",  phoneNumber="+1 555-1004", createdAt="2025-05-01", updatedAt="2026-01-01"),
        "client_eve"    to ClientDto(id="client_eve",    name="Eve Robinson",    email="eve@example.com",   phoneNumber="+1 555-1005", createdAt="2025-06-01", updatedAt="2026-01-01"),
        "client_frank"  to ClientDto(id="client_frank",  name="Frank Lee",       email="frank@example.com", phoneNumber="+1 555-1006", createdAt="2025-07-01", updatedAt="2026-01-01"),
        "client_grace"  to ClientDto(id="client_grace",  name="Grace Kim",       email="grace@example.com", phoneNumber="+1 555-1007", createdAt="2025-08-01", updatedAt="2026-01-01")
    )

    override suspend fun getClient(clientId: String): Result<ClientDto> {
        delay(300)
        return Result.Success(seedClients[clientId]
            ?: ClientDto(id=clientId, name="Client $clientId", email="$clientId@example.com", phoneNumber="", createdAt="2025-01-01", updatedAt="2026-01-01"))
    }

    override suspend fun updateClient(client: ClientDto): Result<ClientDto> {
        delay(300)
        return Result.Success(client)
    }

    // ── Providers ─────────────────────────────────────────────────────────────

    override suspend fun getProvider(providerId: String): Result<ProviderDto> {
        delay(300)
        return Result.Success(seedProviders.find { it.id == providerId } ?: seedProviders.first())
    }

    override suspend fun searchProviders(latitude: Double, longitude: Double, radius: Int): Result<List<ProviderDto>> {
        delay(500)
        return Result.Success(seedProviders)
    }

    // ── Services ──────────────────────────────────────────────────────────────

    override suspend fun getService(serviceId: String): Result<ServiceDto> {
        delay(300)
        val service = seedServices.values.flatten().find { it.id == serviceId }
            ?: ServiceDto(serviceId,"p1","Unknown Service","",0.0,60,"OTHER",isActive=true,createdAt="",updatedAt="")
        return Result.Success(service)
    }

    override suspend fun getServicesForProvider(providerId: String): Result<List<ServiceDto>> {
        delay(400)
        return Result.Success(seedServices[providerId] ?: emptyList())
    }

    override suspend fun searchServicesByCategory(category: String): Result<List<ServiceDto>> {
        delay(400)
        return Result.Success(seedServices.values.flatten().filter { it.category == category })
    }

    override suspend fun createService(service: ServiceDto): Result<ServiceDto> {
        delay(300)
        return Result.Success(service)
    }

    // ── Bookings ──────────────────────────────────────────────────────────────

    override suspend fun createBooking(booking: CreateBookingRequest): Result<BookingDto> {
        delay(600)
        // Resolve price from the service
        val service = seedServices.values.flatten().find { it.id == booking.serviceId }
        val dto = BookingDto(
            id = "b${System.currentTimeMillis()}",
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
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        )
        bookingStore.add(dto)
        return Result.Success(dto)
    }

    override suspend fun getBooking(bookingId: String): Result<BookingDto> {
        delay(300)
        val booking = bookingStore.find { it.id == bookingId }
            ?: return Result.Error(Exception("Booking $bookingId not found"))
        return Result.Success(booking)
    }

    override suspend fun getClientBookings(clientId: String): Result<List<BookingDto>> {
        delay(400)
        return Result.Success(bookingStore.filter { it.clientId == clientId })
    }

    override suspend fun getProviderBookings(providerId: String): Result<List<BookingDto>> {
        delay(400)
        return Result.Success(bookingStore.filter { it.providerId == providerId })
    }

    override suspend fun updateBookingStatus(bookingId: String, status: String): Result<BookingDto> {
        delay(400)
        val idx = bookingStore.indexOfFirst { it.id == bookingId }
        if (idx < 0) return Result.Error(Exception("Booking not found"))
        val updated = bookingStore[idx].copy(status = status, updatedAt = System.currentTimeMillis().toString())
        bookingStore[idx] = updated
        return Result.Success(updated)
    }

    override suspend fun cancelBooking(bookingId: String): Result<BookingDto> =
        updateBookingStatus(bookingId, "CANCELLED")

    // ── Reviews ───────────────────────────────────────────────────────────────

    override suspend fun createReview(review: CreateReviewRequest): Result<ReviewDto> {
        delay(400)
        return Result.Success(ReviewDto(id="r${System.currentTimeMillis()}",bookingId=review.bookingId,clientId=review.clientId,providerId=review.providerId,rating=review.rating,comment=review.comment,imageUrls=review.imageUrls,createdAt=System.currentTimeMillis().toString(),updatedAt=System.currentTimeMillis().toString()))
    }

    override suspend fun getReviewsForProvider(providerId: String): Result<List<ReviewDto>> {
        delay(300)
        return Result.Success(emptyList())
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    override suspend fun createPayment(payment: CreatePaymentRequest): Result<PaymentDto> {
        delay(400)
        return Result.Success(PaymentDto(id="pay${System.currentTimeMillis()}",bookingId=payment.bookingId,clientId=payment.clientId,providerId=payment.providerId,amount=payment.amount,status="SUCCEEDED",method=payment.method,transactionId="txn_${System.currentTimeMillis()}",createdAt=System.currentTimeMillis().toString(),updatedAt=System.currentTimeMillis().toString()))
    }

    override suspend fun getPayment(paymentId: String): Result<PaymentDto> {
        delay(300)
        return Result.Success(PaymentDto(id=paymentId,bookingId="b1",clientId="demo_customer",providerId="p1",amount=79.0,status="SUCCEEDED",method="CARD",transactionId="txn_demo",createdAt="2026-03-10",updatedAt="2026-03-10"))
    }
}
