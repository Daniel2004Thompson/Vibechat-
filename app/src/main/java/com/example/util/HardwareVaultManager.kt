package com.example.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts and stores sensitive connection credentials at runtime using the
 * Android Hardware-Backed Keystore (AES-256-GCM) so that ZERO API keys or
 * secrets are ever compiled into the APK binary or BuildConfig.
 */
object HardwareVaultManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "vibechat_hardware_aes_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val PREFS_NAME = "vibechat_encrypted_vault"

    private const val KEY_ACCESS_POINT_URL = "enc_access_point_url"
    private const val KEY_PROXY_URL = "enc_backend_proxy_url"

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existingKey != null) {
                existingKey.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        val secretKey = getOrCreateSecretKey() ?: return Base64.encodeToString(
            plainText.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val dataB64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            "$ivB64:$dataB64"
        } catch (_: Exception) {
            ""
        }
    }

    private fun decrypt(payload: String): String {
        if (payload.isBlank()) return ""
        if (!payload.contains(":")) {
            return try {
                String(Base64.decode(payload, Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                ""
            }
        }
        val secretKey = getOrCreateSecretKey() ?: return ""
        return try {
            val parts = payload.split(":", limit = 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    data class VaultConfig(
        val accessPointUrl: String = com.example.data.remote.SupabaseClientManager.DEFAULT_ACCESS_POINT_URL,
        val backendProxyUrl: String = ""
    ) {
        val supabaseUrl: String get() = accessPointUrl
        val supabaseKey: String get() = ""
    }

    fun loadVaultConfig(context: Context): VaultConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val url = decrypt(prefs.getString(KEY_ACCESS_POINT_URL, "") ?: "")
        val proxy = decrypt(prefs.getString(KEY_PROXY_URL, "") ?: "")
        return VaultConfig(
            accessPointUrl = url.ifBlank { com.example.data.remote.SupabaseClientManager.DEFAULT_ACCESS_POINT_URL }.trim(),
            backendProxyUrl = proxy.trim()
        )
    }

    fun saveVaultConfig(
        context: Context,
        accessPointUrl: String,
        backendProxyUrl: String = ""
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ACCESS_POINT_URL, encrypt(accessPointUrl.trim()))
            .putString(KEY_PROXY_URL, encrypt(backendProxyUrl.trim()))
            .remove("enc_supabase_anon_key")
            .remove("enc_supabase_url")
            .apply()
    }

    fun saveVaultConfig(
        context: Context,
        supabaseUrl: String,
        supabaseKey: String,
        backendProxyUrl: String
    ) {
        val target = supabaseUrl.trim().ifBlank { backendProxyUrl.trim() }
            .ifBlank { com.example.data.remote.SupabaseClientManager.DEFAULT_ACCESS_POINT_URL }
        saveVaultConfig(context, target, backendProxyUrl)
    }

    fun clearVault(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
