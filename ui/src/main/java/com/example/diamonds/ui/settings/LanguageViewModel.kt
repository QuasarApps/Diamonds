package com.example.diamonds.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.local.preferences.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    val currentLanguage: StateFlow<String> = preferencesDataStore.observeLanguage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "en")

    fun setLanguage(code: String) {
        viewModelScope.launch {
            preferencesDataStore.saveLanguage(code)
        }
    }
}
