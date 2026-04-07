package com.example.diamonds.ui.splash

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.ui.base.BaseViewModel
import com.example.diamonds.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Minimum time the splash is visible so the animation can play (ms). */
private const val MIN_SPLASH_DURATION_MS = 1_800L

data class SplashUiState(
    val isLoading: Boolean = true
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<SplashUiState>(connectivityObserver, SplashUiState()) {

    private val _destination = MutableStateFlow<Screen?>(null)
    /** Emits once with the screen the app should navigate to after the splash. */
    val destination: StateFlow<Screen?> = _destination.asStateFlow()

    init {
        checkAuthAndRoute()
    }

    private fun checkAuthAndRoute() {
        viewModelScope.launch {
            // Run the minimum display duration and the auth check in parallel;
            // navigate only after both complete.
            val startMs = System.currentTimeMillis()

            val session = authRepository.getCurrentUserSession().first()

            val elapsed = System.currentTimeMillis() - startMs
            val remaining = MIN_SPLASH_DURATION_MS - elapsed
            if (remaining > 0) delay(remaining)

            val next = when {
                session == null || !session.isAuthenticated -> Screen.Login
                else -> Screen.AppShell
            }

            updateState { it.copy(isLoading = false) }
            _destination.value = next
        }
    }
}
