package com.example.okakapp.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PurchaseEvent {
    data class Success(val productId: String, val purchaseToken: String) : PurchaseEvent()
    data class Cancelled(val productId: String) : PurchaseEvent()
    data class Error(val code: Int, val message: String) : PurchaseEvent()
}

class BillingManager(context: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val products: StateFlow<Map<String, ProductDetails>> = _products.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

    val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    init {
        connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    scope.launch { queryProducts() }
                }
            }

            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
                scope.launch {
                    delay(5_000)
                    if (billingClient.isReady.not()) connect()
                }
            }
        })
    }

    private suspend fun queryProducts() {
        val productList = PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _products.value = result.productDetailsList?.associateBy { it.productId } ?: emptyMap()
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String): BillingResult {
        val details = _products.value[productId]
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .setDebugMessage("Product $productId not loaded from Play Store")
                .build()

        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .setDebugMessage("No offer available for $productId")
                .build()

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        return billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        val productId = purchase.products.firstOrNull() ?: return@forEach
                        scope.launch {
                            _purchaseEvents.emit(PurchaseEvent.Success(productId, purchase.purchaseToken))
                            acknowledgePurchase(purchase)
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                scope.launch { _purchaseEvents.emit(PurchaseEvent.Cancelled("")) }
            }
            else -> {
                scope.launch {
                    _purchaseEvents.emit(PurchaseEvent.Error(result.responseCode, result.debugMessage))
                }
            }
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params)
    }

    fun destroy() {
        scope.cancel()
        if (billingClient.isReady) billingClient.endConnection()
    }

    companion object {
        val PRODUCT_IDS = listOf("basic_monthly", "pro_monthly")
    }
}
