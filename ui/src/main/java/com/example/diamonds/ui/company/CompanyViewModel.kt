package com.example.diamonds.ui.company

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.IClientRepository
import com.example.diamonds.domain.repository.IProviderRepository
import com.example.diamonds.domain.repository.IServiceRepository
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class CompanyDashboardUiState(
    val isLoading: Boolean = false,
    val companyName: String = "",
    /** Total pending requests across ALL employed cleaners */
    val pendingCount: Int = 0,
    /** Bookings in progress or accepted for today across ALL cleaners */
    val todayJobCount: Int = 0,
    /** Completed jobs this week across ALL cleaners */
    val weekEarnings: Double = 0.0,
    /** Number of employed cleaners linked to this company */
    val teamSize: Int = 0,
    /** Active (accepted/in-progress) jobs right now */
    val activeJobCount: Int = 0
)

data class CompanyTeamMember(
    val provider: Provider,
    val activeJobCount: Int,
    val pendingRequestCount: Int,
    val weekEarnings: Double
)

data class CompanyTeamUiState(
    val isLoading: Boolean = false,
    val members: List<CompanyTeamMember> = emptyList()
)

data class CompanyBookingItem(
    val booking: Booking,
    val clientName: String,
    val cleanerName: String,
    val serviceName: String,
    val servicePrice: Double
)

data class CompanyBookingsUiState(
    val isLoading: Boolean = false,
    val pending: List<CompanyBookingItem> = emptyList(),
    val active: List<CompanyBookingItem> = emptyList(),
    val completed: List<CompanyBookingItem> = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CompanyViewModel @Inject constructor(
    private val bookingRepository: IBookingRepository,
    private val providerRepository: IProviderRepository,
    private val serviceRepository: IServiceRepository,
    private val clientRepository: IClientRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    private val _dashboardState = MutableStateFlow(CompanyDashboardUiState())
    val dashboardState: StateFlow<CompanyDashboardUiState> = _dashboardState.asStateFlow()

    private val _teamState = MutableStateFlow(CompanyTeamUiState())
    val teamState: StateFlow<CompanyTeamUiState> = _teamState.asStateFlow()

    private val _bookingsState = MutableStateFlow(CompanyBookingsUiState())
    val bookingsState: StateFlow<CompanyBookingsUiState> = _bookingsState.asStateFlow()

    private suspend fun companyId(): String? =
        authRepository.getCurrentUserSession().first()?.userId

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Fetch all cleaners employed by this company from the provider search results. */
    private suspend fun fetchEmployedCleaners(companyId: String): List<Provider> {
        val result = providerRepository.searchProviders(0.0, 0.0)
        return if (result is Result.Success) {
            result.data.filter { it.employerId == companyId }
        } else emptyList()
    }

    /** Fetch all bookings for a list of provider IDs. */
    private suspend fun fetchAllBookings(providerIds: List<String>): List<Booking> {
        return providerIds.flatMap { pid ->
            val r = bookingRepository.getProviderBookings(pid)
            if (r is Result.Success) r.data else emptyList()
        }
    }

    private fun todayString() = java.time.LocalDate.now().toString()
    private fun weekStartString(): String {
        val now = java.time.LocalDate.now()
        return now.minusDays(now.dayOfWeek.value.toLong() - 1).toString()
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboardState.value = CompanyDashboardUiState(isLoading = true)
            clearError()

            val cid = companyId() ?: run {
                _dashboardState.value = CompanyDashboardUiState()
                return@launch
            }
            val session = authRepository.getCurrentUserSession().first()
            val companyName = session?.displayName ?: "My Company"

            val cleaners = fetchEmployedCleaners(cid)
            val allBookings = fetchAllBookings(cleaners.map { it.id })

            val today = todayString()
            val weekStart = weekStartString()

            _dashboardState.value = CompanyDashboardUiState(
                companyName   = companyName,
                teamSize      = cleaners.size,
                pendingCount  = allBookings.count { it.status == BookingStatus.PENDING },
                todayJobCount = allBookings.count {
                    it.scheduledDate == today &&
                        (it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS)
                },
                activeJobCount = allBookings.count {
                    it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS
                },
                weekEarnings = allBookings
                    .filter { it.status == BookingStatus.COMPLETED && it.scheduledDate >= weekStart }
                    .sumOf { it.totalPrice }
            )
        }
    }

    // ── Team ──────────────────────────────────────────────────────────────────

    fun loadTeam() {
        viewModelScope.launch {
            _teamState.value = CompanyTeamUiState(isLoading = true)
            clearError()

            val cid = companyId() ?: run {
                _teamState.value = CompanyTeamUiState()
                return@launch
            }

            val cleaners = fetchEmployedCleaners(cid)
            val weekStart = weekStartString()

            val members = cleaners.map { cleaner ->
                val bookings = bookingRepository.getProviderBookings(cleaner.id)
                    .let { if (it is Result.Success) it.data else emptyList() }
                CompanyTeamMember(
                    provider = cleaner,
                    activeJobCount = bookings.count {
                        it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS
                    },
                    pendingRequestCount = bookings.count { it.status == BookingStatus.PENDING },
                    weekEarnings = bookings
                        .filter { it.status == BookingStatus.COMPLETED && it.scheduledDate >= weekStart }
                        .sumOf { it.totalPrice }
                )
            }

            _teamState.value = CompanyTeamUiState(members = members)
        }
    }

    // ── Company Bookings ──────────────────────────────────────────────────────

    fun loadBookings() {
        viewModelScope.launch {
            _bookingsState.value = CompanyBookingsUiState(isLoading = true)
            clearError()

            val cid = companyId() ?: run {
                _bookingsState.value = CompanyBookingsUiState()
                return@launch
            }

            val cleaners = fetchEmployedCleaners(cid)
            val cleanerMap = cleaners.associateBy { it.id }
            val allBookings = fetchAllBookings(cleaners.map { it.id })

            suspend fun Booking.toItem(): CompanyBookingItem {
                val clientName = try {
                    (clientRepository.getClient(clientId) as? Result.Success)?.data?.name ?: clientId
                } catch (_: Exception) { clientId }
                val service = (serviceRepository.getService(serviceId) as? Result.Success)?.data
                return CompanyBookingItem(
                    booking      = this,
                    clientName   = clientName,
                    cleanerName  = cleanerMap[providerId]?.name ?: providerId,
                    serviceName  = service?.title    ?: serviceId,
                    servicePrice = service?.basePrice ?: totalPrice
                )
            }

            _bookingsState.value = CompanyBookingsUiState(
                pending   = allBookings.filter { it.status == BookingStatus.PENDING  }.map { it.toItem() },
                active    = allBookings.filter { it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS }.map { it.toItem() },
                completed = allBookings.filter { it.status == BookingStatus.COMPLETED }.sortedByDescending { it.scheduledDate }.take(20).map { it.toItem() }
            )
        }
    }

    // ── Accept / Decline (company can act on behalf of a cleaner) ─────────────

    fun acceptRequest(bookingId: String) {
        viewModelScope.launch {
            when (val r = bookingRepository.updateBookingStatus(bookingId, BookingStatus.ACCEPTED)) {
                is Result.Success -> { loadDashboard(); loadBookings() }
                is Result.Error   -> setError(r.exception.message ?: "Action failed")
                is Result.Loading -> Unit
            }
        }
    }

    fun declineRequest(bookingId: String) {
        viewModelScope.launch {
            when (val r = bookingRepository.updateBookingStatus(bookingId, BookingStatus.CANCELLED)) {
                is Result.Success -> { loadDashboard(); loadBookings() }
                is Result.Error   -> setError(r.exception.message ?: "Action failed")
                is Result.Loading -> Unit
            }
        }
    }
}
