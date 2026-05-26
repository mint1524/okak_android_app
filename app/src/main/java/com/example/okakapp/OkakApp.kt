package com.example.okakapp

import android.app.Application
import com.example.okakapp.billing.BillingManager
import com.example.okakapp.data.local.TokenStorage
import com.example.okakapp.data.local.cache.OkakDatabase
import com.example.okakapp.data.local.SettingsStorage
import com.example.okakapp.data.remote.ApiClient
import com.example.okakapp.data.remote.OkakApi
import com.example.okakapp.data.remote.StreamingClient
import com.example.okakapp.data.repository.AuthRepository
import com.example.okakapp.data.repository.ChatRepository
import com.example.okakapp.data.repository.SubscriptionRepository

class OkakApp : Application() {

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var settingsStorage: SettingsStorage
        private set

    lateinit var api: OkakApi
        private set

    lateinit var streaming: StreamingClient
        private set

    lateinit var authRepo: AuthRepository
        private set

    lateinit var chatRepo: ChatRepository
        private set

    lateinit var subscriptionRepo: SubscriptionRepository
        private set

    lateinit var billingManager: BillingManager
        private set

    lateinit var database: OkakDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        tokenStorage = TokenStorage(applicationContext)
        settingsStorage = SettingsStorage(applicationContext)
        api = ApiClient.api(tokenStorage)
        val httpClient = ApiClient.httpClient(tokenStorage)
        streaming = StreamingClient(httpClient, ApiClient.BASE_URL)
        database = OkakDatabase.build(applicationContext)
        authRepo = AuthRepository(api, tokenStorage)
        chatRepo = ChatRepository(api, streaming, database.chatDao(), database.messageDao())
        subscriptionRepo = SubscriptionRepository(api)
        billingManager = BillingManager(this)
    }

    companion object {
        @Volatile
        private var instance: OkakApp? = null

        fun get(): OkakApp = instance ?: error("OkakApp is not initialized")
    }
}
