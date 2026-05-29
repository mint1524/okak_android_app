package com.example.okakapp.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
    val verificationCode: String? = null
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class EmailCodeRequest(val email: String)

@Serializable
data class EmailCodeResponse(
    val sent: Boolean,
    val expiresInMinutes: Long,
    val debugCode: String? = null
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val subscriptionStatus: String
)

@Serializable
data class ChatDto(
    val id: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateChatRequest(val title: String? = null)

@Serializable
data class UpdateChatRequest(val title: String)

@Serializable
data class MessageDto(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: String
)

@Serializable
data class SendMessageRequest(val content: String)

@Serializable
data class SendMessageResponse(
    val userMessage: MessageDto,
    val assistantMessage: MessageDto,
    val tokensUsed: Int
)

@Serializable
data class DeleteChatResponse(val success: Boolean)

@Serializable
data class PlanDto(
    val id: String,
    val name: String,
    val price: Double,
    val requestLimit: Int,
    val tokenLimit: Int,
    val modelName: String,
    val productId: String
)

@Serializable
data class SubscriptionStatusDto(
    val status: String,
    val plan: String? = null,
    val expiresAt: String? = null,
    val requestLimit: Int? = null,
    val requestsUsed: Int? = null,
    val tokenLimit: Int? = null,
    val tokensUsed: Int? = null
)

@Serializable
data class VerifyRequest(
    val productId: String,
    val purchaseToken: String
)

@Serializable
data class ApiError(
    val error: String,
    val message: String
)
