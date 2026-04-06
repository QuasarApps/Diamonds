package com.example.diamonds.ui.auth

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────

data class AuthUiState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val signupSuccess: Boolean = false,
    val passwordResetSent: Boolean = false,
    val authenticatedRole: UserRole? = null,
    /** Field-level validation errors */
    val fieldErrors: Map<AuthField, String> = emptyMap()
)

enum class AuthField { NAME, EMAIL, PHONE, PASSWORD, CONFIRM_PASSWORD }

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<AuthUiState>(connectivityObserver, AuthUiState()) {

    // Expose current form values so the VM owns them (survives config changes)
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _selectedRole = MutableStateFlow(UserRole.CLIENT)
    val selectedRole: StateFlow<UserRole> = _selectedRole.asStateFlow()

    // ── Form input handlers ────────────────────────────────────────────────────

    fun onEmailChange(value: String) {
        _email.value = value
        clearFieldError(AuthField.EMAIL)
    }

    fun onPasswordChange(value: String) {
        _password.value = value
        clearFieldError(AuthField.PASSWORD)
    }

    fun onConfirmPasswordChange(value: String) {
        _confirmPassword.value = value
        clearFieldError(AuthField.CONFIRM_PASSWORD)
    }

    fun onNameChange(value: String) {
        _name.value = value
        clearFieldError(AuthField.NAME)
    }

    fun onPhoneChange(value: String) {
        _phone.value = value
        clearFieldError(AuthField.PHONE)
    }

    fun onRoleChange(role: UserRole) {
        _selectedRole.value = role
    }

    // ── Auth actions ──────────────────────────────────────────────────────────

    fun login() {
        val errors = buildMap {
            if (!_email.value.isValidEmail()) put(AuthField.EMAIL, "Enter a valid email address")
            if (_password.value.length < 6) put(AuthField.PASSWORD, "Password must be at least 6 characters")
        }
        if (errors.isNotEmpty()) { updateState { it.copy(fieldErrors = errors) }; return }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            clearError()

            when (val result = authRepository.login(_email.value.trim(), _password.value)) {
                is Result.Success -> updateState { it.copy(isLoading = false, loginSuccess = true, authenticatedRole = UserRole.CLIENT) }
                is Result.Error  -> {
                    updateState { it.copy(isLoading = false) }
                    setError(result.exception.message ?: "Login failed. Please try again.")
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun signup() {
        val errors = buildMap {
            if (_name.value.isBlank()) put(AuthField.NAME, "Name is required")
            if (!_email.value.isValidEmail()) put(AuthField.EMAIL, "Enter a valid email address")
            if (!_phone.value.isValidPhone()) put(AuthField.PHONE, "Enter a valid phone number")
            if (_password.value.length < 6) put(AuthField.PASSWORD, "Password must be at least 6 characters")
            if (_confirmPassword.value != _password.value) put(AuthField.CONFIRM_PASSWORD, "Passwords don't match")
        }
        if (errors.isNotEmpty()) { updateState { it.copy(fieldErrors = errors) }; return }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            clearError()

            val result = authRepository.signup(
                name = _name.value.trim(),
                email = _email.value.trim(),
                password = _password.value,
                phoneNumber = _phone.value.trim(),
                role = _selectedRole.value
            )

            when (result) {
                is Result.Success -> updateState { it.copy(isLoading = false, signupSuccess = true, authenticatedRole = _selectedRole.value) }
                is Result.Error   -> {
                    updateState { it.copy(isLoading = false) }
                    setError(result.exception.message ?: "Sign up failed. Please try again.")
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun sendPasswordReset() {
        if (!_email.value.isValidEmail()) {
            updateState { it.copy(fieldErrors = mapOf(AuthField.EMAIL to "Enter your email first")) }
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            clearError()
            when (val r = authRepository.sendPasswordReset(_email.value.trim())) {
                is Result.Success -> updateState { it.copy(isLoading = false, passwordResetSent = true) }
                is Result.Error   -> {
                    updateState { it.copy(isLoading = false) }
                    setError(r.exception.message ?: "Could not send reset email")
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearAuthSuccess() {
        updateState { it.copy(loginSuccess = false, signupSuccess = false, passwordResetSent = false, authenticatedRole = null) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun clearFieldError(field: AuthField) {
        updateState { it.copy(fieldErrors = it.fieldErrors - field) }
    }
}

// ── Validation helpers ────────────────────────────────────────────────────────

private fun String.isValidEmail(): Boolean =
    isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

private fun String.isValidPhone(): Boolean =
    isNotBlank() && length >= 7 && all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
