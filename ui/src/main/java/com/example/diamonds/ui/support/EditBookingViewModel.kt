package com.example.diamonds.ui.support

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.ISupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditBookingUiState(
    val booking: Booking? = null,
    val address: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditBookingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingRepository: IBookingRepository,
    private val supportRepository: ISupportRepository
) : ViewModel() {

    private val bookingId: String = savedStateHandle["bookingId"] ?: ""

    private val _state = MutableStateFlow(EditBookingUiState(isLoading = true))
    val state: StateFlow<EditBookingUiState> = _state.asStateFlow()

    init {
        loadBooking()
    }

    private fun loadBooking() {
        viewModelScope.launch {
            when (val r = bookingRepository.getBooking(bookingId)) {
                is Result.Success -> _state.value = _state.value.copy(
                    booking = r.data, address = r.data.address, isLoading = false
                )

                is Result.Error -> _state.value =
                    _state.value.copy(error = r.exception.message, isLoading = false)

                is Result.Loading -> {}
            }
        }
    }

    fun onAddressChanged(address: String) {
        _state.value = _state.value.copy(address = address, error = null)
    }

    fun saveChanges() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val newAddr = _state.value.address.ifBlank { null }
            when (val r = supportRepository.editBooking(bookingId, newAddress = newAddr)) {
                is Result.Success -> _state.value =
                    _state.value.copy(isSaved = true, isLoading = false)

                is Result.Error -> _state.value =
                    _state.value.copy(error = r.exception.message, isLoading = false)

                is Result.Loading -> {}
            }
        }
    }
}
