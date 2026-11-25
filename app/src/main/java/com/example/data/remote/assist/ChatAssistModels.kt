package com.example.data.remote.assist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatAssistCorrection(
    val original: String = "",
    val suggestion: String = ""
)

@Serializable
data class CorrectResponse(
    val corrections: List<ChatAssistCorrection> = emptyList(),
    val corrected: String = ""
)

@Serializable
data class ChatAssistMessage(
    val from: String, // "me" or "them"
    val text: String
)

@Serializable
data class RepliesResponse(
    val replies: List<String> = emptyList()
)

@Serializable
data class TextResponse(
    val text: String = ""
)

@Serializable
data class AddWordResponse(
    val ok: Boolean = false
)

@Serializable
data class ErrorResponse(
    val error: String? = null
)

// --- Request DTOs ---

@Serializable
data class CorrectRequest(
    val action: String = "correct",
    val text: String
)

@Serializable
data class RepliesRequest(
    val action: String = "replies",
    val messages: List<ChatAssistMessage>
)

@Serializable
data class RewriteRequest(
    val action: String = "rewrite",
    val text: String,
    val tone: String // "casual" | "formal" | "friendly" | "shorter" | "funny"
)

@Serializable
data class TranslateRequest(
    val action: String = "translate",
    val text: String,
    @SerialName("targetLang") val targetLang: String
)

@Serializable
data class AddWordRequest(
    val action: String = "add_word",
    val word: String
)
