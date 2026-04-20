package com.example.diamonds.ui.support

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.CancellationReason
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.ISupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CancelBookingUiState(
    val booking: Booking? = null,
    val selectedReason: CancellationReason? = null,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isCancelled: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CancelBookingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingRepository: IBookingRepository,
    private val supportRepository: ISupportRepository
) : ViewModel() {

    private val bookingId: String = savedStateHandle["bookingId"] ?: ""

    private val _state = MutableStateFlow(CancelBookingUiState(isLoading = true))
    val state: StateFlow<CancelBookingUiState> = _state.asStateFlow()

    init {
        loadBooking()
    }

    private fun loadBooking() {
        viewModelScope.launch {
            when (val r = bookingRepository.getBooking(bookingId)) {
                is Result.Success -> _state.value =
                    _state.value.copy(booking = r.data, isLoading = false)

                is Result.Error -> _state.value =
                    _state.value.copy(error = r.exception.message, isLoading = false)

                is Result.Loading -> {}
            }
        }
    }

    fun onReasonSelected(reason: CancellationReason) {
        _state.value = _state.value.copy(selectedReason = reason, error = null)
    }

    fun onNotesChanged(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }

    fun confirmCancellation() {
        val reason = _state.value.selectedReason ?: run {
            _state.value = _state.value.copy(error = "Please select a reason")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val r = supportRepository.cancelBookingWithReason(
                bookingId,
                reason,
                _state.value.notes.ifBlank { null })) {
                is Result.Success -> _state.value =
                    _state.value.copy(isCancelled = true, isLoading = false)

                is Result.Error -> _state.value =
                    _state.value.copy(error = r.exception.message, isLoading = false)

                is Result.Loading -> {}
            }
        }
    }
}
