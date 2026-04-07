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
class CompanyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bookingRepository: IBookingRepository
    private lateinit var providerRepository: IProviderRepository
    private lateinit var serviceRepository: IServiceRepository
    private lateinit var clientRepository: IClientRepository
    private lateinit var authRepository: IAuthRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var viewModel: CompanyViewModel

    private val mockSession = UserSession(
        userId = "p5", email = "company@test.com", role = UserRole.CLEANER,
        cleanerType = CleanerType.COMPANY, authToken = "tok", isAuthenticated = true
    )

    private fun makeProvider(id: String, name: String, employerId: String? = null) = Provider(
        id = id, name = name, email = "$id@test.com", phoneNumber = "+1",
        rating = 4.5f, reviewCount = 10, verificationStatus = VerificationStatus.APPROVED,
        cleanerType = if (employerId != null) CleanerType.EMPLOYED else CleanerType.INDEPENDENT,
        employerId = employerId,
        createdAt = "2023-01-01", updatedAt = "2026-01-01"
    )

    private fun makeBooking(
        id: String,
        providerId: String,
        status: BookingStatus,
        date: String = LocalDate.now().toString()
    ) = Booking(
        id = id, clientId = "c1", providerId = providerId, serviceId = "s1",
        status = status, scheduledDate = date, scheduledTime = "10:00",
        estimatedDuration = 120, totalPrice = 89.0, address = "1 Test St",
        createdAt = date, updatedAt = date
    )

    @Before
    fun setUp() {
        bookingRepository  = mockk()
        providerRepository = mockk()
        serviceRepository  = mockk()
        clientRepository   = mockk()
        authRepository     = mockk()
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns flowOf(ConnectivityState.ONLINE)
        }
        every { authRepository.getCurrentUserSession() } returns flowOf(mockSession)
        viewModel = CompanyViewModel(
            bookingRepository, providerRepository, serviceRepository,
            clientRepository, authRepository, connectivityObserver
        )
    }

    @Test
    fun `loadDashboard counts team size from employed cleaners`() = runTest {
        val cleaners = listOf(
            makeProvider("p1", "Maria",  employerId = "p5"),
            makeProvider("p2", "James",  employerId = "p5"),
            makeProvider("p5", "Company",employerId = null)  // the company itself
        )
        coEvery { providerRepository.searchProviders(any(), any()) } returns Result.Success(cleaners)
        coEvery { bookingRepository.getProviderBookings(any()) } returns Result.Success(emptyList())

        viewModel.loadDashboard()
        advanceUntilIdle()

        // Only p1 and p2 have employerId == "p5"
        assertEquals(2, viewModel.dashboardState.value.teamSize)
    }

    @Test
    fun `loadDashboard sums pending bookings across all team members`() = runTest {
        val cleaners = listOf(
            makeProvider("p1", "Maria", employerId = "p5"),
            makeProvider("p2", "James", employerId = "p5")
        )
        coEvery { providerRepository.searchProviders(any(), any()) } returns Result.Success(cleaners)
        coEvery { bookingRepository.getProviderBookings("p1") } returns Result.Success(listOf(
            makeBooking("b1", "p1", BookingStatus.PENDING),
            makeBooking("b2", "p1", BookingStatus.PENDING)
        ))
        coEvery { bookingRepository.getProviderBookings("p2") } returns Result.Success(listOf(
            makeBooking("b3", "p2", BookingStatus.PENDING)
        ))

        viewModel.loadDashboard()
        advanceUntilIdle()

        assertEquals(3, viewModel.dashboardState.value.pendingCount)
    }

    @Test
    fun `loadTeam returns team members with booking counts`() = runTest {
        val cleaners = listOf(
            makeProvider("p1", "Maria", employerId = "p5"),
            makeProvider("p2", "James", employerId = "p5")
        )
        coEvery { providerRepository.searchProviders(any(), any()) } returns Result.Success(cleaners)
        coEvery { bookingRepository.getProviderBookings("p1") } returns Result.Success(listOf(
            makeBooking("b1", "p1", BookingStatus.ACCEPTED),
            makeBooking("b2", "p1", BookingStatus.PENDING)
        ))
        coEvery { bookingRepository.getProviderBookings("p2") } returns Result.Success(emptyList())

        viewModel.loadTeam()
        advanceUntilIdle()

        val team = viewModel.teamState.value.members
        assertEquals(2, team.size)

        val maria = team.find { it.provider.name == "Maria" }!!
        assertEquals(1, maria.activeJobCount)
        assertEquals(1, maria.pendingRequestCount)
    }

    @Test
    fun `loadBookings separates pending, active, and completed bookings`() = runTest {
        val cleaners = listOf(makeProvider("p1", "Maria", employerId = "p5"))
        coEvery { providerRepository.searchProviders(any(), any()) } returns Result.Success(cleaners)
        coEvery { bookingRepository.getProviderBookings("p1") } returns Result.Success(listOf(
            makeBooking("b1", "p1", BookingStatus.PENDING),
            makeBooking("b2", "p1", BookingStatus.ACCEPTED),
            makeBooking("b3", "p1", BookingStatus.COMPLETED),
            makeBooking("b4", "p1", BookingStatus.IN_PROGRESS)
        ))
        coEvery { serviceRepository.getService(any()) } returns Result.Error(Exception())
        coEvery { clientRepository.getClient(any()) }  returns Result.Error(Exception())

        viewModel.loadBookings()
        advanceUntilIdle()

        val bs = viewModel.bookingsState.value
        assertEquals(1, bs.pending.size)
        assertEquals(2, bs.active.size)     // ACCEPTED + IN_PROGRESS
        assertEquals(1, bs.completed.size)
    }
}
