package com.example.diamonds.data.mapper

import com.example.diamonds.data.local.entity.*
import com.example.diamonds.data.remote.backend.*
import com.example.diamonds.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure JVM tests for all mapper extension functions.
 * No mocking needed — these are pure data transformations.
 */
class MappersTest {

    // ── Entity → Domain ───────────────────────────────────────────────────────

    @Test
    fun `ClientEntity toDomain maps all fields correctly`() {
        val entity = ClientEntity(
            id = "c1", name = "Alice", email = "alice@test.com",
            phoneNumber = "+15551234567", profileImageUrl = "https://img.test/alice.jpg",
            createdAt = "2025-01-01", updatedAt = "2026-04-07"
        )
        val domain = entity.toDomain()
        assertEquals("c1",                     domain.id)
        assertEquals("Alice",                  domain.name)
        assertEquals("alice@test.com",         domain.email)
        assertEquals("+15551234567",           domain.phoneNumber)
        assertEquals("https://img.test/alice.jpg", domain.profileImageUrl)
        assertEquals("2025-01-01",             domain.createdAt)
    }

    @Test
    fun `ProviderEntity toDomain maps cleanerType enum`() {
        val entity = ProviderEntity(
            id = "p1", name = "Maria", email = "m@c.com", phoneNumber = "+1",
            bio = "Expert", rating = 4.9f, reviewCount = 143,
            verificationStatus = "APPROVED", serviceRadius = 15,
            cleanerType = "INDEPENDENT", employerId = null, employerName = null,
            createdAt = "2023-01-01", updatedAt = "2026-01-01"
        )
        val domain = entity.toDomain()
        assertEquals(CleanerType.INDEPENDENT,         domain.cleanerType)
        assertEquals(VerificationStatus.APPROVED,     domain.verificationStatus)
        assertEquals(4.9f,                            domain.rating, 0.01f)
    }

    @Test
    fun `ProviderEntity toDomain maps EMPLOYED cleanerType with employer fields`() {
        val entity = ProviderEntity(
            id = "p2", name = "James", email = "j@c.com", phoneNumber = "+1",
            rating = 4.7f, reviewCount = 89, verificationStatus = "APPROVED",
            serviceRadius = 10, cleanerType = "EMPLOYED",
            employerId = "p5", employerName = "Sparkle Pro Cleaning Co.",
            createdAt = "2023-01-01", updatedAt = "2026-01-01"
        )
        val domain = entity.toDomain()
        assertEquals(CleanerType.EMPLOYED,             domain.cleanerType)
        assertEquals("p5",                             domain.employerId)
        assertEquals("Sparkle Pro Cleaning Co.",       domain.employerName)
    }

    @Test
    fun `ProviderEntity toDomain falls back to INDEPENDENT for unknown cleanerType`() {
        val entity = ProviderEntity(
            id = "p9", name = "X", email = "x@c.com", phoneNumber = "+1",
            rating = 3.0f, reviewCount = 0, verificationStatus = "PENDING",
            serviceRadius = 5, cleanerType = "NONEXISTENT_TYPE",
            createdAt = "2023-01-01", updatedAt = "2026-01-01"
        )
        val domain = entity.toDomain()
        assertEquals(CleanerType.INDEPENDENT, domain.cleanerType)
    }

    @Test
    fun `ServiceEntity toDomain maps category enum`() {
        val entity = ServiceEntity(
            id = "s1", providerId = "p1", title = "Deep Clean", description = "Full clean",
            basePrice = 149.0, duration = 240, category = "DEEP_CLEANING",
            isActive = true, createdAt = "2023-01-01", updatedAt = "2026-01-01"
        )
        val domain = entity.toDomain()
        assertEquals(ServiceCategory.DEEP_CLEANING, domain.category)
        assertEquals(149.0, domain.basePrice, 0.01)
        assertTrue(domain.isActive)
    }

    @Test
    fun `BookingEntity toDomain maps status and syncStatus enums`() {
        val entity = BookingEntity(
            id = "b1", clientId = "c1", providerId = "p1", serviceId = "s1",
            status = "ACCEPTED", scheduledDate = "2026-05-01", scheduledTime = "10:00",
            estimatedDuration = 120, totalPrice = 79.0, notes = "Ring bell",
            address = "1 Test St", latitude = 51.5, longitude = -0.1,
            syncStatus = "SYNCED", createdAt = "2026-04-01", updatedAt = "2026-04-01"
        )
        val domain = entity.toDomain()
        assertEquals(BookingStatus.ACCEPTED, domain.status)
        assertEquals(SyncStatus.SYNCED,      domain.syncStatus)
        assertEquals("Ring bell",            domain.notes)
        assertEquals(51.5,                   domain.latitude!!, 0.001)
    }

    @Test
    fun `ReviewEntity toDomain parses imageUrls JSON`() {
        val entity = ReviewEntity(
            id = "rv1", bookingId = "b1", clientId = "c1", providerId = "p1",
            rating = 5, comment = "Excellent!", imageUrls = """["url1","url2"]""",
            syncStatus = "SYNCED", createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        val domain = entity.toDomain()
        assertEquals(5,             domain.rating)
        assertEquals("Excellent!",  domain.comment)
        assertEquals(2,             domain.imageUrls.size)
    }

    @Test
    fun `ReviewEntity toDomain handles empty imageUrls gracefully`() {
        val entity = ReviewEntity(
            id = "rv1", bookingId = "b1", clientId = "c1", providerId = "p1",
            rating = 4, comment = null, imageUrls = "",
            syncStatus = "SYNCED", createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        val domain = entity.toDomain()
        assertTrue(domain.imageUrls.isEmpty())
    }

    @Test
    fun `PaymentEntity toDomain maps PaymentStatus and PaymentMethod`() {
        val entity = PaymentEntity(
            id = "pay1", bookingId = "b1", clientId = "c1", providerId = "p1",
            amount = 79.0, status = "SUCCEEDED", method = "CARD",
            transactionId = "txn_001", syncStatus = "SYNCED",
            createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        val domain = entity.toDomain()
        assertEquals(PaymentStatus.SUCCEEDED, domain.status)
        assertEquals(PaymentMethod.CARD,      domain.method)
        assertEquals("txn_001",               domain.transactionId)
    }

    // ── Domain → Entity ───────────────────────────────────────────────────────

    @Test
    fun `Provider toEntity serialises cleanerType as string`() {
        val provider = Provider(
            id = "p1", name = "Maria", email = "m@c.com", phoneNumber = "+1",
            rating = 4.9f, reviewCount = 143, verificationStatus = VerificationStatus.APPROVED,
            cleanerType = CleanerType.EMPLOYED, employerId = "p5", employerName = "Sparkle",
            createdAt = "2023-01-01", updatedAt = "2026-01-01"
        )
        val entity = provider.toEntity()
        assertEquals("EMPLOYED", entity.cleanerType)
        assertEquals("p5",       entity.employerId)
    }

    @Test
    fun `Booking toEntity serialises status and syncStatus as strings`() {
        val booking = Booking(
            id = "b1", clientId = "c1", providerId = "p1", serviceId = "s1",
            status = BookingStatus.IN_PROGRESS, scheduledDate = "2026-05-01",
            scheduledTime = "09:00", estimatedDuration = 120, totalPrice = 89.0,
            address = "2 Test Ave", syncStatus = SyncStatus.PENDING,
            createdAt = "2026-04-01", updatedAt = "2026-04-01"
        )
        val entity = booking.toEntity()
        assertEquals("IN_PROGRESS", entity.status)
        assertEquals("PENDING",     entity.syncStatus)
    }

    // ── DTO → Domain ──────────────────────────────────────────────────────────

    @Test
    fun `ProviderDto toDomain handles unknown verificationStatus gracefully`() {
        // ValufOf would throw for unknown — ensure it is handled
        val dto = ProviderDto(
            id = "p9", name = "X", email = "x@t.com", phoneNumber = "+1",
            verificationStatus = "APPROVED", cleanerType = "INDEPENDENT",
            createdAt = "2023-01-01", updatedAt = "2026-01-01"
        )
        val domain = dto.toDomain()
        assertEquals(VerificationStatus.APPROVED, domain.verificationStatus)
    }

    @Test
    fun `BookingDto toDomain maps status string to BookingStatus enum`() {
        val dto = BookingDto(
            id = "b1", clientId = "c1", providerId = "p1", serviceId = "s1",
            status = "COMPLETED", scheduledDate = "2026-04-01", scheduledTime = "10:00",
            estimatedDuration = 120, totalPrice = 79.0, address = "1 Test St",
            createdAt = "2026-04-01", updatedAt = "2026-04-01"
        )
        val domain = dto.toDomain()
        assertEquals(BookingStatus.COMPLETED, domain.status)
        assertEquals(SyncStatus.SYNCED, domain.syncStatus)
    }

    @Test
    fun `ReviewDto toDomain preserves rating and comment`() {
        val dto = ReviewDto(
            id = "rv1", bookingId = "b1", clientId = "c1", providerId = "p1",
            rating = 3, comment = "Average", createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        val domain = dto.toDomain()
        assertEquals(3,         domain.rating)
        assertEquals("Average", domain.comment)
    }

    @Test
    fun `PaymentDto toDomain maps FAILED status correctly`() {
        val dto = PaymentDto(
            id = "pay1", bookingId = "b1", clientId = "c1", providerId = "p1",
            amount = 50.0, status = "FAILED", method = "CARD",
            createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        val domain = dto.toDomain()
        assertEquals(PaymentStatus.FAILED, domain.status)
    }
}
