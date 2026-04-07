package com.example.diamonds.ui.payment

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

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var paymentRepository: IPaymentRepository
    private lateinit var bookingRepository: IBookingRepository
    private lateinit var providerRepository: IProviderRepository
    private lateinit var serviceRepository: IServiceRepository
    private lateinit var authRepository: IAuthRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var viewModel: PaymentViewModel

    private val mockSession = UserSession(
        userId = "c1", email = "customer@test.com", role = UserRole.CUSTOMER,
        authToken = "tok", isAuthenticated = true
    )

    private fun makeBooking() = Booking(
        id = "b1", clientId = "c1", providerId = "p1", serviceId = "s1",
        status = BookingStatus.PENDING, scheduledDate = "2026-05-01", scheduledTime = "10:00",
        estimatedDuration = 120, totalPrice = 79.0, address = "1 Test St",
        createdAt = "2026-04-01", updatedAt = "2026-04-01"
    )
    private fun makeProvider() = Provider(
        id = "p1", name = "Maria Garcia", email = "m@c.com", phoneNumber = "+1",
        rating = 4.9f, reviewCount = 143, verificationStatus = VerificationStatus.APPROVED,
        cleanerType = CleanerType.INDEPENDENT, createdAt = "2023-01-01", updatedAt = "2026-01-01"
    )
    private fun makeService() = Service(
        id = "s1", providerId = "p1", title = "Deep Clean", description = "Full clean",
        basePrice = 79.0, duration = 120, category = ServiceCategory.DEEP_CLEANING,
        isActive = true, createdAt = "2023-01-01", updatedAt = "2026-01-01"
    )
    private fun makePayment() = Payment(
        id = "pay1", bookingId = "b1", clientId = "c1", providerId = "p1",
        amount = 79.0, status = PaymentStatus.SUCCEEDED, method = PaymentMethod.CARD,
        transactionId = "txn_001", createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    @Before
    fun setUp() {
        paymentRepository  = mockk()
        bookingRepository  = mockk()
        providerRepository = mockk()
        serviceRepository  = mockk()
        authRepository     = mockk()
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns flowOf(ConnectivityState.ONLINE)
        }
        every { authRepository.getCurrentUserSession() } returns flowOf(mockSession)
        viewModel = PaymentViewModel(
            paymentRepository, bookingRepository, providerRepository,
            serviceRepository, authRepository, connectivityObserver
        )
    }

    // ── Card input formatting ─────────────────────────────────────────────────

    @Test
    fun `onCardNumberChange formats number as 4-digit groups`() {
        viewModel.onCardNumberChange("1234567890123456")
        assertEquals("1234 5678 9012 3456", viewModel.paymentState.value.cardNumber)
    }

    @Test
    fun `onCardNumberChange truncates beyond 16 digits`() {
        viewModel.onCardNumberChange("12345678901234567890")
        val digits = viewModel.paymentState.value.cardNumber.filter { it.isDigit() }
        assertEquals(16, digits.length)
    }

    @Test
    fun `onCardHolderChange uppercases input`() {
        viewModel.onCardHolderChange("alice smith")
        assertEquals("ALICE SMITH", viewModel.paymentState.value.cardHolder)
    }

    @Test
    fun `onExpiryChange inserts slash after 2 digits`() {
        viewModel.onExpiryChange("1225")
        assertEquals("12/25", viewModel.paymentState.value.expiry)
    }

    @Test
    fun `onExpiryChange handles partial input without slash`() {
        viewModel.onExpiryChange("12")
        assertEquals("12", viewModel.paymentState.value.expiry)
    }

    @Test
    fun `onCvvChange limits to 4 digits`() {
        viewModel.onCvvChange("12345")
        assertEquals("1234", viewModel.paymentState.value.cvv)
    }

    @Test
    fun `onCardNumberChange clears previous cardNumber error`() = runTest {
        // Trigger a validation error first
        viewModel.processPayment("b1") // card is empty → validation fails
        advanceUntilIdle()
        assertNotNull(viewModel.paymentState.value.cardNumberError)

        viewModel.onCardNumberChange("1234567890123456")
        assertNull(viewModel.paymentState.value.cardNumberError)
    }

    // ── loadForBooking ────────────────────────────────────────────────────────

    @Test
    fun `loadForBooking populates service name, provider name, and amount`() = runTest {
        coEvery { bookingRepository.getBooking("b1") }   returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }   returns Result.Success(makeService())

        viewModel.loadForBooking("b1")
        advanceUntilIdle()

        assertEquals("Deep Clean",   viewModel.paymentState.value.serviceName)
        assertEquals("Maria Garcia", viewModel.paymentState.value.providerName)
        assertEquals(79.0,           viewModel.paymentState.value.amount, 0.01)
    }

    // ── processPayment validation ─────────────────────────────────────────────

    @Test
    fun `processPayment with short card number sets cardNumberError`() = runTest {
        viewModel.onCardNumberChange("1234")
        viewModel.onCardHolderChange("ALICE SMITH")
        viewModel.onExpiryChange("1225")
        viewModel.onCvvChange("123")

        viewModel.processPayment("b1")
        advanceUntilIdle()

        assertNotNull(viewModel.paymentState.value.cardNumberError)
        coVerify(exactly = 0) { paymentRepository.createPayment(any()) }
    }

    @Test
    fun `processPayment with blank holder sets cardHolderError`() = runTest {
        viewModel.onCardNumberChange("1234567890123456")
        viewModel.onCardHolderChange("")
        viewModel.onExpiryChange("1225")
        viewModel.onCvvChange("123")

        viewModel.processPayment("b1")
        advanceUntilIdle()

        assertNotNull(viewModel.paymentState.value.cardHolderError)
    }

    @Test
    fun `processPayment with invalid month sets expiryError`() = runTest {
        viewModel.onCardNumberChange("1234567890123456")
        viewModel.onCardHolderChange("ALICE SMITH")
        viewModel.onExpiryChange("1325") // month 13 is invalid
        viewModel.onCvvChange("123")

        viewModel.processPayment("b1")
        advanceUntilIdle()

        assertNotNull(viewModel.paymentState.value.expiryError)
    }

    @Test
    fun `processPayment with short CVV sets cvvError`() = runTest {
        viewModel.onCardNumberChange("1234567890123456")
        viewModel.onCardHolderChange("ALICE SMITH")
        viewModel.onExpiryChange("1225")
        viewModel.onCvvChange("12") // too short

        viewModel.processPayment("b1")
        advanceUntilIdle()

        assertNotNull(viewModel.paymentState.value.cvvError)
    }

    @Test
    fun `processPayment success sets paymentSuccess and paymentId`() = runTest {
        coEvery { bookingRepository.getBooking("b1") }   returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }   returns Result.Success(makeService())
        coEvery { paymentRepository.createPayment(any()) } returns Result.Success(makePayment())

        viewModel.loadForBooking("b1")
        advanceUntilIdle()

        viewModel.onCardNumberChange("1234567890123456")
        viewModel.onCardHolderChange("ALICE SMITH")
        viewModel.onExpiryChange("1225")
        viewModel.onCvvChange("123")
        viewModel.processPayment("b1")
        advanceUntilIdle()

        assertTrue(viewModel.paymentState.value.paymentSuccess)
        assertEquals("pay1", viewModel.paymentState.value.paymentId)
    }

    @Test
    fun `processPayment error sets error message`() = runTest {
        coEvery { bookingRepository.getBooking("b1") }   returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }   returns Result.Success(makeService())
        coEvery { paymentRepository.createPayment(any()) } returns
            Result.Error(Exception("Payment declined"))

        viewModel.loadForBooking("b1")
        advanceUntilIdle()

        viewModel.onCardNumberChange("1234567890123456")
        viewModel.onCardHolderChange("ALICE SMITH")
        viewModel.onExpiryChange("1225")
        viewModel.onCvvChange("123")
        viewModel.processPayment("b1")
        advanceUntilIdle()

        assertEquals("Payment declined", viewModel.error.value)
        assertFalse(viewModel.paymentState.value.paymentSuccess)
    }

    // ── loadHistory ───────────────────────────────────────────────────────────

    @Test
    fun `loadHistory fetches and enriches payment history`() = runTest {
        coEvery { paymentRepository.getPaymentsForClient("c1") } returns
            Result.Success(listOf(makePayment()))
        coEvery { bookingRepository.getBooking("b1") }   returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }   returns Result.Success(makeService())

        viewModel.loadHistory()
        advanceUntilIdle()

        assertEquals(1, viewModel.historyState.value.items.size)
        assertEquals("Deep Clean",   viewModel.historyState.value.items[0].serviceName)
        assertEquals("Maria Garcia", viewModel.historyState.value.items[0].providerName)
    }

    @Test
    fun `loadHistory error sets error message`() = runTest {
        coEvery { paymentRepository.getPaymentsForClient("c1") } returns
            Result.Error(Exception("History unavailable"))

        viewModel.loadHistory()
        advanceUntilIdle()

        assertEquals("History unavailable", viewModel.error.value)
        assertTrue(viewModel.historyState.value.items.isEmpty())
    }
}
