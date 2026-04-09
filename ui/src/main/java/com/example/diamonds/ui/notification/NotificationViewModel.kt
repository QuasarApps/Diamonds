package com.example.diamonds.ui.notification

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Notification
import com.example.diamonds.domain.model.NotificationPreferences
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.INotificationRepository
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true,
    val preferences: NotificationPreferences = NotificationPreferences()
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: INotificationRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<NotificationUiState>(connectivityObserver, NotificationUiState()) {

    /** Live unread count — collected by the AppShell top bar badge. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadCount: StateFlow<Int> = authRepository.getCurrentUserSession()
        .flatMapLatest { session ->
            if (session != null) notificationRepository.observeUnreadCount(session.userId)
            else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        loadNotifications()
        loadPreferences()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            authRepository.getCurrentUserSession().collect { session ->
                if (session == null) return@collect
                notificationRepository.observeNotifications(session.userId).collect { list ->
                    updateState { it.copy(notifications = list, isLoading = false) }
                }
            }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val prefs = notificationRepository.getNotificationPreferences()
            updateState { it.copy(preferences = prefs) }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            authRepository.getCurrentUserSession().collect { session ->
                if (session == null) return@collect
                notificationRepository.markAllAsRead(session.userId)
                return@collect
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun updatePreferences(preferences: NotificationPreferences) {
        viewModelScope.launch {
            notificationRepository.updateNotificationPreferences(preferences)
            updateState { it.copy(preferences = preferences) }
        }
    }

    fun refresh() {
        updateState { it.copy(isLoading = true) }
        loadNotifications()
    }
}
