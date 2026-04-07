package com.example.diamonds.ui.company

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
class CompanyEarningsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bookingRepository: IBookingRepository
    private lateinit var providerRepository: IProviderRepository
    private lateinit var serviceRepository: IServiceRepository
    private lateinit var authRepository: IAuthRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var viewModel: CompanyEarningsViewModel

    private val mockSession = UserSession(
        userId = "p5", email = "company@test.com", role = UserRole.CLEANER,
        cleanerType = CleanerType.COMPANY, authToken = "tok", isAuthenticated = true
    )

    private fun makeCleaner(id: String, name: String) = Provider(
        id = id, name = name, email = "$id@test.com", phoneNumber = "+1",
        rating = 4.5f, reviewCount = 5, verificationStatus = VerificationStatus.APPROVED,
        cleanerType = CleanerType.EMPLOYED, employerId = "p5",
        createdAt = "2023-01-01", updatedAt = "2026-01-01"
    )

    private fun completedBooking(id: String, pid: String, price: Double, date: String) = Booking(
        id = id, clientId = "c1", providerId = pid, serviceId = "s1",
        status = BookingStatus.COMPLETED, scheduledDate = date, scheduledTime = "10:00",
        estimatedDuration = 120, totalPrice = price, address = "1 Test St",
        createdAt = date, updatedAt = date
    )

    @Before
    fun setUp() {
        bookingRepository  = mockk()
        providerRepository = mockk()
        serviceRepository  = mockk()
        authRepository     = mockk()
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns flowOf(ConnectivityState.ONLINE)
        }
        every { authRepository.getCurrentUserSession() } returns flowOf(mockSession)
        viewModel = CompanyEarningsViewModel(
            bookingRepository, providerRepository, serviceRepository,
            authRepository, connectivityObserver
        )
    }

    @Test
    fun `loadEarnings sums all employed cleaner bookings for week totals`() = runTest {
        val today = LocalDate.now().toString()
        val cleaners = listOf(makeCleaner("p1", "Maria"), makeCleaner("p2", "James"))
        coEvery { providerRepository.searchProviders(any(), any()) } returns Result.Success(cleaners)
        coEvery { bookingRepository.getProviderBookings("p1") } returns Result.Success(listOf(
            completedBooking("b1", "p1", 79.0, today),
            completedBooking("b2", "p1", 79.0, today)
        ))
        coEvery { bookingRepository.getProviderBookings("p2") } returns Result.Success(listOf(
            completedBooking("b3", "p2", 119.0, today)
        ))
        coEvery { serviceRepository.getService(any()) } returns Result.Error(Exception())

        viewModel.loadEarnings()
        advanceUntilIdle()

        val state = viewModel.earningsState.value
        assertEquals(3,   state.weekJobCount)
        assertEquals(79.0 * 2 + 119.0, state.weekTotal, 0.01)
        assertEquals(2,   state.teamSize)
    }

    @Test
    fun `loadEarnings builds cleaner breakdown sorted by revenue`() = runTest {
        val monthStart = LocalDate.now().withDayOfMonth(1).toString()
        val cleaners = listOf(makeCleaner("p1", "Maria"), makeCleaner("p2", "James"))
        coEvery { providerRepository.searchProviders(any(), any()) } returns Result.Success(cleaners)
        coEvery { bookingRepository.getProviderBookings("p1") } returns Result.Success(listOf(
            completedBooking("b1", "p1", 300.0, monthStart)
        ))
        coEvery { bookingRepository.getProviderBookings("p2") } returns Result.Success(listOf(
            completedBooking("b2", "p2", 100.0, monthStart)
        ))
        coEvery { serviceRepository.getService(any()) } returns Result.Error(Exception())

        viewModel.loadEarnings()
        advanceUntilIdle()

        val breakdown = viewModel.earningsState.value.cleanerBreakdown
        assertEquals(2, breakdown.size)
        // Sorted descending by revenue — Maria first
        assertEquals("Maria", breakdown[0].cleanerName)
        assertEquals(300.0, breakdown[0].revenue, 0.01)
        assertEquals("James", breakdown[1].cleanerName)
    }

    @Test
    fun `loadEarnings with no employed cleaners returns empty state`() = runTest {
        // searchProviders returns no providers with employerId == "p5"
        coEvery { providerRepository.searchProviders(any(), any()) } returns Result.Success(
            listOf(Provider(
                id = "p9", name = "Other", email = "o@t.com", phoneNumber = "+1",
                rating = 4.0f, reviewCount = 1, verificationStatus = VerificationStatus.APPROVED,
                cleanerType = CleanerType.INDEPENDENT,   // NOT employed by p5
                createdAt = "2023-01-01", updatedAt = "2026-01-01"
            ))
        )

        viewModel.loadEarnings()
        advanceUntilIdle()

        val state = viewModel.earningsState.value
        assertEquals(0, state.teamSize)
        assertEquals(0.0, state.weekTotal, 0.01)
        assertTrue(state.cleanerBreakdown.isEmpty())
    }
}
