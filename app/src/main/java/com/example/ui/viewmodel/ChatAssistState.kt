package com.example.ui.viewmodel

import com.example.data.remote.assist.ChatAssistCorrection

sealed interface SpellCheckState {
    object Idle : SpellCheckState
    object Checking : SpellCheckState
    data class Suggestions(
        val originalText: String,
        val corrections: List<ChatAssistCorrection>,
        val correctedText: String
    ) : SpellCheckState
    data class Error(val message: String) : SpellCheckState
}

sealed interface SmartRepliesState {
    object Idle : SmartRepliesState
    object Loading : SmartRepliesState
    data class Success(val replies: List<String>) : SmartRepliesState
    data class Error(val message: String) : SmartRepliesState
}

sealed interface RewriteState {
    object Idle : RewriteState
    data class Loading(val tone: String) : RewriteState
    data class Success(val rewrittenText: String, val tone: String) : RewriteState
    data class Error(val message: String) : RewriteState
}

sealed interface TranslateState {
    object Idle : TranslateState
    data class Loading(val messageId: String, val targetLang: String) : TranslateState
    data class Success(
        val messageId: String,
        val originalText: String,
        val translatedText: String,
        val targetLang: String
    ) : TranslateState
    data class Error(val messageId: String, val message: String) : TranslateState
}

sealed interface AddWordState {
    object Idle : AddWordState
    data class Loading(val word: String) : AddWordState
    data class Success(val word: String) : AddWordState
    data class Error(val message: String) : AddWordState
}

data class ChatAssistUiState(
    val spellCheck: SpellCheckState = SpellCheckState.Idle,
    val smartReplies: SmartRepliesState = SmartRepliesState.Idle,
    val rewrite: RewriteState = RewriteState.Idle,
    val translate: TranslateState = TranslateState.Idle,
    val addWord: AddWordState = AddWordState.Idle
)
