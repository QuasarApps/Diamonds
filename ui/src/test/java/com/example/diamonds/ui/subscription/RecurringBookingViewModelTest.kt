package com.example.diamonds.ui.subscription

import com.example.diamonds.common.util.ConnectivityState
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.RecurringBooking
import com.example.diamonds.domain.model.RecurringBookingStatus
import com.example.diamonds.domain.model.RecurringFrequency
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.ISubscriptionRepository
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import com.example.diamonds.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringBookingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var subscriptionRepository: ISubscriptionRepository
    private lateinit var authRepository: IAuthRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var viewModel: RecurringBookingViewModel

    private val mockSession = UserSession(
        userId = "c1", email = "customer@test.com", role = UserRole.CUSTOMER,
        authToken = "tok", isAuthenticated = true
    )

    private fun makeRecurringBooking(id: String = "rb1") = RecurringBooking(
        id = id, clientId = "c1", providerId = "p1", providerName = "Maria",
        serviceId = "s1", serviceName = "Deep Clean",
        frequency = RecurringFrequency.WEEKLY, preferredDay = 1, preferredTime = "09:00",
        address = "1 Test St", totalPrice = 79.0,
        status = RecurringBookingStatus.ACTIVE,
        nextBookingDate = "2026-05-05",
        createdAt = "2026-04-01", updatedAt = "2026-04-01"
    )

    @Before
    fun setUp() {
        subscriptionRepository = mockk(relaxed = true)
        authRepository = mockk {
            every { getCurrentUserSession() } returns flowOf(mockSession)
        }
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns flowOf(ConnectivityState.ONLINE)
            every { isOnline() } returns true
        }
        every {
            subscriptionRepository.observeRecurringBookingsForClient(any())
        } returns flowOf(emptyList())

        viewModel = RecurringBookingViewModel(
            subscriptionRepository, authRepository, connectivityObserver
        )
    }

    // ── initSetup ─────────────────────────────────────────────────────────────

    @Test
    fun `initSetup populates setup state fields`() {
        viewModel.initSetup("p1", "s1", "Maria", "Deep Clean", 79.0)

        val state = viewModel.uiState.value
        assertEquals("p1", state.providerId)
        assertEquals("s1", state.serviceId)
        assertEquals("Maria", state.providerName)
        assertEquals("Deep Clean", state.serviceName)
        assertEquals(79.0, state.totalPrice, 0.01)
    }

    // ── input handlers ────────────────────────────────────────────────────────

    @Test
    fun `onFrequencyChanged updates frequency`() {
        viewModel.onFrequencyChanged(RecurringFrequency.MONTHLY)
        assertEquals(RecurringFrequency.MONTHLY, viewModel.uiState.value.frequency)
    }

    @Test
    fun `onDayChanged updates preferredDay`() {
        viewModel.onDayChanged(3)
        assertEquals(3, viewModel.uiState.value.preferredDay)
    }

    @Test
    fun `onTimeChanged updates preferredTime`() {
        viewModel.onTimeChanged("14:00")
        assertEquals("14:00", viewModel.uiState.value.preferredTime)
    }

    @Test
    fun `onAddressChanged updates address`() {
        viewModel.onAddressChanged("99 Maple Ave")
        assertEquals("99 Maple Ave", viewModel.uiState.value.address)
    }

    // ── submitRecurringBooking ────────────────────────────────────────────────

    @Test
    fun `submitRecurringBooking with blank address sets errorMessage`() = runTest {
        viewModel.initSetup("p1", "s1", "Maria", "Deep Clean", 79.0)
        // address is blank by default

        viewModel.submitRecurringBooking()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Address"))
        coVerify(exactly = 0) { subscriptionRepository.createRecurringBooking(any()) }
    }

    @Test
    fun `submitRecurringBooking success sets isSuccess true`() = runTest {
        viewModel.initSetup("p1", "s1", "Maria", "Deep Clean", 79.0)
        viewModel.onAddressChanged("1 Test St")
        coEvery {
            subscriptionRepository.createRecurringBooking(any())
        } returns Result.Success(makeRecurringBooking())

        viewModel.submitRecurringBooking()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `submitRecurringBooking failure sets errorMessage`() = runTest {
        viewModel.initSetup("p1", "s1", "Maria", "Deep Clean", 79.0)
        viewModel.onAddressChanged("1 Test St")
        coEvery {
            subscriptionRepository.createRecurringBooking(any())
        } returns Result.Error(Exception("No internet connection"))

        viewModel.submitRecurringBooking()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `submitRecurringBooking proceeds when session flow never completes`() =
        // Real-time cap (5s): a reintroduced hang fails fast instead of the default ~60s.
        runTest(timeout = 5.seconds) {
            // Regression for the infinite-suspend bug: getCurrentUserSession() is a hot,
            // never-completing flow (DataStore). The old collect{ return@collect } helper
            // suspended forever, so submit never reached createRecurringBooking. With
            // .first() the submit proceeds even on a flow that never closes.
            every { authRepository.getCurrentUserSession() } returns MutableStateFlow(mockSession)
            viewModel.initSetup("p1", "s1", "Maria", "Deep Clean", 79.0)
            viewModel.onAddressChanged("1 Test St")
            coEvery {
                subscriptionRepository.createRecurringBooking(any())
            } returns Result.Success(makeRecurringBooking())

            viewModel.submitRecurringBooking()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSuccess)
            coVerify { subscriptionRepository.createRecurringBooking(any()) }
        }

    @Test
    fun `submitRecurringBooking with null session surfaces error and clears submitting`() = runTest {
        // .first() can legitimately emit null (logged out / DataStore not yet loaded);
        // the submit must not leave a stuck "submitting" spinner.
        every { authRepository.getCurrentUserSession() } returns flowOf<UserSession?>(null)
        viewModel.initSetup("p1", "s1", "Maria", "Deep Clean", 79.0)
        viewModel.onAddressChanged("1 Test St")

        viewModel.submitRecurringBooking()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals("Session not found", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { subscriptionRepository.createRecurringBooking(any()) }
    }

    // ── loadRecurringBookings ─────────────────────────────────────────────────

    @Test
    fun `loadRecurringBookings populates mgmtState with bookings`() = runTest {
        val bookings = listOf(makeRecurringBooking())
        every {
            subscriptionRepository.observeRecurringBookingsForClient("c1")
        } returns flowOf(bookings)
        coEvery {
            subscriptionRepository.getRecurringBookingsForClient("c1")
        } returns Result.Success(bookings)

        viewModel.loadRecurringBookings()
        advanceUntilIdle()

        val state = viewModel.mgmtState.value
        assertEquals(1, state.recurringBookings.size)
        assertEquals("rb1", state.recurringBookings.first().id)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadRecurringBookings with null session surfaces error and clears loading`() = runTest {
        // mgmtState.isLoading defaults to true, so a null session must reset it or the
        // screen shows a permanent spinner.
        every { authRepository.getCurrentUserSession() } returns flowOf<UserSession?>(null)

        viewModel.loadRecurringBookings()
        advanceUntilIdle()

        assertFalse(viewModel.mgmtState.value.isLoading)
        assertEquals("Session not found", viewModel.mgmtState.value.errorMessage)
    }

    // ── pause / resume / cancel ───────────────────────────────────────────────

    @Test
    fun `pauseRecurringBooking calls updateRecurringBookingStatus with PAUSED`() = runTest {
        coEvery {
            subscriptionRepository.updateRecurringBookingStatus(
                "rb1",
                RecurringBookingStatus.PAUSED
            )
        } returns Result.Success(makeRecurringBooking().copy(status = RecurringBookingStatus.PAUSED))

        viewModel.pauseRecurringBooking("rb1")
        advanceUntilIdle()

        coVerify {
            subscriptionRepository.updateRecurringBookingStatus(
                "rb1",
                RecurringBookingStatus.PAUSED
            )
        }
    }

    @Test
    fun `resumeRecurringBooking calls updateRecurringBookingStatus with ACTIVE`() = runTest {
        coEvery {
            subscriptionRepository.updateRecurringBookingStatus(
                "rb1",
                RecurringBookingStatus.ACTIVE
            )
        } returns Result.Success(makeRecurringBooking())

        viewModel.resumeRecurringBooking("rb1")
        advanceUntilIdle()

        coVerify {
            subscriptionRepository.updateRecurringBookingStatus(
                "rb1",
                RecurringBookingStatus.ACTIVE
            )
        }
    }

    @Test
    fun `cancelRecurringBooking calls updateRecurringBookingStatus with CANCELLED`() = runTest {
        coEvery {
            subscriptionRepository.updateRecurringBookingStatus(
                "rb1", RecurringBookingStatus.CANCELLED
            )
        } returns Result.Success(
            makeRecurringBooking().copy(status = RecurringBookingStatus.CANCELLED)
        )

        viewModel.cancelRecurringBooking("rb1")
        advanceUntilIdle()

        coVerify {
            subscriptionRepository.updateRecurringBookingStatus(
                "rb1", RecurringBookingStatus.CANCELLED
            )
        }
    }
}
