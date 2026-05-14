package com.example.diamonds.ui.chat

import com.example.diamonds.common.util.ConnectivityState
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Conversation
import com.example.diamonds.domain.model.Message
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IMessageRepository
import com.example.diamonds.domain.repository.UserRole
import com.example.diamonds.domain.repository.UserSession
import com.example.diamonds.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var messageRepository: IMessageRepository
    private lateinit var authRepository: IAuthRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var viewModel: ChatViewModel

    private val mockSession = UserSession(
        userId = "c1", email = "customer@test.com", role = UserRole.CUSTOMER,
        authToken = "tok", isAuthenticated = true,
        displayName = "Alice"
    )

    private fun makeConversation(id: String = "conv1") = Conversation(
        id = id, bookingId = "b1",
        clientId = "c1", clientName = "Alice",
        providerId = "p1", providerName = "Maria",
        lastMessage = "Hello", lastMessageAt = "2026-04-01T10:00:00",
        unreadCount = 0, updatedAt = "2026-04-01T10:00:00"
    )

    private fun makeMessage(id: String = "m1") = Message(
        id = id, conversationId = "conv1",
        senderId = "c1", senderName = "Alice",
        body = "Hi!", isRead = false, createdAt = "2026-04-01T10:00:00"
    )

    @Before
    fun setUp() {
        messageRepository = mockk(relaxed = true)
        authRepository = mockk {
            every { getCurrentUserSession() } returns flowOf(mockSession)
        }
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns flowOf(ConnectivityState.ONLINE)
            every { isOnline() } returns true
        }
        // Default stubs
        every { messageRepository.observeUnreadMessageCount(any()) } returns flowOf(0)
        every { messageRepository.observeConversationsForUser(any()) } returns flowOf(emptyList())
        every { messageRepository.observeMessages(any()) } returns flowOf(emptyList())

        viewModel = ChatViewModel(messageRepository, authRepository, connectivityObserver)
    }

    // ── loadConversations ─────────────────────────────────────────────────────

    @Test
    fun `loadConversations populates listState with conversations`() = runTest {
        val convos = listOf(makeConversation())
        every { messageRepository.observeConversationsForUser("c1") } returns flowOf(convos)
        coEvery { messageRepository.getConversationsForUser("c1") } returns
                Result.Success(convos)

        viewModel.loadConversations()
        advanceUntilIdle()

        val state = viewModel.listState.value
        assertEquals(1, state.conversations.size)
        assertEquals("conv1", state.conversations.first().id)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadConversations sets isLoading false after flow emission`() = runTest {
        every { messageRepository.observeConversationsForUser("c1") } returns flowOf(emptyList())
        coEvery { messageRepository.getConversationsForUser("c1") } returns
                Result.Success(emptyList())

        viewModel.loadConversations()
        advanceUntilIdle()

        assertFalse(viewModel.listState.value.isLoading)
    }

    // ── openConversation ──────────────────────────────────────────────────────

    @Test
    fun `openConversation sets conversationId in state`() = runTest {
        every { messageRepository.observeMessages("conv1") } returns
                flowOf(listOf(makeMessage()))
        coEvery { messageRepository.getMessages("conv1") } returns
                Result.Success(listOf(makeMessage()))

        viewModel.openConversation("conv1")
        advanceUntilIdle()

        assertEquals("conv1", viewModel.uiState.value.conversationId)
    }

    @Test
    fun `openConversation populates messages from flow`() = runTest {
        val msgs = listOf(makeMessage("m1"), makeMessage("m2"))
        every { messageRepository.observeMessages("conv1") } returns flowOf(msgs)
        coEvery { messageRepository.getMessages("conv1") } returns Result.Success(msgs)

        viewModel.openConversation("conv1")
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `openConversation does not re-open already open conversation`() = runTest {
        every { messageRepository.observeMessages("conv1") } returns flowOf(emptyList())
        coEvery { messageRepository.getMessages("conv1") } returns Result.Success(emptyList())

        viewModel.openConversation("conv1")
        advanceUntilIdle()
        viewModel.openConversation("conv1") // second call with same id
        advanceUntilIdle()

        // observeMessages should only be set up once (flow only collected once)
        verify(atMost = 1) { messageRepository.observeMessages("conv1") }
    }

    // ── openOrCreateConversation ──────────────────────────────────────────────

    @Test
    fun `openOrCreateConversation calls onReady with conversation id on success`() = runTest {
        val conv = makeConversation()
        coEvery {
            messageRepository.getOrCreateConversation(any(), any(), any(), any(), any())
        } returns Result.Success(conv)
        every { messageRepository.observeMessages("conv1") } returns flowOf(emptyList())
        coEvery { messageRepository.getMessages("conv1") } returns Result.Success(emptyList())

        var readyId: String? = null
        viewModel.openOrCreateConversation(
            "b1", "c1", "Alice", "p1", "Maria"
        ) { readyId = it }
        advanceUntilIdle()

        assertEquals("conv1", readyId)
        assertEquals("conv1", viewModel.uiState.value.conversationId)
    }

    @Test
    fun `openOrCreateConversation sets errorMessage on backend failure`() = runTest {
        coEvery {
            messageRepository.getOrCreateConversation(any(), any(), any(), any(), any())
        } returns Result.Error(Exception("Cannot create conversation"))

        viewModel.openOrCreateConversation("b1", "c1", "Alice", "p1", "Maria") {}
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Cannot create"))
    }

    // ── sendMessage ───────────────────────────────────────────────────────────

    @Test
    fun `sendMessage succeeds and clears isSending`() = runTest {
        // Set up a conversation first
        every { messageRepository.observeMessages("conv1") } returns flowOf(emptyList())
        coEvery { messageRepository.getMessages("conv1") } returns Result.Success(emptyList())
        viewModel.openConversation("conv1")
        advanceUntilIdle()

        coEvery { messageRepository.sendMessage(any()) } returns Result.Success(makeMessage())

        viewModel.sendMessage("Hello!")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSending)
        assertNull(viewModel.uiState.value.errorMessage)
        coVerify { messageRepository.sendMessage(any()) }
    }

    @Test
    fun `sendMessage with blank body does not call repository`() = runTest {
        every { messageRepository.observeMessages("conv1") } returns flowOf(emptyList())
        coEvery { messageRepository.getMessages("conv1") } returns Result.Success(emptyList())
        viewModel.openConversation("conv1")
        advanceUntilIdle()

        viewModel.sendMessage("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { messageRepository.sendMessage(any()) }
    }

    @Test
    fun `sendMessage without open conversation sets errorMessage`() = runTest {
        // No openConversation called — conversationId is null
        viewModel.sendMessage("Hello!")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { messageRepository.sendMessage(any()) }
    }

    @Test
    fun `sendMessage sets errorMessage on repository failure`() = runTest {
        every { messageRepository.observeMessages("conv1") } returns flowOf(emptyList())
        coEvery { messageRepository.getMessages("conv1") } returns Result.Success(emptyList())
        viewModel.openConversation("conv1")
        advanceUntilIdle()

        coEvery { messageRepository.sendMessage(any()) } returns
                Result.Error(Exception("Send failed"))

        viewModel.sendMessage("Hello!")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSending)
        assertEquals("Send failed", viewModel.uiState.value.errorMessage)
    }

    // ── markRead ──────────────────────────────────────────────────────────────

    @Test
    fun `markRead delegates to messageRepository`() = runTest {
        coEvery {
            messageRepository.markConversationRead("conv1", "c1")
        } returns Result.Success(Unit)

        viewModel.markRead("conv1")
        advanceUntilIdle()

        coVerify { messageRepository.markConversationRead("conv1", "c1") }
    }

    // ── unreadMessageCount ────────────────────────────────────────────────────

    @Test
    fun `unreadMessageCount reflects repository value`() = runTest {
        every { messageRepository.observeUnreadMessageCount("c1") } returns flowOf(5)

        // Re-create ViewModel so it picks up the new stub
        viewModel = ChatViewModel(messageRepository, authRepository, connectivityObserver)

        // Collect to activate WhileSubscribed
        val job = launch { viewModel.unreadMessageCount.collect {} }
        advanceUntilIdle()

        assertEquals(5, viewModel.unreadMessageCount.value)
        job.cancel()
    }
}
