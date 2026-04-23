package com.example.okakapp

import android.app.Application
import com.example.okakapp.data.local.TokenStorage
import com.example.okakapp.data.remote.ApiClient
import com.example.okakapp.data.remote.OkakApi
import com.example.okakapp.data.repository.AuthRepository
import com.example.okakapp.data.repository.ChatRepository
import com.example.okakapp.data.repository.SubscriptionRepository

class OkakApp : Application() {

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var api: OkakApi
        private set

    lateinit var authRepo: AuthRepository
        private set

    lateinit var chatRepo: ChatRepository
        private set

    lateinit var subscriptionRepo: SubscriptionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        tokenStorage = TokenStorage(applicationContext)
        api = ApiClient.create(tokenStorage)
        authRepo = AuthRepository(api, tokenStorage)
        chatRepo = ChatRepository(api)
        subscriptionRepo = SubscriptionRepository(api)
    }

    companion object {
        @Volatile
        private var instance: OkakApp? = null

        fun get(): OkakApp = instance ?: error("OkakApp is not initialized")
    }
}
