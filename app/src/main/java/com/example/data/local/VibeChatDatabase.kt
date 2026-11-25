package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        StatusStoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VibeChatDatabase : RoomDatabase() {
    abstract fun vibeChatDao(): VibeChatDao

    companion object {
        @Volatile
        private var INSTANCE: VibeChatDatabase? = null

        fun getInstance(context: Context): VibeChatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VibeChatDatabase::class.java,
                    "vibechat_room.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

/**
 * Empty factories - all chats and users strictly come from real Supabase registered accounts.
 */
object SeedDataFactory {
    fun defaultContacts(): List<UserProfileEntity> = emptyList()
    fun defaultConversations(currentUserId: String): List<ConversationEntity> = emptyList()
    fun defaultMessages(currentUserId: String, currentUserName: String): List<MessageEntity> = emptyList()
    fun defaultStatusStories(): List<StatusStoryEntity> = emptyList()
}
