package com.example.okakapp.data.local.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OkakDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        fun build(context: Context): OkakDatabase = Room.databaseBuilder(
            context,
            OkakDatabase::class.java,
            "okak.db"
        ).fallbackToDestructiveMigration().build()
    }
}
