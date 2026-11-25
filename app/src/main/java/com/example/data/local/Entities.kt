package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val bio: String = "Hey there! I'm vibing on VibeChat ✨",
    val avatarColor: String = "#10B981",
    val avatarUri: String? = null,
    val statusText: String = "Available",
    val isOnline: Boolean = true,
    val lastSeenMillis: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val isSavedContact: Boolean = false
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isGroup: Boolean = false,
    val description: String = "",
    val avatarColor: String = "#10B981",
    val avatarUri: String? = null,
    val participantIdsCsv: String = "",
    val adminIdsCsv: String = "",
    val lastMessageText: String = "",
    val lastMessageSenderName: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val lastMessageType: String = "TEXT",
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val typingUser: String? = null
) {
    fun participantIds(): List<String> =
        participantIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun adminIds(): List<String> =
        adminIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarColor: String = "#10B981",
    val content: String,
    val messageType: String = "TEXT", // TEXT, IMAGE, VOICE, FILE, VIBE_CARD
    val mediaUri: String? = null,
    val voiceDurationSec: Int = 0,
    val waveformCsv: String = "",
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToPreview: String? = null,
    val reactionsCsv: String = "", // format: emoji:userId,emoji:userId
    val isStarred: Boolean = false,
    val deliveryStatus: String = "SENT", // SENT, DELIVERED, READ
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun waveformAmplitudes(): List<Float> {
        if (waveformCsv.isBlank()) {
            return listOf(0.25f, 0.5f, 0.8f, 0.4f, 0.9f, 0.6f, 0.35f, 0.75f, 0.95f, 0.5f, 0.3f, 0.7f, 0.6f, 0.4f, 0.8f, 0.5f)
        }
        return waveformCsv.split(",").mapNotNull { it.toFloatOrNull() }
    }

    fun parsedReactions(): Map<String, List<String>> {
        if (reactionsCsv.isBlank()) return emptyMap()
        val map = mutableMapOf<String, MutableList<String>>()
        reactionsCsv.split(",").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val emoji = parts[0]
                val uid = parts[1]
                map.getOrPut(emoji) { mutableListOf() }.add(uid)
            }
        }
        return map
    }
}

@Entity(tableName = "status_stories")
data class StatusStoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userHandle: String,
    val userAvatarColor: String,
    val content: String,
    val caption: String = "",
    val mediaUri: String? = null,
    val backgroundColor: String = "#065F46",
    val fontStyle: String = "DISPLAY", // DISPLAY, MODERN, MONO
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = System.currentTimeMillis() + 86_400_000L,
    val viewerIdsCsv: String = "",
    val reactionsCsv: String = "",
    val isViewedByMe: Boolean = false
) {
    fun viewerIds(): List<String> =
        viewerIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun reactionsList(): List<String> =
        reactionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
