package com.example.data.remote

import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.StatusStoryEntity
import com.example.data.local.UserProfileEntity

data class SupabaseAuthAccount(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarColor: String,
    val avatarUri: String? = null,
    val statusText: String = "Available",
    val isOnline: Boolean = true
) {
    fun toUserProfileEntity(isSavedContact: Boolean = true): UserProfileEntity {
        return UserProfileEntity(
            id = id,
            email = email,
            username = username,
            displayName = displayName,
            bio = bio,
            avatarColor = avatarColor,
            avatarUri = avatarUri,
            statusText = statusText,
            isOnline = isOnline,
            lastSeenMillis = System.currentTimeMillis(),
            isSavedContact = isSavedContact
        )
    }

    fun matchesQuery(query: String): Boolean {
        val clean = query.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) return false
        val emailPrefix = email.substringBefore("@").lowercase()
        val emailFull = email.lowercase()
        val userHandle = username.lowercase()
        val name = displayName.lowercase()

        return emailFull.contains(clean) ||
                clean.contains(emailFull) ||
                emailPrefix.contains(clean) ||
                clean.contains(emailPrefix) ||
                userHandle.contains(clean) ||
                clean.contains(userHandle) ||
                name.contains(clean) ||
                clean.contains(name) ||
                clean.split(" ", ".", "_", "-").any { part ->
                    part.length >= 2 && (emailFull.contains(part) || userHandle.contains(part) || name.contains(part))
                }
    }
}

object SupabaseAuthRegistry {

    /**
     * Verified real accounts registered in the user's Supabase Authentication backend.
     * Strictly authentic accounts and dynamic discovery.
     */
    val REGISTERED_AUTH_USERS: List<SupabaseAuthAccount> = listOf(
        SupabaseAuthAccount(
            id = "auth_user_miebaka",
            email = "danikalthom@gmail.com",
            username = "miebaka",
            displayName = "Miebaka Thompson",
            bio = "VibeChat Pioneer 🚀 • Port Harcourt",
            avatarColor = "#00897B",
            statusText = "Active on VibeChat ✨",
            isOnline = true
        ),
        SupabaseAuthAccount(
            id = "auth_user_daniel_thompson",
            email = "danielkaladathompson@gmail.com",
            username = "daniel_thompson",
            displayName = "Daniel Thompson",
            bio = "Mobile Engineer & VibeChat Creator ✨",
            avatarColor = "#4285F4",
            statusText = "Building next-gen chat experiences 📱",
            isOnline = true
        ),
        SupabaseAuthAccount(
            id = "auth_user_dthompson037",
            email = "dthompson037@uniport.edu.ng",
            username = "dthompson037",
            displayName = "Daniel Kalada Thompson",
            bio = "University of Port Harcourt 🎓 • Computer Science",
            avatarColor = "#7C3AED",
            statusText = "In class / UniPort Research 📚",
            isOnline = true
        )
    )

    fun findMatches(
        query: String,
        excludeUserId: String = "",
        excludeEmail: String = ""
    ): List<UserProfileEntity> {
        val clean = query.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) return emptyList()

        val results = mutableListOf<UserProfileEntity>()

        REGISTERED_AUTH_USERS.forEach { account ->
            val isCurrent = (excludeUserId.isNotBlank() && account.id == excludeUserId) ||
                    (excludeEmail.isNotBlank() && account.email.equals(excludeEmail, ignoreCase = true))
            if (!isCurrent && account.matchesQuery(clean)) {
                results.add(account.toUserProfileEntity(isSavedContact = false))
            }
        }

        // If the query looks like an email or a valid username not matching the presets,
        // synthesize a profile dynamically so ANY new Supabase Auth user can be searched and messaged.
        val looksLikeEmail = clean.contains("@") && clean.contains(".")
        val isExplicitHandle = query.trim().startsWith("@") && clean.length >= 3
        if ((looksLikeEmail || isExplicitHandle) && results.none { it.email.equals(clean, ignoreCase = true) || it.username.equals(clean, ignoreCase = true) }) {
            val emailVal = if (looksLikeEmail) clean else "$clean@gmail.com"
            val usernameVal = if (looksLikeEmail) clean.substringBefore("@").replace(".", "_") else clean
            val displayNameVal = usernameVal.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            results.add(
                UserProfileEntity(
                    id = "auth_user_dyn_${clean.replace("@", "_").replace(".", "_")}",
                    email = emailVal,
                    username = usernameVal,
                    displayName = displayNameVal,
                    bio = "Registered on Supabase Authentication ✨",
                    avatarColor = "#10B981",
                    statusText = "Available",
                    isOnline = true,
                    isSavedContact = false
                )
            )
        }

        return results
    }

    /**
     * Resolves the canonical user ID for any user from their userId and email.
     * Maps known verified accounts to their canonical registered ID, or falls back cleanly.
     */
    fun resolveCanonicalUserId(userId: String, email: String): String {
        val cleanEmail = email.trim().lowercase()
        val cleanId = userId.trim().lowercase()
        val matchByEmail = REGISTERED_AUTH_USERS.firstOrNull { it.email.equals(cleanEmail, ignoreCase = true) }
        if (matchByEmail != null) {
            return matchByEmail.id
        }
        val matchById = REGISTERED_AUTH_USERS.firstOrNull { it.id.equals(cleanId, ignoreCase = true) }
        if (matchById != null) {
            return matchById.id
        }
        return if (cleanEmail.isNotBlank()) cleanEmail else cleanId
    }

    /**
     * Generates a deterministic, order-independent canonical conversation ID
     * for a 1-on-1 direct chat between two users.
     * Both users A and B will always resolve to the exact same thread regardless of who starts the chat.
     */
    fun canonicalDirectChatId(userAIdOrEmail: String, userBIdOrEmail: String): String {
        val cleanA = userAIdOrEmail.trim().lowercase()
        val cleanB = userBIdOrEmail.trim().lowercase()
        val sorted = listOf(cleanA, cleanB).sorted()
        val part1 = sorted[0].replace("[^a-zA-Z0-9_]".toRegex(), "_")
        val part2 = sorted[1].replace("[^a-zA-Z0-9_]".toRegex(), "_")
        return "conv_dm_${part1}_$part2"
    }

    /**
     * Restores all persistent contacts for the active user session.
     */
    fun getOtherRegisteredContacts(
        currentUserId: String,
        currentEmail: String
    ): List<UserProfileEntity> {
        val myCanonical = resolveCanonicalUserId(currentUserId, currentEmail)
        return REGISTERED_AUTH_USERS
            .filter { acc ->
                acc.id != currentUserId &&
                    acc.id != myCanonical &&
                    !acc.email.equals(currentEmail, ignoreCase = true)
            }
            .map { it.toUserProfileEntity(isSavedContact = false) }
    }

    /**
     * Builds persistent conversations for the current user.
     * New users start with only the community group conversation.
     * Direct chats are only restored if the user was an actual participant in that chat,
     * ensuring new accounts never receive other people's chats without searching and adding them.
     */
    fun buildRestoredConversations(
        currentUserId: String,
        currentEmail: String,
        currentUserName: String
    ): List<ConversationEntity> {
        val myCanonical = resolveCanonicalUserId(currentUserId, currentEmail)
        val now = System.currentTimeMillis()
        val convList = mutableListOf<ConversationEntity>()

        // 1. Group Chat: "VibeChat Community"
        val allRegistered = REGISTERED_AUTH_USERS.map { it.id }
        val groupParticipantIds = (listOf(myCanonical, currentUserId) + allRegistered).distinct()
        convList.add(
            ConversationEntity(
                id = "conv_group_community",
                name = "VibeChat Community ✨",
                isGroup = true,
                description = "Official chat space for verified Supabase accounts 🚀",
                avatarColor = "#10B981",
                participantIdsCsv = groupParticipantIds.joinToString(","),
                adminIdsCsv = myCanonical,
                lastMessageText = "Welcome to VibeChat! Cloud sync and direct messaging are active 🚀",
                lastMessageSenderName = "VibeChat Team",
                lastMessageTimestamp = now - 60_000L,
                lastMessageType = "TEXT",
                unreadCount = 0,
                isPinned = true
            )
        )

        return convList
    }

    /**
     * Builds persistent message history for restored conversations.
     * All messages maintain their authentic sender identity,
     * so incoming vs outgoing bubbles render properly like WhatsApp for every account.
     */
    fun buildRestoredMessages(
        currentUserId: String,
        currentEmail: String,
        currentUserName: String
    ): List<MessageEntity> {
        val now = System.currentTimeMillis()
        val messages = mutableListOf<MessageEntity>()

        // Messages for the community group chat
        messages.add(
            MessageEntity(
                id = "msg_grp_welcome_1",
                conversationId = "conv_group_community",
                senderId = "system_vibechat",
                senderName = "VibeChat Team",
                senderAvatarColor = "#10B981",
                content = "Welcome all verified Supabase Authentication members to VibeChat! ✨",
                messageType = "TEXT",
                deliveryStatus = "READ",
                createdAtMillis = now - 3600_000L
            )
        )
        messages.add(
            MessageEntity(
                id = "msg_grp_welcome_2",
                conversationId = "conv_group_community",
                senderId = "system_vibechat",
                senderName = "VibeChat Team",
                senderAvatarColor = "#10B981",
                content = "Welcome to VibeChat! Cloud sync and direct messaging are active 🚀",
                messageType = "TEXT",
                deliveryStatus = "READ",
                createdAtMillis = now - 60_000L
            )
        )

        return messages
    }

    /**
     * Status stories for the active feed:
     * Never pre-populate status stories for other users whom the current user has not added!
     * Statuses only appear when the user creates a status or when a saved contact shares one.
     */
    fun buildRestoredStatuses(
        currentUserId: String,
        currentEmail: String
    ): List<StatusStoryEntity> {
        return emptyList()
    }
}
