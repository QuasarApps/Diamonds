package com.example.diamonds.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diamonds.domain.model.HelpArticle
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SupportTicket
import com.example.diamonds.domain.model.SupportTicketType
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.ISupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HelpCenterUiState(
    val articles: List<HelpArticle> = emptyList(),
    val tickets: List<SupportTicket> = emptyList(),
    val isLoading: Boolean = false,
    val contactSubject: String = "",
    val contactDescription: String = "",
    val showContactForm: Boolean = false,
    val ticketCreated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HelpCenterViewModel @Inject constructor(
    private val supportRepository: ISupportRepository,
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HelpCenterUiState(isLoading = true))
    val state: StateFlow<HelpCenterUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val session = authRepository.getCurrentUserSession().first()
            val articlesResult = supportRepository.getHelpArticles()
            val articles = (articlesResult as? Result.Success)?.data ?: emptyList()

            val tickets = session?.let { s ->
                when (val r = supportRepository.getTicketsForUser(s.userId)) {
                    is Result.Success -> r.data
                    is Result.Error -> emptyList()
                    is Result.Loading -> emptyList()
                }
            } ?: emptyList()

            _state.value =
                _state.value.copy(articles = articles, tickets = tickets, isLoading = false)
        }
    }

    fun toggleContactForm() {
        _state.value = _state.value.copy(
            showContactForm = !_state.value.showContactForm,
            ticketCreated = false
        )
    }

    fun onSubjectChanged(s: String) {
        _state.value = _state.value.copy(contactSubject = s)
    }

    fun onDescriptionChanged(s: String) {
        _state.value = _state.value.copy(contactDescription = s)
    }

    fun submitContactTicket() {
        viewModelScope.launch {
            val session = authRepository.getCurrentUserSession().first() ?: return@launch
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())
            val ticket = SupportTicket(
                id = "", userId = session.userId, userRole = session.role.name,
                type = SupportTicketType.SATISFACTION, subject = _state.value.contactSubject,
                description = _state.value.contactDescription, createdAt = now, updatedAt = now
            )
            _state.value = _state.value.copy(isLoading = true)
            when (val r = supportRepository.createSupportTicket(ticket)) {
                is Result.Success -> _state.value = _state.value.copy(
                    ticketCreated = true, showContactForm = false, isLoading = false,
                    contactSubject = "", contactDescription = ""
                ).also { load() }

                is Result.Error -> _state.value =
                    _state.value.copy(error = r.exception.message, isLoading = false)

                is Result.Loading -> {}
            }
        }
    }
}
