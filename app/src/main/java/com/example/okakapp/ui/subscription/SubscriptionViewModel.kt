package com.example.okakapp.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.okakapp.OkakApp
import com.example.okakapp.data.remote.PlanDto
import com.example.okakapp.data.remote.SubscriptionStatusDto
import com.example.okakapp.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val plans: List<PlanDto> = emptyList(),
    val status: SubscriptionStatusDto? = null,
    val isPurchasing: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class SubscriptionViewModel(private val repo: SubscriptionRepository) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val plans = repo.plans()
            val status = repo.status()
            val plansList = plans.getOrElse { emptyList() }
            val statusVal = status.getOrNull()
            val err = plans.exceptionOrNull()?.message ?: status.exceptionOrNull()?.message
            _state.update {
                it.copy(
                    isLoading = false,
                    plans = plansList,
                    status = statusVal,
                    error = err
                )
            }
        }
    }

    fun buy(plan: PlanDto) {
        // на этом этапе Google Play Billing не подключаем - имитируем покупку
        _state.update { it.copy(isPurchasing = true, error = null, message = null) }
        viewModelScope.launch {
            val fakeToken = "stub-${plan.productId}-${System.currentTimeMillis()}"
            repo.verify(plan.productId, fakeToken)
                .onSuccess { newStatus ->
                    _state.update {
                        it.copy(
                            isPurchasing = false,
                            status = newStatus,
                            message = "Подписка ${plan.name} активирована"
                        )
                    }
                    load()
                }
                .onFailure { e ->
                    _state.update { it.copy(isPurchasing = false, error = e.message ?: "ошибка") }
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SubscriptionViewModel(OkakApp.get().subscriptionRepo) as T
            }
        }
    }
}
