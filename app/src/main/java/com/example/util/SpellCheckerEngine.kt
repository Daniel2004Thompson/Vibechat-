package com.example.util

import com.example.data.remote.assist.ChatAssistCorrection
import com.example.data.remote.assist.CorrectResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, offline-first spell checking and auto-correction engine.
 * Ensures instant word checking and autocorrect suggestions with support for
 * user dictionaries and cloud Edge Function integration.
 */
object SpellCheckerEngine {

    private val userCustomWords = ConcurrentHashMap.newKeySet<String>()

    private val TYPO_DICTIONARY: Map<String, String> = mapOf(
        // Frequent transpositions & contractions
        "teh" to "the",
        "adn" to "and",
        "taht" to "that",
        "waht" to "what",
        "wiht" to "with",
        "thier" to "their",
        "recieve" to "receive",
        "seperate" to "separate",
        "definately" to "definitely",
        "definitley" to "definitely",
        "tommorow" to "tomorrow",
        "tomorow" to "tomorrow",
        "untill" to "until",
        "alot" to "a lot",
        "beleive" to "believe",
        "occured" to "occurred",
        "truely" to "truly",
        "calender" to "calendar",
        "embarass" to "embarrass",
        "accomodate" to "accommodate",
        "wrng" to "wrong",
        "cheking" to "checking",
        "chek" to "check",
        "spel" to "spell",
        "speling" to "spelling",
        "messege" to "message",
        "mesage" to "message",
        "msgs" to "messages",
        "autocorrect" to "auto-correct",
        "conect" to "connect",
        "conection" to "connection",
        "freind" to "friend",
        "freinds" to "friends",
        "peopel" to "people",
        "becuase" to "because",
        "becasue" to "because",
        "beautifull" to "beautiful",
        "awsome" to "awesome",
        "exellent" to "excellent",
        "comitted" to "committed",
        "goverment" to "government",
        "enviroment" to "environment",
        "guarentee" to "guarantee",
        "neccessary" to "necessary",
        "necesary" to "necessary",
        "privilege" to "privilege",
        "reccomend" to "recommend",
        "recomend" to "recommend",
        "suprise" to "surprise",
        "threw" to "through",
        "wierd" to "weird",
        "wether" to "whether",
        "wich" to "which",

        // Contractions without apostrophes
        "im" to "I'm",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "didnt" to "didn't",
        "isnt" to "isn't",
        "arent" to "aren't",
        "wasnt" to "wasn't",
        "werent" to "weren't",
        "hasnt" to "hasn't",
        "havent" to "haven't",
        "hadnt" to "hadn't",
        "wouldnt" to "wouldn't",
        "couldnt" to "couldn't",
        "shouldnt" to "shouldn't",
        "youre" to "you're",
        "theyre" to "they're",
        "weve" to "we've",
        "youve" to "you've",
        "theyve" to "they've",
        "ive" to "I've",
        "ill" to "I'll",
        "youll" to "you'll",
        "theyll" to "they'll",
        "hell" to "he'll",
        "shell" to "she'll",
        "thats" to "that's",
        "whats" to "what's",
        "wheres" to "where's",
        "hows" to "how's",
        "lets" to "let's",

        // Internet slang / abbreviations to clean auto-corrections
        "u" to "you",
        "r" to "are",
        "ur" to "your",
        "pls" to "please",
        "plz" to "please",
        "thx" to "thanks",
        "ty" to "thank you",
        "idk" to "I don't know",
        "tbh" to "to be honest",
        "imo" to "in my opinion",
        "brb" to "be right back",
        "omg" to "oh my god",
        "gonna" to "going to",
        "wanna" to "want to",
        "gotta" to "got to",
        "kinda" to "kind of",
        "dunno" to "don't know",
        "lemme" to "let me",
        "gimme" to "give me",
        "probly" to "probably",
        "prolly" to "probably"
    )

    fun addWord(word: String): Boolean {
        val clean = word.trim().lowercase()
        if (clean.isBlank()) return false
        userCustomWords.add(clean)
        return true
    }

    fun isWordInUserDictionary(word: String): Boolean {
        return userCustomWords.contains(word.trim().lowercase())
    }

    fun checkText(text: String): CorrectResponse {
        if (text.isBlank()) return CorrectResponse(emptyList(), text)

        val wordsRegex = "\\b[a-zA-Z']+\\b".toRegex()
        val matches = wordsRegex.findAll(text).toList()

        val corrections = mutableListOf<ChatAssistCorrection>()
        val seenOriginals = mutableSetOf<String>()
        var correctedText = text

        for (m in matches) {
            val rawWord = m.value
            val lowerWord = rawWord.lowercase()

            if (userCustomWords.contains(lowerWord)) continue

            val suggestion = TYPO_DICTIONARY[lowerWord]
            if (suggestion != null && !suggestion.equals(rawWord, ignoreCase = false)) {
                // Preserve capitalization style
                val formattedSuggestion = matchCapitalization(rawWord, suggestion)
                if (formattedSuggestion != rawWord && !seenOriginals.contains(lowerWord)) {
                    seenOriginals.add(lowerWord)
                    corrections.add(
                        ChatAssistCorrection(
                            original = rawWord,
                            suggestion = formattedSuggestion
                        )
                    )
                }
            }
        }

        // Apply replacements to build correctedText
        for (c in corrections) {
            val regex = "\\b${Regex.escape(c.original)}\\b".toRegex(RegexOption.IGNORE_CASE)
            correctedText = regex.replace(correctedText, c.suggestion)
        }

        return CorrectResponse(
            corrections = corrections,
            corrected = correctedText
        )
    }

    private fun matchCapitalization(original: String, target: String): String {
        return when {
            original.all { it.isUpperCase() } -> target.uppercase()
            original.firstOrNull()?.isUpperCase() == true -> {
                target.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            else -> target
        }
    }
}
