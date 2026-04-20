package com.example.diamonds.ui.cleaner

import com.example.diamonds.common.util.ConnectivityState
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.IClientRepository
import com.example.diamonds.domain.repository.IServiceRepository
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import com.example.diamonds.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CleanerEarningsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bookingRepository: IBookingRepository
    private lateinit var serviceRepository: IServiceRepository
    private lateinit var clientRepository: IClientRepository
    private lateinit var authRepository: IAuthRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var viewModel: CleanerEarningsViewModel

    private val mockSession = UserSession(
        userId = "p1", email = "cleaner@test.com", role = UserRole.CLEANER,
        authToken = "tok", isAuthenticated = true
    )

    private fun completedBooking(
        id: String,
        price: Double,
        date: String
    ) = Booking(
        id = id, clientId = "c1", providerId = "p1", serviceId = "s1",
        status = BookingStatus.COMPLETED, scheduledDate = date, scheduledTime = "10:00",
        estimatedDuration = 120, totalPrice = price, address = "1 Test St",
        createdAt = date, updatedAt = date
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
        viewModel = CleanerEarningsViewModel(
            bookingRepository, serviceRepository, clientRepository,
            authRepository, connectivityObserver
        )
    }

    @Test
    fun `loadEarnings calculates week and month totals from completed bookings`() = runTest {
        val today  = LocalDate.now().toString()
        val weekStart = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1).toString()

        val bookings = listOf(
            completedBooking("b1", 79.0, today),
            completedBooking("b2", 119.0, today),
            completedBooking("b3", 50.0, weekStart)
        )
        coEvery { bookingRepository.getProviderBookings("p1") } returns Result.Success(bookings)
        coEvery { serviceRepository.getService(any()) } returns Result.Error(Exception())
        coEvery { clientRepository.getClient(any()) } returns Result.Error(Exception())

        viewModel.loadEarnings()
        advanceUntilIdle()

        val state = viewModel.earningsState.value
        assertFalse(state.isLoading)
        // All 3 are this week (weekStart ≤ today)
        assertEquals(3, state.weekCompletedCount)
        assertEquals(79.0 + 119.0 + 50.0, state.weekTotal, 0.01)
        // All 3 are this month
        assertEquals(3, state.monthCompletedCount)
        assertEquals(79.0 + 119.0 + 50.0, state.monthTotal, 0.01)
    }

    @Test
    fun `loadEarnings builds dailyTotals list of 7 entries`() = runTest {
        val today = LocalDate.now().toString()
        coEvery { bookingRepository.getProviderBookings("p1") } returns Result.Success(
            listOf(completedBooking("b1", 79.0, today))
        )
        coEvery { serviceRepository.getService(any()) } returns Result.Error(Exception())
        coEvery { clientRepository.getClient(any()) } returns Result.Error(Exception())

        viewModel.loadEarnings()
        advanceUntilIdle()

        assertEquals(7, viewModel.earningsState.value.dailyTotals.size)
    }

    @Test
    fun `loadEarnings with no completed bookings shows zeros`() = runTest {
        coEvery { bookingRepository.getProviderBookings("p1") } returns
            Result.Success(listOf())

        viewModel.loadEarnings()
        advanceUntilIdle()

        val state = viewModel.earningsState.value
        assertEquals(0, state.weekCompletedCount)
        assertEquals(0.0, state.weekTotal, 0.01)
    }

    @Test
    fun `loadEarnings error sets error message`() = runTest {
        coEvery { bookingRepository.getProviderBookings("p1") } returns
            Result.Error(Exception("Offline"))

        viewModel.loadEarnings()
        advanceUntilIdle()

        assertEquals("Offline", viewModel.error.value)
    }
}
