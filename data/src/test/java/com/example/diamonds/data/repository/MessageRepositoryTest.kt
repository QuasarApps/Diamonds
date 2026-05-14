package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.ConversationDao
import com.example.diamonds.data.local.dao.MessageDao
import com.example.diamonds.data.local.entity.ConversationEntity
import com.example.diamonds.data.local.entity.MessageEntity
import com.example.diamonds.data.remote.backend.ConversationDto
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.MessageDto
import com.example.diamonds.domain.model.Message
import com.example.diamonds.domain.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MessageRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var conversationDao: ConversationDao
    private lateinit var messageDao: MessageDao
    private lateinit var backendService: IBackendService
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var repository: MessageRepository

    private fun makeConversationEntity(id: String = "conv1", bookingId: String = "b1") =
        ConversationEntity(
            id = id, bookingId = bookingId,
            clientId = "c1", clientName = "Alice",
            providerId = "p1", providerName = "Maria",
            lastMessage = "Hello", lastMessageAt = "2026-04-01T10:00:00",
            unreadCount = 0, updatedAt = "2026-04-01T10:00:00"
        )

    private fun makeMessageEntity(id: String = "m1", conversationId: String = "conv1") =
        MessageEntity(
            id = id, conversationId = conversationId,
            senderId = "c1", senderName = "Alice",
            body = "Hi there!", isRead = false, createdAt = "2026-04-01T10:00:00"
        )

    private fun makeConversationDto(id: String = "conv1") = ConversationDto(
        id = id, bookingId = "b1",
        clientId = "c1", clientName = "Alice",
        providerId = "p1", providerName = "Maria",
        lastMessage = "", lastMessageAt = "", unreadCount = 0,
        updatedAt = "2026-04-01T10:00:00"
    )

    private fun makeMessage(id: String = "m1", conversationId: String = "conv1") = Message(
        id = id, conversationId = conversationId,
        senderId = "c1", senderName = "Alice",
        body = "Hello!", isRead = false, createdAt = "2026-04-01T10:00:00"
    )

    @Before
    fun setUp() {
        conversationDao = mockk(relaxed = true)
        messageDao = mockk(relaxed = true)
        db = mockk {
            every { conversationDao() } returns conversationDao
            every { messageDao() } returns messageDao
        }
        backendService = mockk()
        connectivityObserver = mockk { every { isOnline() } returns true }
        repository = MessageRepository(db, backendService, connectivityObserver)
    }

    // ── getOrCreateConversation ───────────────────────────────────────────────

    @Test
    fun `getOrCreateConversation returns cached conversation when found`() = runTest {
        coEvery { conversationDao.getByBookingId("b1") } returns makeConversationEntity()

        val result = repository.getOrCreateConversation("b1", "c1", "Alice", "p1", "Maria")

        assertTrue(result is Result.Success)
        assertEquals("conv1", (result as Result.Success).data.id)
        coVerify(exactly = 0) { backendService.getOrCreateConversation(any()) }
    }

    @Test
    fun `getOrCreateConversation fetches from backend on cache miss when online`() = runTest {
        coEvery { conversationDao.getByBookingId("b1") } returns null
        coEvery { backendService.getOrCreateConversation(any()) } returns
                Result.Success(makeConversationDto())

        val result = repository.getOrCreateConversation("b1", "c1", "Alice", "p1", "Maria")

        assertTrue(result is Result.Success)
        assertEquals("conv1", (result as Result.Success).data.id)
        coVerify { conversationDao.upsert(any()) }
    }

    @Test
    fun `getOrCreateConversation returns error when offline and cache miss`() = runTest {
        coEvery { conversationDao.getByBookingId("b1") } returns null
        every { connectivityObserver.isOnline() } returns false

        val result = repository.getOrCreateConversation("b1", "c1", "Alice", "p1", "Maria")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `getOrCreateConversation propagates backend error`() = runTest {
        coEvery { conversationDao.getByBookingId("b1") } returns null
        coEvery { backendService.getOrCreateConversation(any()) } returns
                Result.Error(Exception("server error"))

        val result = repository.getOrCreateConversation("b1", "c1", "Alice", "p1", "Maria")

        assertTrue(result is Result.Error)
        assertEquals("server error", (result as Result.Error).exception.message)
    }

    // ── getConversationsForUser ───────────────────────────────────────────────

    @Test
    fun `getConversationsForUser returns cached conversations`() = runTest {
        val entities = listOf(makeConversationEntity())
        coEvery { backendService.getConversationsForUser("c1") } returns
                Result.Success(listOf(makeConversationDto()))
        coEvery { conversationDao.getForUser("c1") } returns entities

        val result = repository.getConversationsForUser("c1") as Result.Success
        assertEquals(1, result.data.size)
        assertEquals("conv1", result.data.first().id)
    }

    @Test
    fun `getConversationsForUser returns empty list when cache empty and offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false
        coEvery { conversationDao.getForUser("c1") } returns emptyList()

        val result = repository.getConversationsForUser("c1") as Result.Success
        assertTrue(result.data.isEmpty())
    }

    // ── getMessages ──────────────────────────────────────────────────────────

    @Test
    fun `getMessages refreshes from backend when online then returns cache`() = runTest {
        val entities = listOf(makeMessageEntity())
        coEvery { backendService.getMessages("conv1") } returns Result.Success(emptyList())
        coEvery { messageDao.getForConversation("conv1") } returns entities

        val result = repository.getMessages("conv1") as Result.Success
        assertEquals(1, result.data.size)
        assertEquals("m1", result.data.first().id)
    }

    @Test
    fun `getMessages returns local cache when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false
        coEvery { messageDao.getForConversation("conv1") } returns listOf(makeMessageEntity())

        val result = repository.getMessages("conv1") as Result.Success
        assertEquals(1, result.data.size)
    }

    // ── observeMessages ──────────────────────────────────────────────────────

    @Test
    fun `observeMessages emits mapped domain messages`() = runTest {
        every { messageDao.observeForConversation("conv1") } returns
                flowOf(listOf(makeMessageEntity()))

        val messages = repository.observeMessages("conv1").first()
        assertEquals(1, messages.size)
        assertEquals("m1", messages.first().id)
        assertEquals("Hi there!", messages.first().body)
    }

    // ── sendMessage ──────────────────────────────────────────────────────────

    @Test
    fun `sendMessage inserts locally immediately and syncs when online`() = runTest {
        val sentDto = MessageDto(
            id = "m1", conversationId = "conv1", senderId = "c1", senderName = "Alice",
            body = "Hello!", isRead = false, createdAt = "2026-04-01T10:00:00"
        )
        coEvery { backendService.sendMessage(any()) } returns Result.Success(sentDto)

        val message = makeMessage()
        val result = repository.sendMessage(message)

        assertTrue(result is Result.Success)
        coVerify { messageDao.insert(any()) }
        coVerify { conversationDao.incrementUnreadAndUpdateLastMessage(any(), any(), any(), any()) }
        coVerify { backendService.sendMessage(any()) }
    }

    @Test
    fun `sendMessage inserts locally even when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val message = makeMessage()
        val result = repository.sendMessage(message)

        assertTrue(result is Result.Success)
        coVerify { messageDao.insert(any()) }
        // Should NOT call backend when offline
        coVerify(exactly = 0) { backendService.sendMessage(any()) }
    }

    // ── markConversationRead ─────────────────────────────────────────────────

    @Test
    fun `markConversationRead clears local unread count and syncs when online`() = runTest {
        coEvery { backendService.markConversationRead(any(), any()) } returns Result.Success(Unit)

        val result = repository.markConversationRead("conv1", "c1")

        assertTrue(result is Result.Success)
        coVerify { messageDao.markAllReadInConversation("conv1", "c1") }
        coVerify { conversationDao.resetUnreadCount("conv1") }
        coVerify { backendService.markConversationRead("conv1", "c1") }
    }

    @Test
    fun `markConversationRead clears local state without backend call when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val result = repository.markConversationRead("conv1", "c1")

        assertTrue(result is Result.Success)
        coVerify { messageDao.markAllReadInConversation("conv1", "c1") }
        coVerify(exactly = 0) { backendService.markConversationRead(any(), any()) }
    }

    // ── observeUnreadMessageCount ─────────────────────────────────────────────

    @Test
    fun `observeUnreadMessageCount emits total from dao`() = runTest {
        every { conversationDao.observeTotalUnreadForUser("c1") } returns flowOf(3)

        val count = repository.observeUnreadMessageCount("c1").first()
        assertEquals(3, count)
    }

    @Test
    fun `observeUnreadMessageCount emits 0 when dao returns null`() = runTest {
        every { conversationDao.observeTotalUnreadForUser("c1") } returns flowOf(null)

        val count = repository.observeUnreadMessageCount("c1").first()
        assertEquals(0, count)
    }
}
