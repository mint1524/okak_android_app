package com.example.okakapp.ui

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHATS = "chats"
    const val CHAT = "chat"
    const val SUBSCRIPTION = "subscription"
    const val PROFILE = "profile"

    fun chat(chatId: String) = "$CHAT/$chatId"
    const val CHAT_ARG = "chatId"
}
