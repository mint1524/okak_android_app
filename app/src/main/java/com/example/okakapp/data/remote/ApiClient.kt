package com.example.okakapp.data.remote

import com.example.okakapp.data.local.TokenStorage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://app-api.okak.club/"

    @Volatile
    private var api: OkakApi? = null

    fun create(tokenStorage: TokenStorage): OkakApi {
        api?.let { return it }
        synchronized(this) {
            api?.let { return it }
            val json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

            val authInterceptor = Interceptor { chain ->
                val token = runBlocking { tokenStorage.get() }
                val req = if (token.isNullOrBlank()) chain.request()
                else chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(req)
            }

            val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

            val created = retrofit.create(OkakApi::class.java)
            api = created
            return created
        }
    }
}
