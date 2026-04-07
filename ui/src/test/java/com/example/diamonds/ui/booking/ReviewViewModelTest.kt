package com.example.diamonds.ui.booking

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
class ReviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var reviewRepository: IReviewRepository
    private lateinit var bookingRepository: IBookingRepository
    private lateinit var providerRepository: IProviderRepository
    private lateinit var serviceRepository: IServiceRepository
    private lateinit var authRepository: IAuthRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var viewModel: ReviewViewModel

    private val mockSession = UserSession(
        userId = "c1", email = "customer@test.com", role = UserRole.CUSTOMER,
        authToken = "tok", isAuthenticated = true
    )

    private fun makeBooking() = Booking(
        id = "b1", clientId = "c1", providerId = "p1", serviceId = "s1",
        status = BookingStatus.COMPLETED, scheduledDate = "2026-04-01", scheduledTime = "10:00",
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
        basePrice = 149.0, duration = 240, category = ServiceCategory.DEEP_CLEANING,
        isActive = true, createdAt = "2023-01-01", updatedAt = "2026-01-01"
    )
    private fun makeReview() = Review(
        id = "rv1", bookingId = "b1", clientId = "c1", providerId = "p1",
        rating = 5, comment = "Excellent!", createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    @Before
    fun setUp() {
        reviewRepository   = mockk()
        bookingRepository  = mockk()
        providerRepository = mockk()
        serviceRepository  = mockk()
        authRepository     = mockk()
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns flowOf(ConnectivityState.ONLINE)
        }
        every { authRepository.getCurrentUserSession() } returns flowOf(mockSession)
        viewModel = ReviewViewModel(
            reviewRepository, bookingRepository, providerRepository,
            serviceRepository, authRepository, connectivityObserver
        )
    }

    // ── Form input ────────────────────────────────────────────────────────────

    @Test
    fun `onRatingChange updates rating in form state`() {
        viewModel.onRatingChange(4)
        assertEquals(4, viewModel.formState.value.rating)
    }

    @Test
    fun `onCommentChange updates comment in form state`() {
        viewModel.onCommentChange("Great service!")
        assertEquals("Great service!", viewModel.formState.value.comment)
    }

    @Test
    fun `clearSubmitSuccess resets submitSuccess flag`() = runTest {
        coEvery { reviewRepository.createReview(any()) } returns Result.Success(makeReview())
        coEvery { bookingRepository.getBooking(any()) }  returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider(any()) } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService(any()) }  returns Result.Success(makeService())
        coEvery { reviewRepository.getReviewsForBooking(any()) } returns Result.Success(null)

        viewModel.loadForBooking("b1")
        advanceUntilIdle()
        viewModel.onRatingChange(5)
        viewModel.submitReview("b1", "p1")
        advanceUntilIdle()

        assertTrue(viewModel.formState.value.submitSuccess)
        viewModel.clearSubmitSuccess()
        assertFalse(viewModel.formState.value.submitSuccess)
    }

    // ── loadForBooking ────────────────────────────────────────────────────────

    @Test
    fun `loadForBooking populates provider and service names`() = runTest {
        coEvery { bookingRepository.getBooking("b1") }   returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }   returns Result.Success(makeService())
        coEvery { reviewRepository.getReviewsForBooking("b1") } returns Result.Success(null)

        viewModel.loadForBooking("b1")
        advanceUntilIdle()

        assertEquals("Maria Garcia", viewModel.formState.value.providerName)
        assertEquals("Deep Clean",   viewModel.formState.value.serviceName)
        assertNull(viewModel.formState.value.existingReview)
    }

    @Test
    fun `loadForBooking populates existingReview when review already submitted`() = runTest {
        coEvery { bookingRepository.getBooking("b1") }   returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }   returns Result.Success(makeService())
        coEvery { reviewRepository.getReviewsForBooking("b1") } returns Result.Success(makeReview())

        viewModel.loadForBooking("b1")
        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.existingReview)
        assertEquals(5, viewModel.formState.value.rating)
        assertEquals("Excellent!", viewModel.formState.value.comment)
    }

    // ── submitReview ──────────────────────────────────────────────────────────

    @Test
    fun `submitReview with rating 0 sets error without calling repository`() = runTest {
        coEvery { bookingRepository.getBooking("b1") }   returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }   returns Result.Success(makeService())
        coEvery { reviewRepository.getReviewsForBooking("b1") } returns Result.Success(null)

        viewModel.loadForBooking("b1")
        advanceUntilIdle()

        viewModel.onRatingChange(0)
        viewModel.submitReview("b1", "p1")
        advanceUntilIdle()

        assertNotNull(viewModel.error.value)
        coVerify(exactly = 0) { reviewRepository.createReview(any()) }
    }

    @Test
    fun `submitReview success sets submitSuccess`() = runTest {
        coEvery { bookingRepository.getBooking("b1") }   returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }   returns Result.Success(makeService())
        coEvery { reviewRepository.getReviewsForBooking("b1") } returns Result.Success(null)
        coEvery { reviewRepository.createReview(any()) } returns Result.Success(makeReview())

        viewModel.loadForBooking("b1")
        advanceUntilIdle()
        viewModel.onRatingChange(5)
        viewModel.onCommentChange("Great!")
        viewModel.submitReview("b1", "p1")
        advanceUntilIdle()

        assertTrue(viewModel.formState.value.submitSuccess)
        assertEquals("rv1", viewModel.formState.value.existingReview?.id)
    }

    @Test
    fun `submitReview error sets error message`() = runTest {
        coEvery { bookingRepository.getBooking("b1") }   returns Result.Success(makeBooking())
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { serviceRepository.getService("s1") }   returns Result.Success(makeService())
        coEvery { reviewRepository.getReviewsForBooking("b1") } returns Result.Success(null)
        coEvery { reviewRepository.createReview(any()) } returns
            Result.Error(Exception("Submission failed"))

        viewModel.loadForBooking("b1")
        advanceUntilIdle()
        viewModel.onRatingChange(4)
        viewModel.submitReview("b1", "p1")
        advanceUntilIdle()

        assertEquals("Submission failed", viewModel.error.value)
        assertFalse(viewModel.formState.value.submitSuccess)
    }

    // ── loadProviderRatings ───────────────────────────────────────────────────

    @Test
    fun `loadProviderRatings computes star counts and average`() = runTest {
        val reviews = listOf(
            makeReview().copy(id = "r1", rating = 5),
            makeReview().copy(id = "r2", rating = 5),
            makeReview().copy(id = "r3", rating = 4),
            makeReview().copy(id = "r4", rating = 3)
        )
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { reviewRepository.getReviewsForProvider("p1") } returns Result.Success(reviews)

        viewModel.loadProviderRatings("p1")
        advanceUntilIdle()

        val state = viewModel.ratingsState.value
        assertEquals(4, state.totalCount)
        assertEquals(2, state.starCounts[4]) // 5-stars count at index 4
        assertEquals(1, state.starCounts[3]) // 4-stars
        assertEquals(1, state.starCounts[2]) // 3-stars
        assertEquals(4.25f, state.averageRating, 0.01f)
    }

    @Test
    fun `loadProviderRatings assigns friendly client display names`() = runTest {
        val review = makeReview().copy(clientId = "client_alice")
        coEvery { providerRepository.getProvider("p1") } returns Result.Success(makeProvider())
        coEvery { reviewRepository.getReviewsForProvider("p1") } returns Result.Success(listOf(review))

        viewModel.loadProviderRatings("p1")
        advanceUntilIdle()

        assertEquals("Alice J.", viewModel.ratingsState.value.reviews[0].clientName)
    }
}
