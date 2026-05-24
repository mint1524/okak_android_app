package com.example.okakapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.okakapp.OkakApp
import com.example.okakapp.data.local.SettingsStorage
import com.example.okakapp.data.local.cache.ChatDao
import com.example.okakapp.data.local.cache.ChatEntity
import com.example.okakapp.data.local.cache.MessageDao
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
    val error: String? = null,
    val searchQuery: String = "",
    val filteredChats: List<ChatEntity> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val isSearchActive: Boolean = false,
    val isSearching: Boolean = false
)

class ChatsListViewModel(
    private val repo: ChatRepository,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val settingsStorage: SettingsStorage
) : ViewModel() {

    private val _state = MutableStateFlow(ChatsListUiState())
    val state: StateFlow<ChatsListUiState> = _state.asStateFlow()

    init {
        repo.observeChats()
            .onEach { chats ->
                _state.update { s ->
                    val filtered = filterChats(chats, s.searchQuery)
                    s.copy(chats = chats, filteredChats = filtered)
                }
            }
            .launchIn(viewModelScope)
        settingsStorage.searchHistoryFlow
            .onEach { history -> _state.update { it.copy(searchHistory = history) } }
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

    fun onSearchQueryChange(query: String) {
        _state.update { s ->
            val filtered = filterChats(s.chats, query)
            s.copy(searchQuery = query, filteredChats = filtered, isSearchActive = query.isNotBlank())
        }
        if (query.isNotBlank()) {
            performDeepSearch(query)
        }
    }

    private fun performDeepSearch(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val byTitle = chatDao.searchByTitle(query)
            val byContentIds = messageDao.findChatIdsByContent(query)
            val allChatIds = (byTitle.map { it.id } + byContentIds).toSet()
            val allChats = chatDao.listAll()
            val combined = allChats.filter { it.id in allChatIds }
                .sortedByDescending { it.updatedAt }
            _state.update { s ->
                if (s.searchQuery == query) {
                    s.copy(filteredChats = combined, isSearching = false)
                } else {
                    s.copy(isSearching = false)
                }
            }
        }
    }

    fun onSearchCleared() {
        _state.update { s ->
            s.copy(searchQuery = "", filteredChats = s.chats, isSearchActive = false, isSearching = false)
        }
    }

    fun onSearchFocused() {
        _state.update { it.copy(isSearchActive = true) }
    }

    fun onSearchFocusLost() {
        val s = _state.value
        if (s.searchQuery.isBlank()) {
            _state.update { it.copy(isSearchActive = false) }
        }
    }

    fun onHistoryItemClicked(query: String) {
        onSearchQueryChange(query)
    }

    fun onClearHistory() {
        viewModelScope.launch { settingsStorage.clearSearchHistory() }
    }

    fun onSearchSubmitted() {
        val query = _state.value.searchQuery.trim()
        if (query.isNotBlank()) {
            viewModelScope.launch { settingsStorage.addSearchQuery(query) }
        }
    }

    private fun filterChats(chats: List<ChatEntity>, query: String): List<ChatEntity> {
        if (query.isBlank()) return chats
        val lower = query.lowercase()
        return chats.filter { it.title.lowercase().contains(lower) }
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
                val app = OkakApp.get()
                return ChatsListViewModel(app.chatRepo, app.database.chatDao(), app.database.messageDao(), app.settingsStorage) as T
            }
        }
    }
}
