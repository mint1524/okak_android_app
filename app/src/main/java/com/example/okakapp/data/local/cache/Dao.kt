package com.example.okakapp.data.local.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    suspend fun listAll(): List<ChatEntity>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun findById(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chats: List<ChatEntity>)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun delete(id: String)

    @Transaction
    suspend fun replaceAll(chats: List<ChatEntity>) {
        clear()
        upsertAll(chats)
    }

    @Query("DELETE FROM chats")
    suspend fun clear()

    @Query("SELECT id FROM chats")
    suspend fun allIds(): List<String>

    @Query("DELETE FROM chats WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE chats SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchUpdatedAt(id: String, updatedAt: String)

    @Query("UPDATE chats SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Transaction
    suspend fun syncAll(chats: List<ChatEntity>) {
        val remoteIds = chats.map { it.id }.toSet()
        val toDelete = allIds().filter { it !in remoteIds }
        if (toDelete.isNotEmpty()) deleteByIds(toDelete)
        chats.forEach { chat ->
            val existing = findById(chat.id)
            if (existing != null) {
                updateTitle(chat.id, chat.title)
                touchUpdatedAt(chat.id, chat.updatedAt)
            } else {
                upsert(chat)
            }
        }
    }
}

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    fun observeByChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    suspend fun listByChat(chatId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteByChat(chatId: String)

    @Query("SELECT id FROM messages WHERE chatId = :chatId")
    suspend fun idsByChat(chatId: String): List<String>

    @Transaction
    suspend fun replaceForChat(chatId: String, messages: List<MessageEntity>) {
        val remoteIds = messages.map { it.id }.toSet()
        val toDelete = idsByChat(chatId).filter { it !in remoteIds }
        if (toDelete.isNotEmpty()) deleteByIds(toDelete)
        upsertAll(messages)
    }

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
