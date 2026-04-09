package com.example.diamonds.ui.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.Service
import com.example.diamonds.domain.model.ServiceCategory
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
import javax.inject.Inject

// ── UI State ─────────────────────────────────────────────────────────────────

data class ProviderSearchUiState(
    val isLoading: Boolean = false,
    val providers: List<Provider> = emptyList(),
    val selectedCategory: ServiceCategory? = null
)

data class ServiceListUiState(
    val isLoading: Boolean = false,
    val provider: Provider? = null,
    val services: List<Service> = emptyList()
)

data class BookingFormUiState(
    val isLoading: Boolean = false,
    val service: Service? = null,
    val provider: Provider? = null,
    val date: String = "",
    val time: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String = "",
    val bookingSuccess: Boolean = false,
    val createdBookingId: String? = null,
    val fieldErrors: Map<BookingField, String> = emptyMap()
)

data class BookingsListUiState(
    val isLoading: Boolean = false,
    val bookings: List<BookingWithDetails> = emptyList()
)

data class BookingDetailUiState(
    val isLoading: Boolean = false,
    val booking: Booking? = null,
    val provider: Provider? = null,
    val service: Service? = null,
    val cancelSuccess: Boolean = false
)

/** Booking enriched with resolved provider/service names for list display. */
data class BookingWithDetails(
    val booking: Booking,
    val providerName: String,
    val serviceName: String,
    val servicePrice: Double
)

enum class BookingField { DATE, TIME, ADDRESS }

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: IBookingRepository,
    private val providerRepository: IProviderRepository,
    private val serviceRepository: IServiceRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    // ── Provider search ───────────────────────────────────────────────────────

    private val _searchState = MutableStateFlow(ProviderSearchUiState())
    val searchState: StateFlow<ProviderSearchUiState> = _searchState.asStateFlow()

    fun loadProviders(category: ServiceCategory? = null) {
        viewModelScope.launch {
            _searchState.value = _searchState.value.copy(isLoading = true, selectedCategory = category)
            clearError()
            when (val r = providerRepository.searchProviders(0.0, 0.0)) {
                is Result.Success -> _searchState.value = _searchState.value.copy(
                    isLoading = false,
                    providers = if (category == null) r.data
                    else r.data.filter { p -> p.serviceRadius > 0 } // placeholder until server-side filtering
                )
                is Result.Error -> {
                    _searchState.value = _searchState.value.copy(isLoading = false)
                    setError(r.exception.message ?: "Could not load providers")
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun selectCategory(category: ServiceCategory?) {
        loadProviders(category)
    }

    fun refreshProviders() {
        loadProviders(_searchState.value.selectedCategory)
    }

    // ── Service list ──────────────────────────────────────────────────────────

    private val _serviceListState = MutableStateFlow(ServiceListUiState())
    val serviceListState: StateFlow<ServiceListUiState> = _serviceListState.asStateFlow()

    fun loadServicesForProvider(providerId: String) {
        viewModelScope.launch {
            _serviceListState.value = ServiceListUiState(isLoading = true)
            clearError()

            val providerResult = providerRepository.getProvider(providerId)
            val servicesResult = serviceRepository.getServicesForProvider(providerId)

            val provider = (providerResult as? Result.Success)?.data
            val services = (servicesResult as? Result.Success)?.data ?: emptyList()

            if (providerResult is Result.Error) {
                setError(providerResult.exception.message ?: "Could not load provider")
            }

            _serviceListState.value = ServiceListUiState(
                isLoading = false,
                provider = provider,
                services = services
            )
        }
    }

    fun refreshServicesForProvider() {
        _serviceListState.value.provider?.id?.let { loadServicesForProvider(it) }
    }

    // ── Booking form ──────────────────────────────────────────────────────────

    // Keys for SavedStateHandle persistence (survive process death)
    companion object {
        private const val KEY_FORM_DATE = "form_date"
        private const val KEY_FORM_TIME = "form_time"
        private const val KEY_FORM_ADDRESS = "form_address"
        private const val KEY_FORM_LAT = "form_lat"
        private const val KEY_FORM_LNG = "form_lng"
        private const val KEY_FORM_NOTES = "form_notes"
        private const val KEY_FORM_PROVIDER = "form_provider_id"
        private const val KEY_FORM_SERVICE = "form_service_id"
    }

    private val _formState = MutableStateFlow(
        BookingFormUiState(
            date = savedStateHandle.get<String>(KEY_FORM_DATE) ?: "",
            time = savedStateHandle.get<String>(KEY_FORM_TIME) ?: "",
            address = savedStateHandle.get<String>(KEY_FORM_ADDRESS) ?: "",
            latitude = savedStateHandle.get<Double>(KEY_FORM_LAT),
            longitude = savedStateHandle.get<Double>(KEY_FORM_LNG),
            notes = savedStateHandle.get<String>(KEY_FORM_NOTES) ?: ""
        )
    )
    val formState: StateFlow<BookingFormUiState> = _formState.asStateFlow()

    fun prepareBookingForm(providerId: String, serviceId: String) {
        // If the form is already prepared for the same provider/service, do not reset —
        // this preserves date, time, address and other fields the user may have filled in
        // before navigating away (e.g. to the map picker) and returning.
        val current = _formState.value
        // Also check savedStateHandle to handle process-death restore
        val savedProviderId = savedStateHandle.get<String>(KEY_FORM_PROVIDER)
        val savedServiceId = savedStateHandle.get<String>(KEY_FORM_SERVICE)
        if ((current.provider?.id == providerId && current.service?.id == serviceId) ||
            (savedProviderId == providerId && savedServiceId == serviceId && current.provider != null)
        ) return

        viewModelScope.launch {
            _formState.value = current.copy(isLoading = true)
            clearError()
            val provider = (providerRepository.getProvider(providerId) as? Result.Success)?.data
            val service  = (serviceRepository.getService(serviceId) as? Result.Success)?.data
            // Preserve any user-entered fields; only overwrite provider/service/loading.
            _formState.value = _formState.value.copy(
                isLoading = false,
                provider = provider,
                service = service
            )
            // Persist the provider/service IDs so we can guard on process-death restore
            savedStateHandle[KEY_FORM_PROVIDER] = providerId
            savedStateHandle[KEY_FORM_SERVICE] = serviceId
        }
    }

    fun onDateChange(v: String) {
        savedStateHandle[KEY_FORM_DATE] = v
        _formState.value = _formState.value.copy(
            date = v,
            fieldErrors = _formState.value.fieldErrors - BookingField.DATE
        )
    }

    fun onTimeChange(v: String) {
        savedStateHandle[KEY_FORM_TIME] = v
        _formState.value = _formState.value.copy(
            time = v,
            fieldErrors = _formState.value.fieldErrors - BookingField.TIME
        )
    }

    fun onAddressChange(v: String) {
        savedStateHandle[KEY_FORM_ADDRESS] = v
        _formState.value = _formState.value.copy(
            address = v,
            fieldErrors = _formState.value.fieldErrors - BookingField.ADDRESS
        )
    }

    fun onNotesChange(v: String) {
        savedStateHandle[KEY_FORM_NOTES] = v
        _formState.value = _formState.value.copy(notes = v)
    }

    /** Called when the user selects a location from the map screen. */
    fun onLocationSelected(lat: Double, lng: Double, address: String) {
        val resolvedAddress = if (address.isNotBlank()) address else _formState.value.address
        savedStateHandle[KEY_FORM_LAT] = lat
        savedStateHandle[KEY_FORM_LNG] = lng
        savedStateHandle[KEY_FORM_ADDRESS] = resolvedAddress
        _formState.value = _formState.value.copy(
            latitude = lat,
            longitude = lng,
            address = resolvedAddress,
            fieldErrors = _formState.value.fieldErrors - BookingField.ADDRESS
        )
    }

    fun submitBooking() {
        val state = _formState.value
        val errors = buildMap<BookingField, String> {
            if (state.date.isBlank())    put(BookingField.DATE,    "Please select a date")
            if (state.time.isBlank())    put(BookingField.TIME,    "Please select a time")
            if (state.address.isBlank()) put(BookingField.ADDRESS, "Address is required")
        }
        if (errors.isNotEmpty()) { _formState.value = state.copy(fieldErrors = errors); return }

        viewModelScope.launch {
            _formState.value = state.copy(isLoading = true)
            clearError()

            val session = authRepository.getCurrentUserSession().first()
            val clientId = session?.userId ?: "anonymous"
            val provider = state.provider ?: return@launch
            val service = state.service ?: return@launch
            val providerId = provider.id
            val serviceId = service.id

            val newBooking = Booking(
                id = "",
                clientId = clientId,
                providerId = providerId,
                serviceId = serviceId,
                status = BookingStatus.PENDING,
                scheduledDate = state.date,
                scheduledTime = state.time,
                estimatedDuration = service.duration,
                totalPrice = service.basePrice,
                notes = state.notes.ifBlank { null },
                address = state.address,
                latitude = state.latitude,
                longitude = state.longitude,
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString()
            )

            when (val r = bookingRepository.createBooking(newBooking)) {
                is Result.Success -> _formState.value = state.copy(
                    isLoading = false,
                    bookingSuccess = true,
                    createdBookingId = r.data.id
                )
                is Result.Error -> {
                    _formState.value = state.copy(isLoading = false)
                    setError(r.exception.message ?: "Booking failed. Please try again.")
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun refreshBookingsList() {
        loadMyBookings()
    }

    fun refreshBookingDetail(bookingId: String) {
        loadBookingDetail(bookingId)
    }

    fun clearBookingSuccess() {
        // Clear saved form state so a fresh form is shown next time
        savedStateHandle.remove<String>(KEY_FORM_DATE)
        savedStateHandle.remove<String>(KEY_FORM_TIME)
        savedStateHandle.remove<String>(KEY_FORM_ADDRESS)
        savedStateHandle.remove<Double>(KEY_FORM_LAT)
        savedStateHandle.remove<Double>(KEY_FORM_LNG)
        savedStateHandle.remove<String>(KEY_FORM_NOTES)
        savedStateHandle.remove<String>(KEY_FORM_PROVIDER)
        savedStateHandle.remove<String>(KEY_FORM_SERVICE)
        _formState.value = _formState.value.copy(bookingSuccess = false, createdBookingId = null)
    }

    // ── Bookings list ─────────────────────────────────────────────────────────

    private val _bookingsListState = MutableStateFlow(BookingsListUiState())
    val bookingsListState: StateFlow<BookingsListUiState> = _bookingsListState.asStateFlow()

    fun loadMyBookings() {
        viewModelScope.launch {
            _bookingsListState.value = BookingsListUiState(isLoading = true)
            clearError()

            val session = authRepository.getCurrentUserSession().first()
            val clientId = session?.userId ?: return@launch

            when (val r = bookingRepository.getClientBookings(clientId)) {
                is Result.Success -> {
                    val enriched = r.data.map { booking ->
                        val provName = (providerRepository.getProvider(booking.providerId) as? Result.Success)?.data?.name ?: booking.providerId
                        val svc      = (serviceRepository.getService(booking.serviceId) as? Result.Success)?.data
                        BookingWithDetails(
                            booking      = booking,
                            providerName = provName,
                            serviceName  = svc?.title    ?: booking.serviceId,
                            servicePrice = svc?.basePrice ?: booking.totalPrice
                        )
                    }
                    _bookingsListState.value = BookingsListUiState(bookings = enriched)
                }
                is Result.Error -> {
                    _bookingsListState.value = BookingsListUiState()
                    setError(r.exception.message ?: "Could not load bookings")
                }
                is Result.Loading -> Unit
            }
        }
    }

    // ── Booking detail ────────────────────────────────────────────────────────

    private val _detailState = MutableStateFlow(BookingDetailUiState())
    val detailState: StateFlow<BookingDetailUiState> = _detailState.asStateFlow()

    fun loadBookingDetail(bookingId: String) {
        viewModelScope.launch {
            _detailState.value = BookingDetailUiState(isLoading = true)
            clearError()

            when (val r = bookingRepository.getBooking(bookingId)) {
                is Result.Success -> {
                    val booking  = r.data
                    val provider = (providerRepository.getProvider(booking.providerId) as? Result.Success)?.data
                    val service  = (serviceRepository.getService(booking.serviceId) as? Result.Success)?.data
                    _detailState.value = BookingDetailUiState(booking = booking, provider = provider, service = service)
                }
                is Result.Error -> {
                    _detailState.value = BookingDetailUiState()
                    setError(r.exception.message ?: "Could not load booking")
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true)
            clearError()
            when (val r = bookingRepository.cancelBooking(bookingId)) {
                is Result.Success -> _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    booking = r.data,
                    cancelSuccess = true
                )
                is Result.Error -> {
                    _detailState.value = _detailState.value.copy(isLoading = false)
                    setError(r.exception.message ?: "Could not cancel booking")
                }
                is Result.Loading -> Unit
            }
        }
    }
}
