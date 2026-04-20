package com.example.diamonds.ui.cleaner

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Booking
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
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

/** A booking enriched with the client name and service info for display. */
data class CleanerBookingItem(
    val booking: Booking,
    val clientName: String,
    val serviceName: String,
    val servicePrice: Double
)

data class CleanerRequestsUiState(
    val isLoading: Boolean = false,
    val requests: List<CleanerBookingItem> = emptyList()
)

data class CleanerScheduleUiState(
    val isLoading: Boolean = false,
    /** Today's accepted/in-progress jobs. */
    val todayJobs: List<CleanerBookingItem> = emptyList(),
    /** Future (after today) accepted jobs. */
    val upcomingJobs: List<CleanerBookingItem> = emptyList()
)

data class CleanerDashboardUiState(
    val isLoading: Boolean = false,
    val pendingCount: Int = 0,
    val todayCount: Int = 0,
    val weekEarnings: Double = 0.0
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CleanerViewModel @Inject constructor(
    private val bookingRepository: IBookingRepository,
    private val serviceRepository: IServiceRepository,
    private val clientRepository: IClientRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    private val _requestsState = MutableStateFlow(CleanerRequestsUiState())
    val requestsState: StateFlow<CleanerRequestsUiState> = _requestsState.asStateFlow()

    private val _scheduleState = MutableStateFlow(CleanerScheduleUiState())
    val scheduleState: StateFlow<CleanerScheduleUiState> = _scheduleState.asStateFlow()

    private val _dashboardState = MutableStateFlow(CleanerDashboardUiState())
    val dashboardState: StateFlow<CleanerDashboardUiState> = _dashboardState.asStateFlow()

    // ── Load provider id from session ─────────────────────────────────────────

    private suspend fun providerId(): String? =
        authRepository.getCurrentUserSession().first()?.userId

    // ── Booking requests (PENDING) ────────────────────────────────────────────

    fun loadRequests() {
        viewModelScope.launch {
            _requestsState.value = CleanerRequestsUiState(isLoading = true)
            clearError()

            val pid = providerId() ?: run {
                setError("Not signed in")
                _requestsState.value = CleanerRequestsUiState()
                return@launch
            }

            try {
                when (val r = bookingRepository.getProviderBookings(pid)) {
                    is Result.Success -> {
                        val pending = r.data.filter { it.status == BookingStatus.PENDING }
                        _requestsState.value = CleanerRequestsUiState(
                            requests = pending.map { it.enrichSafe() }
                        )
                        clearError()
                    }

                    is Result.Error -> {
                        _requestsState.value = CleanerRequestsUiState()
                        setError(r.exception.message ?: "Could not load requests")
                    }

                    is Result.Loading -> Unit
                }
            } catch (e: Exception) {
                _requestsState.value = CleanerRequestsUiState()
                setError(e.message ?: "Could not load requests")
            }
        }
    }

    // ── Schedule (ACCEPTED / IN_PROGRESS) ─────────────────────────────────────

    fun loadSchedule() {
        viewModelScope.launch {
            _scheduleState.value = CleanerScheduleUiState(isLoading = true)
            clearError()

            val pid = providerId() ?: run {
                setError("Not signed in")
                _scheduleState.value = CleanerScheduleUiState()
                return@launch
            }

            try {
                when (val r = bookingRepository.getProviderBookings(pid)) {
                    is Result.Success -> {
                        val active = r.data.filter {
                            it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS
                        }
                        val today = todayDateString()
                        val todayJobs = active.filter { it.scheduledDate == today }
                        val upcomingJobs = active.filter { it.scheduledDate > today }
                            .sortedWith(compareBy({ it.scheduledDate }, { it.scheduledTime }))
                        val overdueJobs = active.filter { it.scheduledDate < today }
                            .sortedWith(compareByDescending<Booking> { it.scheduledDate }
                                .thenByDescending { it.scheduledTime })

                        _scheduleState.value = CleanerScheduleUiState(
                            todayJobs = (overdueJobs + todayJobs).map { it.enrichSafe() },
                            upcomingJobs = upcomingJobs.map { it.enrichSafe() }
                        )
                    }

                    is Result.Error -> {
                        _scheduleState.value = CleanerScheduleUiState()
                        setError(r.exception.message ?: "Could not load schedule")
                    }

                    is Result.Loading -> Unit
                }
            } catch (e: Exception) {
                _scheduleState.value = CleanerScheduleUiState()
                setError(e.message ?: "Could not load schedule")
            }
        }
    }

    // ── Dashboard summary ─────────────────────────────────────────────────────

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboardState.value = CleanerDashboardUiState(isLoading = true)
            clearError()

            val pid = providerId() ?: run {
                _dashboardState.value = CleanerDashboardUiState()
                return@launch
            }

            try {
                when (val r = bookingRepository.getProviderBookings(pid)) {
                    is Result.Success -> {
                        val all = r.data
                        val today = todayDateString()
                        val pendingCount = all.count { it.status == BookingStatus.PENDING }
                        val todayCount = all.count {
                            it.scheduledDate == today &&
                                    (it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS)
                        }
                        // Earnings this week = completed bookings in last 7 days
                        val weekStart = weekStartDateString()
                        val weekEarnings = all
                            .filter { it.status == BookingStatus.COMPLETED && it.scheduledDate >= weekStart }
                            .sumOf { it.totalPrice }

                        _dashboardState.value = CleanerDashboardUiState(
                            pendingCount = pendingCount,
                            todayCount = todayCount,
                            weekEarnings = weekEarnings
                        )
                    }

                    is Result.Error -> {
                        _dashboardState.value = CleanerDashboardUiState()
                    }

                    is Result.Loading -> Unit
                }
            } catch (_: Exception) {
                _dashboardState.value = CleanerDashboardUiState()
            }
        }
    }

    // ── Accept / Decline ──────────────────────────────────────────────────────

    fun acceptRequest(bookingId: String) = updateStatus(bookingId, BookingStatus.ACCEPTED)
    fun declineRequest(bookingId: String) = updateStatus(bookingId, BookingStatus.CANCELLED)
    fun startJob(bookingId: String)       = updateStatus(bookingId, BookingStatus.IN_PROGRESS)
    fun completeJob(bookingId: String)    = updateStatus(bookingId, BookingStatus.COMPLETED)

    private fun updateStatus(bookingId: String, newStatus: BookingStatus) {
        viewModelScope.launch {
            clearError()
            try {
                when (val r = bookingRepository.updateBookingStatus(bookingId, newStatus)) {
                    is Result.Success -> {
                        // Refresh whichever list is currently displayed.
                        // Each tab screen has its own ViewModel instance (hiltViewModel
                        // is scoped per NavBackStackEntry), so we refresh the three
                        // slices that THIS instance owns. The other tabs will reload
                        // on their own when the user navigates to them.
                        loadRequests()
                        loadSchedule()
                        loadDashboard()
                    }

                    is Result.Error -> setError(r.exception.message ?: "Action failed")
                    is Result.Loading -> Unit
                }
            } catch (e: Exception) {
                setError(e.message ?: "Action failed")
            }
        }
    }

    // ── Refresh helpers ───────────────────────────────────────────────────────

    fun refreshRequests() {
        loadRequests()
    }

    fun refreshSchedule() {
        loadSchedule()
    }

    fun refreshDashboard() {
        loadDashboard()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Enrich a [Booking] with resolved client name + service info. Never throws. */
    private suspend fun Booking.enrichSafe(): CleanerBookingItem {
        return try {
            enrich()
        } catch (_: Exception) {
            CleanerBookingItem(
                booking = this,
                clientName = clientId,
                serviceName = serviceId,
                servicePrice = totalPrice
            )
        }
    }

    /** Enrich a [Booking] with resolved client name + service info. */
    private suspend fun Booking.enrich(): CleanerBookingItem {
        val clientName = try {
            (clientRepository.getClient(clientId) as? Result.Success)?.data?.name ?: clientId
        } catch (_: Exception) { clientId }
        val service = (serviceRepository.getService(serviceId) as? Result.Success)?.data
        return CleanerBookingItem(
            booking      = this,
            clientName   = clientName,
            serviceName  = service?.title    ?: serviceId,
            servicePrice = service?.basePrice ?: totalPrice
        )
    }

    private fun todayDateString(): String {
        val now = LocalDate.now()
        return now.toString() // "yyyy-MM-dd"
    }

    private fun weekStartDateString(): String {
        val now = LocalDate.now()
        return now.minusDays(now.dayOfWeek.value.toLong() - 1).toString()
    }
}
