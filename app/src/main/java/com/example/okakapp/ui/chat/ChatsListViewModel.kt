package com.example.okakapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.okakapp.OkakApp
import com.example.okakapp.data.remote.ChatDto
import com.example.okakapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatsListUiState(
    val isLoading: Boolean = false,
    val chats: List<ChatDto> = emptyList(),
    val error: String? = null
)

class ChatsListViewModel(private val repo: ChatRepository) : ViewModel() {

    private val _state = MutableStateFlow(ChatsListUiState())
    val state: StateFlow<ChatsListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repo.list()
                .onSuccess { list -> _state.update { it.copy(isLoading = false, chats = list) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "ошибка") } }
        }
    }

    fun create(title: String?, onCreated: (ChatDto) -> Unit) {
        viewModelScope.launch {
            repo.create(title)
                .onSuccess { chat ->
                    _state.update { it.copy(chats = listOf(chat) + it.chats) }
                    onCreated(chat)
                }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "ошибка") } }
        }
    }

    fun delete(chatId: String) {
        viewModelScope.launch {
            repo.delete(chatId)
                .onSuccess { _state.update { st -> st.copy(chats = st.chats.filter { it.id != chatId }) } }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "ошибка") } }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatsListViewModel(OkakApp.get().chatRepo) as T
            }
        }
    }
}
