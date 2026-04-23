package com.example.okakapp.data.repository

import com.example.okakapp.data.remote.ChatDto
import com.example.okakapp.data.remote.CreateChatRequest
import com.example.okakapp.data.remote.MessageDto
import com.example.okakapp.data.remote.OkakApi
import com.example.okakapp.data.remote.SendMessageRequest
import com.example.okakapp.data.remote.SendMessageResponse

class ChatRepository(private val api: OkakApi) {

    suspend fun list(): Result<List<ChatDto>> = runCatching { api.listChats() }.mapErrors()

    suspend fun create(title: String?): Result<ChatDto> = runCatching {
        api.createChat(CreateChatRequest(title?.takeIf { it.isNotBlank() }))
    }.mapErrors()

    suspend fun delete(chatId: String): Result<Unit> = runCatching {
        api.deleteChat(chatId)
        Unit
    }.mapErrors()

    suspend fun messages(chatId: String): Result<List<MessageDto>> =
        runCatching { api.listMessages(chatId) }.mapErrors()

    suspend fun send(chatId: String, content: String): Result<SendMessageResponse> =
        runCatching { api.sendMessage(chatId, SendMessageRequest(content)) }.mapErrors()
}
