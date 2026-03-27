package com.example.diamonds.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diamonds.common.util.ConnectivityState
import com.example.diamonds.data.connectivity.ConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel with common state management for all screens
 */
abstract class BaseViewModel<UiState>(
    private val connectivityObserver: ConnectivityObserver,
    initialState: UiState
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _pendingOperationCount = MutableStateFlow(0)
    val pendingOperationCount: StateFlow<Int> = _pendingOperationCount.asStateFlow()

    init {
        observeConnectivity()
    }

    protected fun updateState(block: (UiState) -> UiState) {
        _uiState.value = block(_uiState.value)
    }

    protected fun setError(message: String?) {
        _error.value = message
    }

    protected fun clearError() {
        _error.value = null
    }

    protected fun setPendingOperationCount(count: Int) {
        _pendingOperationCount.value = count
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.observeConnectivityState().collect { state ->
                _isOnline.value = state == ConnectivityState.ONLINE || state == ConnectivityState.METERED
            }
        }
    }
}
