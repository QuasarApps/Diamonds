package com.example.diamonds.ui.support

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IBookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HelpAction(
    val label: String,
    val icon: String,
    val route: String
)

data class ContextualHelpUiState(
    val booking: Booking? = null,
    val actions: List<HelpAction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ContextualHelpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingRepository: IBookingRepository
) : ViewModel() {

    private val bookingId: String = savedStateHandle["bookingId"] ?: ""

    private val _state = MutableStateFlow(ContextualHelpUiState(isLoading = true))
    val state: StateFlow<ContextualHelpUiState> = _state.asStateFlow()

    init {
        loadBookingAndActions()
    }

    private fun loadBookingAndActions() {
        viewModelScope.launch {
            when (val r = bookingRepository.getBooking(bookingId)) {
                is Result.Success -> {
                    val b = r.data
                    val actions = buildActionsForStatus(b)
                    _state.value =
                        ContextualHelpUiState(booking = b, actions = actions, isLoading = false)
                }

                is Result.Error -> _state.value =
                    ContextualHelpUiState(error = r.exception.message, isLoading = false)

                is Result.Loading -> {}
            }
        }
    }

    private fun buildActionsForStatus(booking: Booking): List<HelpAction> {
        val actions = mutableListOf<HelpAction>()
        when (booking.status) {
            BookingStatus.PENDING -> {
                actions.add(HelpAction("Edit Booking", "✏️", "support/edit/${booking.id}"))
                actions.add(HelpAction("Cancel & Refund", "❌", "support/cancel/${booking.id}"))
                actions.add(HelpAction("Chat with Cleaner", "💬", ""))
                actions.add(HelpAction("FAQ", "❓", "support/help"))
            }

            BookingStatus.ACCEPTED -> {
                actions.add(HelpAction("Cancel & Refund", "❌", "support/cancel/${booking.id}"))
                actions.add(HelpAction("Chat with Cleaner", "💬", ""))
                actions.add(HelpAction("FAQ", "❓", "support/help"))
            }

            BookingStatus.IN_PROGRESS -> {
                actions.add(HelpAction("🚨 Emergency / Safety Help", "🚨", "EMERGENCY"))
                actions.add(HelpAction("Chat with Cleaner", "💬", ""))
                actions.add(HelpAction("Report Issue", "⚠️", "support/claim/${booking.id}"))
            }

            BookingStatus.COMPLETED -> {
                actions.add(HelpAction("File a Claim", "📋", "support/claim/${booking.id}"))
                actions.add(HelpAction("Leave a Review", "⭐", ""))
                actions.add(HelpAction("Contact Support", "📞", "support/help"))
            }

            BookingStatus.CANCELLED -> {
                actions.add(HelpAction("Refund Status", "💰", ""))
                actions.add(HelpAction("Contact Support", "📞", "support/help"))
            }

            BookingStatus.NO_SHOW -> {
                actions.add(HelpAction("File a Claim", "📋", "support/claim/${booking.id}"))
                actions.add(HelpAction("Request Refund", "💰", ""))
                actions.add(HelpAction("Contact Support", "📞", "support/help"))
            }
        }
        return actions
    }
}
