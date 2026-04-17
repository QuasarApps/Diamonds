package com.example.diamonds.ui.subscription

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.RecurringBooking
import com.example.diamonds.domain.model.RecurringBookingStatus
import com.example.diamonds.domain.model.RecurringFrequency
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.ISubscriptionRepository
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

// ── UI States ─────────────────────────────────────────────────────────────────

data class RecurringBookingSetupUiState(
    val providerId: String = "",
    val providerName: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val totalPrice: Double = 0.0,
    val frequency: RecurringFrequency = RecurringFrequency.WEEKLY,
    val preferredDay: Int = 1, // 1=Mon for weekly; day-of-month for monthly
    val preferredTime: String = "09:00",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class SubscriptionManagementUiState(
    val recurringBookings: List<RecurringBooking> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class RecurringBookingViewModel @Inject constructor(
    private val subscriptionRepository: ISubscriptionRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<RecurringBookingSetupUiState>(
    connectivityObserver,
    RecurringBookingSetupUiState()
) {

    private val _mgmtState = MutableStateFlow(SubscriptionManagementUiState())
    val mgmtState: StateFlow<SubscriptionManagementUiState> = _mgmtState.asStateFlow()

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ── Setup screen actions ──────────────────────────────────────────────

    fun initSetup(
        providerId: String,
        serviceId: String,
        providerName: String,
        serviceName: String,
        price: Double
    ) {
        updateState {
            it.copy(
                providerId = providerId, serviceId = serviceId,
                providerName = providerName, serviceName = serviceName,
                totalPrice = price
            )
        }
    }

    fun onFrequencyChanged(freq: RecurringFrequency) = updateState { it.copy(frequency = freq) }
    fun onDayChanged(day: Int) = updateState { it.copy(preferredDay = day) }
    fun onTimeChanged(time: String) = updateState { it.copy(preferredTime = time) }
    fun onAddressChanged(address: String) = updateState { it.copy(address = address) }

    fun submitRecurringBooking() {
        val s = uiState.value
        if (s.address.isBlank()) {
            updateState { it.copy(errorMessage = "Address is required") }
            return
        }
        updateState { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val session = getCurrentSession() ?: return@launch
            val nextDate = calculateNextBookingDate(s.frequency, s.preferredDay)
            val rb = RecurringBooking(
                id = UUID.randomUUID().toString(),
                clientId = session.userId,
                providerId = s.providerId,
                providerName = s.providerName,
                serviceId = s.serviceId,
                serviceName = s.serviceName,
                frequency = s.frequency,
                preferredDay = s.preferredDay,
                preferredTime = s.preferredTime,
                address = s.address,
                latitude = s.latitude,
                longitude = s.longitude,
                totalPrice = s.totalPrice,
                status = RecurringBookingStatus.ACTIVE,
                nextBookingDate = nextDate,
                createdAt = dateFmt.format(Date()),
                updatedAt = dateFmt.format(Date())
            )
            when (val result = subscriptionRepository.createRecurringBooking(rb)) {
                is Result.Success -> updateState { it.copy(isSubmitting = false, isSuccess = true) }
                is Result.Error -> updateState {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = result.exception.message
                            ?: "Failed to create recurring booking"
                    )
                }

                else -> updateState { it.copy(isSubmitting = false) }
            }
        }
    }

    // ── Management screen actions ─────────────────────────────────────────

    fun loadRecurringBookings() {
        viewModelScope.launch {
            _mgmtState.value = _mgmtState.value.copy(isLoading = true, errorMessage = null)
            val session = getCurrentSession() ?: return@launch
            // Observe from Room
            subscriptionRepository.observeRecurringBookingsForClient(session.userId)
                .collect { list ->
                    _mgmtState.value = _mgmtState.value.copy(
                        recurringBookings = list,
                        isLoading = false
                    )
                }
        }
        // Also do a remote refresh
        viewModelScope.launch {
            val session = getCurrentSession() ?: return@launch
            subscriptionRepository.getRecurringBookingsForClient(session.userId)
        }
    }

    fun pauseRecurringBooking(id: String) {
        viewModelScope.launch {
            subscriptionRepository.updateRecurringBookingStatus(id, RecurringBookingStatus.PAUSED)
        }
    }

    fun resumeRecurringBooking(id: String) {
        viewModelScope.launch {
            subscriptionRepository.updateRecurringBookingStatus(id, RecurringBookingStatus.ACTIVE)
        }
    }

    fun cancelRecurringBooking(id: String) {
        viewModelScope.launch {
            subscriptionRepository.updateRecurringBookingStatus(
                id,
                RecurringBookingStatus.CANCELLED
            )
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private suspend fun getCurrentSession() =
        authRepository.getCurrentUserSession().let { flow ->
            var session: com.example.diamonds.domain.repository.UserSession? = null
            flow.collect { session = it; return@collect }
            session
        }

    private fun calculateNextBookingDate(frequency: RecurringFrequency, preferredDay: Int): String {
        val cal = Calendar.getInstance()
        when (frequency) {
            RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY -> {
                // Move to next occurrence of preferredDay (1=Mon)
                val targetDow =
                    if (preferredDay in 1..7) preferredDay + 1 else Calendar.MONDAY // Calendar uses SUN=1
                val wrapped = if (targetDow > 7) targetDow - 7 else targetDow
                while (cal.get(Calendar.DAY_OF_WEEK) != wrapped) cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            RecurringFrequency.FORTNIGHTLY -> {
                val targetDow = if (preferredDay in 1..7) preferredDay + 1 else Calendar.MONDAY
                val wrapped = if (targetDow > 7) targetDow - 7 else targetDow
                while (cal.get(Calendar.DAY_OF_WEEK) != wrapped) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.add(Calendar.WEEK_OF_YEAR, 1) // extra week for fortnightly
            }

            RecurringFrequency.MONTHLY -> {
                cal.add(Calendar.MONTH, 1)
                cal.set(
                    Calendar.DAY_OF_MONTH,
                    preferredDay.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                )
            }
        }
        return dateFmt.format(cal.time)
    }
}
