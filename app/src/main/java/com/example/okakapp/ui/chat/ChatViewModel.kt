package com.example.okakapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.okakapp.OkakApp
import com.example.okakapp.data.remote.MessageDto
import com.example.okakapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val messages: List<MessageDto> = emptyList(),
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
        loadMessages()
    }

    fun onDraftChange(value: String) = _state.update { it.copy(draft = value) }

    fun loadMessages() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repo.messages(chatId)
                .onSuccess { msgs -> _state.update { it.copy(isLoading = false, messages = msgs) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "ошибка") } }
        }
    }

    fun send() {
        val text = _state.value.draft.trim()
        if (text.isEmpty() || _state.value.isSending) return
        _state.update { it.copy(isSending = true, draft = "", error = null) }
        viewModelScope.launch {
            repo.send(chatId, text)
                .onSuccess { resp ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            messages = it.messages + resp.userMessage + resp.assistantMessage
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSending = false, error = e.message ?: "ошибка", draft = text) }
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
