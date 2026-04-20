package com.example.diamonds.ui.chat

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Conversation
import com.example.diamonds.domain.model.Message
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IMessageRepository
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class ConversationListUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class ChatUiState(
    val conversation: Conversation? = null,
    /** Kept separately so sendMessage() always has a conversationId even before
     *  the full Conversation object is loaded from the DB. */
    val conversationId: String? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<ChatUiState>(connectivityObserver, ChatUiState()) {

    /** Separate state for the conversation list screen. */
    private val _listState = MutableStateFlow(ConversationListUiState())
    val listState: StateFlow<ConversationListUiState> = _listState.asStateFlow()

    /** Total unread message count for the current user — drives the badge in AppShell. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadMessageCount: StateFlow<Int> = authRepository.getCurrentUserSession()
        .flatMapLatest { session ->
            if (session != null) messageRepository.observeUnreadMessageCount(session.userId)
            else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Conversation list ─────────────────────────────────────────────────────

    fun loadConversations() {
        // Observe live Room updates
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, errorMessage = null)
            authRepository.getCurrentUserSession().collect { session ->
                if (session == null) return@collect
                messageRepository.observeConversationsForUser(session.userId).collect { convos ->
                    _listState.value = _listState.value.copy(
                        conversations = convos,
                        isLoading = false
                    )
                }
            }
        }
        // Trigger a backend refresh to populate the local cache
        viewModelScope.launch {
            authRepository.getCurrentUserSession().collect { session ->
                if (session == null) return@collect
                messageRepository.getConversationsForUser(session.userId)
                return@collect
            }
        }
    }

    // ── Single conversation / chat ────────────────────────────────────────────

    /**
     * Called when navigating to ChatScreen with a known [conversationId].
     * Stores the id in state immediately so [sendMessage] works at once,
     * then begins observing messages from Room.
     */
    fun openConversation(conversationId: String) {
        // Only set conversationId if not already set
        if (uiState.value.conversationId != conversationId) {
            updateState { it.copy(conversationId = conversationId, isLoading = true) }

            // Launch observation coroutine that collects messages
            viewModelScope.launch {
                try {
                    messageRepository.observeMessages(conversationId).collect { messageList ->
                        updateState { it.copy(messages = messageList, isLoading = false) }
                    }
                } catch (e: Exception) {
                    updateState { it.copy(isLoading = false, errorMessage = e.message) }
                }
            }
        }

        // Fetch from backend to populate cache (fire and forget)
        viewModelScope.launch {
            try {
                messageRepository.getMessages(conversationId)
            } catch (e: Exception) {
                // Silently fail, already have cached data
            }
        }
    }

    /**
     * Called from BookingDetailScreen "Chat with Cleaner".
     * Gets or creates the conversation for the given booking, then calls
     * [openConversation] so messages start flowing.
     *
     * Returns the conversationId via [onReady] so the caller can navigate
     * to ChatScreen with the correct id.
     */
    fun openOrCreateConversation(
        bookingId: String,
        clientId: String,
        clientName: String,
        providerId: String,
        providerName: String,
        onReady: (conversationId: String) -> Unit
    ) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            val result = messageRepository.getOrCreateConversation(
                bookingId, clientId, clientName, providerId, providerName
            )
            if (result is Result.Success) {
                val conv = result.data
                updateState {
                    it.copy(
                        conversation = conv,
                        conversationId = conv.id,
                        isLoading = false
                    )
                }
                openConversation(conv.id)
                onReady(conv.id)
            } else if (result is Result.Error) {
                updateState { it.copy(isLoading = false, errorMessage = result.exception.message) }
            } else {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    fun sendMessage(body: String) {
        viewModelScope.launch {
            // Use conversationId directly — works whether conversation object is loaded or not
            val convId = uiState.value.conversationId
            if (convId.isNullOrBlank() || body.isBlank()) {
                updateState { it.copy(errorMessage = "Conversation ID or message is empty") }
                return@launch
            }

            try {
                val session = authRepository.getCurrentUserSession().first()
                if (session == null) {
                    updateState { it.copy(errorMessage = "Session not found") }
                    return@launch
                }

                updateState { it.copy(isSending = true) }
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .format(Date())
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    senderId = session.userId,
                    senderName = session.displayName ?: session.email,
                    body = body.trim(),
                    isRead = false,
                    createdAt = timestamp
                )
                val result = messageRepository.sendMessage(message)
                if (result is Result.Success) {
                    updateState { it.copy(isSending = false, errorMessage = null) }
                } else if (result is Result.Error) {
                    updateState {
                        it.copy(
                            isSending = false,
                            errorMessage = result.exception.message
                        )
                    }
                } else {
                    updateState { it.copy(isSending = false) }
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isSending = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun markRead(conversationId: String) {
        viewModelScope.launch {
            val session = authRepository.getCurrentUserSession().first() ?: return@launch
            messageRepository.markConversationRead(conversationId, session.userId)
        }
    }
}
