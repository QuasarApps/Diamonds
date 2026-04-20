package com.example.diamonds.ui.booking

import androidx.lifecycle.SavedStateHandle
import com.example.diamonds.common.util.ConnectivityState
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.Service
import com.example.diamonds.domain.model.ServiceCategory
import com.example.diamonds.domain.model.VerificationStatus
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.IProviderRepository
import com.example.diamonds.domain.repository.IReviewRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bookingRepository: IBookingRepository
    private lateinit var providerRepository: IProviderRepository
    private lateinit var serviceRepository: IServiceRepository
    private lateinit var reviewRepository: IReviewRepository
    private lateinit var authRepository: IAuthRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: BookingViewModel

    private val mockSession = UserSession(
        userId = "c1", email = "customer@test.com", role = UserRole.CUSTOMER,
        authToken = "tok", isAuthenticated = true
    )

    private fun makeProvider() = Provider(
        id = "p1", name = "Maria Garcia", email = "m@c.com", phoneNumber = "+1",
        rating = 4.9f, reviewCount = 143, verificationStatus = VerificationStatus.APPROVED,
        cleanerType = CleanerType.INDEPENDENT, createdAt = "2023-01-01", updatedAt = "2026-01-01"
    )

    private fun makeService() = Service(
        id = "s1", providerId = "p1", title = "Deep Clean", description = "Full clean",
        basePrice = 149.0, duration = 240, category = ServiceCategory.DEEP_CLEANING,
        isActive = true, createdAt = "2023-01-01", updatedAt = "2026-01-01"
    )

    private fun makeBooking(status: BookingStatus = BookingStatus.PENDING) = Booking(
        id = "b1", clientId = "c1", providerId = "p1", serviceId = "s1",
        status = status, scheduledDate = "2026-05-01", scheduledTime = "10:00",
        estimatedDuration = 240, totalPrice = 149.0, address = "1 Test St",
        createdAt = "2026-04-01", updatedAt = "2026-04-01"
    )

    @Before
    fun setUp() {
        bookingRepository    = mockk()
        providerRepository   = mockk()
        serviceRepository    = mockk()
        reviewRepository = mockk()
        authRepository       = mockk()
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns flowOf(ConnectivityState.ONLINE)
        }
        savedStateHandle = SavedStateHandle()
        every { authRepository.getCurrentUserSession() } returns flowOf(mockSession)
        viewModel = BookingViewModel(
            bookingRepository, providerRepository, serviceRepository,
            reviewRepository, authRepository, connectivityObserver, savedStateHandle
        )
    }

    // ── loadProviders ─────────────────────────────────────────────────────────

    @Test
    fun `loadProviders success updates providers list`() = runTest {
        val provider = makeProvider()
        coEvery { providerRepository.searchProviders(any(), any()) } returns
            Result.Success(listOf(provider))

        viewModel.loadProviders()
        advanceUntilIdle()

        assertEquals(1, viewModel.searchState.value.providers.size)
        assertEquals("Maria Garcia", viewModel.searchState.value.providers[0].name)
        assertFalse(viewModel.searchState.value.isLoading)
    }

    @Test
    fun `loadProviders error sets error state`() = runTest {
        coEvery { providerRepository.searchProviders(any(), any()) } returns
            Result.Error(Exception("Network error"))

        viewModel.loadProviders()
        advanceUntilIdle()

        assertEquals("Network error", viewModel.error.value)
        assertFalse(viewModel.searchState.value.isLoading)
    }

    // ── loadServicesForProvider ───────────────────────────────────────────────

    @Test
    fun `loadServicesForProvider populates services and provider`() = runTest {
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getServicesForProvider("p1") } returns
            Result.Success(listOf(makeService()))

        viewModel.loadServicesForProvider("p1")
        advanceUntilIdle()

        assertEquals(1, viewModel.serviceListState.value.services.size)
        assertEquals("Deep Clean", viewModel.serviceListState.value.services[0].title)
        assertEquals("Maria Garcia", viewModel.serviceListState.value.provider?.name)
    }

    // ── prepareBookingForm ────────────────────────────────────────────────────

    @Test
    fun `prepareBookingForm loads service and provider`() = runTest {
        coEvery { serviceRepository.getService("s1") }  returns Result.Success(makeService())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())

        viewModel.prepareBookingForm("p1", "s1")
        advanceUntilIdle()

        assertEquals("Deep Clean", viewModel.formState.value.service?.title)
        assertEquals(149.0, viewModel.formState.value.service?.basePrice)
    }

    // ── confirmBooking ────────────────────────────────────────────────────────

    @Test
    fun `confirmBooking with missing date sets fieldError`() = runTest {
        viewModel.onDateChange("")
        viewModel.onTimeChange("10:00")
        viewModel.onAddressChange("123 Test St")

        viewModel.submitBooking()
        advanceUntilIdle()

        assertTrue(viewModel.formState.value.fieldErrors.containsKey(BookingField.DATE))
    }

    @Test
    fun `confirmBooking success sets bookingSuccess and createdBookingId`() = runTest {
        coEvery { serviceRepository.getService("s1") }  returns Result.Success(makeService())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { bookingRepository.createBooking(any()) } returns Result.Success(makeBooking())

        viewModel.prepareBookingForm("p1", "s1")
        advanceUntilIdle()

        viewModel.onDateChange("2026-05-01")
        viewModel.onTimeChange("10:00")
        viewModel.onAddressChange("123 Test St")
        viewModel.submitBooking()
        advanceUntilIdle()

        assertTrue(viewModel.formState.value.bookingSuccess)
        assertEquals("b1", viewModel.formState.value.createdBookingId)
    }

    // ── loadMyBookings ────────────────────────────────────────────────────────

    @Test
    fun `loadMyBookings enriches bookings with provider and service names`() = runTest {
        coEvery { bookingRepository.getClientBookings("c1") } returns
            Result.Success(listOf(makeBooking()))
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }  returns Result.Success(makeService())

        viewModel.loadMyBookings()
        advanceUntilIdle()

        assertEquals(1, viewModel.bookingsListState.value.bookings.size)
        assertEquals("Maria Garcia", viewModel.bookingsListState.value.bookings[0].providerName)
        assertEquals("Deep Clean",   viewModel.bookingsListState.value.bookings[0].serviceName)
    }

    // ── loadBookingDetail ─────────────────────────────────────────────────────

    @Test
    fun `loadBookingDetail populates detail state`() = runTest {
        coEvery { bookingRepository.getBooking("b1") }  returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }  returns Result.Success(makeService())

        viewModel.loadBookingDetail("b1")
        advanceUntilIdle()

        assertEquals("b1", viewModel.detailState.value.booking?.id)
        assertEquals("Maria Garcia", viewModel.detailState.value.provider?.name)
        assertEquals("Deep Clean",   viewModel.detailState.value.service?.title)
    }

    @Test
    fun `loadBookingDetail error sets error message`() = runTest {
        coEvery { bookingRepository.getBooking("b1") } returns
            Result.Error(Exception("Not found"))

        viewModel.loadBookingDetail("b1")
        advanceUntilIdle()

        assertEquals("Not found", viewModel.error.value)
    }

    // ── cancelBooking ─────────────────────────────────────────────────────────

    @Test
    fun `cancelBooking sets cancelSuccess on success`() = runTest {
        coEvery { bookingRepository.cancelBooking("b1") } returns
            Result.Success(makeBooking(BookingStatus.CANCELLED))

        viewModel.cancelBooking("b1")
        advanceUntilIdle()

        assertTrue(viewModel.detailState.value.cancelSuccess)
        assertEquals(BookingStatus.CANCELLED, viewModel.detailState.value.booking?.status)
    }

    @Test
    fun `cancelBooking error sets error message`() = runTest {
        coEvery { bookingRepository.cancelBooking("b1") } returns
            Result.Error(Exception("Could not cancel"))

        viewModel.cancelBooking("b1")
        advanceUntilIdle()

        assertEquals("Could not cancel", viewModel.error.value)
    }
}
