package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.RecurringBookingDao
import com.example.diamonds.data.local.entity.RecurringBookingEntity
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.RecurringBookingDto
import com.example.diamonds.domain.model.RecurringBooking
import com.example.diamonds.domain.model.RecurringBookingStatus
import com.example.diamonds.domain.model.RecurringFrequency
import com.example.diamonds.domain.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubscriptionRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var recurringBookingDao: RecurringBookingDao
    private lateinit var backendService: IBackendService
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var repository: SubscriptionRepository

    private fun makeDto(id: String = "rb1") = RecurringBookingDto(
        id = id, clientId = "c1", providerId = "p1", providerName = "Maria",
        serviceId = "s1", serviceName = "Deep Clean", frequency = "WEEKLY",
        preferredDay = 1, preferredTime = "09:00", address = "1 Test St",
        totalPrice = 79.0, status = "ACTIVE", nextBookingDate = "2026-05-05",
        createdAt = "2026-04-01", updatedAt = "2026-04-01"
    )

    private fun makeEntity(id: String = "rb1") = RecurringBookingEntity(
        id = id, clientId = "c1", providerId = "p1", providerName = "Maria",
        serviceId = "s1", serviceName = "Deep Clean", frequency = "WEEKLY",
        preferredDay = 1, preferredTime = "09:00", address = "1 Test St",
        totalPrice = 79.0, status = "ACTIVE", nextBookingDate = "2026-05-05",
        createdAt = "2026-04-01", updatedAt = "2026-04-01"
    )

    private fun makeDomain(id: String = "rb1") = RecurringBooking(
        id = id, clientId = "c1", providerId = "p1", providerName = "Maria",
        serviceId = "s1", serviceName = "Deep Clean",
        frequency = RecurringFrequency.WEEKLY, preferredDay = 1, preferredTime = "09:00",
        address = "1 Test St", totalPrice = 79.0, status = RecurringBookingStatus.ACTIVE,
        nextBookingDate = "2026-05-05", createdAt = "2026-04-01", updatedAt = "2026-04-01"
    )

    @Before
    fun setUp() {
        recurringBookingDao = mockk(relaxed = true)
        db = mockk { every { recurringBookingDao() } returns recurringBookingDao }
        backendService = mockk()
        connectivityObserver = mockk { every { isOnline() } returns true }
        repository = SubscriptionRepository(db, backendService, connectivityObserver)
    }

    // ── createRecurringBooking ────────────────────────────────────────────────

    @Test
    fun `createRecurringBooking returns error when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val result = repository.createRecurringBooking(makeDomain())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception.message!!.contains("internet"))
    }

    @Test
    fun `createRecurringBooking calls backend and caches when online`() = runTest {
        coEvery { backendService.createRecurringBooking(any()) } returns Result.Success(makeDto())

        val result = repository.createRecurringBooking(makeDomain())

        assertTrue(result is Result.Success)
        assertEquals("rb1", (result as Result.Success).data.id)
        coVerify { recurringBookingDao.upsert(any()) }
    }

    @Test
    fun `createRecurringBooking propagates backend error`() = runTest {
        coEvery { backendService.createRecurringBooking(any()) } returns
                Result.Error(Exception("server error"))

        val result = repository.createRecurringBooking(makeDomain())

        assertTrue(result is Result.Error)
        assertEquals("server error", (result as Result.Error).exception.message)
    }

    // ── getRecurringBooking ───────────────────────────────────────────────────

    @Test
    fun `getRecurringBooking returns cached entity without hitting backend`() = runTest {
        coEvery { recurringBookingDao.getById("rb1") } returns makeEntity()

        val result = repository.getRecurringBooking("rb1")

        assertTrue(result is Result.Success)
        assertEquals("rb1", (result as Result.Success).data.id)
        coVerify(exactly = 0) { backendService.getRecurringBooking(any()) }
    }

    @Test
    fun `getRecurringBooking fetches from backend on cache miss when online`() = runTest {
        coEvery { recurringBookingDao.getById("rb1") } returns null
        coEvery { backendService.getRecurringBooking("rb1") } returns Result.Success(makeDto())

        val result = repository.getRecurringBooking("rb1")

        assertTrue(result is Result.Success)
        coVerify { recurringBookingDao.upsert(any()) }
    }

    @Test
    fun `getRecurringBooking returns error on cache miss when offline`() = runTest {
        coEvery { recurringBookingDao.getById("rb1") } returns null
        every { connectivityObserver.isOnline() } returns false

        val result = repository.getRecurringBooking("rb1")

        assertTrue(result is Result.Error)
    }

    // ── getRecurringBookingsForClient ─────────────────────────────────────────

    @Test
    fun `getRecurringBookingsForClient returns cached list`() = runTest {
        coEvery { backendService.getRecurringBookingsForClient("c1") } returns
                Result.Success(listOf(makeDto()))
        coEvery { recurringBookingDao.getForClient("c1") } returns listOf(makeEntity())

        val result = repository.getRecurringBookingsForClient("c1") as Result.Success
        assertEquals(1, result.data.size)
        assertEquals("rb1", result.data.first().id)
    }

    @Test
    fun `getRecurringBookingsForClient returns empty list when offline and cache empty`() =
        runTest {
            every { connectivityObserver.isOnline() } returns false
            coEvery { recurringBookingDao.getForClient("c1") } returns emptyList()

            val result = repository.getRecurringBookingsForClient("c1") as Result.Success
            assertTrue(result.data.isEmpty())
        }

    // ── updateRecurringBookingStatus ──────────────────────────────────────────

    @Test
    fun `updateRecurringBookingStatus returns error when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val result = repository.updateRecurringBookingStatus("rb1", RecurringBookingStatus.PAUSED)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `updateRecurringBookingStatus updates status via backend and caches`() = runTest {
        val pausedDto = makeDto().copy(status = "PAUSED")
        coEvery {
            backendService.updateRecurringBookingStatus("rb1", "PAUSED")
        } returns Result.Success(pausedDto)

        val result = repository.updateRecurringBookingStatus("rb1", RecurringBookingStatus.PAUSED)

        assertTrue(result is Result.Success)
        assertEquals(RecurringBookingStatus.PAUSED, (result as Result.Success).data.status)
        coVerify { recurringBookingDao.upsert(any()) }
    }

    // ── updateSchedule ────────────────────────────────────────────────────────

    @Test
    fun `updateSchedule returns error when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val result = repository.updateSchedule("rb1", 2, "10:00")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `updateSchedule calls backend and caches result`() = runTest {
        coEvery {
            backendService.updateRecurringBookingSchedule("rb1", 2, "10:00")
        } returns Result.Success(makeDto().copy(preferredDay = 2, preferredTime = "10:00"))

        val result = repository.updateSchedule("rb1", 2, "10:00")

        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).data.preferredDay)
        coVerify { recurringBookingDao.upsert(any()) }
    }

    // ── advanceNextBookingDate ────────────────────────────────────────────────

    @Test
    fun `advanceNextBookingDate updates dao and syncs when online`() = runTest {
        coEvery { backendService.advanceRecurringBookingDate("rb1", "2026-05-12") } returns
                Result.Success(Unit)

        val result = repository.advanceNextBookingDate("rb1", "2026-05-12")

        assertTrue(result is Result.Success)
        coVerify { recurringBookingDao.advanceNextBookingDate("rb1", "2026-05-12", any()) }
        coVerify { backendService.advanceRecurringBookingDate("rb1", "2026-05-12") }
    }

    @Test
    fun `advanceNextBookingDate still succeeds when offline (local only)`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val result = repository.advanceNextBookingDate("rb1", "2026-05-12")

        assertTrue(result is Result.Success)
        coVerify { recurringBookingDao.advanceNextBookingDate("rb1", "2026-05-12", any()) }
        coVerify(exactly = 0) { backendService.advanceRecurringBookingDate(any(), any()) }
    }

    // ── observeRecurringBookingsForClient ─────────────────────────────────────

    @Test
    fun `observeRecurringBookingsForClient emits mapped domain objects`() = runTest {
        every { recurringBookingDao.observeForClient("c1") } returns flowOf(listOf(makeEntity()))

        val result = repository.observeRecurringBookingsForClient("c1").first()

        assertEquals(1, result.size)
        assertEquals("rb1", result.first().id)
        assertEquals(RecurringFrequency.WEEKLY, result.first().frequency)
    }

    // ── getActiveRecurringBookingsDue ─────────────────────────────────────────

    @Test
    fun `getActiveRecurringBookingsDue returns due bookings from dao`() = runTest {
        coEvery { recurringBookingDao.getActiveDue("2026-05-05") } returns listOf(makeEntity())

        val result = repository.getActiveRecurringBookingsDue("2026-05-05") as Result.Success
        assertEquals(1, result.data.size)
        assertEquals("rb1", result.data.first().id)
    }
}
