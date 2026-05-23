package com.example.okakapp.data.local.cache

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String
)

@Entity(
    tableName = "messages",
    indices = [Index("chatId"), Index(value = ["chatId", "createdAt"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val createdAt: String
)
