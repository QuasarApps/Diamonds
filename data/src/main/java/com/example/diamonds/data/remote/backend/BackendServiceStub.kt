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
            specializations = listOf("DEEP_CLEAN", "POST_CONSTRUCTION", "END_OF_TENANCY"),
            createdAt = "2023-01-10", updatedAt = "2026-01-01"
        ),
        ProviderDto(
            id = "p4", name = "Daniel Choi", email = "daniel@clean.com",
            phoneNumber = "+1 555-0404", bio = "Carpet and upholstery expert. Steam cleaning available.",
            rating = 4.6f, reviewCount = 57, verificationStatus = "APPROVED",
            serviceRadius = 12, cleanerType = "INDEPENDENT",
            specializations = listOf("CARPET_AND_UPHOLSTERY", "STANDARD"),
            createdAt = "2024-06-20", updatedAt = "2026-03-01"
        ),
        // ── Cleaning company ──────────────────────────────────────────────
        ProviderDto(
            id = "p5", name = "Sparkle Pro Cleaning Co.", email = "hello@sparklepro.com",
            phoneNumber = "+1 555-0500", bio = "Fully insured commercial & residential cleaning company. Vetted team of 12 cleaners.",
            rating = 4.8f, reviewCount = 304, verificationStatus = "APPROVED",
            serviceRadius = 25, cleanerType = "INDEPENDENT", // company itself has no employer
            specializations = listOf(
                "STANDARD",
                "DEEP_CLEAN",
                "OFFICE_COMMERCIAL",
                "MOVE_IN_MOVE_OUT"
            ),
            createdAt = "2021-06-01", updatedAt = "2026-03-15"
        ),
        // ── Employed cleaners (work for Sparkle Pro) ──────────────────────
        ProviderDto(
            id = "p2", name = "James Okafor", email = "james@sparklepro.com",
            phoneNumber = "+1 555-0202", bio = "Fast, reliable, eco-friendly products only.",
            rating = 4.7f, reviewCount = 89, verificationStatus = "APPROVED",
            serviceRadius = 10, cleanerType = "EMPLOYED",
            employerId = "p5", employerName = "Sparkle Pro Cleaning Co.",
            specializations = listOf("STANDARD", "DEEP_CLEAN"),
            createdAt = "2023-03-15", updatedAt = "2025-12-01"
        ),
        ProviderDto(
            id = "p3", name = "Sofia Petrov", email = "sofia@sparklepro.com",
            phoneNumber = "+1 555-0303", bio = "Office and commercial specialist. Flexible evening slots.",
            rating = 4.8f, reviewCount = 212, verificationStatus = "APPROVED",
            serviceRadius = 20, cleanerType = "EMPLOYED",
            employerId = "p5", employerName = "Sparkle Pro Cleaning Co.",
            specializations = listOf("OFFICE_COMMERCIAL", "WINDOW_CLEANING"),
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
            scheduledDate = "2026-04-25",
            scheduledTime = "14:00",
            estimatedDuration = 180,
            totalPrice = 119.0,
            notes = "Move-in clean please",
            address = "12 Birch Ave",
            createdAt = "2026-04-15",
            updatedAt = "2026-04-15"
        ),

        // ── Cleaner (p1) demo bookings ─────────────────────────────────────
        // Pending requests waiting for acceptance
        BookingDto(id="r1", clientId="client_alice", providerId="p1", serviceId="s1-2", status="PENDING",
            scheduledDate = "2026-04-22",
            scheduledTime = "10:00",
            estimatedDuration = 240,
            totalPrice = 149.0,
            notes="Please bring eco-friendly products", address="88 Maple Drive",
            createdAt = "2026-04-18",
            updatedAt = "2026-04-18"
        ),
        BookingDto(id="r2", clientId="client_bob", providerId="p1", serviceId="s1-1", status="PENDING",
            scheduledDate = "2026-04-24",
            scheduledTime = "13:00",
            estimatedDuration = 120,
            totalPrice = 79.0,
            notes=null, address="22 Elm Street",
            createdAt = "2026-04-18",
            updatedAt = "2026-04-18"
        ),

        // Accepted – scheduled for today (2026-04-07) and upcoming
        BookingDto(id="j1", clientId="client_carol", providerId="p1", serviceId="s1-1", status="ACCEPTED",
            scheduledDate = "2026-04-20",
            scheduledTime = "09:00",
            estimatedDuration = 120,
            totalPrice = 79.0,
            notes=null, address="5 Pine Avenue",
            createdAt = "2026-04-15",
            updatedAt = "2026-04-18"
        ),
        BookingDto(id="j2", clientId="client_dave", providerId="p1", serviceId="s1-3", status="IN_PROGRESS",
            scheduledDate = "2026-04-20",
            scheduledTime = "14:00",
            estimatedDuration = 300,
            totalPrice = 199.0,
            notes="Post-construction – lots of dust", address="101 Cedar Road",
            createdAt = "2026-04-14",
            updatedAt = "2026-04-20"
        ),
        BookingDto(id="j3", clientId="client_eve", providerId="p1", serviceId="s1-2", status="ACCEPTED",
            scheduledDate = "2026-04-23",
            scheduledTime = "11:00",
            estimatedDuration = 240,
            totalPrice = 149.0,
            notes=null, address="34 Willow Lane",
            createdAt = "2026-04-16",
            updatedAt = "2026-04-16"
        ),

        // Completed this week – contributes to earnings
        BookingDto(id="e1", clientId="client_frank", providerId="p1", serviceId="s1-1", status="COMPLETED",
            scheduledDate="2026-04-06", scheduledTime="09:00", estimatedDuration=120, totalPrice=79.0,
            notes=null, address="9 Birch Court",
            createdAt="2026-03-31", updatedAt="2026-04-06"),
        BookingDto(id="e2", clientId="client_grace", providerId="p1", serviceId="s1-2", status="COMPLETED",
            scheduledDate="2026-04-05", scheduledTime="10:00", estimatedDuration=240, totalPrice=149.0,
            notes=null, address="77 Oak Terrace",
            createdAt="2026-03-30", updatedAt="2026-04-05"),
        BookingDto(id="e3", clientId="client_alice", providerId="p1", serviceId="s1-1", status="COMPLETED",
            scheduledDate="2026-04-03", scheduledTime="09:00", estimatedDuration=120, totalPrice=79.0,
            notes=null, address="14 Maple Court",
            createdAt="2026-03-28", updatedAt="2026-04-03"),
        BookingDto(id="e4", clientId="client_bob", providerId="p1", serviceId="s1-3", status="COMPLETED",
            scheduledDate="2026-04-02", scheduledTime="11:00", estimatedDuration=300, totalPrice=199.0,
            notes=null, address="3 Cedar Place",
            createdAt="2026-03-27", updatedAt="2026-04-02"),
        BookingDto(id="e5", clientId="client_carol", providerId="p1", serviceId="s1-2", status="COMPLETED",
            scheduledDate="2026-04-01", scheduledTime="10:00", estimatedDuration=240, totalPrice=149.0,
            notes=null, address="56 Willow Way",
            createdAt="2026-03-26", updatedAt="2026-04-01"),
        // Previous month completions for p1
        BookingDto(id="e6", clientId="client_dave", providerId="p1", serviceId="s1-1", status="COMPLETED",
            scheduledDate="2026-03-28", scheduledTime="09:00", estimatedDuration=120, totalPrice=79.0,
            notes=null, address="88 Pine Road",
            createdAt="2026-03-22", updatedAt="2026-03-28"),
        BookingDto(id="e7", clientId="client_eve", providerId="p1", serviceId="s1-2", status="COMPLETED",
            scheduledDate="2026-03-21", scheduledTime="14:00", estimatedDuration=240, totalPrice=149.0,
            notes=null, address="19 Elm Close",
            createdAt="2026-03-15", updatedAt="2026-03-21"),

        // ── Employed cleaner (p2) demo bookings ────────────────────────────
        BookingDto(id="p2r1", clientId="client_alice", providerId="p2", serviceId="s2-1", status="PENDING",
            scheduledDate = "2026-04-25",
            scheduledTime = "10:00",
            estimatedDuration = 180,
            totalPrice = 119.0,
            notes=null, address="34 Ash Lane",
            createdAt = "2026-04-18",
            updatedAt = "2026-04-18"
        ),
        BookingDto(id="p2j1", clientId="client_bob", providerId="p2", serviceId="s2-2", status="ACCEPTED",
            scheduledDate = "2026-04-20",
            scheduledTime = "08:00",
            estimatedDuration = 60,
            totalPrice = 49.0,
            notes=null, address="7 Beech Street",
            createdAt = "2026-04-17",
            updatedAt = "2026-04-18"
        ),
        BookingDto(id="p2e1", clientId="client_carol", providerId="p2", serviceId="s2-1", status="COMPLETED",
            scheduledDate="2026-04-05", scheduledTime="09:00", estimatedDuration=180, totalPrice=119.0,
            notes=null, address="21 Sycamore Ave",
            createdAt="2026-04-01", updatedAt="2026-04-05"),
        BookingDto(id="p2e2", clientId="client_dave", providerId="p2", serviceId="s2-2", status="COMPLETED",
            scheduledDate="2026-04-03", scheduledTime="13:00", estimatedDuration=60, totalPrice=49.0,
            notes=null, address="5 Hazel Court",
            createdAt="2026-03-29", updatedAt="2026-04-03"),

        // ── Employed cleaner (p3) demo bookings ────────────────────────────
        BookingDto(id="p3r1", clientId="client_eve", providerId="p3", serviceId="s3-1", status="PENDING",
            scheduledDate = "2026-04-26",
            scheduledTime = "09:00",
            estimatedDuration = 90,
            totalPrice = 89.0,
            notes="Weekly clean needed", address="101 Commerce Blvd",
            createdAt = "2026-04-18",
            updatedAt = "2026-04-18"
        ),
        BookingDto(id="p3j1", clientId="client_frank", providerId="p3", serviceId="s3-2", status="ACCEPTED",
            scheduledDate = "2026-04-21",
            scheduledTime = "17:00",
            estimatedDuration = 210,
            totalPrice = 189.0,
            notes="Evening slot", address="200 Business Park",
            createdAt = "2026-04-16",
            updatedAt = "2026-04-18"
        ),
        BookingDto(id="p3e1", clientId="client_grace", providerId="p3", serviceId="s3-3", status="COMPLETED",
            scheduledDate="2026-04-04", scheduledTime="10:00", estimatedDuration=90, totalPrice=69.0,
            notes=null, address="Floor 3, Tower A",
            createdAt="2026-03-30", updatedAt="2026-04-04"),
        BookingDto(id="p3e2", clientId="client_alice", providerId="p3", serviceId="s3-1", status="COMPLETED",
            scheduledDate="2026-04-01", scheduledTime="09:00", estimatedDuration=90, totalPrice=89.0,
            notes=null, address="12 Market Street",
            createdAt="2026-03-26", updatedAt="2026-04-01")
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

    /** Mutable working set, so profiles created/edited in-session are visible to later reads. */
    private val clientStore = seedClients.toMutableMap()

    override suspend fun getClient(clientId: String): Result<ClientDto> {
        delay(300)
        return Result.Success(clientStore[clientId]
            ?: ClientDto(id=clientId, name="Client $clientId", email="$clientId@example.com", phoneNumber="", createdAt="2025-01-01", updatedAt="2026-01-01"))
    }

    override suspend fun updateClient(client: ClientDto): Result<ClientDto> {
        delay(300)
        clientStore[client.id] = client
        return Result.Success(client)
    }

    // ── Providers ─────────────────────────────────────────────────────────────

    /** Mutable working set, so profiles created/edited in-session are visible to later reads. */
    private val providerStore = seedProviders.toMutableList()

    override suspend fun getProvider(providerId: String): Result<ProviderDto> {
        delay(300)
        return Result.Success(providerStore.find { it.id == providerId } ?: providerStore.first())
    }

    override suspend fun updateProvider(provider: ProviderDto): Result<ProviderDto> {
        delay(300)
        val idx = providerStore.indexOfFirst { it.id == provider.id }
        if (idx >= 0) providerStore[idx] = provider else providerStore.add(provider)
        return Result.Success(provider)
    }

    override suspend fun searchProviders(latitude: Double, longitude: Double, radius: Int): Result<List<ProviderDto>> {
        delay(500)
        return Result.Success(providerStore.toList())
    }

    // ── Services ──────────────────────────────────────────────────────────────

    /** Mutable working set, so services created/edited in-session are visible to later reads. */
    private val serviceStore = seedServices.values.flatten().toMutableList()

    override suspend fun getService(serviceId: String): Result<ServiceDto> {
        delay(300)
        val service = serviceStore.find { it.id == serviceId }
            ?: ServiceDto(serviceId,"p1","Unknown Service","",0.0,60,"OTHER",isActive=true,createdAt="",updatedAt="")
        return Result.Success(service)
    }

    override suspend fun getServicesForProvider(providerId: String): Result<List<ServiceDto>> {
        delay(400)
        return Result.Success(serviceStore.filter { it.providerId == providerId })
    }

    override suspend fun searchServicesByCategory(category: String): Result<List<ServiceDto>> {
        delay(400)
        return Result.Success(serviceStore.filter { it.category == category })
    }

    override suspend fun createService(service: ServiceDto): Result<ServiceDto> {
        delay(300)
        serviceStore.add(service)
        return Result.Success(service)
    }

    override suspend fun updateService(service: ServiceDto): Result<ServiceDto> {
        delay(300)
        val idx = serviceStore.indexOfFirst { it.id == service.id }
        if (idx >= 0) serviceStore[idx] = service else serviceStore.add(service)
        return Result.Success(service)
    }

    // ── Bookings ──────────────────────────────────────────────────────────────

    override suspend fun createBooking(booking: CreateBookingRequest): Result<BookingDto> {
        delay(600)
        // Resolve price from the service
        val service = serviceStore.find { it.id == booking.serviceId }
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
        return if (idx >= 0) {
            val updated = bookingStore[idx].copy(
                status = status,
                updatedAt = System.currentTimeMillis().toString()
            )
            bookingStore[idx] = updated
            Result.Success(updated)
        } else {
            // Booking came from a previous session (persisted in Room but not in this
            // in-memory store). Synthesise a minimal DTO so the update succeeds.
            val synthetic = BookingDto(
                id = bookingId, clientId = "", providerId = "", serviceId = "",
                status = status, scheduledDate = "", scheduledTime = "",
                estimatedDuration = 0, totalPrice = 0.0, address = "",
                createdAt = "", updatedAt = System.currentTimeMillis().toString()
            )
            bookingStore.add(synthetic)
            Result.Success(synthetic)
        }
    }

    override suspend fun cancelBooking(bookingId: String): Result<BookingDto> =
        updateBookingStatus(bookingId, "CANCELLED")

    // ── Reviews ───────────────────────────────────────────────────────────────

    private val seedReviews = listOf(
        // ── p1 Maria Garcia ───────────────────────────────────────────────
        ReviewDto(id="rv1",  bookingId="e1", clientId="client_frank",  providerId="p1", rating=5, comment="Absolutely spotless — Maria is a true professional. Will book again!", createdAt="2026-04-06", updatedAt="2026-04-06"),
        ReviewDto(id="rv2",  bookingId="e2", clientId="client_grace",  providerId="p1", rating=5, comment="Deep clean was thorough and she brought her own eco products. Highly recommend.", createdAt="2026-04-05", updatedAt="2026-04-05"),
        ReviewDto(id="rv3",  bookingId="e3", clientId="client_alice",  providerId="p1", rating=5, comment="Arrived on time, very efficient. The apartment has never looked this good.", createdAt="2026-04-03", updatedAt="2026-04-03"),
        ReviewDto(id="rv4",  bookingId="e4", clientId="client_bob",    providerId="p1", rating=4, comment="Great post-construction clean. Missed a small patch behind the radiator but otherwise perfect.", createdAt="2026-04-02", updatedAt="2026-04-02"),
        ReviewDto(id="rv5",  bookingId="e5", clientId="client_carol",  providerId="p1", rating=5, comment="Incredibly detailed. Maria noticed things I would have missed completely. 5 stars.", createdAt="2026-04-01", updatedAt="2026-04-01"),
        ReviewDto(id="rv6",  bookingId="e6", clientId="client_dave",   providerId="p1", rating=5, comment="Always reliable. This is the third time I've booked Maria and she never disappoints.", createdAt="2026-03-28", updatedAt="2026-03-28"),
        ReviewDto(id="rv7",  bookingId="e7", clientId="client_eve",    providerId="p1", rating=4, comment="Very good service, kitchen was immaculate. Slight delay at the start but she made up the time.", createdAt="2026-03-21", updatedAt="2026-03-21"),
        // ── p2 James Okafor ───────────────────────────────────────────────
        ReviewDto(id="rv8",  bookingId="p2e1", clientId="client_carol", providerId="p2", rating=5, comment="James was brilliant — fast, friendly, and left the house smelling fresh. Eco products are a bonus.", createdAt="2026-04-05", updatedAt="2026-04-05"),
        ReviewDto(id="rv9",  bookingId="p2e2", clientId="client_dave",  providerId="p2", rating=4, comment="Quick express clean, did exactly what was asked. Good value for money.", createdAt="2026-04-03", updatedAt="2026-04-03"),
        // ── p3 Sofia Petrov ───────────────────────────────────────────────
        ReviewDto(id="rv10", bookingId="p3e1", clientId="client_grace",  providerId="p3", rating=5, comment="Office looks incredible. Sofia and her team were in and out in 90 minutes without any disruption.", createdAt="2026-04-04", updatedAt="2026-04-04"),
        ReviewDto(id="rv11", bookingId="p3e2", clientId="client_alice",  providerId="p3", rating=5, comment="Consistent high quality every week. Would not use anyone else for our office.", createdAt="2026-04-01", updatedAt="2026-04-01"),
        // ── p4 Daniel Choi ────────────────────────────────────────────────
        ReviewDto(id="rv12", bookingId="b1",   clientId="demo_customer", providerId="p4", rating=4, comment="Carpet looks brand new after the steam clean. Took a bit longer than quoted but worth it.", createdAt="2026-03-10", updatedAt="2026-03-10"),

        // ── Reverse reviews (PROVIDER_REVIEWS_CLIENT) ─────────────────────
        ReviewDto(
            id = "rrv1",
            bookingId = "e1",
            clientId = "client_frank",
            providerId = "p1",
            rating = 5,
            comment = "Frank was very welcoming and had everything prepared. Clear instructions.",
            direction = "PROVIDER_REVIEWS_CLIENT",
            locationTags = listOf("Clear instructions", "Easy parking"),
            createdAt = "2026-04-06",
            updatedAt = "2026-04-06"
        ),
        ReviewDto(
            id = "rrv2",
            bookingId = "e3",
            clientId = "client_alice",
            providerId = "p1",
            rating = 4,
            comment = "Nice apartment, but the entrance was a bit hard to find.",
            direction = "PROVIDER_REVIEWS_CLIENT",
            locationTags = listOf("Pet-friendly"),
            createdAt = "2026-04-03",
            updatedAt = "2026-04-03"
        ),
        ReviewDto(
            id = "rrv3",
            bookingId = "p2e1",
            clientId = "client_carol",
            providerId = "p2",
            rating = 5,
            comment = "Carol was extremely friendly and offered tea. Great client!",
            direction = "PROVIDER_REVIEWS_CLIENT",
            locationTags = listOf("Easy parking", "Clear instructions", "Pet-friendly"),
            createdAt = "2026-04-05",
            updatedAt = "2026-04-05"
        ),
        ReviewDto(
            id = "rrv4",
            bookingId = "b1",
            clientId = "demo_customer",
            providerId = "p4",
            rating = 4,
            comment = "Decent location but tight parking. Client was polite and easy to work with.",
            direction = "PROVIDER_REVIEWS_CLIENT",
            locationTags = listOf("Tight parking"),
            createdAt = "2026-03-10",
            updatedAt = "2026-03-10"
        ),
    )

    // Mutable so created reviews can be appended
    private val reviewStore = seedReviews.toMutableList()

    override suspend fun createReview(review: CreateReviewRequest): Result<ReviewDto> {
        delay(400)
        val dto = ReviewDto(
            id        = "rv${System.currentTimeMillis()}",
            bookingId = review.bookingId,
            clientId  = review.clientId,
            providerId = review.providerId,
            rating    = review.rating,
            comment   = review.comment,
            imageUrls = review.imageUrls,
            direction = review.direction,
            locationTags = review.locationTags,
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        )
        reviewStore.add(dto)
        return Result.Success(dto)
    }

    override suspend fun getReviewsForProvider(providerId: String): Result<List<ReviewDto>> {
        delay(300)
        return Result.Success(reviewStore.filter { it.providerId == providerId && it.direction == "CLIENT_REVIEWS_PROVIDER" })
    }

    override suspend fun getReviewsForBooking(bookingId: String): Result<ReviewDto?> {
        delay(200)
        return Result.Success(reviewStore.find { it.bookingId == bookingId && it.direction == "CLIENT_REVIEWS_PROVIDER" })
    }

    override suspend fun getReviewsForClient(clientId: String): Result<List<ReviewDto>> {
        delay(300)
        return Result.Success(reviewStore.filter { it.clientId == clientId && it.direction == "PROVIDER_REVIEWS_CLIENT" })
    }

    override suspend fun getReviewForBookingByDirection(
        bookingId: String,
        direction: String
    ): Result<ReviewDto?> {
        delay(200)
        return Result.Success(reviewStore.find { it.bookingId == bookingId && it.direction == direction })
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    private val paymentStore = mutableListOf(
        PaymentDto(id="pay1", bookingId="b1",   clientId="demo_customer", providerId="p1", amount=79.0,  status="SUCCEEDED", method="CARD", transactionId="txn_001", createdAt="2026-03-10", updatedAt="2026-03-10"),
        PaymentDto(id="pay2", bookingId="b2",   clientId="demo_customer", providerId="p2", amount=119.0, status="SUCCEEDED", method="CARD", transactionId="txn_002", createdAt="2026-04-01", updatedAt="2026-04-01"),
        PaymentDto(id="pay3", bookingId="e1",   clientId="client_frank",  providerId="p1", amount=79.0,  status="SUCCEEDED", method="CARD", transactionId="txn_003", createdAt="2026-04-06", updatedAt="2026-04-06"),
        PaymentDto(id="pay4", bookingId="e2",   clientId="client_grace",  providerId="p1", amount=149.0, status="SUCCEEDED", method="CARD", transactionId="txn_004", createdAt="2026-04-05", updatedAt="2026-04-05"),
        PaymentDto(id="pay5", bookingId="p2e1", clientId="client_carol",  providerId="p2", amount=119.0, status="SUCCEEDED", method="CARD", transactionId="txn_005", createdAt="2026-04-05", updatedAt="2026-04-05"),
        PaymentDto(id="pay6", bookingId="p3e1", clientId="client_grace",  providerId="p3", amount=69.0,  status="SUCCEEDED", method="CARD", transactionId="txn_006", createdAt="2026-04-04", updatedAt="2026-04-04"),
    )

    override suspend fun createPayment(payment: CreatePaymentRequest): Result<PaymentDto> {
        delay(800) // Simulate payment processing time
        // Simulate a 5% failure rate for declined cards
        val dto = PaymentDto(
            id            = "pay${System.currentTimeMillis()}",
            bookingId     = payment.bookingId,
            clientId      = payment.clientId,
            providerId    = payment.providerId,
            amount        = payment.amount,
            status        = "SUCCEEDED",
            method        = payment.method,
            transactionId = "txn_${System.currentTimeMillis()}",
            createdAt     = System.currentTimeMillis().toString(),
            updatedAt     = System.currentTimeMillis().toString()
        )
        paymentStore.add(dto)
        return Result.Success(dto)
    }

    override suspend fun getPayment(paymentId: String): Result<PaymentDto> {
        delay(300)
        val p = paymentStore.find { it.id == paymentId }
            ?: return Result.Error(Exception("Payment $paymentId not found"))
        return Result.Success(p)
    }

    override suspend fun getPaymentsForClient(clientId: String): Result<List<PaymentDto>> {
        delay(400)
        return Result.Success(paymentStore.filter { it.clientId == clientId }
            .sortedByDescending { it.createdAt })
    }

    // ── Location / Tracking ─────────────────────────────────────────────────

    /** Mutable store so location updates persist within the session. */
    private val providerLocationStore = mutableMapOf(
        "p1" to ProviderLocationDto(
            "p1",
            40.7128,
            -74.0060,
            45f,
            System.currentTimeMillis().toString()
        ),
        "p2" to ProviderLocationDto(
            "p2",
            40.7580,
            -73.9855,
            120f,
            System.currentTimeMillis().toString()
        ),
        "p3" to ProviderLocationDto(
            "p3",
            40.7489,
            -73.9680,
            200f,
            System.currentTimeMillis().toString()
        ),
        "p4" to ProviderLocationDto(
            "p4",
            40.7282,
            -73.7949,
            0f,
            System.currentTimeMillis().toString()
        ),
        "p5" to ProviderLocationDto(
            "p5",
            40.7614,
            -73.9776,
            90f,
            System.currentTimeMillis().toString()
        )
    )

    /** Seed service areas centred on each provider's "home base" location. */
    private val serviceAreaStore = mapOf(
        "p1" to ServiceAreaDto("p1", 40.7128, -74.0060, 15.0),
        "p2" to ServiceAreaDto("p2", 40.7580, -73.9855, 10.0),
        "p3" to ServiceAreaDto("p3", 40.7489, -73.9680, 20.0),
        "p4" to ServiceAreaDto("p4", 40.7282, -73.7949, 12.0),
        "p5" to ServiceAreaDto("p5", 40.7614, -73.9776, 25.0)
    )

    override suspend fun getProviderLocation(providerId: String): Result<ProviderLocationDto> {
        delay(300)
        val loc = providerLocationStore[providerId]
            ?: return Result.Error(Exception("Provider location for $providerId not found"))
        return Result.Success(loc)
    }

    override suspend fun updateProviderLocation(location: ProviderLocationDto): Result<Unit> {
        delay(200)
        providerLocationStore[location.providerId] = location
        return Result.Success(Unit)
    }

    override suspend fun getServiceArea(providerId: String): Result<ServiceAreaDto> {
        delay(300)
        val area = serviceAreaStore[providerId]
            ?: return Result.Error(Exception("Service area for $providerId not found"))
        return Result.Success(area)
    }

    // ── Chat / Messaging ─────────────────────────────────────────────────────

    private val conversationStore = mutableListOf(
        ConversationDto(
            id = "conv1",
            bookingId = "b1",
            clientId = "demo_customer",
            clientName = "Demo Customer",
            providerId = "p1",
            providerName = "Maria Garcia",
            lastMessage = "See you at 9am!",
            lastMessageAt = "2026-03-10T08:45:00",
            unreadCount = 0,
            updatedAt = "2026-03-10T08:45:00"
        ),
        ConversationDto(
            id = "conv2",
            bookingId = "b2",
            clientId = "demo_customer",
            clientName = "Demo Customer",
            providerId = "p2",
            providerName = "James Okafor",
            lastMessage = "I'll bring the eco-friendly products.",
            lastMessageAt = "2026-04-02T10:20:00",
            unreadCount = 1,
            updatedAt = "2026-04-02T10:20:00"
        ),
        ConversationDto(
            id = "conv3",
            bookingId = "j1",
            clientId = "client_carol",
            clientName = "Carol Martinez",
            providerId = "p1",
            providerName = "Maria Garcia",
            lastMessage = "Great, see you then!",
            lastMessageAt = "2026-04-06T18:00:00",
            unreadCount = 0,
            updatedAt = "2026-04-06T18:00:00"
        )
    )

    private val messageStore = mutableListOf(
        // conv1 – Demo Customer & Maria Garcia (booking b1)
        MessageDto(
            id = "msg1",
            conversationId = "conv1",
            senderId = "demo_customer",
            senderName = "Demo Customer",
            body = "Hi Maria, just confirming tomorrow's standard apartment clean at 9am.",
            isRead = true,
            createdAt = "2026-03-10T08:30:00"
        ),
        MessageDto(
            id = "msg2", conversationId = "conv1", senderId = "p1", senderName = "Maria Garcia",
            body = "Hi! Yes, confirmed. I'll bring all my own supplies. Any specific areas to focus on?",
            isRead = true, createdAt = "2026-03-10T08:35:00"
        ),
        MessageDto(
            id = "msg3",
            conversationId = "conv1",
            senderId = "demo_customer",
            senderName = "Demo Customer",
            body = "Please pay extra attention to the kitchen and bathroom tiles.",
            isRead = true,
            createdAt = "2026-03-10T08:40:00"
        ),
        MessageDto(
            id = "msg4", conversationId = "conv1", senderId = "p1", senderName = "Maria Garcia",
            body = "Of course! Not a problem. See you at 9am!",
            isRead = true, createdAt = "2026-03-10T08:45:00"
        ),

        // conv2 – Demo Customer & James Okafor (booking b2)
        MessageDto(
            id = "msg5",
            conversationId = "conv2",
            senderId = "demo_customer",
            senderName = "Demo Customer",
            body = "Hello James, we're moving in on the 15th. Can you do a thorough clean?",
            isRead = true,
            createdAt = "2026-04-02T10:00:00"
        ),
        MessageDto(
            id = "msg6", conversationId = "conv2", senderId = "p2", senderName = "James Okafor",
            body = "Absolutely! Move-in cleans are my specialty. I'll bring eco-friendly products.",
            isRead = true, createdAt = "2026-04-02T10:15:00"
        ),
        MessageDto(
            id = "msg7", conversationId = "conv2", senderId = "p2", senderName = "James Okafor",
            body = "I'll bring the eco-friendly products.",
            isRead = false, createdAt = "2026-04-02T10:20:00"
        ),

        // conv3 – Carol & Maria (booking j1)
        MessageDto(
            id = "msg8",
            conversationId = "conv3",
            senderId = "client_carol",
            senderName = "Carol Martinez",
            body = "Hi Maria, I've left a key under the mat.",
            isRead = true,
            createdAt = "2026-04-06T17:50:00"
        ),
        MessageDto(
            id = "msg9", conversationId = "conv3", senderId = "p1", senderName = "Maria Garcia",
            body = "Perfect, thank you Carol. I'll lock up when done.",
            isRead = true, createdAt = "2026-04-06T17:55:00"
        ),
        MessageDto(
            id = "msg10",
            conversationId = "conv3",
            senderId = "client_carol",
            senderName = "Carol Martinez",
            body = "Great, see you then!",
            isRead = true,
            createdAt = "2026-04-06T18:00:00"
        )
    )

    override suspend fun getOrCreateConversation(request: CreateConversationRequest): Result<ConversationDto> {
        delay(300)
        val existing = conversationStore.find { it.bookingId == request.bookingId }
        if (existing != null) return Result.Success(existing)
        val new = ConversationDto(
            id = "conv${System.currentTimeMillis()}",
            bookingId = request.bookingId,
            clientId = request.clientId,
            clientName = request.clientName,
            providerId = request.providerId,
            providerName = request.providerName,
            lastMessage = "",
            lastMessageAt = "",
            unreadCount = 0,
            updatedAt = System.currentTimeMillis().toString()
        )
        conversationStore.add(new)
        return Result.Success(new)
    }

    override suspend fun getConversationsForUser(userId: String): Result<List<ConversationDto>> {
        delay(400)
        return Result.Success(
            conversationStore
                .filter { it.clientId == userId || it.providerId == userId }
                .sortedByDescending { it.updatedAt }
        )
    }

    override suspend fun getMessages(conversationId: String): Result<List<MessageDto>> {
        delay(300)
        return Result.Success(messageStore.filter { it.conversationId == conversationId }
            .sortedBy { it.createdAt })
    }

    override suspend fun sendMessage(message: SendMessageRequest): Result<MessageDto> {
        delay(200)
        val dto = MessageDto(
            id = message.id,
            conversationId = message.conversationId,
            senderId = message.senderId,
            senderName = message.senderName,
            body = message.body,
            isRead = false,
            createdAt = message.createdAt
        )
        messageStore.add(dto)
        // Update conversation's lastMessage
        val convIdx = conversationStore.indexOfFirst { it.id == message.conversationId }
        if (convIdx >= 0) {
            val conv = conversationStore[convIdx]
            conversationStore[convIdx] = conv.copy(
                lastMessage = message.body,
                lastMessageAt = message.createdAt,
                updatedAt = message.createdAt
            )
        }
        return Result.Success(dto)
    }

    override suspend fun markConversationRead(
        conversationId: String,
        userId: String
    ): Result<Unit> {
        delay(100)
        // Mark all messages from others as read
        val idxList = messageStore.indices.filter {
            messageStore[it].conversationId == conversationId && messageStore[it].senderId != userId
        }
        idxList.forEach { i -> messageStore[i] = messageStore[i].copy(isRead = true) }
        // Reset unread count on conversation
        val convIdx = conversationStore.indexOfFirst { it.id == conversationId }
        if (convIdx >= 0) {
            conversationStore[convIdx] = conversationStore[convIdx].copy(unreadCount = 0)
        }
        return Result.Success(Unit)
    }

    // ── Recurring Bookings ────────────────────────────────────────────────

    private val recurringBookingStore = mutableListOf(
        RecurringBookingDto(
            id = "rb1", clientId = "c1", providerId = "p1", providerName = "Maria Garcia",
            serviceId = "s1", serviceName = "Apartment Deep Clean",
            frequency = "WEEKLY", preferredDay = 3, preferredTime = "09:00",
            address = "123 Main St, Apt 4B", latitude = 40.7128, longitude = -74.0060,
            totalPrice = 120.0, status = "ACTIVE", nextBookingDate = "2026-04-23",
            createdAt = "2026-03-01", updatedAt = "2026-04-10"
        ),
        RecurringBookingDto(
            id = "rb2", clientId = "c1", providerId = "p4", providerName = "Daniel Choi",
            serviceId = "s5", serviceName = "Carpet Steam Clean",
            frequency = "MONTHLY", preferredDay = 15, preferredTime = "14:00",
            address = "456 Oak Ave", latitude = 40.7200, longitude = -74.0100,
            totalPrice = 180.0, status = "ACTIVE", nextBookingDate = "2026-05-15",
            createdAt = "2026-02-15", updatedAt = "2026-04-01"
        ),
        RecurringBookingDto(
            id = "rb3", clientId = "c1", providerId = "p2", providerName = "James Wilson",
            serviceId = "s3", serviceName = "Office Cleaning",
            frequency = "FORTNIGHTLY", preferredDay = 1, preferredTime = "08:00",
            address = "789 Business Park", latitude = 40.7300, longitude = -73.9900,
            totalPrice = 200.0, status = "PAUSED", nextBookingDate = "2026-04-28",
            createdAt = "2026-01-10", updatedAt = "2026-03-20"
        )
    )

    override suspend fun createRecurringBooking(request: CreateRecurringBookingRequest): Result<RecurringBookingDto> {
        delay(400)
        val now =
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val dto = RecurringBookingDto(
            id = "rb${recurringBookingStore.size + 1}_${System.currentTimeMillis()}",
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
            nextBookingDate = now, // first occurrence ASAP
            createdAt = now,
            updatedAt = now
        )
        recurringBookingStore.add(dto)
        return Result.Success(dto)
    }

    override suspend fun getRecurringBooking(id: String): Result<RecurringBookingDto> {
        delay(200)
        val rb = recurringBookingStore.find { it.id == id }
            ?: return Result.Error(Exception("Recurring booking not found"))
        return Result.Success(rb)
    }

    override suspend fun getRecurringBookingsForClient(clientId: String): Result<List<RecurringBookingDto>> {
        delay(300)
        return Result.Success(recurringBookingStore.filter { it.clientId == clientId })
    }

    override suspend fun updateRecurringBookingStatus(
        id: String,
        status: String
    ): Result<RecurringBookingDto> {
        delay(300)
        val idx = recurringBookingStore.indexOfFirst { it.id == id }
        if (idx < 0) return Result.Error(Exception("Recurring booking not found"))
        val now =
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        recurringBookingStore[idx] =
            recurringBookingStore[idx].copy(status = status, updatedAt = now)
        return Result.Success(recurringBookingStore[idx])
    }

    override suspend fun updateRecurringBookingSchedule(
        id: String,
        preferredDay: Int,
        preferredTime: String
    ): Result<RecurringBookingDto> {
        delay(300)
        val idx = recurringBookingStore.indexOfFirst { it.id == id }
        if (idx < 0) return Result.Error(Exception("Recurring booking not found"))
        val now =
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        recurringBookingStore[idx] = recurringBookingStore[idx].copy(
            preferredDay = preferredDay, preferredTime = preferredTime, updatedAt = now
        )
        return Result.Success(recurringBookingStore[idx])
    }

    override suspend fun advanceRecurringBookingDate(id: String, newDate: String): Result<Unit> {
        delay(100)
        val idx = recurringBookingStore.indexOfFirst { it.id == id }
        if (idx < 0) return Result.Error(Exception("Recurring booking not found"))
        recurringBookingStore[idx] = recurringBookingStore[idx].copy(nextBookingDate = newDate)
        return Result.Success(Unit)
    }

    // ── Support & Claims ───────────────────────────────────────────────────

    private val supportTicketStore = mutableListOf(
        SupportTicketDto(
            id = "ticket-1", userId = "c1", userRole = "CUSTOMER", bookingId = "b1",
            type = "POST_SERVICE", status = "RESOLVED", subject = "Missed bathroom",
            description = "The cleaner forgot to clean the guest bathroom.",
            createdAt = "2026-03-10", updatedAt = "2026-03-12"
        ),
        SupportTicketDto(
            id = "ticket-2", userId = "c1", userRole = "CUSTOMER",
            type = "ORDER_ISSUE", status = "OPEN", subject = "Cannot apply discount code",
            description = "I have a 20% off code but the app won't accept it.",
            createdAt = "2026-04-15", updatedAt = "2026-04-15"
        )
    )

    private val claimStore = mutableListOf(
        ClaimDto(
            id = "claim-1", bookingId = "b3", filedByUserId = "c1", filedByRole = "CUSTOMER",
            claimType = "UNSATISFACTORY_SERVICE", status = "APPROVED",
            description = "Floor was still dirty after the service.",
            resolutionNotes = "Full refund issued.", refundAmount = 120.0,
            createdAt = "2026-02-20", updatedAt = "2026-02-25"
        ),
        ClaimDto(
            id = "claim-2", bookingId = "b5", filedByUserId = "p1", filedByRole = "CLEANER",
            claimType = "DANGEROUS_PROPERTY", status = "UNDER_REVIEW",
            description = "Exposed wiring in the kitchen area. Unsafe to work.",
            createdAt = "2026-04-01", updatedAt = "2026-04-02"
        )
    )

    private val helpArticles = listOf(
        HelpArticleDto(
            "ha-1",
            "How to book a cleaning",
            "Browse providers, pick a service, choose a date and time, then confirm your booking.",
            "booking",
            listOf("booking", "getting-started")
        ),
        HelpArticleDto(
            "ha-2",
            "How to cancel a booking",
            "Go to your booking detail screen, tap 'Cancel Booking', select a reason, and confirm.",
            "booking",
            listOf("booking", "cancel")
        ),
        HelpArticleDto(
            "ha-3",
            "Payment methods",
            "We accept all major credit and debit cards. You can pay at checkout or skip payment in demo mode.",
            "payment",
            listOf("payment", "cards")
        ),
        HelpArticleDto(
            "ha-4",
            "How to file a claim",
            "After a completed booking, tap 'File a Claim' on the booking detail screen. Describe the issue and attach photos if available.",
            "claims",
            listOf("claims", "refund")
        ),
        HelpArticleDto(
            "ha-5",
            "Safety during a service",
            "If you feel unsafe at any point, use the Emergency button to contact local services and create an urgent support ticket.",
            "safety",
            listOf("safety", "emergency")
        )
    )

    private fun now(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())

    override suspend fun createSupportTicket(request: CreateSupportTicketRequest): Result<SupportTicketDto> {
        delay(400)
        val ticket = SupportTicketDto(
            id = "ticket-${System.currentTimeMillis()}", userId = request.userId,
            userRole = request.userRole, bookingId = request.bookingId,
            type = request.type, status = "OPEN", subject = request.subject,
            description = request.description, createdAt = now(), updatedAt = now()
        )
        supportTicketStore.add(ticket)
        return Result.Success(ticket)
    }

    override suspend fun getSupportTicket(ticketId: String): Result<SupportTicketDto> {
        delay(200)
        val t = supportTicketStore.find { it.id == ticketId }
            ?: return Result.Error(Exception("Ticket not found"))
        return Result.Success(t)
    }

    override suspend fun getTicketsForUser(userId: String): Result<List<SupportTicketDto>> {
        delay(300)
        return Result.Success(supportTicketStore.filter { it.userId == userId })
    }

    override suspend fun fileClaim(request: FileClaimRequest): Result<ClaimDto> {
        delay(400)
        val claim = ClaimDto(
            id = "claim-${System.currentTimeMillis()}", bookingId = request.bookingId,
            filedByUserId = request.filedByUserId, filedByRole = request.filedByRole,
            claimType = request.claimType, status = "SUBMITTED",
            description = request.description, evidenceImageUrls = request.evidenceImageUrls,
            createdAt = now(), updatedAt = now()
        )
        claimStore.add(claim)
        return Result.Success(claim)
    }

    override suspend fun getClaim(claimId: String): Result<ClaimDto> {
        delay(200)
        val c = claimStore.find { it.id == claimId }
            ?: return Result.Error(Exception("Claim not found"))
        return Result.Success(c)
    }

    override suspend fun getClaimsForUser(userId: String): Result<List<ClaimDto>> {
        delay(300)
        return Result.Success(claimStore.filter { it.filedByUserId == userId })
    }

    override suspend fun getClaimForBooking(bookingId: String): Result<ClaimDto?> {
        delay(200)
        return Result.Success(claimStore.find { it.bookingId == bookingId })
    }

    override suspend fun cancelBookingWithReason(request: CancelBookingWithReasonRequest): Result<BookingDto> {
        delay(400)
        val idx = bookingStore.indexOfFirst { it.id == request.bookingId }
        if (idx < 0) return Result.Error(Exception("Booking not found"))
        val b = bookingStore[idx]
        if (b.status != "PENDING" && b.status != "ACCEPTED") {
            return Result.Error(Exception("Only PENDING or ACCEPTED bookings can be cancelled"))
        }
        bookingStore[idx] = b.copy(status = "CANCELLED", updatedAt = now())
        return Result.Success(bookingStore[idx])
    }

    override suspend fun editBooking(request: EditBookingRequest): Result<BookingDto> {
        delay(400)
        val idx = bookingStore.indexOfFirst { it.id == request.bookingId }
        if (idx < 0) return Result.Error(Exception("Booking not found"))
        val b = bookingStore[idx]
        if (b.status != "PENDING") {
            return Result.Error(Exception("Only PENDING bookings can be edited"))
        }
        bookingStore[idx] = b.copy(
            address = request.newAddress ?: b.address,
            serviceId = request.newServiceId ?: b.serviceId,
            updatedAt = now()
        )
        return Result.Success(bookingStore[idx])
    }

    override suspend fun requestRefund(bookingId: String): Result<PaymentDto> {
        delay(400)
        val idx = paymentStore.indexOfFirst { it.bookingId == bookingId }
        if (idx < 0) return Result.Error(Exception("Payment not found for booking"))
        paymentStore[idx] = paymentStore[idx].copy(status = "REFUNDED", updatedAt = now())
        return Result.Success(paymentStore[idx])
    }

    override suspend fun getHelpArticles(): Result<List<HelpArticleDto>> {
        delay(200)
        return Result.Success(helpArticles)
    }

    // ── Saved Locations ───────────────────────────────────────────────────

    private val savedLocationStore = mutableListOf(
        SavedLocationDto(
            id = "loc-1", clientId = "demo_customer", label = "Home",
            address = "45 Oak Street, Maplewood", latitude = 37.7749, longitude = -122.4194,
            locationType = "HOUSE", roomCount = 3, bathroomCount = 2, sqFootage = 1400,
            createdAt = "2025-01-01", updatedAt = "2025-01-01"
        ),
        SavedLocationDto(
            id = "loc-2", clientId = "demo_customer", label = "Office",
            address = "200 Market St, San Francisco", latitude = 37.7935, longitude = -122.3964,
            locationType = "OFFICE", roomCount = 5, bathroomCount = 2, sqFootage = 2000,
            createdAt = "2025-03-15", updatedAt = "2025-03-15"
        )
    )

    override suspend fun getSavedLocations(clientId: String): Result<List<SavedLocationDto>> {
        delay(300)
        return Result.Success(savedLocationStore.filter { it.clientId == clientId })
    }

    override suspend fun upsertSavedLocation(location: SavedLocationDto): Result<SavedLocationDto> {
        delay(300)
        val idx = savedLocationStore.indexOfFirst { it.id == location.id }
        if (idx >= 0) savedLocationStore[idx] = location else savedLocationStore.add(location)
        return Result.Success(location)
    }

    override suspend fun deleteSavedLocation(locationId: String): Result<Unit> {
        delay(200)
        savedLocationStore.removeAll { it.id == locationId }
        return Result.Success(Unit)
    }

    // ── Push notifications ────────────────────────────────────────────────────

    /** Keyed by token, mirroring the Firestore document layout. */
    private val fcmTokenStore = mutableMapOf<String, FcmTokenDto>()

    /**
     * Test seam. [IBackendService] has no read side for tokens — a real sender queries Firestore
     * directly — so there is no way to assert registration through the interface alone.
     */
    fun registeredFcmTokens(): Map<String, FcmTokenDto> = fcmTokenStore.toMap()

    override suspend fun registerFcmToken(userId: String, token: String): Result<Unit> {
        delay(200)
        fcmTokenStore[token] = FcmTokenDto(
            token = token,
            userId = userId,
            updatedAt = System.currentTimeMillis().toString()
        )
        return Result.Success(Unit)
    }

    override suspend fun unregisterFcmToken(token: String): Result<Unit> {
        delay(200)
        fcmTokenStore.remove(token)
        return Result.Success(Unit)
    }
}
