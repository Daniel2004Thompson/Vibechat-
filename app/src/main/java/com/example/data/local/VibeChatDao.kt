package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VibeChatDao {

    // --- User Profiles / Contacts ---
    @Query("SELECT * FROM user_profiles WHERE id != :currentUserId AND isSavedContact = 1 ORDER BY isOnline DESC, displayName ASC")
    fun observeContacts(currentUserId: String): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles ORDER BY isOnline DESC, displayName ASC")
    fun observeAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id != :currentUserId AND (displayName LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%') ORDER BY displayName ASC")
    suspend fun searchLocalProfiles(currentUserId: String, query: String): List<UserProfileEntity>

    @Query("UPDATE user_profiles SET isSavedContact = 1 WHERE id = :userId")
    suspend fun markAsSavedContact(userId: String)

    @Query("SELECT * FROM user_profiles WHERE id = :userId LIMIT 1")
    suspend fun getProfileById(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE email = :email LIMIT 1")
    suspend fun getProfileByEmail(email: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE LOWER(email) = LOWER(:identifier) OR LOWER(username) = LOWER(:cleanUsername) LIMIT 1")
    suspend fun getProfileByEmailOrUsername(identifier: String, cleanUsername: String): UserProfileEntity?

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun countProfiles(): Int

    @Query("SELECT * FROM user_profiles")
    suspend fun getAllProfilesOnce(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles WHERE isSavedContact = 1")
    suspend fun getSavedProfilesOnce(): List<UserProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(profiles: List<UserProfileEntity>)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    // --- Conversations ---
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversationById(conversationId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    @Query("SELECT * FROM conversations")
    suspend fun getAllConversationsOnce(): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversations(conversations: List<ConversationEntity>)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("UPDATE messages SET conversationId = :newId WHERE conversationId = :oldId")
    suspend fun migrateConversationMessages(oldId: String, newId: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markConversationRead(conversationId: String)

    @Query("UPDATE messages SET deliveryStatus = 'READ' WHERE conversationId = :conversationId AND senderId != :currentUserId AND deliveryStatus != 'READ'")
    suspend fun markIncomingMessagesAsRead(conversationId: String, currentUserId: String)

    @Query("UPDATE messages SET deliveryStatus = 'READ' WHERE conversationId = :conversationId AND senderId = :currentUserId AND deliveryStatus != 'READ'")
    suspend fun markOutgoingMessagesAsRead(conversationId: String, currentUserId: String)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE id = :messageId")
    suspend fun updateMessageDeliveryStatus(messageId: String, status: String)

    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :conversationId")
    suspend fun setConversationPinned(conversationId: String, isPinned: Boolean)

    @Query("UPDATE conversations SET isMuted = :isMuted WHERE id = :conversationId")
    suspend fun setConversationMuted(conversationId: String, isMuted: Boolean)

    // --- Messages ---
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAtMillis ASC")
    fun observeMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY createdAtMillis DESC")
    fun observeStarredMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages ORDER BY createdAtMillis ASC")
    suspend fun getAllMessagesOnce(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAtMillis ASC")
    suspend fun getMessagesForConversationOnce(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearMessagesForConversation(conversationId: String)

    // --- Status Stories ---
    @Query("SELECT * FROM status_stories WHERE expiresAtMillis > :nowMillis ORDER BY createdAtMillis DESC")
    fun observeActiveStatusStories(nowMillis: Long): Flow<List<StatusStoryEntity>>

    @Query("SELECT * FROM status_stories WHERE id = :statusId LIMIT 1")
    suspend fun getStatusById(statusId: String): StatusStoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatusStory(story: StatusStoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatusStories(stories: List<StatusStoryEntity>)

    @Query("SELECT * FROM status_stories")
    suspend fun getAllStatusStoriesOnce(): List<StatusStoryEntity>

    @Query("DELETE FROM status_stories WHERE id = :statusId")
    suspend fun deleteStatusById(statusId: String)

    // --- Mock Data Purge Queries ---
    @Query("DELETE FROM conversations WHERE id IN ('conv_maya', 'conv_group_launch', 'conv_liam', 'conv_group_music', 'conv_zara') OR id LIKE 'conv_mock%' OR id LIKE 'mock_%'")
    suspend fun deleteLegacyMockConversations()

    @Query("DELETE FROM messages WHERE id LIKE 'msg_maya_%' OR id LIKE 'msg_group_%' OR id LIKE 'msg_liam_%' OR id LIKE 'msg_synth_%' OR id LIKE 'msg_zara_%' OR id LIKE 'msg_mock_%' OR id LIKE 'mock_%'")
    suspend fun deleteLegacyMockMessages()

    @Query("DELETE FROM user_profiles WHERE id IN ('user_maya', 'user_liam', 'user_sofia', 'user_noah', 'user_zara', 'user_lucas') OR id LIKE 'user_mock_%' OR id LIKE 'mock_%'")
    suspend fun deleteLegacyMockProfiles()

    @Query("DELETE FROM status_stories WHERE id LIKE 'status_story_%' OR id LIKE 'story_%' OR userId IN ('user_maya', 'user_liam', 'user_sofia', 'user_noah', 'user_zara', 'user_lucas') OR userId LIKE 'mock_%' OR userId LIKE 'user_mock_%'")
    suspend fun deleteLegacyMockStatuses()
}
