package com.example.diamonds.ui.profile

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.LocationType
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SavedLocation
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.ISavedLocationRepository
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SavedLocationsUiState(
    val isLoading: Boolean = false,
    val locations: List<SavedLocation> = emptyList(),
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SavedLocationsViewModel @Inject constructor(
    private val savedLocationRepository: ISavedLocationRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    private val _savedLocationsState = MutableStateFlow(SavedLocationsUiState())
    val savedLocationsState: StateFlow<SavedLocationsUiState> = _savedLocationsState.asStateFlow()

    init {
        loadLocations()
    }

    fun loadLocations() {
        viewModelScope.launch {
            _savedLocationsState.value = _savedLocationsState.value.copy(isLoading = true)
            val session = authRepository.getCurrentUserSession().first()
            if (session == null) {
                _savedLocationsState.value = _savedLocationsState.value.copy(isLoading = false)
                return@launch
            }
            savedLocationRepository.observeSavedLocations(session.userId).collect { list ->
                _savedLocationsState.value =
                    _savedLocationsState.value.copy(isLoading = false, locations = list)
            }
        }
    }

    fun addLocation(
        label: String,
        address: String,
        locationType: LocationType = LocationType.HOUSE,
        roomCount: Int = 1,
        bathroomCount: Int = 1,
        sqFootage: Int? = null
    ) {
        viewModelScope.launch {
            val session = authRepository.getCurrentUserSession().first() ?: return@launch
            val now = java.time.Instant.now().toString()
            val location = SavedLocation(
                id = UUID.randomUUID().toString(),
                clientId = session.userId,
                label = label,
                address = address,
                locationType = locationType,
                roomCount = roomCount,
                bathroomCount = bathroomCount,
                sqFootage = sqFootage,
                createdAt = now,
                updatedAt = now
            )
            when (savedLocationRepository.upsertSavedLocation(location)) {
                is Result.Success -> _savedLocationsState.value =
                    _savedLocationsState.value.copy(saveSuccess = true)

                is Result.Error -> _savedLocationsState.value =
                    _savedLocationsState.value.copy(errorMessage = "Failed to save location")

                else -> {}
            }
        }
    }

    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            savedLocationRepository.deleteSavedLocation(locationId)
        }
    }

    fun clearSaveSuccess() {
        _savedLocationsState.value = _savedLocationsState.value.copy(saveSuccess = false)
    }
}
