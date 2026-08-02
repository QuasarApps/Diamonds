package com.example.diamonds.data.remote.backend

import com.example.diamonds.domain.model.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BackendServiceStub].
 *
 * These are pure JVM tests — no Android context or Room needed.
 */
class BackendServiceStubTest {

    private lateinit var stub: BackendServiceStub

    @Before
    fun setUp() {
        stub = BackendServiceStub()
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    @Test
    fun `login always returns Success with token`() = runTest {
        val result = stub.login("any@test.com", "password")
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.startsWith("stub_token_"))
    }

    @Test
    fun `signup always returns Success with token`() = runTest {
        val result = stub.signup("Jane", "jane@test.com", "pass", "+1555", "CUSTOMER")
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.startsWith("stub_token_"))
    }

    // ── Providers ─────────────────────────────────────────────────────────────

    @Test
    fun `getProvider returns p1 Maria Garcia by id`() = runTest {
        val result = stub.getProvider("p1")
        assertTrue(result is Result.Success)
        assertEquals("Maria Garcia", (result as Result.Success).data.name)
        assertEquals("INDEPENDENT", result.data.cleanerType)
    }

    @Test
    fun `getProvider returns p2 James who is EMPLOYED by p5`() = runTest {
        val result = stub.getProvider("p2") as Result.Success
        assertEquals("James Okafor", result.data.name)
        assertEquals("EMPLOYED", result.data.cleanerType)
        assertEquals("p5", result.data.employerId)
        assertEquals("Sparkle Pro Cleaning Co.", result.data.employerName)
    }

    @Test
    fun `searchProviders returns all 5 seeded providers`() = runTest {
        val result = stub.searchProviders(0.0, 0.0, 50) as Result.Success
        assertEquals(5, result.data.size)
    }

    @Test
    fun `getProvider with unknown id returns fallback first provider`() = runTest {
        val result = stub.getProvider("unknown_id") as Result.Success
        // Falls back to first in seedProviders list
        assertNotNull(result.data.id)
    }

    // ── Services ──────────────────────────────────────────────────────────────

    @Test
    fun `getServicesForProvider p1 returns 3 services`() = runTest {
        val result = stub.getServicesForProvider("p1") as Result.Success
        assertEquals(3, result.data.size)
    }

    @Test
    fun `getServicesForProvider p5 returns 4 company services`() = runTest {
        val result = stub.getServicesForProvider("p5") as Result.Success
        assertEquals(4, result.data.size)
    }

    @Test
    fun `getServicesForProvider unknown provider returns empty list`() = runTest {
        val result = stub.getServicesForProvider("nonexistent") as Result.Success
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `getService returns correct service by id`() = runTest {
        val result = stub.getService("s1-1") as Result.Success
        assertEquals("Standard Apartment Clean", result.data.title)
        assertEquals(79.0, result.data.basePrice, 0.01)
        assertEquals("p1", result.data.providerId)
    }

    @Test
    fun `searchServicesByCategory CARPET_CLEANING returns Daniel Choi services`() = runTest {
        val result = stub.searchServicesByCategory("CARPET_CLEANING") as Result.Success
        assertTrue(result.data.all { it.category == "CARPET_CLEANING" })
        assertEquals(2, result.data.size)
    }

    @Test
    fun `createService returns the same service dto`() = runTest {
        val svc = ServiceDto("new-1","p1","New Service","Desc",50.0,60,"OTHER",isActive=true,createdAt="2026-04-07",updatedAt="2026-04-07")
        val result = stub.createService(svc) as Result.Success
        assertEquals("new-1", result.data.id)
        assertEquals("New Service", result.data.title)
    }

    @Test
    fun `createService generates an id when none is supplied`() = runTest {
        // FirebaseBackendService.createService assigns servicesCol.document().id for a blank id.
        // Handing back "" instead would produce a DTO that ServiceDto.toDomain() rejects outright
        // (PR #28), so the debug backend would fail a create the Firebase one completes.
        val created = (stub.createService(
            ServiceDto(providerId = "p1", title = "Generated", category = "OTHER")
        ) as Result.Success).data

        assertTrue(created.id.isNotBlank())
        assertEquals("Generated", (stub.getService(created.id) as Result.Success).data.title)
    }

    @Test
    fun `createService with an existing id overwrites rather than duplicating`() = runTest {
        val before = (stub.getServicesForProvider("p1") as Result.Success).data.size
        val existing = (stub.getService("s1-1") as Result.Success).data

        stub.createService(existing.copy(title = "Renamed"))

        assertEquals(before, (stub.getServicesForProvider("p1") as Result.Success).data.size)
        assertEquals("Renamed", (stub.getService("s1-1") as Result.Success).data.title)
    }

    @Test
    fun `updateService edits in place and is visible to later reads`() = runTest {
        val before = (stub.getServicesForProvider("p1") as Result.Success).data.size

        stub.updateService(
            (stub.getService("s1-1") as Result.Success).data.copy(basePrice = 99.0)
        )

        assertEquals(before, (stub.getServicesForProvider("p1") as Result.Success).data.size)
        assertEquals(99.0, (stub.getService("s1-1") as Result.Success).data.basePrice, 0.001)
    }

    // ── Bookings ──────────────────────────────────────────────────────────────

    @Test
    fun `getClientBookings for demo_customer returns at least 2 bookings`() = runTest {
        val result = stub.getClientBookings("demo_customer") as Result.Success
        assertTrue(result.data.size >= 2)
        assertTrue(result.data.all { it.clientId == "demo_customer" })
    }

    @Test
    fun `getProviderBookings for p1 returns bookings including completed ones`() = runTest {
        val result = stub.getProviderBookings("p1") as Result.Success
        assertTrue(result.data.isNotEmpty())
        assertTrue(result.data.any { it.status == "COMPLETED" })
        assertTrue(result.data.any { it.status == "PENDING" })
    }

    @Test
    fun `createBooking appends new booking and sets status PENDING`() = runTest {
        val req = CreateBookingRequest(
            clientId      = "test_client",
            providerId    = "p1",
            serviceId     = "s1-1",
            scheduledDate = "2026-05-01",
            scheduledTime = "10:00",
            address       = "123 Test St"
        )
        val result = stub.createBooking(req) as Result.Success
        assertEquals("PENDING", result.data.status)
        assertEquals("test_client", result.data.clientId)
        assertEquals(79.0, result.data.totalPrice, 0.01) // resolves price from s1-1

        // Verify it appears in subsequent query
        val bookings = stub.getClientBookings("test_client") as Result.Success
        assertTrue(bookings.data.any { it.id == result.data.id })
    }

    @Test
    fun `updateBookingStatus changes status and returns updated booking`() = runTest {
        val result = stub.updateBookingStatus("r1", "ACCEPTED") as Result.Success
        assertEquals("ACCEPTED", result.data.status)

        // Subsequent get reflects the change
        val fetched = stub.getBooking("r1") as Result.Success
        assertEquals("ACCEPTED", fetched.data.status)
    }

    @Test
    fun `cancelBooking sets status to CANCELLED`() = runTest {
        val result = stub.cancelBooking("r2") as Result.Success
        assertEquals("CANCELLED", result.data.status)
    }

    @Test
    fun `getBooking with unknown id returns Error`() = runTest {
        val result = stub.getBooking("not_a_real_id")
        assertTrue(result is Result.Error)
    }

    // ── Reviews ───────────────────────────────────────────────────────────────

    @Test
    fun `getReviewsForProvider p1 returns seeded reviews`() = runTest {
        val result = stub.getReviewsForProvider("p1") as Result.Success
        assertTrue(result.data.isNotEmpty())
        assertTrue(result.data.all { it.providerId == "p1" })
    }

    @Test
    fun `getReviewsForBooking returns correct review`() = runTest {
        val result = stub.getReviewsForBooking("e1") as Result.Success
        assertNotNull(result.data)
        assertEquals("p1", result.data!!.providerId)
        assertEquals(5, result.data!!.rating)
    }

    @Test
    fun `getReviewsForBooking unknown booking returns null`() = runTest {
        val result = stub.getReviewsForBooking("no_such_booking") as Result.Success
        assertNull(result.data)
    }

    @Test
    fun `createReview appends and is findable by bookingId`() = runTest {
        val req = CreateReviewRequest(
            bookingId  = "b_new",
            clientId   = "demo_customer",
            providerId = "p1",
            rating     = 4,
            comment    = "Great job!"
        )
        val created = stub.createReview(req) as Result.Success
        assertEquals(4, created.data.rating)

        val fetched = stub.getReviewsForBooking("b_new") as Result.Success
        assertNotNull(fetched.data)
        assertEquals(4, fetched.data!!.rating)
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    @Test
    fun `getPaymentsForClient demo_customer returns seeded payments`() = runTest {
        val result = stub.getPaymentsForClient("demo_customer") as Result.Success
        assertTrue(result.data.isNotEmpty())
        assertTrue(result.data.all { it.clientId == "demo_customer" })
    }

    @Test
    fun `createPayment returns SUCCEEDED status and stores payment`() = runTest {
        val req = CreatePaymentRequest(
            bookingId  = "b2",
            clientId   = "demo_customer",
            providerId = "p2",
            amount     = 119.0,
            method     = "CARD"
        )
        val result = stub.createPayment(req) as Result.Success
        assertEquals("SUCCEEDED", result.data.status)
        assertEquals(119.0, result.data.amount, 0.01)

        // Appears in client history
        val history = stub.getPaymentsForClient("demo_customer") as Result.Success
        assertTrue(history.data.any { it.id == result.data.id })
    }

    @Test
    fun `getPayment with valid id returns correct payment`() = runTest {
        val result = stub.getPayment("pay1") as Result.Success
        assertEquals("b1", result.data.bookingId)
        assertEquals(79.0, result.data.amount, 0.01)
    }

    @Test
    fun `getPayment with unknown id returns Error`() = runTest {
        val result = stub.getPayment("no_such_payment")
        assertTrue(result is Result.Error)
    }

    // ── Clients ───────────────────────────────────────────────────────────────

    @Test
    fun `getClient demo_customer returns correct client`() = runTest {
        val result = stub.getClient("demo_customer") as Result.Success
        assertEquals("Demo Customer", result.data.name)
        assertEquals("customer@demo.com", result.data.email)
    }

    @Test
    fun `getClient unknown id returns synthetic fallback`() = runTest {
        val result = stub.getClient("mystery_person") as Result.Success
        assertEquals("mystery_person", result.data.id)
    }

    @Test
    fun `updateClient returns the updated client`() = runTest {
        val updated = ClientDto(
            id = "demo_customer", name = "Updated Name", email = "u@test.com",
            phoneNumber = "+1000", createdAt = "2025-01-01", updatedAt = "2026-04-07"
        )
        val result = stub.updateClient(updated) as Result.Success
        assertEquals("Updated Name", result.data.name)
    }

    @Test
    fun `updateClient persists the edit instead of only echoing it back`() = runTest {
        stub.updateClient(
            ClientDto(
                id = "demo_customer", name = "Updated Name", email = "u@test.com",
                phoneNumber = "+1000", createdAt = "2025-01-01", updatedAt = "2026-04-07"
            )
        )

        val readBack = (stub.getClient("demo_customer") as Result.Success).data
        assertEquals("Updated Name", readBack.name)
        assertEquals("+1000", readBack.phoneNumber)
    }

    @Test
    fun `a client profile created at signup is readable afterwards`() = runTest {
        // Without a store, getClient would fall through to the synthetic placeholder and hand
        // back a fabricated name/email with an empty phone number — discarding what the user
        // typed at signup moments earlier.
        stub.updateClient(
            ClientDto(
                id = "uid_new", name = "Fresh Signup", email = "fresh@test.com",
                phoneNumber = "+1777", createdAt = "2026-04-07", updatedAt = "2026-04-07"
            )
        )

        val readBack = (stub.getClient("uid_new") as Result.Success).data
        assertEquals("Fresh Signup", readBack.name)
        assertEquals("fresh@test.com", readBack.email)
        assertEquals("+1777", readBack.phoneNumber)
    }

    // ── Provider upserts ──────────────────────────────────────────────────────

    @Test
    fun `updateProvider inserts a new provider that later reads can see`() = runTest {
        val created = ProviderDto(
            id = "new_cleaner", name = "Fresh Signup", email = "fresh@test.com",
            phoneNumber = "+1777", verificationStatus = "PENDING",
            createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )

        assertEquals(created, (stub.updateProvider(created) as Result.Success).data)
        // Without a mutable store this would fall through to the seed list's first entry.
        assertEquals("Fresh Signup", (stub.getProvider("new_cleaner") as Result.Success).data.name)
    }

    @Test
    fun `updateProvider overwrites an existing provider instead of duplicating it`() = runTest {
        val before = (stub.searchProviders(0.0, 0.0, 10) as Result.Success).data
        val existing = before.first()

        stub.updateProvider(existing.copy(name = "Renamed"))

        val after = (stub.searchProviders(0.0, 0.0, 10) as Result.Success).data
        assertEquals(before.size, after.size)
        assertEquals("Renamed", (stub.getProvider(existing.id) as Result.Success).data.name)
    }

    // ── Push notifications ────────────────────────────────────────────────────

    @Test
    fun `registerFcmToken records the token against the user`() = runTest {
        stub.registerFcmToken("uid_1", "tok_a")

        val row = stub.registeredFcmTokens()["tok_a"]
        assertNotNull(row)
        assertEquals("uid_1", row!!.userId)
        assertEquals("tok_a", row.token)
    }

    @Test
    fun `re-registering a token for a different user replaces the old mapping`() = runTest {
        // A device that switches accounts must not keep delivering the previous user's push.
        stub.registerFcmToken("uid_1", "tok_a")
        stub.registerFcmToken("uid_2", "tok_a")

        assertEquals(1, stub.registeredFcmTokens().size)
        assertEquals("uid_2", stub.registeredFcmTokens()["tok_a"]?.userId)
    }

    @Test
    fun `one user can register several devices`() = runTest {
        stub.registerFcmToken("uid_1", "tok_phone")
        stub.registerFcmToken("uid_1", "tok_tablet")

        assertEquals(2, stub.registeredFcmTokens().size)
    }

    @Test
    fun `unregisterFcmToken removes the delivery target`() = runTest {
        stub.registerFcmToken("uid_1", "tok_a")
        stub.unregisterFcmToken("tok_a")

        assertTrue(stub.registeredFcmTokens().isEmpty())
    }

    @Test
    fun `unregistering an unknown token is a no-op rather than an error`() = runTest {
        val result = stub.unregisterFcmToken("never_registered")
        assertTrue(result is Result.Success)
    }

    @Test
    fun `searchProviders returns a snapshot rather than a live view of the store`() = runTest {
        val before = (stub.searchProviders(0.0, 0.0, 10) as Result.Success).data
        val sizeBefore = before.size

        stub.updateProvider(
            ProviderDto(id = "brand_new", name = "Brand New", email = "bn@test.com", phoneNumber = "+1")
        )

        // The already-returned list must not grow under the caller's feet.
        assertEquals(sizeBefore, before.size)
        assertEquals(sizeBefore + 1, (stub.searchProviders(0.0, 0.0, 10) as Result.Success).data.size)
    }
}
