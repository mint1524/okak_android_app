package com.example.okakapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

sealed class StreamEvent {
    data class UserMessage(val message: MessageDto) : StreamEvent()
    data class Delta(val content: String) : StreamEvent()
    data class AssistantMessage(val message: MessageDto, val tokensUsed: Int) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
    object Done : StreamEvent()
}

class StreamingClient(
    private val httpClient: OkHttpClient,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    fun sendMessageStream(chatId: String, content: String): Flow<StreamEvent> = flow {
        val body = json.encodeToString(SendMessageRequest.serializer(), SendMessageRequest(content))
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chats/$chatId/messages/stream")
            .post(body)
            .header("Accept", "text/event-stream, application/json")
            .build()

        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val rawBody = resp.body?.string().orEmpty()
                val errorMessage = runCatching {
                    json.decodeFromString(ApiError.serializer(), rawBody).message
                }.getOrDefault(rawBody.ifBlank { "HTTP ${resp.code}" })
                val pretty = when (resp.code) {
                    403 -> "нужна активная подписка"
                    429 -> "лимит запросов или токенов исчерпан"
                    else -> errorMessage
                }
                emit(StreamEvent.Error(pretty))
                return@flow
            }
            val source = resp.body?.source() ?: run {
                emit(StreamEvent.Error("пустой ответ"))
                return@flow
            }
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank() || !line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                val frame = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: continue
                val type = frame["type"]?.jsonPrimitive?.content ?: continue
                val data = frame["payload"] as? JsonObject
                when (type) {
                    "user_message" -> data?.let {
                        emit(StreamEvent.UserMessage(it.toMessageDto()))
                    }
                    "delta" -> data?.get("content")?.jsonPrimitive?.content?.let {
                        emit(StreamEvent.Delta(it))
                    }
                    "assistant_message" -> data?.let {
                        val tokens = it["tokensUsed"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        emit(StreamEvent.AssistantMessage(it.toMessageDto(), tokens))
                    }
                    "error" -> data?.get("message")?.jsonPrimitive?.content?.let {
                        emit(StreamEvent.Error(it))
                    }
                    "done" -> emit(StreamEvent.Done)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun JsonObject.toMessageDto(): MessageDto = MessageDto(
        id = this["id"]!!.jsonPrimitive.content,
        role = this["role"]!!.jsonPrimitive.content,
        content = this["content"]!!.jsonPrimitive.content,
        createdAt = this["createdAt"]!!.jsonPrimitive.content
    )
}
