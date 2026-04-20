package com.example.diamonds.ui.support

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.Claim
import com.example.diamonds.domain.model.ClaimType
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.ISupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileClaimUiState(
    val booking: Booking? = null,
    val selectedClaimType: ClaimType? = null,
    val description: String = "",
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val submittedClaim: Claim? = null,
    val error: String? = null
)

data class ClaimDetailUiState(
    val claim: Claim? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FileClaimViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingRepository: IBookingRepository,
    private val supportRepository: ISupportRepository
) : ViewModel() {

    private val bookingId: String = savedStateHandle["bookingId"] ?: ""

    private val _state = MutableStateFlow(FileClaimUiState(isLoading = true))
    val state: StateFlow<FileClaimUiState> = _state.asStateFlow()

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

    fun onClaimTypeSelected(type: ClaimType) {
        _state.value = _state.value.copy(selectedClaimType = type, error = null)
    }

    fun onDescriptionChanged(desc: String) {
        _state.value = _state.value.copy(description = desc)
    }

    fun submitClaim(userId: String, role: String) {
        val type = _state.value.selectedClaimType ?: run {
            _state.value = _state.value.copy(error = "Please select a claim type")
            return
        }
        if (_state.value.description.isBlank()) {
            _state.value = _state.value.copy(error = "Please describe the issue")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())
            val claim = Claim(
                id = "", bookingId = bookingId, filedByUserId = userId, filedByRole = role,
                claimType = type, description = _state.value.description,
                createdAt = now, updatedAt = now
            )
            when (val r = supportRepository.fileClaim(claim)) {
                is Result.Success -> _state.value = _state.value.copy(
                    isSubmitted = true, submittedClaim = r.data, isLoading = false
                )

                is Result.Error -> _state.value =
                    _state.value.copy(error = r.exception.message, isLoading = false)

                is Result.Loading -> {}
            }
        }
    }
}

@HiltViewModel
class ClaimDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val supportRepository: ISupportRepository
) : ViewModel() {

    private val claimId: String = savedStateHandle["claimId"] ?: ""

    private val _state = MutableStateFlow(ClaimDetailUiState(isLoading = true))
    val state: StateFlow<ClaimDetailUiState> = _state.asStateFlow()

    init {
        loadClaim()
    }

    private fun loadClaim() {
        viewModelScope.launch {
            when (val r = supportRepository.getClaim(claimId)) {
                is Result.Success -> _state.value =
                    ClaimDetailUiState(claim = r.data, isLoading = false)

                is Result.Error -> _state.value =
                    ClaimDetailUiState(error = r.exception.message, isLoading = false)

                is Result.Loading -> {}
            }
        }
    }
}
