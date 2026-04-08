package com.example.diamonds.ui.map

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.repository.LocationRepository
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.GeoLocation
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.ProviderLocation
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.ServiceArea
import com.example.diamonds.domain.model.TrackingState
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.ILocationRepository
import com.example.diamonds.domain.repository.IProviderRepository
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ─────────────────────────────────────────────────────────────────

data class BookingMapUiState(
    val isLoading: Boolean = false,
    val selectedLocation: GeoLocation? = null,
    val selectedAddress: String = "",
    val deviceLocation: GeoLocation? = null,
    val serviceArea: ServiceArea? = null
)

data class ProviderTrackingUiState(
    val isLoading: Boolean = false,
    val booking: Booking? = null,
    val provider: Provider? = null,
    val trackingState: TrackingState = TrackingState()
)

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: ILocationRepository,
    private val bookingRepository: IBookingRepository,
    private val providerRepository: IProviderRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    // ── Booking Map (address picker) ────────────────────────────────────────

    private val _mapState = MutableStateFlow(BookingMapUiState())
    val mapState: StateFlow<BookingMapUiState> = _mapState.asStateFlow()

    /**
     * Initialise the map — attempt to centre on the user's current location.
     * Optionally load a provider's service area circle.
     */
    fun initMap(providerId: String? = null) {
        viewModelScope.launch {
            _mapState.value = _mapState.value.copy(isLoading = true)
            clearError()

            // Attempt to get device location
            when (val loc = locationRepository.getCurrentLocation()) {
                is Result.Success -> _mapState.value = _mapState.value.copy(
                    deviceLocation = loc.data,
                    selectedLocation = _mapState.value.selectedLocation ?: loc.data
                )

                is Result.Error -> { /* permission denied or location unavailable — keep default */
                }

                is Result.Loading -> Unit
            }

            // Load service area if a provider context is given
            if (providerId != null) {
                when (val area = locationRepository.getServiceArea(providerId)) {
                    is Result.Success -> _mapState.value =
                        _mapState.value.copy(serviceArea = area.data)

                    else -> { /* non-critical */
                    }
                }
            }

            _mapState.value = _mapState.value.copy(isLoading = false)
        }
    }

    fun onMapClick(lat: Double, lng: Double) {
        _mapState.value = _mapState.value.copy(
            selectedLocation = GeoLocation(lat, lng)
        )
    }

    fun onAddressEntered(address: String) {
        _mapState.value = _mapState.value.copy(selectedAddress = address)
    }

    fun useMyLocation() {
        viewModelScope.launch {
            when (val loc = locationRepository.getCurrentLocation()) {
                is Result.Success -> _mapState.value = _mapState.value.copy(
                    selectedLocation = loc.data,
                    deviceLocation = loc.data
                )

                is Result.Error -> setError("Could not get your location")
                is Result.Loading -> Unit
            }
        }
    }

    /** Returns the selected location + address for the caller. */
    fun confirmLocation(): Pair<GeoLocation?, String> {
        val state = _mapState.value
        return state.selectedLocation to state.selectedAddress
    }

    // ── Provider Tracking ───────────────────────────────────────────────────

    private val _trackingState = MutableStateFlow(ProviderTrackingUiState())
    val trackingState: StateFlow<ProviderTrackingUiState> = _trackingState.asStateFlow()

    private var trackingJob: Job? = null

    /**
     * Load booking details and start polling the provider's location.
     */
    fun startTracking(bookingId: String) {
        viewModelScope.launch {
            _trackingState.value = ProviderTrackingUiState(isLoading = true)
            clearError()

            val bookingResult = bookingRepository.getBooking(bookingId)
            val booking = (bookingResult as? Result.Success)?.data
            if (booking == null) {
                setError("Could not load booking")
                _trackingState.value = ProviderTrackingUiState()
                return@launch
            }

            val provider =
                (providerRepository.getProvider(booking.providerId) as? Result.Success)?.data

            val bookingLat = booking.latitude
            val bookingLng = booking.longitude
            val jobLocation = if (bookingLat != null && bookingLng != null) {
                GeoLocation(bookingLat, bookingLng)
            } else null

            _trackingState.value = ProviderTrackingUiState(
                booking = booking,
                provider = provider,
                trackingState = TrackingState(
                    jobLocation = jobLocation,
                    isActive = true
                )
            )

            // Refresh the provider location from backend first
            if (locationRepository is LocationRepository) {
                (locationRepository as LocationRepository).refreshProviderLocation(booking.providerId)
            }

            // Start periodic polling (every 5 seconds)
            trackingJob?.cancel()
            trackingJob = viewModelScope.launch {
                while (isActive) {
                    refreshTrackingData(booking)
                    delay(5_000)
                }
            }
        }
    }

    private suspend fun refreshTrackingData(booking: Booking) {
        // Refresh from backend
        if (locationRepository is LocationRepository) {
            (locationRepository as LocationRepository).refreshProviderLocation(booking.providerId)
        }

        // Read the latest cached location
        val provLoc = try {
            // Use a one-shot read from the DAO through the flow
            var latest: ProviderLocation? = null
            val job = viewModelScope.launch {
                locationRepository.observeProviderLocation(booking.providerId).collect {
                    latest = it
                    // Got one emission, cancel the collector
                    return@collect
                }
            }
            delay(500) // give it time to emit
            job.cancel()
            latest
        } catch (_: Exception) {
            null
        }

        val jobLoc = _trackingState.value.trackingState.jobLocation

        val eta = if (provLoc != null && jobLoc != null) {
            (locationRepository.calculateEta(provLoc.location, jobLoc) as? Result.Success)?.data
        } else null

        val dist = if (provLoc != null && jobLoc != null) {
            locationRepository.calculateDistance(provLoc.location, jobLoc)
        } else null

        _trackingState.value = _trackingState.value.copy(
            trackingState = _trackingState.value.trackingState.copy(
                providerLocation = provLoc,
                etaMinutes = eta,
                distanceKm = dist
            )
        )
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _trackingState.value = _trackingState.value.copy(
            trackingState = _trackingState.value.trackingState.copy(isActive = false)
        )
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
    }
}
