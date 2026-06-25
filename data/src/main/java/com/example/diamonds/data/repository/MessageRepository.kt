package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.data.remote.backend.CreateConversationRequest
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.SendMessageRequest
import com.example.diamonds.domain.model.Conversation
import com.example.diamonds.domain.model.Message
import com.example.diamonds.domain.model.OfflineException
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IMessageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MessageRepository @Inject constructor(
    private val db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : IMessageRepository {

    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()

    override suspend fun getOrCreateConversation(
        bookingId: String,
        clientId: String,
        clientName: String,
        providerId: String,
        providerName: String
    ): Result<Conversation> {
        // Check cache first
        val cached = conversationDao.getByBookingId(bookingId)
        if (cached != null) return Result.Success(cached.toDomain())

        return try {
            if (!connectivityObserver.isOnline()) {
                return Result.Error(Exception("No internet connection"))
            }
            val result = backendService.getOrCreateConversation(
                CreateConversationRequest(bookingId, clientId, clientName, providerId, providerName)
            )
            if (result is Result.Success) {
                val conversation = result.data.toDomain()
                conversationDao.upsert(conversation.toEntity())
                Result.Success(conversation)
            } else if (result is Result.Error) {
                result
            } else {
                Result.Error(Exception("Unexpected result state"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getConversationsForUser(userId: String): Result<List<Conversation>> {
        // Refresh from backend if online
        if (connectivityObserver.isOnline()) {
            try {
                val result = backendService.getConversationsForUser(userId)
                if (result is Result.Success) {
                    result.data.forEach { dto ->
                        conversationDao.upsert(dto.toDomain().toEntity())
                    }
                }
            } catch (_: Exception) { /* fall through to cache */
            }
        }
        val cached = conversationDao.getForUser(userId)
        return Result.Success(cached.map { it.toDomain() })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeConversationsForUser(userId: String): Flow<List<Conversation>> {
        // Each time the conversation list changes, combine every conversation's
        // real unread count (messages not sent by this user) so the badge is correct
        // for both sides of the conversation.
        return conversationDao.observeForUser(userId).flatMapLatest { entities ->
            if (entities.isEmpty()) return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyList())
            val unreadFlows = entities.map { entity ->
                conversationDao.observeUnreadCountForUser(entity.id, userId)
                    .map { count -> entity.toDomain().copy(unreadCount = count) }
            }
            combine(unreadFlows) { it.toList() }
        }
    }

    override suspend fun getMessages(conversationId: String): Result<List<Message>> {
        // Refresh from backend if online
        if (connectivityObserver.isOnline()) {
            try {
                val result = backendService.getMessages(conversationId)
                if (result is Result.Success) {
                    result.data.forEach { dto -> messageDao.insert(dto.toDomain().toEntity()) }
                }
            } catch (_: Exception) { /* fall through to cache */
            }
        }
        val cached = messageDao.getForConversation(conversationId)
        return Result.Success(cached.map { it.toDomain() })
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> {
        return messageDao.observeForConversation(conversationId)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun sendMessage(message: Message): Result<Message> {
        return try {
            // Insert locally immediately for instant UI feedback. Keeping the local
            // copy means the text is never lost even when the send fails.
            messageDao.insert(message.toEntity())
            // Update conversation last message
            conversationDao.incrementUnreadAndUpdateLastMessage(
                id = message.conversationId,
                lastMessage = message.body,
                lastMessageAt = message.createdAt,
                updatedAt = message.createdAt
            )

            // A message only counts as sent once the backend accepts it. Reporting a
            // delivery failure (offline or backend error) instead of a false success
            // lets the UI surface it rather than silently dropping the message.
            if (!connectivityObserver.isOnline()) {
                return Result.Error(
                    OfflineException("Message saved locally but not sent — no internet connection")
                )
            }
            when (val remote = backendService.sendMessage(
                SendMessageRequest(
                    id = message.id,
                    conversationId = message.conversationId,
                    senderId = message.senderId,
                    senderName = message.senderName,
                    body = message.body,
                    createdAt = message.createdAt
                )
            )) {
                is Result.Success -> Result.Success(message)
                is Result.Error -> remote
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markConversationRead(
        conversationId: String,
        userId: String
    ): Result<Unit> {
        return try {
            messageDao.markAllReadInConversation(conversationId, userId)
            conversationDao.resetUnreadCount(conversationId)
            if (connectivityObserver.isOnline()) {
                backendService.markConversationRead(conversationId, userId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeUnreadMessageCount(userId: String): Flow<Int> {
        return conversationDao.observeTotalUnreadForUser(userId).map { it ?: 0 }
    }
}
