package com.example.okakapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.okakapp.OkakApp
import com.example.okakapp.data.remote.SubscriptionStatusDto
import com.example.okakapp.data.remote.UserDto
import com.example.okakapp.data.repository.AuthRepository
import com.example.okakapp.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: UserDto? = null,
    val subscription: SubscriptionStatusDto? = null,
    val error: String? = null
)

class ProfileViewModel(
    private val authRepo: AuthRepository,
    private val subRepo: SubscriptionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val me = authRepo.me()
            val st = subRepo.status()
            _state.update {
                it.copy(
                    isLoading = false,
                    user = me.getOrNull(),
                    subscription = st.getOrNull(),
                    error = me.exceptionOrNull()?.message
                )
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepo.logout()
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OkakApp.get()
                return ProfileViewModel(app.authRepo, app.subscriptionRepo) as T
            }
        }
    }
}
