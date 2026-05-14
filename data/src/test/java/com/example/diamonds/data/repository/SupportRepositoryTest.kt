package com.example.diamonds.data.repository

import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.BookingDao
import com.example.diamonds.data.local.dao.ClaimDao
import com.example.diamonds.data.local.dao.SupportTicketDao
import com.example.diamonds.data.local.entity.ClaimEntity
import com.example.diamonds.data.local.entity.SupportTicketEntity
import com.example.diamonds.data.remote.backend.BookingDto
import com.example.diamonds.data.remote.backend.ClaimDto
import com.example.diamonds.data.remote.backend.HelpArticleDto
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.PaymentDto
import com.example.diamonds.data.remote.backend.SupportTicketDto
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.CancellationReason
import com.example.diamonds.domain.model.Claim
import com.example.diamonds.domain.model.ClaimStatus
import com.example.diamonds.domain.model.ClaimType
import com.example.diamonds.domain.model.PaymentStatus
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SupportTicket
import com.example.diamonds.domain.model.SupportTicketStatus
import com.example.diamonds.domain.model.SupportTicketType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupportRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var supportTicketDao: SupportTicketDao
    private lateinit var claimDao: ClaimDao
    private lateinit var bookingDao: BookingDao
    private lateinit var backendService: IBackendService
    private lateinit var repository: SupportRepository

    private fun makeTicketDto(id: String = "t1") = SupportTicketDto(
        id = id, userId = "c1", userRole = "CUSTOMER", bookingId = "b1",
        type = "POST_SERVICE", status = "OPEN",
        subject = "Issue with cleaning", description = "Not done properly",
        createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    private fun makeTicketEntity(id: String = "t1") = SupportTicketEntity(
        id = id, userId = "c1", userRole = "CUSTOMER", bookingId = "b1",
        type = "POST_SERVICE", status = "OPEN",
        subject = "Issue with cleaning", description = "Not done properly",
        createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    private fun makeDomainTicket(id: String = "t1") = SupportTicket(
        id = id, userId = "c1", userRole = "CUSTOMER", bookingId = "b1",
        type = SupportTicketType.POST_SERVICE, status = SupportTicketStatus.OPEN,
        subject = "Issue with cleaning", description = "Not done properly",
        createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    private fun makeClaimDto(id: String = "cl1") = ClaimDto(
        id = id, bookingId = "b1", filedByUserId = "c1", filedByRole = "CUSTOMER",
        claimType = "INCOMPLETE_SERVICE", status = "SUBMITTED",
        description = "Half the rooms were not cleaned",
        createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    private fun makeClaimEntity(id: String = "cl1") = ClaimEntity(
        id = id, bookingId = "b1", filedByUserId = "c1", filedByRole = "CUSTOMER",
        claimType = "INCOMPLETE_SERVICE", status = "SUBMITTED",
        description = "Half the rooms were not cleaned",
        createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    private fun makeDomainClaim(id: String = "cl1") = Claim(
        id = id, bookingId = "b1", filedByUserId = "c1", filedByRole = "CUSTOMER",
        claimType = ClaimType.INCOMPLETE_SERVICE, status = ClaimStatus.SUBMITTED,
        description = "Half the rooms were not cleaned",
        createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    private fun makeBookingDto() = BookingDto(
        id = "b1", clientId = "c1", providerId = "p1", serviceId = "s1",
        status = "CANCELLED", scheduledDate = "2026-05-01", scheduledTime = "10:00",
        estimatedDuration = 120, totalPrice = 79.0, address = "1 Test St",
        createdAt = "2026-04-01", updatedAt = "2026-04-01"
    )

    private fun makePaymentDto() = PaymentDto(
        id = "pay1", bookingId = "b1", clientId = "c1", providerId = "p1",
        amount = 79.0, status = "REFUNDED", method = "CARD",
        createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    @Before
    fun setUp() {
        supportTicketDao = mockk(relaxed = true)
        claimDao = mockk(relaxed = true)
        bookingDao = mockk(relaxed = true)
        db = mockk {
            every { supportTicketDao() } returns supportTicketDao
            every { claimDao() } returns claimDao
            every { bookingDao() } returns bookingDao
        }
        backendService = mockk()
        repository = SupportRepository(db, backendService)
    }

    // ── createSupportTicket ───────────────────────────────────────────────────

    @Test
    fun `createSupportTicket calls backend and caches result`() = runTest {
        coEvery { backendService.createSupportTicket(any()) } returns Result.Success(makeTicketDto())

        val result = repository.createSupportTicket(makeDomainTicket())

        assertTrue(result is Result.Success)
        assertEquals("t1", (result as Result.Success).data.id)
        coVerify { supportTicketDao.upsert(any()) }
    }

    @Test
    fun `createSupportTicket propagates backend error`() = runTest {
        coEvery { backendService.createSupportTicket(any()) } returns
                Result.Error(Exception("server unavailable"))

        val result = repository.createSupportTicket(makeDomainTicket())

        assertTrue(result is Result.Error)
        assertEquals("server unavailable", (result as Result.Error).exception.message)
    }

    // ── getSupportTicket ──────────────────────────────────────────────────────

    @Test
    fun `getSupportTicket returns cached entity without calling backend`() = runTest {
        coEvery { supportTicketDao.getById("t1") } returns makeTicketEntity()

        val result = repository.getSupportTicket("t1")

        assertTrue(result is Result.Success)
        assertEquals("t1", (result as Result.Success).data.id)
        coVerify(exactly = 0) { backendService.getSupportTicket(any()) }
    }

    @Test
    fun `getSupportTicket fetches from backend on cache miss`() = runTest {
        coEvery { supportTicketDao.getById("t1") } returns null
        coEvery { backendService.getSupportTicket("t1") } returns Result.Success(makeTicketDto())

        val result = repository.getSupportTicket("t1")

        assertTrue(result is Result.Success)
        coVerify { supportTicketDao.upsert(any()) }
    }

    // ── getTicketsForUser ─────────────────────────────────────────────────────

    @Test
    fun `getTicketsForUser returns backend result and caches it`() = runTest {
        coEvery { backendService.getTicketsForUser("c1") } returns
                Result.Success(listOf(makeTicketDto()))

        val result = repository.getTicketsForUser("c1") as Result.Success
        assertEquals(1, result.data.size)
        coVerify { supportTicketDao.upsert(any()) }
    }

    @Test
    fun `getTicketsForUser falls back to cache on backend error`() = runTest {
        coEvery { backendService.getTicketsForUser("c1") } returns
                Result.Error(Exception("network error"))
        coEvery { supportTicketDao.getForUser("c1") } returns listOf(makeTicketEntity())

        val result = repository.getTicketsForUser("c1") as Result.Success
        assertEquals(1, result.data.size)
    }

    @Test
    fun `getTicketsForUser propagates error when cache also empty`() = runTest {
        coEvery { backendService.getTicketsForUser("c1") } returns
                Result.Error(Exception("network error"))
        coEvery { supportTicketDao.getForUser("c1") } returns emptyList()

        val result = repository.getTicketsForUser("c1")
        assertTrue(result is Result.Error)
    }

    // ── observeTicketsForUser ─────────────────────────────────────────────────

    @Test
    fun `observeTicketsForUser emits mapped domain tickets`() = runTest {
        every { supportTicketDao.observeForUser("c1") } returns
                flowOf(listOf(makeTicketEntity()))

        val tickets = repository.observeTicketsForUser("c1").first()
        assertEquals(1, tickets.size)
        assertEquals("t1", tickets.first().id)
        assertEquals(SupportTicketType.POST_SERVICE, tickets.first().type)
    }

    // ── fileClaim ─────────────────────────────────────────────────────────────

    @Test
    fun `fileClaim calls backend and caches result`() = runTest {
        coEvery { backendService.fileClaim(any()) } returns Result.Success(makeClaimDto())

        val result = repository.fileClaim(makeDomainClaim())

        assertTrue(result is Result.Success)
        assertEquals("cl1", (result as Result.Success).data.id)
        assertEquals(ClaimStatus.SUBMITTED, result.data.status)
        coVerify { claimDao.upsert(any()) }
    }

    @Test
    fun `fileClaim propagates backend error`() = runTest {
        coEvery { backendService.fileClaim(any()) } returns
                Result.Error(Exception("claim rejected"))

        val result = repository.fileClaim(makeDomainClaim())

        assertTrue(result is Result.Error)
    }

    // ── getClaim ──────────────────────────────────────────────────────────────

    @Test
    fun `getClaim returns cached claim without hitting backend`() = runTest {
        coEvery { claimDao.getById("cl1") } returns makeClaimEntity()

        val result = repository.getClaim("cl1")

        assertTrue(result is Result.Success)
        assertEquals("cl1", (result as Result.Success).data.id)
        coVerify(exactly = 0) { backendService.getClaim(any()) }
    }

    @Test
    fun `getClaim fetches from backend on cache miss`() = runTest {
        coEvery { claimDao.getById("cl1") } returns null
        coEvery { backendService.getClaim("cl1") } returns Result.Success(makeClaimDto())

        val result = repository.getClaim("cl1")

        assertTrue(result is Result.Success)
        coVerify { claimDao.upsert(any()) }
    }

    // ── getClaimsForUser ──────────────────────────────────────────────────────

    @Test
    fun `getClaimsForUser returns backend result and caches it`() = runTest {
        coEvery { backendService.getClaimsForUser("c1") } returns
                Result.Success(listOf(makeClaimDto()))

        val result = repository.getClaimsForUser("c1") as Result.Success
        assertEquals(1, result.data.size)
        coVerify { claimDao.upsert(any()) }
    }

    @Test
    fun `getClaimsForUser falls back to cache on backend error`() = runTest {
        coEvery { backendService.getClaimsForUser("c1") } returns
                Result.Error(Exception("offline"))
        coEvery { claimDao.getForUser("c1") } returns listOf(makeClaimEntity())

        val result = repository.getClaimsForUser("c1") as Result.Success
        assertEquals(1, result.data.size)
    }

    // ── getClaimForBooking ────────────────────────────────────────────────────

    @Test
    fun `getClaimForBooking returns cached claim when present`() = runTest {
        coEvery { claimDao.getByBookingId("b1") } returns makeClaimEntity()

        val result = repository.getClaimForBooking("b1") as Result.Success
        assertNotNull(result.data)
        assertEquals("cl1", result.data!!.id)
        coVerify(exactly = 0) { backendService.getClaimForBooking(any()) }
    }

    @Test
    fun `getClaimForBooking returns null when no claim exists`() = runTest {
        coEvery { claimDao.getByBookingId("b1") } returns null
        coEvery { backendService.getClaimForBooking("b1") } returns Result.Success(null)

        val result = repository.getClaimForBooking("b1") as Result.Success
        assertNull(result.data)
    }

    // ── cancelBookingWithReason ───────────────────────────────────────────────

    @Test
    fun `cancelBookingWithReason calls backend and caches the updated booking`() = runTest {
        coEvery {
            backendService.cancelBookingWithReason(any())
        } returns Result.Success(makeBookingDto())

        val result = repository.cancelBookingWithReason(
            "b1", CancellationReason.CHANGED_MIND, null
        )

        assertTrue(result is Result.Success)
        assertEquals(BookingStatus.CANCELLED, (result as Result.Success).data.status)
        coVerify { bookingDao.upsert(any()) }
    }

    @Test
    fun `cancelBookingWithReason propagates backend error`() = runTest {
        coEvery { backendService.cancelBookingWithReason(any()) } returns
                Result.Error(Exception("booking cannot be cancelled"))

        val result = repository.cancelBookingWithReason(
            "b1", CancellationReason.PRICE_ISSUE, "Too expensive"
        )

        assertTrue(result is Result.Error)
    }

    // ── editBooking ───────────────────────────────────────────────────────────

    @Test
    fun `editBooking calls backend with new address and caches result`() = runTest {
        coEvery { backendService.editBooking(any()) } returns
                Result.Success(makeBookingDto().copy(address = "2 New St"))

        val result = repository.editBooking("b1", "2 New St", null)

        assertTrue(result is Result.Success)
        assertEquals("2 New St", (result as Result.Success).data.address)
        coVerify { bookingDao.upsert(any()) }
    }

    @Test
    fun `editBooking propagates backend error`() = runTest {
        coEvery { backendService.editBooking(any()) } returns
                Result.Error(Exception("cannot edit accepted booking"))

        val result = repository.editBooking("b1", null, null)

        assertTrue(result is Result.Error)
    }

    // ── requestRefund ─────────────────────────────────────────────────────────

    @Test
    fun `requestRefund returns refunded payment on success`() = runTest {
        coEvery { backendService.requestRefund("b1") } returns Result.Success(makePaymentDto())

        val result = repository.requestRefund("b1") as Result.Success
        assertEquals(PaymentStatus.REFUNDED, result.data.status)
    }

    @Test
    fun `requestRefund propagates error`() = runTest {
        coEvery { backendService.requestRefund("b1") } returns
                Result.Error(Exception("refund not eligible"))

        val result = repository.requestRefund("b1")
        assertTrue(result is Result.Error)
    }

    // ── getHelpArticles ───────────────────────────────────────────────────────

    @Test
    fun `getHelpArticles returns mapped articles`() = runTest {
        coEvery { backendService.getHelpArticles() } returns Result.Success(
            listOf(HelpArticleDto("a1", "How to book", "Step by step", "BOOKING"))
        )

        val result = repository.getHelpArticles() as Result.Success
        assertEquals(1, result.data.size)
        assertEquals("a1", result.data.first().id)
        assertEquals("How to book", result.data.first().title)
    }

    @Test
    fun `getHelpArticles propagates backend error`() = runTest {
        coEvery { backendService.getHelpArticles() } returns Result.Error(Exception("unavailable"))

        val result = repository.getHelpArticles()
        assertTrue(result is Result.Error)
    }

    // ── observeClaimsForUser ──────────────────────────────────────────────────

    @Test
    fun `observeClaimsForUser emits mapped domain claims`() = runTest {
        every { claimDao.observeForUser("c1") } returns flowOf(listOf(makeClaimEntity()))

        val claims = repository.observeClaimsForUser("c1").first()
        assertEquals(1, claims.size)
        assertEquals("cl1", claims.first().id)
        assertEquals(ClaimType.INCOMPLETE_SERVICE, claims.first().claimType)
    }
}
