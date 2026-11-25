package com.example.data.remote

import com.squareup.moshi.Json

// --- Supabase Auth Models (/auth/v1/) ---

data class SupabaseSignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String> = emptyMap()
)

data class SupabasePasswordSignInRequest(
    val email: String,
    val password: String
)

data class SupabaseIdTokenSignInRequest(
    val provider: String = "google",
    @Json(name = "id_token") val idToken: String,
    val nonce: String? = null
)

data class SupabaseOtpRequest(
    val email: String,
    @Json(name = "create_user") val createUser: Boolean = true,
    val data: Map<String, String> = emptyMap()
)

data class SupabaseRecoverRequest(
    val email: String
)

data class SupabaseVerifyOtpRequest(
    val email: String,
    val token: String,
    val type: String = "email" // "email", "signup", "recovery", "magiclink"
)

data class SupabaseUserDto(
    val id: String?,
    val email: String?,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null
)

data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    val user: SupabaseUserDto? = null,
    val id: String? = null,
    val email: String? = null
)

// --- Supabase PostgREST Table Models (/rest/v1/) ---

data class SupabaseProfileDto(
    val id: String,
    val email: String,
    val username: String,
    @Json(name = "display_name") val displayName: String,
    val bio: String = "Hey there! I'm vibing on VibeChat ✨",
    @Json(name = "avatar_color") val avatarColor: String = "#10B981",
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "status_text") val statusText: String = "Available",
    @Json(name = "is_online") val isOnline: Boolean = true,
    @Json(name = "last_seen_millis") val lastSeenMillis: Long = System.currentTimeMillis()
)

data class SupabaseConversationDto(
    val id: String,
    val name: String,
    @Json(name = "is_group") val isGroup: Boolean = false,
    val description: String = "",
    @Json(name = "avatar_color") val avatarColor: String = "#10B981",
    @Json(name = "participant_ids_csv") val participantIdsCsv: String = "",
    @Json(name = "admin_ids_csv") val adminIdsCsv: String = "",
    @Json(name = "last_message_text") val lastMessageText: String = "",
    @Json(name = "last_message_sender_name") val lastMessageSenderName: String = "",
    @Json(name = "last_message_timestamp") val lastMessageTimestamp: Long = System.currentTimeMillis(),
    @Json(name = "last_message_type") val lastMessageType: String = "TEXT"
)

data class SupabaseMessageDto(
    val id: String,
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_name") val senderName: String,
    @Json(name = "sender_avatar_color") val senderAvatarColor: String = "#10B981",
    val content: String,
    @Json(name = "message_type") val messageType: String = "TEXT",
    @Json(name = "media_uri") val mediaUri: String? = null,
    @Json(name = "voice_duration_sec") val voiceDurationSec: Int = 0,
    @Json(name = "waveform_csv") val waveformCsv: String = "",
    @Json(name = "reply_to_message_id") val replyToMessageId: String? = null,
    @Json(name = "reply_to_sender_name") val replyToSenderName: String? = null,
    @Json(name = "reply_to_preview") val replyToPreview: String? = null,
    @Json(name = "reactions_csv") val reactionsCsv: String = "",
    @Json(name = "delivery_status") val deliveryStatus: String = "SENT",
    @Json(name = "created_at_millis") val createdAtMillis: Long = System.currentTimeMillis()
)

data class SupabaseStatusDto(
    val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "user_handle") val userHandle: String,
    @Json(name = "user_avatar_color") val userAvatarColor: String = "#10B981",
    val content: String,
    val caption: String = "",
    @Json(name = "media_uri") val mediaUri: String? = null,
    @Json(name = "background_color") val backgroundColor: String = "#065F46",
    @Json(name = "font_style") val fontStyle: String = "DISPLAY",
    @Json(name = "created_at_millis") val createdAtMillis: Long = System.currentTimeMillis(),
    @Json(name = "expires_at_millis") val expiresAtMillis: Long = System.currentTimeMillis() + 86_400_000L,
    @Json(name = "viewer_ids_csv") val viewerIdsCsv: String = "",
    @Json(name = "reactions_csv") val reactionsCsv: String = ""
)

data class SupabaseUserTableDto(
    val id: String,
    val email: String,
    val username: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    val bio: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)
