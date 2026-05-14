package com.example.diamonds.ui.support

import androidx.lifecycle.SavedStateHandle
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.CancellationReason
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.ISupportRepository
import com.example.diamonds.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CancelBookingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bookingRepository: IBookingRepository
    private lateinit var supportRepository: ISupportRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: CancelBookingViewModel

    private fun makeBooking(status: BookingStatus = BookingStatus.PENDING) = Booking(
        id = "b1", clientId = "c1", providerId = "p1", serviceId = "s1",
        status = status, scheduledDate = "2026-05-01", scheduledTime = "10:00",
        estimatedDuration = 120, totalPrice = 79.0, address = "1 Test St",
        createdAt = "2026-04-01", updatedAt = "2026-04-01"
    )

    private fun makeCancelledBooking() = makeBooking(BookingStatus.CANCELLED)

    @Before
    fun setUp() {
        bookingRepository = mockk()
        supportRepository = mockk()
        savedStateHandle = SavedStateHandle(mapOf("bookingId" to "b1"))
    }

    private fun createViewModel(): CancelBookingViewModel {
        return CancelBookingViewModel(savedStateHandle, bookingRepository, supportRepository)
    }

    // ── init / loadBooking ────────────────────────────────────────────────────

    @Test
    fun `init loads booking and sets it in state`() = runTest {
        coEvery { bookingRepository.getBooking("b1") } returns Result.Success(makeBooking())

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.booking)
        assertEquals("b1", state.booking!!.id)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `init sets error message when booking load fails`() = runTest {
        coEvery { bookingRepository.getBooking("b1") } returns
                Result.Error(Exception("Booking not found"))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.booking)
        assertFalse(state.isLoading)
        assertEquals("Booking not found", state.error)
    }

    // ── onReasonSelected ─────────────────────────────────────────────────────

    @Test
    fun `onReasonSelected updates selectedReason`() = runTest {
        coEvery { bookingRepository.getBooking("b1") } returns Result.Success(makeBooking())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onReasonSelected(CancellationReason.SCHEDULING_CONFLICT)

        assertEquals(CancellationReason.SCHEDULING_CONFLICT, viewModel.state.value.selectedReason)
        assertNull(viewModel.state.value.error)
    }

    // ── onNotesChanged ────────────────────────────────────────────────────────

    @Test
    fun `onNotesChanged updates notes`() = runTest {
        coEvery { bookingRepository.getBooking("b1") } returns Result.Success(makeBooking())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNotesChanged("Provider was rude")

        assertEquals("Provider was rude", viewModel.state.value.notes)
    }

    // ── confirmCancellation ───────────────────────────────────────────────────

    @Test
    fun `confirmCancellation without reason sets error`() = runTest {
        coEvery { bookingRepository.getBooking("b1") } returns Result.Success(makeBooking())
        viewModel = createViewModel()
        advanceUntilIdle()

        // No reason selected
        viewModel.confirmCancellation()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isCancelled)
        assertEquals("Please select a reason", viewModel.state.value.error)
        coVerify(exactly = 0) { supportRepository.cancelBookingWithReason(any(), any(), any()) }
    }

    @Test
    fun `confirmCancellation succeeds and sets isCancelled true`() = runTest {
        coEvery { bookingRepository.getBooking("b1") } returns Result.Success(makeBooking())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onReasonSelected(CancellationReason.CHANGED_MIND)
        coEvery {
            supportRepository.cancelBookingWithReason("b1", CancellationReason.CHANGED_MIND, null)
        } returns Result.Success(makeCancelledBooking())

        viewModel.confirmCancellation()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isCancelled)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `confirmCancellation passes non-blank notes to repository`() = runTest {
        coEvery { bookingRepository.getBooking("b1") } returns Result.Success(makeBooking())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onReasonSelected(CancellationReason.FOUND_ANOTHER)
        viewModel.onNotesChanged("Found a cheaper option")
        coEvery {
            supportRepository.cancelBookingWithReason(
                "b1", CancellationReason.FOUND_ANOTHER, "Found a cheaper option"
            )
        } returns Result.Success(makeCancelledBooking())

        viewModel.confirmCancellation()
        advanceUntilIdle()

        coVerify {
            supportRepository.cancelBookingWithReason(
                "b1", CancellationReason.FOUND_ANOTHER, "Found a cheaper option"
            )
        }
        assertTrue(viewModel.state.value.isCancelled)
    }

    @Test
    fun `confirmCancellation sets error on repository failure`() = runTest {
        coEvery { bookingRepository.getBooking("b1") } returns Result.Success(makeBooking())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onReasonSelected(CancellationReason.PRICE_ISSUE)
        coEvery {
            supportRepository.cancelBookingWithReason(any(), any(), any())
        } returns Result.Error(Exception("Cancellation not allowed"))

        viewModel.confirmCancellation()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isCancelled)
        assertEquals("Cancellation not allowed", viewModel.state.value.error)
    }

    @Test
    fun `confirmCancellation treats blank notes as null`() = runTest {
        coEvery { bookingRepository.getBooking("b1") } returns Result.Success(makeBooking())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onReasonSelected(CancellationReason.OTHER)
        viewModel.onNotesChanged("   ") // whitespace-only
        coEvery {
            supportRepository.cancelBookingWithReason("b1", CancellationReason.OTHER, null)
        } returns Result.Success(makeCancelledBooking())

        viewModel.confirmCancellation()
        advanceUntilIdle()

        coVerify {
            supportRepository.cancelBookingWithReason("b1", CancellationReason.OTHER, null)
        }
    }
}
