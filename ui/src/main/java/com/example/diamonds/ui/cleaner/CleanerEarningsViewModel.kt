package com.example.diamonds.ui.cleaner

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.IClientRepository
import com.example.diamonds.domain.repository.IServiceRepository
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class CleanerEarningsUiState(
    val isLoading: Boolean = false,
    val weekTotal: Double = 0.0,
    val weekCompletedCount: Int = 0,
    val monthTotal: Double = 0.0,
    val monthCompletedCount: Int = 0,
    /**
     * (day abbreviation, total earnings) for the last 7 days, oldest → newest.
     * e.g. listOf("Mon" to 79.0, "Tue" to 0.0, …)
     */
    val dailyTotals: List<Pair<String, Double>> = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CleanerEarningsViewModel @Inject constructor(
    private val bookingRepository: IBookingRepository,
    private val serviceRepository: IServiceRepository,
    private val clientRepository: IClientRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    private val _earningsState = MutableStateFlow(CleanerEarningsUiState())
    val earningsState: StateFlow<CleanerEarningsUiState> = _earningsState.asStateFlow()

    private suspend fun myProviderId(): String? =
        authRepository.getCurrentUserSession().first()?.userId

    fun loadEarnings() {
        viewModelScope.launch {
            _earningsState.value = CleanerEarningsUiState(isLoading = true)
            clearError()

            val pid = myProviderId() ?: run {
                setError("Not signed in")
                _earningsState.value = CleanerEarningsUiState()
                return@launch
            }

            when (val r = bookingRepository.getProviderBookings(pid)) {
                is Result.Success -> {
                    val all = r.data
                    val completed = all.filter { it.status == BookingStatus.COMPLETED }
                        .sortedByDescending { it.scheduledDate }

                    val today      = LocalDate.now()
                    val weekStart  = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                    val monthStart = today.withDayOfMonth(1)

                    val weekTotal  = completed
                        .filter { it.scheduledDate >= weekStart.toString() }
                        .sumOf { it.totalPrice }
                    val weekCount  = completed.count { it.scheduledDate >= weekStart.toString() }

                    val monthTotal = completed
                        .filter { it.scheduledDate >= monthStart.toString() }
                        .sumOf { it.totalPrice }
                    val monthCount = completed.count { it.scheduledDate >= monthStart.toString() }

                    // Build last 7 days bar chart data
                    val dailyTotals = (6 downTo 0).map { daysAgo ->
                        val date    = today.minusDays(daysAgo.toLong())
                        val dayAbbr = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        val total   = completed
                            .filter { it.scheduledDate == date.toString() }
                            .sumOf { it.totalPrice }
                        dayAbbr to total
                    }

                    _earningsState.value = CleanerEarningsUiState(
                        weekTotal           = weekTotal,
                        weekCompletedCount  = weekCount,
                        monthTotal          = monthTotal,
                        monthCompletedCount = monthCount,
                        dailyTotals = dailyTotals
                    )
                }
                is Result.Error -> {
                    _earningsState.value = CleanerEarningsUiState()
                    setError(r.exception.message ?: "Failed to load earnings")
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun refreshEarnings() {
        loadEarnings()
    }
}
