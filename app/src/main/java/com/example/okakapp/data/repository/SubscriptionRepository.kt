package com.example.okakapp.data.repository

import com.example.okakapp.data.remote.OkakApi
import com.example.okakapp.data.remote.PlanDto
import com.example.okakapp.data.remote.SubscriptionStatusDto
import com.example.okakapp.data.remote.VerifyRequest

class SubscriptionRepository(private val api: OkakApi) {

    suspend fun plans(): Result<List<PlanDto>> = runCatching { api.listPlans() }.mapErrors()

    suspend fun status(): Result<SubscriptionStatusDto> = runCatching { api.subscriptionStatus() }.mapErrors()

    suspend fun verify(productId: String, purchaseToken: String): Result<SubscriptionStatusDto> =
        runCatching { api.verifyPurchase(VerifyRequest(productId, purchaseToken)) }.mapErrors()
}
