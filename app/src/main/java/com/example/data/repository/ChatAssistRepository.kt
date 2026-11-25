package com.example.data.repository

import com.example.BuildConfig
import com.example.data.remote.SupabaseClientManager
import com.example.data.remote.assist.AddWordRequest
import com.example.data.remote.assist.AddWordResponse
import com.example.data.remote.assist.ChatAssistMessage
import com.example.data.remote.assist.CorrectRequest
import com.example.data.remote.assist.CorrectResponse
import com.example.data.remote.assist.ErrorResponse
import com.example.data.remote.assist.RepliesRequest
import com.example.data.remote.assist.RepliesResponse
import com.example.data.remote.assist.RewriteRequest
import com.example.data.remote.assist.TextResponse
import com.example.data.remote.assist.TranslateRequest
import com.example.util.SpellCheckerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

sealed interface ChatAssistResult<out T> {
    data class Success<T>(val data: T) : ChatAssistResult<T>
    data class Error(
        val code: String,
        val message: String,
        val isRateLimited: Boolean = false,
        val isUnauthorized: Boolean = false
    ) : ChatAssistResult<Nothing>
}

interface ChatAssistApi {
    @POST("functions/v1/Chat-assist")
    suspend fun executeAssistAction(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Header("x-target-path") targetPath: String = "/functions/v1/Chat-assist",
        @Header("Content-Type") contentType: String = "application/json",
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("functions/v1/Access-point")
    suspend fun executeViaAccessPoint(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Header("x-target-path") targetPath: String = "/functions/v1/Chat-assist",
        @Header("Content-Type") contentType: String = "application/json",
        @Body body: RequestBody
    ): Response<ResponseBody>
}

class ChatAssistRepository(
    private val sessionTokenProvider: suspend () -> String?,
    private val sessionRefresher: suspend () -> String?,
    private val baseUrl: String = "https://dktdmacqwloszwkvprcy.supabase.co/"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: ChatAssistApi by lazy {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .build()
            .create(ChatAssistApi::class.java)
    }

    /**
     * Correct spelling & grammar (max 600 chars).
     * Action: "correct"
     * Offline-first + cloud Edge Function synchronization.
     */
    suspend fun correct(text: String): ChatAssistResult<CorrectResponse> = withContext(Dispatchers.IO) {
        val bounded = text.trim().take(600)
        if (bounded.length < 2) {
            return@withContext ChatAssistResult.Success(CorrectResponse(emptyList(), bounded))
        }

        // 1. Instant local dictionary check for zero-latency accuracy
        val localResult = SpellCheckerEngine.checkText(bounded)

        // 2. Query cloud Edge Function
        val requestJson = json.encodeToString(CorrectRequest(action = "correct", text = bounded))
        val cloudRes = try {
            executeRequestWithRetry<CorrectResponse>(requestJson)
        } catch (_: Exception) {
            null
        }

        if (cloudRes is ChatAssistResult.Success && cloudRes.data.corrections.isNotEmpty()) {
            val combined = (localResult.corrections + cloudRes.data.corrections)
                .distinctBy { it.original.lowercase() }
            val correctedText = if (cloudRes.data.corrected.isNotBlank()) {
                cloudRes.data.corrected
            } else {
                localResult.corrected
            }
            return@withContext ChatAssistResult.Success(
                CorrectResponse(corrections = combined, corrected = correctedText)
            )
        }

        // Return reliable offline-first result
        ChatAssistResult.Success(localResult)
    }

    /**
     * Smart replies based on up to the last 8 messages.
     * Action: "replies"
     */
    suspend fun replies(messages: List<ChatAssistMessage>): ChatAssistResult<List<String>> = withContext(Dispatchers.IO) {
        val last8 = messages.takeLast(8)
        if (last8.isEmpty()) {
            return@withContext ChatAssistResult.Success(emptyList())
        }
        val requestJson = json.encodeToString(RepliesRequest(action = "replies", messages = last8))
        when (val res = executeRequestWithRetry<RepliesResponse>(requestJson)) {
            is ChatAssistResult.Success -> ChatAssistResult.Success(res.data.replies)
            is ChatAssistResult.Error -> res
        }
    }

    /**
     * Rewrite text with a selected tone: casual, formal, friendly, shorter, funny.
     * Action: "rewrite"
     */
    suspend fun rewrite(text: String, tone: String): ChatAssistResult<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext ChatAssistResult.Error("no_text", "Text cannot be empty for rewrite")
        }
        val validTone = when (tone.lowercase().trim()) {
            "casual", "formal", "friendly", "shorter", "funny" -> tone.lowercase().trim()
            else -> "casual"
        }
        val requestJson = json.encodeToString(RewriteRequest(action = "rewrite", text = text.trim(), tone = validTone))
        when (val res = executeRequestWithRetry<TextResponse>(requestJson)) {
            is ChatAssistResult.Success -> ChatAssistResult.Success(res.data.text)
            is ChatAssistResult.Error -> {
                // Local fallback for rewrite tone if cloud is offline
                val fallback = when (validTone) {
                    "formal" -> "I would like to convey that ${text.trim()}."
                    "friendly" -> "Hey! Just wanted to share: ${text.trim()} 😊"
                    "shorter" -> text.trim().split(" ").take(8).joinToString(" ")
                    "funny" -> "${text.trim()} (or so they say! 🚀)"
                    else -> text.trim()
                }
                ChatAssistResult.Success(fallback)
            }
        }
    }

    /**
     * Translate text to target language (e.g. French, Spanish, German).
     * Action: "translate"
     */
    suspend fun translate(text: String, targetLang: String): ChatAssistResult<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext ChatAssistResult.Error("no_text", "Text cannot be empty for translation")
        }
        val requestJson = json.encodeToString(
            TranslateRequest(action = "translate", text = text.trim(), targetLang = targetLang.trim())
        )
        when (val res = executeRequestWithRetry<TextResponse>(requestJson)) {
            is ChatAssistResult.Success -> ChatAssistResult.Success(res.data.text)
            is ChatAssistResult.Error -> res
        }
    }

    /**
     * Add custom word to user dictionary.
     * Action: "add_word"
     */
    suspend fun addWord(word: String): ChatAssistResult<Boolean> = withContext(Dispatchers.IO) {
        val cleanWord = word.trim()
        if (cleanWord.isBlank()) {
            return@withContext ChatAssistResult.Error("bad_request", "Word cannot be blank")
        }
        SpellCheckerEngine.addWord(cleanWord)
        try {
            val requestJson = json.encodeToString(AddWordRequest(action = "add_word", word = cleanWord))
            executeRequestWithRetry<AddWordResponse>(requestJson)
        } catch (_: Exception) {}
        ChatAssistResult.Success(true)
    }

    /**
     * Core execution helper handling headers, retry on 401 unauthorized, 429 rate limit, 502 assist failure,
     * and 400 bad request without logging sensitive tokens or keys.
     */
    private suspend inline fun <reified T> executeRequestWithRetry(jsonPayload: String): ChatAssistResult<T> {
        var token = sessionTokenProvider()
        if (token.isNullOrBlank()) {
            token = sessionRefresher()
        }

        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val effectiveToken = token?.takeIf { it.isNotBlank() } ?: anonKey

        val firstAttempt = performHttpCall<T>(jsonPayload, effectiveToken, anonKey)
        if (firstAttempt is ChatAssistResult.Error && firstAttempt.isUnauthorized) {
            // 401 received: Refresh session and retry once
            val refreshedToken = sessionRefresher()
            if (!refreshedToken.isNullOrBlank()) {
                return performHttpCall<T>(jsonPayload, refreshedToken, anonKey)
            }
        }
        return firstAttempt
    }

    private suspend inline fun <reified T> performHttpCall(
        jsonPayload: String,
        token: String,
        anonKey: String
    ): ChatAssistResult<T> {
        return try {
            val body = jsonPayload.toRequestBody(jsonMediaType)
            val authHeader = "Bearer $token"

            var response = try {
                api.executeAssistAction(
                    authorization = authHeader,
                    apiKey = anonKey,
                    body = body
                )
            } catch (_: Exception) {
                null
            }

            if (response == null || !response.isSuccessful) {
                // Fallback via Edge Function Access-point gateway
                try {
                    response = api.executeViaAccessPoint(
                        authorization = authHeader,
                        apiKey = anonKey,
                        targetPath = "/functions/v1/Chat-assist",
                        body = jsonPayload.toRequestBody(jsonMediaType)
                    )
                } catch (_: Exception) {}
            }

            if (response == null) {
                return ChatAssistResult.Error(code = "network_error", message = "Edge function unreachable")
            }

            val statusCode = response.code()
            val rawBody = response.body()?.string()
            val errBody = response.errorBody()?.string()

            when {
                response.isSuccessful && rawBody != null -> {
                    try {
                        val parsed = json.decodeFromString<T>(rawBody)
                        ChatAssistResult.Success(parsed)
                    } catch (e: Exception) {
                        ChatAssistResult.Error(
                            code = "assist_failed",
                            message = "Failed to parse AI response"
                        )
                    }
                }
                statusCode == 401 -> {
                    ChatAssistResult.Error(
                        code = "unauthorized",
                        message = "Cloud session expired (401)",
                        isUnauthorized = true
                    )
                }
                statusCode == 429 -> {
                    ChatAssistResult.Error(
                        code = "rate_limited",
                        message = "Rate limit reached (max 40 requests/min). Please slow down.",
                        isRateLimited = true
                    )
                }
                statusCode == 502 -> {
                    val code = extractErrorCode(errBody) ?: "assist_failed"
                    ChatAssistResult.Error(
                        code = code,
                        message = "AI service temporarily unavailable (502)."
                    )
                }
                statusCode == 400 -> {
                    val code = extractErrorCode(errBody) ?: "bad_request"
                    val desc = when (code) {
                        "no_text" -> "No text was provided"
                        "no_messages" -> "No chat messages found"
                        else -> "Invalid request ($code)"
                    }
                    ChatAssistResult.Error(code = code, message = desc)
                }
                else -> {
                    val code = extractErrorCode(errBody) ?: "error_$statusCode"
                    ChatAssistResult.Error(
                        code = code,
                        message = "AI Assist request returned status $statusCode"
                    )
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            ChatAssistResult.Error(
                code = "network_error",
                message = e.localizedMessage ?: "Network connection error"
            )
        }
    }

    private fun extractErrorCode(jsonString: String?): String? {
        if (jsonString.isNullOrBlank()) return null
        return try {
            val errObj = json.decodeFromString<ErrorResponse>(jsonString)
            errObj.error
        } catch (_: Exception) {
            null
        }
    }
}
