package com.example.okakapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.okakapp.OkakApp
import com.example.okakapp.data.local.cache.MessageEntity
import com.example.okakapp.data.remote.StreamEvent
import com.example.okakapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class StreamingDraft(
    val content: String,
    val createdAt: String
)

data class ChatUiState(
    val isRefreshing: Boolean = false,
    val isStreaming: Boolean = false,
    val messages: List<MessageEntity> = emptyList(),
    val streamingDraft: StreamingDraft? = null,
    val draft: String = "",
    val error: String? = null
)

class ChatViewModel(
    private val chatId: String,
    private val repo: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        repo.observeMessages(chatId)
            .onEach { msgs -> _state.update { it.copy(messages = msgs) } }
            .launchIn(viewModelScope)
        refreshFromServer()
    }

    fun onDraftChange(value: String) = _state.update { it.copy(draft = value) }

    fun refreshFromServer() {
        _state.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            repo.refreshMessages(chatId)
                .onSuccess { _state.update { it.copy(isRefreshing = false) } }
                .onFailure { e -> _state.update { it.copy(isRefreshing = false, error = e.message ?: "ошибка") } }
        }
    }

    fun send() {
        val text = _state.value.draft.trim()
        if (text.isEmpty() || _state.value.isStreaming) return
        _state.update {
            it.copy(
                isStreaming = true,
                draft = "",
                error = null,
                streamingDraft = StreamingDraft("", Instant.now().toString())
            )
        }
        viewModelScope.launch {
            val accumulator = StringBuilder()
            try {
                repo.streamMessage(chatId, text).collect { event ->
                    when (event) {
                        is StreamEvent.UserMessage -> {
                            repo.cacheMessage(MessageEntity(
                                id = event.message.id,
                                chatId = chatId,
                                role = event.message.role,
                                content = event.message.content,
                                createdAt = event.message.createdAt
                            ))
                        }
                        is StreamEvent.Delta -> {
                            accumulator.append(event.content)
                            val snapshot = accumulator.toString()
                            _state.update { st ->
                                st.copy(streamingDraft = st.streamingDraft?.copy(content = snapshot))
                            }
                        }
                        is StreamEvent.AssistantMessage -> {
                            repo.cacheMessage(MessageEntity(
                                id = event.message.id,
                                chatId = chatId,
                                role = event.message.role,
                                content = event.message.content,
                                createdAt = event.message.createdAt
                            ))
                        }
                        is StreamEvent.Error -> {
                            _state.update { it.copy(error = event.message) }
                        }
                        StreamEvent.Done -> {
                            _state.update { it.copy(isStreaming = false, streamingDraft = null) }
                            repo.touchChat(chatId)
                        }
                    }
                }
                _state.update { it.copy(isStreaming = false, streamingDraft = null) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isStreaming = false,
                        streamingDraft = null,
                        error = e.message ?: "ошибка отправки",
                        draft = text
                    )
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    companion object {
        fun factory(chatId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(chatId, OkakApp.get().chatRepo) as T
            }
        }
    }
}
