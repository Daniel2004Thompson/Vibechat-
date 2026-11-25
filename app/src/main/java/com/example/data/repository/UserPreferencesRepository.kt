package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vibechat_prefs")

data class UserSessionPrefs(
    val isLoggedIn: Boolean = false,
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val bio: String = "Hey there! I'm vibing on VibeChat ✨",
    val statusText: String = "Available",
    val avatarColor: String = "#10B981",
    val avatarUri: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val darkMode: String = "DARK", // DARK, LIGHT, SYSTEM
    val accentThemeId: String = "emerald",
    val readReceiptsEnabled: Boolean = true,
    val onlineStatusVisible: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val USERNAME = stringPreferencesKey("username")
        val BIO = stringPreferencesKey("bio")
        val STATUS_TEXT = stringPreferencesKey("status_text")
        val AVATAR_COLOR = stringPreferencesKey("avatar_color")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val READ_RECEIPTS = booleanPreferencesKey("read_receipts")
        val ONLINE_STATUS = booleanPreferencesKey("online_status")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val VIBRATION = booleanPreferencesKey("vibration")
    }

    val sessionFlow: Flow<UserSessionPrefs> = context.dataStore.data.map { prefs ->
        UserSessionPrefs(
            isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false,
            userId = prefs[Keys.USER_ID] ?: "",
            email = prefs[Keys.EMAIL] ?: "",
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            username = prefs[Keys.USERNAME] ?: "",
            bio = prefs[Keys.BIO] ?: "Hey there! I'm vibing on VibeChat ✨",
            statusText = prefs[Keys.STATUS_TEXT] ?: "Available",
            avatarColor = prefs[Keys.AVATAR_COLOR] ?: "#10B981",
            avatarUri = prefs[Keys.AVATAR_URI],
            accessToken = prefs[Keys.ACCESS_TOKEN],
            refreshToken = prefs[Keys.REFRESH_TOKEN],
            darkMode = prefs[Keys.DARK_MODE] ?: "DARK",
            accentThemeId = prefs[Keys.ACCENT_THEME] ?: "emerald",
            readReceiptsEnabled = prefs[Keys.READ_RECEIPTS] ?: true,
            onlineStatusVisible = prefs[Keys.ONLINE_STATUS] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION] ?: true
        )
    }

    suspend fun saveSession(
        userId: String,
        email: String,
        displayName: String,
        username: String,
        bio: String,
        avatarColor: String,
        accessToken: String?,
        refreshToken: String? = null,
        avatarUri: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.USER_ID] = userId
            prefs[Keys.EMAIL] = email
            prefs[Keys.DISPLAY_NAME] = displayName
            prefs[Keys.USERNAME] = username
            prefs[Keys.BIO] = bio
            prefs[Keys.AVATAR_COLOR] = avatarColor
            if (!avatarUri.isNullOrBlank()) {
                prefs[Keys.AVATAR_URI] = avatarUri
            }
            if (accessToken != null) {
                prefs[Keys.ACCESS_TOKEN] = accessToken
            } else {
                prefs.remove(Keys.ACCESS_TOKEN)
            }
            if (refreshToken != null) {
                prefs[Keys.REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun updateAccessToken(accessToken: String, refreshToken: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            if (refreshToken != null) {
                prefs[Keys.REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun updateProfileDetails(
        displayName: String,
        username: String,
        bio: String,
        statusText: String,
        avatarColor: String,
        avatarUri: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = displayName
            prefs[Keys.USERNAME] = username
            prefs[Keys.BIO] = bio
            prefs[Keys.STATUS_TEXT] = statusText
            prefs[Keys.AVATAR_COLOR] = avatarColor
            if (!avatarUri.isNullOrBlank()) {
                prefs[Keys.AVATAR_URI] = avatarUri
            } else {
                prefs.remove(Keys.AVATAR_URI)
            }
        }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode }
    }

    suspend fun setAccentTheme(accentId: String) {
        context.dataStore.edit { it[Keys.ACCENT_THEME] = accentId }
    }

    suspend fun setReadReceipts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.READ_RECEIPTS] = enabled }
    }

    suspend fun setOnlineStatusVisible(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ONLINE_STATUS] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
            prefs.remove(Keys.ACCESS_TOKEN)
        }
    }
}
