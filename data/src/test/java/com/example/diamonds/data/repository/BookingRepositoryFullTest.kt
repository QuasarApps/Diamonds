package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.BookingDao
import com.example.diamonds.data.local.dao.SyncQueueDao
import com.example.diamonds.data.remote.backend.BookingDto
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.OfflineException
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookingRepositoryFullTest {

    private lateinit var db: AppDatabase
    private lateinit var bookingDao: BookingDao
    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var backendService: IBackendService
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var repository: BookingRepository

    private fun makeBookingDto(
        id: String = "b1",
        clientId: String = "c1",
        providerId: String = "p1",
        status: String = "PENDING"
    ) = BookingDto(
        id = id, clientId = clientId, providerId = providerId, serviceId = "s1",
        status = status, scheduledDate = "2026-05-01", scheduledTime = "10:00",
        estimatedDuration = 120, totalPrice = 79.0, address = "1 Test St",
        createdAt = "2026-04-01", updatedAt = "2026-04-01"
    )

    @Before
    fun setUp() {
        bookingDao  = mockk(relaxed = true)
        syncQueueDao = mockk(relaxed = true)
        db = mockk {
            every { bookingDao() }   returns bookingDao
            every { syncQueueDao() } returns syncQueueDao
        }
        backendService = mockk()
        connectivityObserver = mockk {
            every { isOnline() } returns true
        }
        repository = BookingRepository(db, backendService, connectivityObserver)
    }

    // ── getBooking ────────────────────────────────────────────────────────────

    @Test
    fun `getBooking returns cached value when present`() = runTest {
        val entity = makeBookingDto().run {
            com.example.diamonds.data.local.entity.BookingEntity(
                id = id,
                clientId = clientId,
                providerId = providerId,
                serviceId = serviceId,
                status = status,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                estimatedDuration = estimatedDuration,
                totalPrice = totalPrice,
                notes = notes,
                address = address,
                syncStatus = SyncStatus.SYNCED.name,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
        coEvery { bookingDao.getById("b1") } returns entity

        val result = repository.getBooking("b1") as Result.Success
        assertEquals("b1", result.data.id)
        coVerify(exactly = 0) { backendService.getBooking(any()) }
    }

    @Test
    fun `getBooking fetches from backend when cache miss and online`() = runTest {
        coEvery { bookingDao.getById("b1") } returns null
        coEvery { backendService.getBooking("b1") } returns Result.Success(makeBookingDto())

        val result = repository.getBooking("b1") as Result.Success
        assertEquals("b1", result.data.id)
        coVerify { backendService.getBooking("b1") }
        coVerify { bookingDao.upsert(any()) }
    }

    @Test
    fun `getBooking returns OfflineException when cache miss and offline`() = runTest {
        coEvery { bookingDao.getById("b1") } returns null
        every { connectivityObserver.isOnline() } returns false

        val result = repository.getBooking("b1")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is OfflineException)
    }

    // ── getClientBookings ─────────────────────────────────────────────────────

    @Test
    fun `getClientBookings returns empty list from backend when cache is empty`() = runTest {
        coEvery { bookingDao.getForClient("c1") } returns emptyList()
        coEvery { backendService.getClientBookings("c1") } returns Result.Success(
            listOf(makeBookingDto())
        )

        val result = repository.getClientBookings("c1") as Result.Success
        assertEquals(1, result.data.size)
        coVerify { bookingDao.upsert(any()) }
    }

    @Test
    fun `getClientBookings offline with empty cache returns OfflineException`() = runTest {
        coEvery { bookingDao.getForClient("c1") } returns emptyList()
        every { connectivityObserver.isOnline() } returns false

        val result = repository.getClientBookings("c1")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is OfflineException)
    }

    // ── createBooking ─────────────────────────────────────────────────────────

    @Test
    fun `createBooking fails with OfflineException when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val booking = Booking(
            id = "", clientId = "c1", providerId = "p1", serviceId = "s1",
            scheduledDate = "2026-05-01", scheduledTime = "10:00",
            estimatedDuration = 120, totalPrice = 79.0, address = "1 Test St",
            createdAt = "2026-04-01", updatedAt = "2026-04-01"
        )
        val result = repository.createBooking(booking)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is OfflineException)
    }

    @Test
    fun `createBooking succeeds and caches the result`() = runTest {
        coEvery { backendService.createBooking(any()) } returns Result.Success(makeBookingDto())

        val booking = Booking(
            id = "", clientId = "c1", providerId = "p1", serviceId = "s1",
            scheduledDate = "2026-05-01", scheduledTime = "10:00",
            estimatedDuration = 120, totalPrice = 79.0, address = "1 Test St",
            createdAt = "2026-04-01", updatedAt = "2026-04-01"
        )
        val result = repository.createBooking(booking) as Result.Success
        assertEquals("PENDING", result.data.status.name)
        coVerify { bookingDao.upsert(any()) }
    }

    // ── updateBookingStatus ───────────────────────────────────────────────────

    @Test
    fun `updateBookingStatus returns updated booking on success`() = runTest {
        val updated = makeBookingDto(status = "ACCEPTED")
        coEvery { backendService.updateBookingStatus("b1", "ACCEPTED") } returns Result.Success(updated)

        val result = repository.updateBookingStatus("b1", BookingStatus.ACCEPTED) as Result.Success
        assertEquals(BookingStatus.ACCEPTED, result.data.status)
        coVerify { bookingDao.upsert(any()) }
    }

    @Test
    fun `updateBookingStatus offline returns OfflineException`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val result = repository.updateBookingStatus("b1", BookingStatus.CANCELLED)
        assertTrue((result as Result.Error).exception is OfflineException)
    }

    // ── cancelBooking ─────────────────────────────────────────────────────────

    @Test
    fun `cancelBooking delegates to updateBookingStatus with CANCELLED`() = runTest {
        val cancelled = makeBookingDto(status = "CANCELLED")
        coEvery { backendService.cancelBooking("b1") } returns Result.Success(cancelled)

        val result = repository.cancelBooking("b1") as Result.Success
        assertEquals(BookingStatus.CANCELLED, result.data.status)
    }
}
