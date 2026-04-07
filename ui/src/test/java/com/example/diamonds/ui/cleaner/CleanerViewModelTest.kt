package com.example.diamonds.ui.cleaner

import com.example.diamonds.common.util.ConnectivityState
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.*
import com.example.diamonds.domain.repository.*
import com.example.diamonds.ui.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CleanerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bookingRepository: IBookingRepository
    private lateinit var serviceRepository: IServiceRepository
    private lateinit var clientRepository: IClientRepository
    private lateinit var authRepository: IAuthRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var viewModel: CleanerViewModel

    private val mockSession = UserSession(
        userId = "p1", email = "cleaner@test.com", role = UserRole.CLEANER,
        authToken = "tok", isAuthenticated = true
    )

    private fun makeBooking(
        id: String = "b1",
        status: BookingStatus = BookingStatus.PENDING,
        date: String = LocalDate.now().toString()
    ) = Booking(
        id = id, clientId = "c1", providerId = "p1", serviceId = "s1",
        status = status, scheduledDate = date, scheduledTime = "10:00",
        estimatedDuration = 120, totalPrice = 79.0, address = "1 Test St",
        createdAt = "2026-04-01", updatedAt = "2026-04-01"
    )

    @Before
    fun setUp() {
        bookingRepository  = mockk()
        serviceRepository  = mockk()
        clientRepository   = mockk()
        authRepository     = mockk()
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns flowOf(ConnectivityState.ONLINE)
        }
        every { authRepository.getCurrentUserSession() } returns flowOf(mockSession)
        viewModel = CleanerViewModel(
            bookingRepository, serviceRepository, clientRepository,
            authRepository, connectivityObserver
        )
    }

    // ── loadRequests ──────────────────────────────────────────────────────────

    @Test
    fun `loadRequests returns only PENDING bookings`() = runTest {
        val bookings = listOf(
            makeBooking("b1", BookingStatus.PENDING),
            makeBooking("b2", BookingStatus.ACCEPTED),
            makeBooking("b3", BookingStatus.PENDING)
        )
        coEvery { bookingRepository.getProviderBookings("p1") } returns Result.Success(bookings)
        coEvery { serviceRepository.getService(any()) } returns Result.Error(Exception())
        coEvery { clientRepository.getClient(any()) } returns Result.Error(Exception())

        viewModel.loadRequests()
        advanceUntilIdle()

        val state = viewModel.requestsState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.requests.size)
        assertTrue(state.requests.all { it.booking.status == BookingStatus.PENDING })
    }

    @Test
    fun `loadRequests with error sets error message`() = runTest {
        coEvery { bookingRepository.getProviderBookings("p1") } returns
            Result.Error(Exception("Network error"))

        viewModel.loadRequests()
        advanceUntilIdle()

        assertEquals("Network error", viewModel.error.value)
    }

    // ── loadSchedule ──────────────────────────────────────────────────────────

    @Test
    fun `loadSchedule separates today and future accepted bookings`() = runTest {
        val today    = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()

        val bookings = listOf(
            makeBooking("b1", BookingStatus.ACCEPTED, today),
            makeBooking("b2", BookingStatus.ACCEPTED, tomorrow),
            makeBooking("b3", BookingStatus.PENDING, today)      // pending → excluded
        )
        coEvery { bookingRepository.getProviderBookings("p1") } returns Result.Success(bookings)
        coEvery { serviceRepository.getService(any()) } returns Result.Error(Exception())
        coEvery { clientRepository.getClient(any()) } returns Result.Error(Exception())

        viewModel.loadSchedule()
        advanceUntilIdle()

        val state = viewModel.scheduleState.value
        assertEquals(1, state.todayJobs.size)
        assertEquals("b1", state.todayJobs[0].booking.id)
        assertEquals(1, state.upcomingJobs.size)
        assertEquals("b2", state.upcomingJobs[0].booking.id)
    }

    // ── loadDashboard ─────────────────────────────────────────────────────────

    @Test
    fun `loadDashboard aggregates pending count and week earnings`() = runTest {
        val today = LocalDate.now().toString()
        val weekStart = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1).toString()

        val bookings = listOf(
            makeBooking("b1", BookingStatus.PENDING, today),
            makeBooking("b2", BookingStatus.PENDING, today),
            makeBooking("b3", BookingStatus.COMPLETED, weekStart),
            makeBooking("b4", BookingStatus.COMPLETED, weekStart)
        )
        coEvery { bookingRepository.getProviderBookings("p1") } returns Result.Success(bookings)

        viewModel.loadDashboard()
        advanceUntilIdle()

        val state = viewModel.dashboardState.value
        assertEquals(2, state.pendingCount)
        assertEquals(79.0 * 2, state.weekEarnings, 0.01)
    }

    // ── acceptBooking / declineBooking ─────────────────────────────────────────

    @Test
    fun `acceptBooking updates status to ACCEPTED`() = runTest {
        val updated = makeBooking("b1", BookingStatus.ACCEPTED)
        coEvery { bookingRepository.updateBookingStatus("b1", BookingStatus.ACCEPTED) } returns
            Result.Success(updated)
        coEvery { bookingRepository.getProviderBookings("p1") } returns
            Result.Success(listOf(updated))
        coEvery { serviceRepository.getService(any()) } returns Result.Error(Exception())
        coEvery { clientRepository.getClient(any()) } returns Result.Error(Exception())

        viewModel.acceptRequest("b1")
        advanceUntilIdle()

        coVerify { bookingRepository.updateBookingStatus("b1", BookingStatus.ACCEPTED) }
    }

    @Test
    fun `declineBooking updates status to CANCELLED`() = runTest {
        val updated = makeBooking("b1", BookingStatus.CANCELLED)
        coEvery { bookingRepository.updateBookingStatus("b1", BookingStatus.CANCELLED) } returns
            Result.Success(updated)
        coEvery { bookingRepository.getProviderBookings("p1") } returns
            Result.Success(listOf(updated))
        coEvery { serviceRepository.getService(any()) } returns Result.Error(Exception())
        coEvery { clientRepository.getClient(any()) } returns Result.Error(Exception())

        viewModel.declineRequest("b1")
        advanceUntilIdle()

        coVerify { bookingRepository.updateBookingStatus("b1", BookingStatus.CANCELLED) }
    }
}
