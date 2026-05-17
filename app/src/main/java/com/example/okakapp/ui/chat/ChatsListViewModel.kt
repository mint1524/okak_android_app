package com.example.okakapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.okakapp.OkakApp
import com.example.okakapp.data.local.cache.ChatEntity
import com.example.okakapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatsListUiState(
    val isRefreshing: Boolean = false,
    val chats: List<ChatEntity> = emptyList(),
    val error: String? = null
)

class ChatsListViewModel(private val repo: ChatRepository) : ViewModel() {

    private val _state = MutableStateFlow(ChatsListUiState())
    val state: StateFlow<ChatsListUiState> = _state.asStateFlow()

    init {
        repo.observeChats()
            .onEach { chats -> _state.update { it.copy(chats = chats) } }
            .launchIn(viewModelScope)
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            repo.refreshChats()
                .onSuccess { _state.update { it.copy(isRefreshing = false) } }
                .onFailure { e -> _state.update { it.copy(isRefreshing = false, error = e.message ?: "ошибка") } }
        }
    }

    fun create(title: String?, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            repo.create(title)
                .onSuccess { chat -> onCreated(chat.id) }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "ошибка") } }
        }
    }

    fun delete(chatId: String) {
        viewModelScope.launch {
            repo.delete(chatId)
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "ошибка") } }
        }
    }

    fun rename(chatId: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repo.rename(chatId, trimmed)
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "ошибка") } }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatsListViewModel(OkakApp.get().chatRepo) as T
            }
        }
    }
}
