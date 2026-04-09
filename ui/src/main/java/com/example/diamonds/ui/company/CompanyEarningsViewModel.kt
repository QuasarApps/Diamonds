package com.example.diamonds.ui.company

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.IProviderRepository
import com.example.diamonds.domain.repository.IServiceRepository
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class CleanerRevenueRow(
    val cleanerName: String,
    val jobCount: Int,
    val revenue: Double
)

data class CompanyEarningsUiState(
    val isLoading: Boolean = false,
    val weekTotal: Double = 0.0,
    val weekJobCount: Int = 0,
    val monthTotal: Double = 0.0,
    val monthJobCount: Int = 0,
    val teamSize: Int = 0,
    val cleanerBreakdown: List<CleanerRevenueRow> = emptyList(),
    val recentCompleted: List<CompanyBookingItem> = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CompanyEarningsViewModel @Inject constructor(
    private val bookingRepository: IBookingRepository,
    private val providerRepository: IProviderRepository,
    private val serviceRepository: IServiceRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    private val _earningsState = MutableStateFlow(CompanyEarningsUiState())
    val earningsState: StateFlow<CompanyEarningsUiState> = _earningsState.asStateFlow()

    private suspend fun companyId(): String? =
        authRepository.getCurrentUserSession().first()?.userId

    fun loadEarnings() {
        viewModelScope.launch {
            _earningsState.value = CompanyEarningsUiState(isLoading = true)
            clearError()

            val cid = companyId() ?: run {
                _earningsState.value = CompanyEarningsUiState()
                return@launch
            }

            // Fetch all employed cleaners
            val cleaners = run {
                val r = providerRepository.searchProviders(0.0, 0.0)
                if (r is Result.Success) r.data.filter { it.employerId == cid }
                else emptyList()
            }
            val cleanerMap = cleaners.associateBy { it.id }

            // Fetch all bookings across all employed cleaners
            val allBookings = cleaners.flatMap { cleaner ->
                val r = bookingRepository.getProviderBookings(cleaner.id)
                if (r is Result.Success) r.data else emptyList()
            }

            val completed = allBookings.filter { it.status == BookingStatus.COMPLETED }
                .sortedByDescending { it.scheduledDate }

            val today      = LocalDate.now()
            val weekStart  = today.minusDays(today.dayOfWeek.value.toLong() - 1).toString()
            val monthStart = today.withDayOfMonth(1).toString()

            val weekCompleted  = completed.filter { it.scheduledDate >= weekStart }
            val monthCompleted = completed.filter { it.scheduledDate >= monthStart }

            // Per-cleaner breakdown for this month
            val breakdown = cleanerMap.entries.mapNotNull { (pid, provider) ->
                val rows = monthCompleted.filter { it.providerId == pid }
                if (rows.isEmpty()) null
                else CleanerRevenueRow(
                    cleanerName = provider.name,
                    jobCount    = rows.size,
                    revenue     = rows.sumOf { it.totalPrice }
                )
            }.sortedByDescending { it.revenue }

            // Enrich recent completed for display (take 20)
            val enriched = completed.take(20).map { booking ->
                val service = (serviceRepository.getService(booking.serviceId) as? Result.Success)?.data
                CompanyBookingItem(
                    booking      = booking,
                    clientName   = booking.clientId,
                    cleanerName  = cleanerMap[booking.providerId]?.name ?: booking.providerId,
                    serviceName  = service?.title    ?: booking.serviceId,
                    servicePrice = service?.basePrice ?: booking.totalPrice
                )
            }

            _earningsState.value = CompanyEarningsUiState(
                weekTotal        = weekCompleted.sumOf { it.totalPrice },
                weekJobCount     = weekCompleted.size,
                monthTotal       = monthCompleted.sumOf { it.totalPrice },
                monthJobCount    = monthCompleted.size,
                teamSize         = cleaners.size,
                cleanerBreakdown = breakdown,
                recentCompleted  = enriched
            )
        }
    }

    fun refreshEarnings() {
        loadEarnings()
    }
}
