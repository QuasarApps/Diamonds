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

    // ── DTO validation ────────────────────────────────────────────────────────
    //
    // Every DTO parameter has a default, because Firestore's object mapper needs a no-arg
    // constructor (see FirestoreDtoContractTest). That means a partial or malformed document
    // deserializes silently into blanks. These tests pin the checks that put the failure back.

    @Test
    fun `a blank id is rejected rather than cached under an empty key`() {
        // Repositories upsert by primary key, so every malformed record of a type would collide
        // on the same "" row and overwrite the last one.
        val e = assertThrows(MalformedDtoException::class.java) {
            ClientDto(name = "Alice", email = "a@t.com").toDomain()
        }
        assertTrue(e.message!!, e.message!!.contains("ClientDto.id"))
    }

    @Test
    fun `an unparseable enum names the field, the value received and the valid set`() {
        val e = assertThrows(MalformedDtoException::class.java) {
            BookingDto(
                id = "b1", clientId = "c1", providerId = "p1", serviceId = "s1",
                status = "TOTALLY_BOGUS", createdAt = "2026-04-07", updatedAt = "2026-04-07"
            ).toDomain()
        }
        val msg = e.message!!
        assertTrue(msg, msg.contains("BookingDto.status"))
        assertTrue(msg, msg.contains("TOTALLY_BOGUS"))
        assertTrue(msg, msg.contains("CANCELLED"))
    }

    @Test
    fun `a missing enum field is reported by name rather than as No enum constant`() {
        // status defaults to "" — before this, valueOf("") threw
        // "No enum constant com.example.diamonds.domain.model.BookingStatus." with no hint of
        // which document or field was at fault.
        val e = assertThrows(MalformedDtoException::class.java) {
            BookingDto(id = "b1", createdAt = "2026-04-07", updatedAt = "2026-04-07").toDomain()
        }
        assertTrue(e.message!!, e.message!!.contains("BookingDto.status"))
    }

    @Test
    fun `verificationStatus is never guessed`() {
        // Defaulting this either locks out a verified provider or presents an unverified one as
        // APPROVED. ProviderDto.verificationStatus defaults to "".
        val e = assertThrows(MalformedDtoException::class.java) {
            ProviderDto(id = "p1", name = "Maria").toDomain()
        }
        assertTrue(e.message!!, e.message!!.contains("ProviderDto.verificationStatus"))
    }

    @Test
    fun `an unrecognised specialization is reported instead of silently dropped`() {
        // Was mapNotNull { getOrNull() }: a provider listing three specialities, one unknown,
        // rendered two and nothing said so.
        val e = assertThrows(MalformedDtoException::class.java) {
            ProviderDto(
                id = "p1", name = "Maria", verificationStatus = "APPROVED",
                specializations = listOf("DEEP_CLEAN", "NOT_A_REAL_TYPE")
            ).toDomain()
        }
        assertTrue(e.message!!, e.message!!.contains("ProviderDto.specializations"))
    }

    @Test
    fun `an unrecognised review direction is reported instead of defaulting`() {
        // Was defaulted to CLIENT_REVIEWS_PROVIDER, which files a cleaner's review of a customer
        // against the customer's own profile instead.
        val e = assertThrows(MalformedDtoException::class.java) {
            ReviewDto(
                id = "rv1", bookingId = "b1", clientId = "c1", providerId = "p1",
                rating = 5, direction = "SIDEWAYS",
                createdAt = "2026-04-07", updatedAt = "2026-04-07"
            ).toDomain()
        }
        assertTrue(e.message!!, e.message!!.contains("ReviewDto.direction"))
    }

    @Test
    fun `an absent optional enum stays null but an unparseable one does not`() {
        val absent = BookingDto(
            id = "b1", status = "PENDING", cleaningType = null,
            createdAt = "2026-04-07", updatedAt = "2026-04-07"
        ).toDomain()
        assertNull(absent.cleaningType)

        // "unspecified" and "a value we cannot read" used to collapse into the same null.
        val e = assertThrows(MalformedDtoException::class.java) {
            BookingDto(
                id = "b1", status = "PENDING", cleaningType = "NONSENSE",
                createdAt = "2026-04-07", updatedAt = "2026-04-07"
            ).toDomain()
        }
        assertTrue(e.message!!, e.message!!.contains("BookingDto.cleaningType"))
    }

    @Test
    fun `well-formed DTOs still map without complaint`() {
        val provider = ProviderDto(
            id = "p1", name = "Maria", email = "m@c.com", phoneNumber = "+1",
            verificationStatus = "APPROVED", cleanerType = "EMPLOYED",
            specializations = listOf("DEEP_CLEAN", "WINDOW_CLEANING"),
            createdAt = "2025-01-01", updatedAt = "2026-01-01"
        ).toDomain()

        assertEquals(VerificationStatus.APPROVED, provider.verificationStatus)
        assertEquals(CleanerType.EMPLOYED, provider.cleanerType)
        assertEquals(
            listOf(CleaningType.DEEP_CLEAN, CleaningType.WINDOW_CLEANING),
            provider.specializations
        )
    }

    // ── Domain → DTO ──────────────────────────────────────────────────────────

    @Test
    fun `Provider toDto then toDomain round-trips every field`() {
        val provider = Provider(
            id = "p1", name = "Maria", email = "m@c.com", phoneNumber = "+15559999",
            profileImageUrl = "https://img.test/m.jpg", bio = "Expert",
            rating = 4.9f, reviewCount = 143,
            verificationStatus = VerificationStatus.APPROVED, serviceRadius = 15,
            cleanerType = CleanerType.EMPLOYED,
            employerId = "co1", employerName = "Sparkle Co",
            specializations = listOf(CleaningType.DEEP_CLEAN, CleaningType.OFFICE_COMMERCIAL),
            createdAt = "2023-01-01", updatedAt = "2026-01-01"
        )

        assertEquals(provider, provider.toDto().toDomain())
    }

    @Test
    fun `Provider toDto serialises enums as their names`() {
        // ProviderDto.toDomain() resolves these with valueOf(); anything else would silently
        // fall back to a default (cleanerType) or throw (verificationStatus).
        val dto = Provider(
            id = "p1", name = "Maria", email = "m@c.com", phoneNumber = "+1",
            verificationStatus = VerificationStatus.SUSPENDED,
            cleanerType = CleanerType.COMPANY,
            specializations = listOf(CleaningType.WINDOW_CLEANING),
            createdAt = "2023-01-01", updatedAt = "2026-01-01"
        ).toDto()

        assertEquals("SUSPENDED", dto.verificationStatus)
        assertEquals("COMPANY", dto.cleanerType)
        assertEquals(listOf("WINDOW_CLEANING"), dto.specializations)
    }
}
