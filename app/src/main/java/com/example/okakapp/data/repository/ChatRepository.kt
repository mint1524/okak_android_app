package com.example.okakapp.data.repository

import com.example.okakapp.data.local.cache.ChatDao
import com.example.okakapp.data.local.cache.ChatEntity
import com.example.okakapp.data.local.cache.MessageDao
import com.example.okakapp.data.local.cache.MessageEntity
import com.example.okakapp.data.remote.ChatDto
import com.example.okakapp.data.remote.CreateChatRequest
import com.example.okakapp.data.remote.MessageDto
import com.example.okakapp.data.remote.OkakApi
import com.example.okakapp.data.remote.SendMessageRequest
import com.example.okakapp.data.remote.SendMessageResponse
import com.example.okakapp.data.remote.StreamEvent
import com.example.okakapp.data.remote.StreamingClient
import com.example.okakapp.data.remote.UpdateChatRequest
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val api: OkakApi,
    private val streamingClient: StreamingClient,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {

    fun observeChats(): Flow<List<ChatEntity>> = chatDao.observeAll()
    fun observeChat(chatId: String): Flow<ChatEntity?> = chatDao.observeById(chatId)
    fun observeMessages(chatId: String): Flow<List<MessageEntity>> = messageDao.observeByChat(chatId)

    suspend fun refreshChats(): Result<Unit> = runCatching {
        val remote = api.listChats()
        chatDao.syncAll(remote.map { it.toEntity() })
    }.mapErrors()

    suspend fun create(title: String?): Result<ChatDto> = runCatching {
        val chat = api.createChat(CreateChatRequest(title?.takeIf { it.isNotBlank() }))
        chatDao.upsert(chat.toEntity())
        chat
    }.mapErrors()

    suspend fun delete(chatId: String): Result<Unit> = runCatching {
        api.deleteChat(chatId)
        chatDao.delete(chatId)
    }.mapErrors()

    suspend fun rename(chatId: String, title: String): Result<ChatDto> = runCatching {
        val updated = api.renameChat(chatId, UpdateChatRequest(title))
        chatDao.updateTitle(chatId, title)
        updated
    }.mapErrors()

    suspend fun refreshMessages(chatId: String): Result<List<MessageEntity>> = runCatching {
        val remote = api.listMessages(chatId)
        val entities = remote.map { it.toEntity(chatId) }
        messageDao.replaceForChat(chatId, entities)
        entities
    }.mapErrors()

    suspend fun sendNonStreaming(chatId: String, content: String): Result<SendMessageResponse> =
        runCatching {
            val resp = api.sendMessage(chatId, SendMessageRequest(content))
            messageDao.upsert(resp.userMessage.toEntity(chatId))
            messageDao.upsert(resp.assistantMessage.toEntity(chatId))
            resp
        }.mapErrors()

    fun streamMessage(chatId: String, content: String): Flow<StreamEvent> =
        streamingClient.sendMessageStream(chatId, content)

    suspend fun cacheMessage(message: MessageEntity) {
        messageDao.upsert(message)
    }

    suspend fun touchChat(id: String) {
        chatDao.touchUpdatedAt(id, nowIso())
    }

    suspend fun cacheChatTitle(id: String, title: String) {
        chatDao.updateTitle(id, title)
    }
}

internal fun ChatDto.toEntity() = ChatEntity(id, title, createdAt, updatedAt)

internal fun MessageDto.toEntity(chatId: String) = MessageEntity(id, chatId, role, content, createdAt)

internal fun nowIso(): String = java.time.Instant.now().toString()
