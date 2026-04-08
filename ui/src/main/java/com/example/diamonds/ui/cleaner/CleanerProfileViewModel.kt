package com.example.diamonds.ui.cleaner

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.Service
import com.example.diamonds.domain.model.ServiceCategory
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IProviderRepository
import com.example.diamonds.domain.repository.IReviewRepository
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

data class CleanerProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val provider: Provider? = null,
    val services: List<Service> = emptyList(),
    val averageRating: Float = 0f,
    val reviewCount: Int = 0,
    val savedSuccess: Boolean = false
)

data class ServiceManageUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val services: List<Service> = emptyList(),
    /** Non-null after a successful save – the edit screen observes this to pop itself. */
    val savedServiceId: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CleanerProfileViewModel @Inject constructor(
    private val providerRepository: IProviderRepository,
    private val serviceRepository: IServiceRepository,
    private val reviewRepository: IReviewRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    private val _profileState = MutableStateFlow(CleanerProfileUiState())
    val profileState: StateFlow<CleanerProfileUiState> = _profileState.asStateFlow()

    private val _serviceManageState = MutableStateFlow(ServiceManageUiState())
    val serviceManageState: StateFlow<ServiceManageUiState> = _serviceManageState.asStateFlow()

    // Edit fields (two-way bound from the profile screen)
    private val _editName  = MutableStateFlow("")
    val editName: StateFlow<String> = _editName.asStateFlow()

    private val _editBio   = MutableStateFlow("")
    val editBio: StateFlow<String> = _editBio.asStateFlow()

    private val _editPhone = MutableStateFlow("")
    val editPhone: StateFlow<String> = _editPhone.asStateFlow()

    fun onNameChange(v: String)  { _editName.value  = v }
    fun onBioChange(v: String)   { _editBio.value   = v }
    fun onPhoneChange(v: String) { _editPhone.value = v }

    private suspend fun myProviderId(): String? =
        authRepository.getCurrentUserSession().first()?.userId

    // ── Profile load ──────────────────────────────────────────────────────────

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = CleanerProfileUiState(isLoading = true)
            clearError()

            val pid = myProviderId() ?: run {
                setError("Not signed in")
                _profileState.value = CleanerProfileUiState()
                return@launch
            }

            val providerResult  = providerRepository.getProvider(pid)
            val servicesResult  = serviceRepository.getServicesForProvider(pid)

            if (providerResult is Result.Success) {
                val provider = providerResult.data
                val services = (servicesResult as? Result.Success)?.data ?: emptyList()

                // Seed edit fields from current values
                _editName.value  = provider.name
                _editBio.value   = provider.bio   ?: ""
                _editPhone.value = provider.phoneNumber

                _profileState.value = CleanerProfileUiState(
                    provider     = provider,
                    services     = services,
                    averageRating = provider.rating,
                    reviewCount  = provider.reviewCount
                )
            } else {
                _profileState.value = CleanerProfileUiState()
                setError((providerResult as? Result.Error)?.exception?.message ?: "Failed to load profile")
            }
        }
    }

    // ── Profile save ──────────────────────────────────────────────────────────

    fun saveProfile() {
        viewModelScope.launch {
            val current = _profileState.value.provider ?: return@launch
            _profileState.value = _profileState.value.copy(isSaving = true)
            clearError()

            val updated = current.copy(
                name        = _editName.value.trim(),
                bio         = _editBio.value.trim().ifBlank { null },
                phoneNumber = _editPhone.value.trim()
            )

            when (val r = providerRepository.updateProvider(updated)) {
                is Result.Success -> {
                    _profileState.value = _profileState.value.copy(
                        isSaving     = false,
                        provider     = r.data,
                        savedSuccess = true
                    )
                }
                is Result.Error -> {
                    _profileState.value = _profileState.value.copy(isSaving = false)
                    setError(r.exception.message ?: "Save failed")
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearSavedSuccess() {
        _profileState.value = _profileState.value.copy(savedSuccess = false)
    }

    // ── Service management ────────────────────────────────────────────────────

    fun loadServices() {
        viewModelScope.launch {
            _serviceManageState.value = ServiceManageUiState(isLoading = true)
            clearError()

            val pid = myProviderId() ?: run {
                setError("Not signed in")
                _serviceManageState.value = ServiceManageUiState()
                return@launch
            }

            when (val r = serviceRepository.getServicesForProvider(pid)) {
                is Result.Success -> _serviceManageState.value = ServiceManageUiState(services = r.data)
                is Result.Error   -> {
                    _serviceManageState.value = ServiceManageUiState()
                    setError(r.exception.message ?: "Failed to load services")
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun toggleServiceActive(service: Service) {
        viewModelScope.launch {
            val toggled = service.copy(isActive = !service.isActive)
            _serviceManageState.value = _serviceManageState.value.copy(
                services = _serviceManageState.value.services.map {
                    if (it.id == service.id) toggled else it
                }
            )
            serviceRepository.updateService(toggled)
        }
    }

    /** Called by ServiceEditScreen after it observes savedServiceId and navigates away. */
    fun clearSavedServiceId() {
        _serviceManageState.value = _serviceManageState.value.copy(savedServiceId = null)
    }

    fun saveService(
        title: String,
        description: String,
        price: Double,
        durationMinutes: Int,
        category: ServiceCategory,
        serviceId: String? = null
    ) {
        viewModelScope.launch {
            _serviceManageState.value = _serviceManageState.value.copy(isSaving = true)
            clearError()

            val pid = myProviderId() ?: run {
                _serviceManageState.value = _serviceManageState.value.copy(isSaving = false)
                return@launch
            }

            val existing = serviceId?.let { id ->
                _serviceManageState.value.services.find { it.id == id }
            }
            val now = java.time.LocalDate.now().toString()
            val service = if (existing != null) {
                existing.copy(
                    title       = title.trim(),
                    description = description.trim(),
                    basePrice   = price,
                    duration    = durationMinutes,
                    category    = category,
                    updatedAt   = now
                )
            } else {
                Service(
                    id          = "s_${System.currentTimeMillis()}",
                    providerId  = pid,
                    title       = title.trim(),
                    description = description.trim(),
                    basePrice   = price,
                    duration    = durationMinutes,
                    category    = category,
                    isActive    = true,
                    createdAt   = now,
                    updatedAt   = now
                )
            }

            val result = if (existing != null)
                serviceRepository.updateService(service)
            else
                serviceRepository.createService(service)

            when (result) {
                is Result.Success -> {
                    _serviceManageState.value = _serviceManageState.value.copy(
                        isSaving       = false,
                        savedServiceId = service.id
                    )
                    loadServices() // refresh list
                }
                is Result.Error -> {
                    _serviceManageState.value = _serviceManageState.value.copy(isSaving = false)
                    setError(result.exception.message ?: "Save failed")
                }
                is Result.Loading -> Unit
            }
        }
    }
}
