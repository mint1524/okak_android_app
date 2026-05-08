package com.example.okakapp.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OkakApi {

    @POST("auth/register")
    suspend fun register(@Body req: AuthRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body req: AuthRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body req: RefreshRequest): AuthResponse

    @GET("user/me")
    suspend fun me(): UserDto

    @GET("chats")
    suspend fun listChats(): List<ChatDto>

    @POST("chats")
    suspend fun createChat(@Body req: CreateChatRequest): ChatDto

    @GET("chats/{chatId}/messages")
    suspend fun listMessages(
        @Path("chatId") chatId: String,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int? = null
    ): List<MessageDto>

    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body req: SendMessageRequest
    ): SendMessageResponse

    @DELETE("chats/{chatId}")
    suspend fun deleteChat(@Path("chatId") chatId: String): DeleteChatResponse

    @GET("subscriptions/plans")
    suspend fun listPlans(): List<PlanDto>

    @GET("subscriptions/status")
    suspend fun subscriptionStatus(): SubscriptionStatusDto

    @POST("subscriptions/verify")
    suspend fun verifyPurchase(@Body req: VerifyRequest): SubscriptionStatusDto
}
