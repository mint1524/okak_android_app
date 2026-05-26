package com.example.okakapp.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.example.okakapp.OkakApp
import com.example.okakapp.billing.BillingManager
import com.example.okakapp.billing.PurchaseEvent
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

class SubscriptionViewModel(
    private val repo: SubscriptionRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            billingManager.purchaseEvents.collect { event ->
                when (event) {
                    is PurchaseEvent.Success -> verifyPurchase(event.productId, event.purchaseToken)
                    is PurchaseEvent.Cancelled -> _state.update { it.copy(isPurchasing = false) }
                    is PurchaseEvent.Error -> _state.update {
                        it.copy(isPurchasing = false, error = "Ошибка оплаты (код ${event.code}): ${event.message}")
                    }
                }
            }
        }
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

    fun buy(plan: PlanDto, activity: Activity) {
        if (!billingManager.isConnected.value) {
            _state.update { it.copy(error = "Google Play недоступен. Проверьте подключение.") }
            return
        }
        _state.update { it.copy(isPurchasing = true, error = null, message = null) }
        val result = billingManager.launchBillingFlow(activity, plan.productId)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            val errorMsg = when (result.responseCode) {
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "Товар недоступен. Убедитесь, что приложение установлено из Google Play."
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Google Play Billing недоступен на этом устройстве."
                else -> "Не удалось открыть оплату: ${result.debugMessage}"
            }
            _state.update { it.copy(isPurchasing = false, error = errorMsg) }
        }
    }

    private fun verifyPurchase(productId: String, purchaseToken: String) {
        viewModelScope.launch {
            repo.verify(productId, purchaseToken)
                .onSuccess { newStatus ->
                    _state.update {
                        it.copy(
                            isPurchasing = false,
                            status = newStatus,
                            message = "Подписка активирована!"
                        )
                    }
                    load()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isPurchasing = false, error = e.message ?: "Ошибка подтверждения покупки")
                    }
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OkakApp.get()
                return SubscriptionViewModel(app.subscriptionRepo, app.billingManager) as T
            }
        }
    }
}
