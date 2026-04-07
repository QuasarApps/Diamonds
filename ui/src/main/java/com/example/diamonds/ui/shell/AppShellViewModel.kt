package com.example.diamonds.ui.shell

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.UserSession
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel shared across the entire [AppShell].
 *
 * Exposes the live [UserSession] so that the shell scaffold, bottom nav,
 * and any child screen can react to the current role without each needing
 * its own copy of the session logic.
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    val session: StateFlow<UserSession?> = authRepository
        .getCurrentUserSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
