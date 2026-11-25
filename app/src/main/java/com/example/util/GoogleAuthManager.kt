package com.example.util

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.remote.SupabaseClientManager
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class GoogleSignInResult {
    data class Success(
        val idToken: String,
        val email: String,
        val displayName: String,
        val avatarUrl: String?
    ) : GoogleSignInResult()

    data class Error(val message: String) : GoogleSignInResult()

    object Cancelled : GoogleSignInResult()
}

object GoogleAuthManager {
    private const val TAG = "GoogleAuthManager"

    private val EMAIL_AUTHENTICATOR_KEYWORDS = listOf(
        "mail", "imap", "pop", "smtp", "exchange", "outlook",
        "yahoo", "proton", "zoho", "icloud", "gmx", "yandex", "fastmail"
    )

    /**
     * Collects all account types registered on this device that correspond to Google or email accounts
     * (Gmail, Google Workspace, Outlook, Exchange, IMAP/POP3, Yahoo, etc.).
     */
    fun getEmailAccountTypes(context: Context): Array<String> {
        val types = linkedSetOf("com.google")
        try {
            val am = AccountManager.get(context)
            am.authenticatorTypes.forEach { desc ->
                val type = desc.type
                val lower = type.lowercase()
                val isEmailAuth = lower == "com.google" ||
                    lower.startsWith("com.google.android.gm.") ||
                    lower == "com.android.email" ||
                    lower == "com.android.exchange" ||
                    lower.startsWith("com.microsoft.office.outlook") ||
                    lower.startsWith("com.microsoft.exchange") ||
                    lower.startsWith("com.yahoo.") ||
                    (!lower.contains("voicemail") && EMAIL_AUTHENTICATOR_KEYWORDS.any { lower.contains(it) })
                if (isEmailAuth) {
                    types.add(type)
                }
            }
            // Also include the account type of any visible account whose name is an email address
            am.accounts.forEach { acc ->
                if (Patterns.EMAIL_ADDRESS.matcher(acc.name.trim()).matches()) {
                    types.add(acc.type)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inspect authenticator types: ${e.message}")
        }
        return types.toTypedArray()
    }

    /**
     * Returns all email addresses currently visible to the app from the device's AccountManager.
     * Note: On Android 8+, AccountManager restricts visibility of accounts until chosen via
     * [createAccountChooserIntent], so launching [createAccountChooserIntent] is the primary way
     * to display every email account installed on the device.
     */
    fun getVisibleDeviceEmailAccounts(context: Context): List<String> {
        val seen = linkedSetOf<String>()
        try {
            val am = AccountManager.get(context)
            val allAccounts = buildList {
                addAll(am.accounts)
                addAll(am.getAccountsByType("com.google"))
            }
            for (acc in allAccounts) {
                val candidate = acc.name.trim().lowercase()
                if (Patterns.EMAIL_ADDRESS.matcher(candidate).matches()) {
                    seen.add(candidate)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read device accounts: ${e.message}")
        }
        return seen.toList()
    }

    fun createAccountChooserIntent(context: Context? = null): Intent {
        val allowableTypes = if (context != null) getEmailAccountTypes(context) else arrayOf("com.google")
        val prompt = "Choose an account to continue to VibeChat"
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AccountManager.newChooseAccountIntent(
                null,
                null,
                allowableTypes,
                prompt,
                null,
                null,
                null
            )
        } else {
            @Suppress("DEPRECATION")
            AccountManager.newChooseAccountIntent(
                null,
                null,
                allowableTypes,
                true,
                prompt,
                null,
                null,
                null
            )
        }
        intent.putExtra("alwaysPromptForAccount", true)
        return intent
    }

    /**
     * Launches the real Google OAuth 2.0 Web / Custom Tab flow
     */
    fun launchGoogleOAuthWeb(context: Context) {
        val baseUrl = SupabaseClientManager.getProjectBaseUrl()
        val oauthUrl = "$baseUrl/auth/v1/authorize?provider=google&redirect_to=vibechat://auth-callback"
        Log.i(TAG, "Launching Google OAuth Web: $oauthUrl")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun hasValidWebClientId(clientId: String): Boolean {
        if (!clientId.endsWith(".apps.googleusercontent.com")) return false
        val prefix = clientId.substringBefore("-")
        return prefix.isNotEmpty() && prefix.all { it.isDigit() }
    }

    fun launchGoogleSignIn(
        context: Context,
        scope: CoroutineScope,
        onFallbackToChooser: () -> Unit = { launchGoogleOAuthWeb(context) },
        onResult: (GoogleSignInResult) -> Unit
    ) {
        val activity = context as? Activity
        if (activity == null) {
            onFallbackToChooser()
            return
        }

        val serverClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else ""
        } catch (_: Exception) {
            ""
        }

        if (!hasValidWebClientId(serverClientId)) {
            Log.i(TAG, "Web client ID not present, launching Google Chooser")
            scope.launch(Dispatchers.Main) {
                onFallbackToChooser()
            }
            return
        }

        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    credentialManager.getCredential(activity, request)
                }
                val credential = response.credential

                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleId = GoogleIdTokenCredential.createFrom(credential.data)
                    val token = googleId.idToken
                    val email = googleId.id
                    val displayName = googleId.displayName ?: googleId.givenName ?: email.substringBefore("@")
                    val avatarUrl = googleId.profilePictureUri?.toString()

                    onResult(
                        GoogleSignInResult.Success(
                            idToken = token,
                            email = email,
                            displayName = displayName,
                            avatarUrl = avatarUrl
                        )
                    )
                } else {
                    withContext(Dispatchers.Main) {
                        onFallbackToChooser()
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                Log.i(TAG, "Google Sign-In was cancelled by user: ${e.message}")
                onResult(GoogleSignInResult.Cancelled)
            } catch (e: NoCredentialException) {
                withContext(Dispatchers.Main) {
                    onFallbackToChooser()
                }
            } catch (e: Exception) {
                Log.w(TAG, "CredentialManager exception, launching Google Chooser: ${e.message}")
                withContext(Dispatchers.Main) {
                    onFallbackToChooser()
                }
            }
        }
    }

    fun signOut(
        context: Context,
        scope: CoroutineScope,
        onComplete: () -> Unit = {}
    ) {
        val credentialManager = CredentialManager.create(context)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                onComplete()
            }
        }
    }
}
