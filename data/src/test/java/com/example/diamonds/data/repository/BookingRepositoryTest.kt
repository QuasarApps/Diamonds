package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.BookingDao
import com.example.diamonds.data.local.dao.SyncQueueDao
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Connectivity-focused unit tests for [BookingRepository].
 * Complementary to [BookingRepositoryFullTest] which covers the full happy-paths.
 */
class BookingRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var bookingDao: BookingDao
    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var backendService: IBackendService
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var repository: BookingRepository

    @Before
    fun setUp() {
        bookingDao = mockk(relaxed = true)
        syncQueueDao = mockk(relaxed = true)
        db = mockk {
            every { bookingDao() } returns bookingDao
            every { syncQueueDao() } returns syncQueueDao
        }
        backendService = mockk(relaxed = true)
        connectivityObserver = mockk { every { isOnline() } returns true }
        repository = BookingRepository(db, backendService, connectivityObserver)
    }

    @Test
    fun `repository can be instantiated`() {
        assertNotNull(repository)
    }

    @Test
    fun `getBooking returns OfflineException when cache empty and offline`() = runTest {
        coEvery { bookingDao.getById(any()) } returns null
        every { connectivityObserver.isOnline() } returns false

        val result = repository.getBooking("missing_id")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is OfflineException)
    }

    @Test
    fun `getClientBookings returns cached results when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false
        val entity = com.example.diamonds.data.local.entity.BookingEntity(
            id = "b1", clientId = "c1", providerId = "p1", serviceId = "s1",
            status = "PENDING", scheduledDate = "2026-05-01", scheduledTime = "10:00",
            estimatedDuration = 120, totalPrice = 79.0, address = "1 Test St",
            syncStatus = SyncStatus.SYNCED.name,
            createdAt = "2026-04-01", updatedAt = "2026-04-01"
        )
        coEvery { bookingDao.getForClient("c1") } returns listOf(entity)

        val result = repository.getClientBookings("c1") as Result.Success
        assertEquals(1, result.data.size)
        coVerify(exactly = 0) { backendService.getClientBookings(any()) }
    }

    @Test
    fun `createBooking wraps exception in Result Error`() = runTest {
        coEvery { backendService.createBooking(any()) } throws RuntimeException("connection reset")

        val booking = Booking(
            id = "", clientId = "c1", providerId = "p1", serviceId = "s1",
            scheduledDate = "2026-05-01", scheduledTime = "10:00",
            estimatedDuration = 120, totalPrice = 79.0, address = "1 Test St",
            createdAt = "2026-04-01", updatedAt = "2026-04-01"
        )
        val result = repository.createBooking(booking)

        assertTrue(result is Result.Error)
        assertEquals("connection reset", (result as Result.Error).exception.message)
    }

    @Test
    fun `updateBookingStatus wraps backend exception in Result Error`() = runTest {
        coEvery {
            backendService.updateBookingStatus(any(), any())
        } throws RuntimeException("timeout")

        val result = repository.updateBookingStatus("b1", BookingStatus.ACCEPTED)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `cancelBooking returns error when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val result = repository.cancelBooking("b1")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is OfflineException)
    }
}
