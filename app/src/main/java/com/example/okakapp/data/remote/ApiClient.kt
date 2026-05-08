package com.example.okakapp.data.remote

import com.example.okakapp.data.local.TokenStorage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL = "https://app-api.okak.club/"

    @Volatile private var api: OkakApi? = null
    @Volatile private var http: OkHttpClient? = null

    fun api(tokenStorage: TokenStorage): OkakApi {
        ensure(tokenStorage)
        return api!!
    }

    fun httpClient(tokenStorage: TokenStorage): OkHttpClient {
        ensure(tokenStorage)
        return http!!
    }

    private fun ensure(tokenStorage: TokenStorage) {
        if (api != null && http != null) return
        synchronized(this) {
            if (api != null && http != null) return
            val json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
            val converter = json.asConverterFactory("application/json".toMediaType())

            val refreshOnly = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(converter)
                .build()
                .create(OkakApi::class.java)

            val authInterceptor = Interceptor { chain ->
                val req = chain.request()
                if (req.header("X-Skip-Auth") != null) return@Interceptor chain.proceed(req.newBuilder().removeHeader("X-Skip-Auth").build())
                val token = runBlocking { tokenStorage.get() }
                val withAuth = if (token.isNullOrBlank()) req
                else req.newBuilder().addHeader("Authorization", "Bearer $token").build()
                chain.proceed(withAuth)
            }

            val authenticator = Authenticator { _, response ->
                if (response.responseCount() >= 2) return@Authenticator null
                val refresh = runBlocking { tokenStorage.getRefresh() } ?: return@Authenticator null
                val newPair = runCatching { runBlocking { refreshOnly.refresh(RefreshRequest(refresh)) } }.getOrNull()
                if (newPair == null) {
                    runBlocking { tokenStorage.clear() }
                    return@Authenticator null
                }
                runBlocking { tokenStorage.save(newPair.accessToken, newPair.refreshToken) }
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newPair.accessToken}")
                    .build()
            }

            val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .authenticator(authenticator)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(converter)
                .build()

            http = client
            api = retrofit.create(OkakApi::class.java)
        }
    }

    private fun okhttp3.Response.responseCount(): Int {
        var current: okhttp3.Response? = priorResponse
        var count = 1
        while (current != null) {
            count++
            current = current.priorResponse
        }
        return count
    }
}
