package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.StatusStoryEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.VibeChatDatabase
import com.example.data.remote.SupabaseAuthRegistry
import com.example.data.remote.SupabaseClientManager
import com.example.data.remote.assist.ChatAssistMessage
import com.example.data.repository.ChatAssistRepository
import com.example.data.repository.ChatAssistResult
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.UserSessionPrefs
import com.example.data.repository.VibeChatRepository
import com.example.util.HardwareVaultManager
import com.example.util.ProfilePictureManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChatFilterType(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    GROUPS("Groups"),
    DIRECT("Direct")
}

enum class ContactFilterType(val label: String) {
    ALL("All Contacts"),
    ONLINE("Online Now"),
    FAVORITES("Favorites")
}

data class GroupedStatusUser(
    val userId: String,
    val userName: String,
    val userHandle: String,
    val userAvatarColor: String,
    val userAvatarUri: String? = null,
    val stories: List<StatusStoryEntity>,
    val hasUnviewed: Boolean
)

class VibeChatViewModel(
    private val appContext: Context,
    private val repository: VibeChatRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val chatAssistRepository: ChatAssistRepository
) : ViewModel() {

    val sessionState: StateFlow<UserSessionPrefs> = prefsRepository.sessionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSessionPrefs()
        )

    // --- AI Chat Assist States ---
    private val _spellCheckState = MutableStateFlow<SpellCheckState>(SpellCheckState.Idle)
    val spellCheckState: StateFlow<SpellCheckState> = _spellCheckState.asStateFlow()

    private val _smartRepliesState = MutableStateFlow<SmartRepliesState>(SmartRepliesState.Idle)
    val smartRepliesState: StateFlow<SmartRepliesState> = _smartRepliesState.asStateFlow()

    private val _rewriteState = MutableStateFlow<RewriteState>(RewriteState.Idle)
    val rewriteState: StateFlow<RewriteState> = _rewriteState.asStateFlow()

    private val _translateState = MutableStateFlow<TranslateState>(TranslateState.Idle)
    val translateState: StateFlow<TranslateState> = _translateState.asStateFlow()

    private val _addWordState = MutableStateFlow<AddWordState>(AddWordState.Idle)
    val addWordState: StateFlow<AddWordState> = _addWordState.asStateFlow()

    private var spellCheckJob: Job? = null
    private var smartRepliesJob: Job? = null
    private var rewriteJob: Job? = null
    private var translateJob: Job? = null

    private val _vaultConfig = MutableStateFlow(HardwareVaultManager.VaultConfig())
    val vaultConfig: StateFlow<HardwareVaultManager.VaultConfig> = _vaultConfig.asStateFlow()

    private val _chatSearchQuery = MutableStateFlow("")
    val chatSearchQuery: StateFlow<String> = _chatSearchQuery.asStateFlow()

    private var searchChatPeopleJob: Job? = null
    private val _chatPeopleSearchResults = MutableStateFlow<List<UserProfileEntity>>(emptyList())
    val chatPeopleSearchResults: StateFlow<List<UserProfileEntity>> = _chatPeopleSearchResults.asStateFlow()

    private val _selectedChatFilter = MutableStateFlow(ChatFilterType.ALL)
    val selectedChatFilter: StateFlow<ChatFilterType> = _selectedChatFilter.asStateFlow()

    private val _contactSearchQuery = MutableStateFlow("")
    val contactSearchQuery: StateFlow<String> = _contactSearchQuery.asStateFlow()

    private var searchSupabaseJob: Job? = null
    private val _supabaseSearchResults = MutableStateFlow<List<UserProfileEntity>>(emptyList())
    val supabaseSearchResults: StateFlow<List<UserProfileEntity>> = _supabaseSearchResults.asStateFlow()

    private val _selectedContactFilter = MutableStateFlow(ContactFilterType.ALL)
    val selectedContactFilter: StateFlow<ContactFilterType> = _selectedContactFilter.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _activeConversationMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeConversationMessages: StateFlow<List<MessageEntity>> = _activeConversationMessages.asStateFlow()

    private val _activeConversation = MutableStateFlow<ConversationEntity?>(null)
    val activeConversation: StateFlow<ConversationEntity?> = _activeConversation.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<MessageEntity?>(null)
    val replyingToMessage: StateFlow<MessageEntity?> = _replyingToMessage.asStateFlow()

    private val _bannerNotification = MutableStateFlow<String?>(null)
    val bannerNotification: StateFlow<String?> = _bannerNotification.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private var messagesJob: Job? = null
    private var conversationJob: Job? = null

    val filteredConversations: StateFlow<List<ConversationEntity>> = combine(
        repository.conversationsFlow,
        repository.contactsFlow,
        _chatSearchQuery,
        _selectedChatFilter,
        sessionState
    ) { conversations, contacts, query, filter, session ->
        val myCanonical = com.example.data.remote.SupabaseAuthRegistry.resolveCanonicalUserId(session.userId, session.email)
        val savedContactIds = contacts.map { it.id }.toSet()
        val savedContactEmails = contacts.map { it.email.lowercase() }.filter { it.isNotBlank() }.toSet()
        val savedContactNames = contacts.map { it.displayName.lowercase() }.toSet()

        conversations.filter { conv ->
            val isUserParticipant = conv.isGroup ||
                conv.participantIds().contains(session.userId) ||
                (session.email.isNotBlank() && conv.participantIds().contains(session.email)) ||
                conv.participantIds().contains(myCanonical) ||
                conv.id.contains(myCanonical) ||
                conv.id.contains(session.userId)

            if (!isUserParticipant) return@filter false

            // Direct conversations must NOT be shown unless it's a self-chat OR the other user has been searched and added to contacts!
            if (!conv.isGroup) {
                val isSelfChat = conv.id.startsWith("conv_self_") ||
                    conv.name.endsWith("(You)") ||
                    conv.name.contains("(Message yourself)", ignoreCase = true)

                if (!isSelfChat) {
                    val otherParticipants = conv.participantIds().filter {
                        it != session.userId &&
                        it != myCanonical &&
                        !it.equals(session.email, ignoreCase = true)
                    }
                    val isOtherSaved = otherParticipants.any {
                        savedContactIds.contains(it) || savedContactEmails.contains(it.lowercase())
                    } || savedContactNames.contains(conv.name.lowercase())

                    if (!isOtherSaved) return@filter false
                }
            }

            val matchesQuery = query.isBlank() ||
                conv.name.contains(query, ignoreCase = true) ||
                conv.lastMessageText.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                ChatFilterType.ALL -> true
                ChatFilterType.UNREAD -> conv.unreadCount > 0
                ChatFilterType.GROUPS -> conv.isGroup
                ChatFilterType.DIRECT -> !conv.isGroup
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allContacts: StateFlow<List<UserProfileEntity>> = repository.contactsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredContacts: StateFlow<List<UserProfileEntity>> = combine(
        repository.contactsFlow,
        _contactSearchQuery,
        _selectedContactFilter,
        sessionState
    ) { contacts, query, filter, session ->
        contacts
            .filter { it.id != session.userId }
            .filter { contact ->
                val matchesQuery = query.isBlank() ||
                    contact.displayName.contains(query, ignoreCase = true) ||
                    contact.username.contains(query, ignoreCase = true) ||
                    contact.email.contains(query, ignoreCase = true)
                val matchesFilter = when (filter) {
                    ContactFilterType.ALL -> true
                    ContactFilterType.ONLINE -> contact.isOnline
                    ContactFilterType.FAVORITES -> contact.isFavorite
                }
                matchesQuery && matchesFilter
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val groupedStatuses: StateFlow<List<GroupedStatusUser>> = combine(
        repository.observeActiveStatuses(),
        sessionState,
        repository.contactsFlow
    ) { stories, session, contacts ->
        val contactAvatarMap = contacts.associate { it.id to it.avatarUri }
        val savedContactIds = contacts.map { it.id }.toSet()
        val savedContactEmails = contacts.map { it.email.lowercase() }.filter { it.isNotBlank() }.toSet()
        val savedContactUsernames = contacts.map { it.username.lowercase().removePrefix("@") }.filter { it.isNotBlank() }.toSet()

        val myId = session.userId
        val myEmail = session.email.lowercase()
        val myHandle = session.username.lowercase().removePrefix("@")
        val myCanonical = SupabaseAuthRegistry.resolveCanonicalUserId(myId, myEmail)

        // Privacy filter: Only show statuses from the logged-in user or contacts that were searched and added
        val filteredStories = stories.filter { story ->
            val storyCanonical = SupabaseAuthRegistry.resolveCanonicalUserId(story.userId, story.userId)
            val isMyStory = story.userId == myId ||
                story.userId == "user_me" ||
                storyCanonical == myCanonical ||
                (myEmail.isNotBlank() && story.userId.equals(myEmail, ignoreCase = true)) ||
                (myHandle.isNotBlank() && story.userHandle.lowercase().removePrefix("@") == myHandle)

            val storyHandle = story.userHandle.lowercase().removePrefix("@")
            val isSavedContactStory = savedContactIds.contains(story.userId) ||
                savedContactEmails.contains(story.userId.lowercase()) ||
                savedContactUsernames.contains(storyHandle)

            isMyStory || isSavedContactStory
        }

        filteredStories
            .groupBy { it.userId }
            .map { (uid, userStories) ->
                val sorted = userStories.sortedBy { it.createdAtMillis }
                val first = sorted.first()
                val isMe = uid == myId || uid == "user_me" || uid == myCanonical || (myEmail.isNotBlank() && uid.equals(myEmail, ignoreCase = true))
                val avatar = if (isMe) session.avatarUri else contactAvatarMap[uid]
                GroupedStatusUser(
                    userId = uid,
                    userName = if (isMe) "My Status" else first.userName,
                    userHandle = first.userHandle,
                    userAvatarColor = first.userAvatarColor,
                    userAvatarUri = avatar,
                    stories = sorted,
                    hasUnviewed = sorted.any { !it.isViewedByMe && !isMe }
                )
            }
            .sortedByDescending { it.stories.maxOfOrNull { s -> s.createdAtMillis } ?: 0L }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val starredMessages: StateFlow<List<MessageEntity>> = repository.starredMessagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            _vaultConfig.value = SupabaseClientManager.initializeFromVault(appContext)
            val prefs = prefsRepository.sessionFlow.first()
            if (prefs.isLoggedIn && prefs.userId.isNotBlank()) {
                repository.purgeAllMockData(prefs.userId, prefs.email)
                SupabaseClientManager.currentAccessToken = prefs.accessToken
                repository.ensureSeededForUser(
                    userId = prefs.userId,
                    email = prefs.email,
                    displayName = prefs.displayName,
                    username = prefs.username,
                    bio = prefs.bio,
                    avatarColor = prefs.avatarColor
                )
                repository.syncWithSupabase(prefs.userId, prefs.email)
            } else {
                repository.purgeAllMockData()
            }
        }
    }

    fun saveHardwareVaultConfig(
        supabaseUrl: String,
        supabaseKey: String,
        backendProxyUrl: String
    ) {
        viewModelScope.launch {
            HardwareVaultManager.saveVaultConfig(
                context = appContext,
                supabaseUrl = supabaseUrl,
                supabaseKey = "",
                backendProxyUrl = backendProxyUrl
            )
            _vaultConfig.value = SupabaseClientManager.initializeFromVault(appContext)
            showBanner("Edge Function Access-Point updated (Zero keys in APK)")
        }
    }

    fun clearHardwareVaultConfig() {
        viewModelScope.launch {
            HardwareVaultManager.clearVault(appContext)
            _vaultConfig.value = SupabaseClientManager.initializeFromVault(appContext)
            showBanner("Hardware Vault cleared")
        }
    }

    fun showBanner(message: String) {
        viewModelScope.launch {
            _bannerNotification.value = message
            delay(3200)
            if (_bannerNotification.value == message) {
                _bannerNotification.value = null
            }
        }
    }

    fun dismissBanner() {
        _bannerNotification.value = null
    }

    // --- Auth Actions ---

    fun signInWithPassword(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            showBanner("Please enter your email or username and password")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = repository.signInWithPassword(email, password)
            _isAuthLoading.value = false
            result.onSuccess { data ->
                prefsRepository.saveSession(
                    userId = data.userId,
                    email = data.email,
                    displayName = data.displayName,
                    username = data.username,
                    bio = data.bio,
                    avatarColor = data.avatarColor,
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken
                )
                showBanner(data.statusMessage)
                onSuccess()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signUpAndRequestVerification(
        email: String,
        password: String,
        displayName: String,
        username: String,
        bio: String,
        avatarColor: String,
        onVerificationSent: () -> Unit
    ) {
        if (email.isBlank() || password.length < 6 || displayName.isBlank()) {
            showBanner("Please provide display name, valid email, and a 6+ character password")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = repository.signUpAndSendVerificationCode(
                email = email,
                password = password,
                displayName = displayName,
                username = username,
                bio = bio,
                avatarColor = avatarColor
            )
            _isAuthLoading.value = false
            result.onSuccess { msg ->
                showBanner(msg)
                onVerificationSent()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Failed to sign up")
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
        username: String,
        bio: String,
        avatarColor: String,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.length < 6 || displayName.isBlank()) {
            showBanner("Please provide display name, valid email, and a 6+ character password")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = repository.signUpWithEmail(
                email = email,
                password = password,
                displayName = displayName,
                username = username,
                bio = bio,
                avatarColor = avatarColor
            )
            _isAuthLoading.value = false
            result.onSuccess { data ->
                prefsRepository.saveSession(
                    userId = data.userId,
                    email = data.email,
                    displayName = data.displayName,
                    username = data.username,
                    bio = data.bio,
                    avatarColor = data.avatarColor,
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken
                )
                showBanner(data.statusMessage)
                onSuccess()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Sign up failed")
            }
        }
    }

    fun requestEmailOtp(
        email: String,
        displayName: String = "",
        username: String = "",
        onOtpSent: () -> Unit
    ) {
        if (email.isBlank()) {
            showBanner("Enter your Email Address or Username to receive a 6-digit code")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val res = repository.sendEmailOtpCode(email, displayName, username)
            _isAuthLoading.value = false
            res.onSuccess { msg ->
                showBanner(msg)
                onOtpSent()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Failed to send verification code")
            }
        }
    }

    fun requestPasswordReset(email: String, onSent: () -> Unit) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            showBanner("Please enter your email address to reset password")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val res = repository.sendPasswordResetOtp(cleanEmail)
            _isAuthLoading.value = false
            res.onSuccess { msg ->
                showBanner(msg)
                onSent()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Failed to send password reset code")
            }
        }
    }

    fun verifyPasswordReset(
        email: String,
        otpCode: String,
        newPassword: String,
        onSuccess: () -> Unit
    ) {
        val cleanCode = otpCode.trim()
        if (cleanCode.length < 6) {
            showBanner("Please enter the 6-digit recovery code from your email")
            return
        }
        if (newPassword.trim().length < 6) {
            showBanner("New password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val res = repository.verifyPasswordReset(email, cleanCode, newPassword)
            _isAuthLoading.value = false
            res.onSuccess { data ->
                prefsRepository.saveSession(
                    userId = data.userId,
                    email = data.email,
                    displayName = data.displayName,
                    username = data.username,
                    bio = data.bio,
                    avatarColor = data.avatarColor,
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken
                )
                showBanner(data.statusMessage)
                onSuccess()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Invalid or expired recovery code")
            }
        }
    }

    fun verifyEmailOtp(
        email: String,
        otpCode: String,
        displayName: String = "",
        username: String = "",
        avatarColor: String = "#10B981",
        onSuccess: () -> Unit
    ) {
        if (otpCode.trim().length < 6) {
            showBanner("Please enter the complete 6-digit verification code")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val res = repository.verifyEmailOtpCode(email, otpCode, displayName, username, avatarColor)
            _isAuthLoading.value = false
            res.onSuccess { data ->
                prefsRepository.saveSession(
                    userId = data.userId,
                    email = data.email,
                    displayName = data.displayName,
                    username = data.username,
                    bio = data.bio,
                    avatarColor = data.avatarColor,
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken
                )
                showBanner(data.statusMessage)
                onSuccess()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Invalid verification code")
            }
        }
    }

    fun signInWithGoogle(
        idToken: String,
        email: String,
        displayName: String,
        avatarUri: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val res = repository.signInWithGoogle(
                idToken = idToken,
                googleEmail = email,
                googleDisplayName = displayName,
                googleAvatarUri = avatarUri
            )
            _isAuthLoading.value = false
            res.onSuccess { data ->
                prefsRepository.saveSession(
                    userId = data.userId,
                    email = data.email,
                    displayName = data.displayName,
                    username = data.username,
                    bio = data.bio,
                    avatarColor = data.avatarColor,
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken,
                    avatarUri = data.avatarUri
                )
                showBanner(data.statusMessage)
                onSuccess()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }

    fun signOut(context: Context? = null) {
        viewModelScope.launch {
            SupabaseClientManager.currentAccessToken = null
            prefsRepository.clearSession()
            if (context != null) {
                com.example.util.GoogleAuthManager.signOut(context, viewModelScope)
            }
            showBanner("Signed out of VibeChat")
        }
    }

    // --- Cloud Sync ---

    fun syncWithSupabaseCloud() {
        val session = sessionState.value
        val uid = session.userId.ifBlank { "user_default" }
        viewModelScope.launch {
            _isSyncingCloud.value = true
            val res = repository.syncWithSupabase(uid, session.email)
            _isSyncingCloud.value = false
            res.onSuccess { msg ->
                showBanner(msg)
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Configure Backend Proxy or Hardware Vault in Settings first")
            }
        }
    }

    // --- Conversation & Message Actions ---

    fun setChatSearchQuery(query: String) {
        _chatSearchQuery.value = query
        searchChatPeopleJob?.cancel()
        val clean = query.trim().removePrefix("@")
        if (clean.isNotBlank()) {
            searchChatPeopleJob = viewModelScope.launch {
                delay(120L)
                val results = repository.searchSupabaseByUsername(clean)
                val currentUid = sessionState.value.userId
                _chatPeopleSearchResults.value = results.filter { it.id != currentUid }
            }
        } else {
            _chatPeopleSearchResults.value = emptyList()
        }
    }

    fun setChatFilter(filter: ChatFilterType) {
        _selectedChatFilter.value = filter
    }

    fun setContactSearchQuery(query: String) {
        _contactSearchQuery.value = query
        searchSupabaseJob?.cancel()
        val clean = query.trim().removePrefix("@")
        if (clean.isNotBlank()) {
            searchSupabaseJob = viewModelScope.launch {
                delay(120L)
                val results = repository.searchSupabaseByUsername(clean)
                val currentUid = sessionState.value.userId
                _supabaseSearchResults.value = results.filter { it.id != currentUid }
            }
        } else {
            _supabaseSearchResults.value = emptyList()
        }
    }

    fun addContactFromSupabase(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveContact(profile)
            showBanner("Added @${profile.username} to your contacts!")
        }
    }

    fun setContactFilter(filter: ContactFilterType) {
        _selectedContactFilter.value = filter
    }

    fun openConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        _replyingToMessage.value = null
        clearSpellCheck()
        clearTranslation()
        resetRewriteState()

        messagesJob?.cancel()
        conversationJob?.cancel()

        viewModelScope.launch {
            repository.markConversationRead(conversationId, sessionState.value.userId)
            repository.syncConversationMessagesFromSupabase(conversationId)
        }

        conversationJob = viewModelScope.launch {
            repository.observeConversation(conversationId).collect { conv ->
                _activeConversation.value = conv
            }
        }

        messagesJob = viewModelScope.launch {
            repository.observeMessages(conversationId).collect { list ->
                _activeConversationMessages.value = list
                loadSmartReplies(list, sessionState.value.userId)
            }
        }
    }

    fun closeActiveConversation() {
        _activeConversationId.value = null
        _replyingToMessage.value = null
        clearSpellCheck()
        dismissSmartReplies()
        clearTranslation()
        resetRewriteState()
    }

    fun setReplyingToMessage(message: MessageEntity?) {
        _replyingToMessage.value = message
    }

    fun sendTextMessage(text: String, messageType: String = "TEXT", mediaUri: String? = null) {
        val convId = _activeConversationId.value ?: return
        val cleanText = text.trim()
        if (cleanText.isEmpty() && mediaUri == null) return

        val session = sessionState.value
        val myCanonical = com.example.data.remote.SupabaseAuthRegistry.resolveCanonicalUserId(session.userId, session.email)
        val conv = _activeConversation.value
        if (conv != null && !conv.isGroup) {
            val otherId = conv.participantIds().firstOrNull { 
                it != session.userId && 
                !it.equals(session.email, ignoreCase = true) && 
                !it.equals(myCanonical, ignoreCase = true) 
            }
            val otherContact = allContacts.value.firstOrNull { 
                it.id == otherId || 
                it.email.equals(otherId, ignoreCase = true) || 
                it.displayName.equals(conv.name, ignoreCase = true) 
            }
            if (otherContact?.isBlocked == true) {
                showBanner("Unblock ${otherContact.displayName} to send messages")
                return
            }
        }

        val senderId = session.userId.ifBlank { myCanonical }
        val senderName = session.displayName.ifBlank { "You" }
        val replyTarget = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            repository.sendMessage(
                conversationId = convId,
                senderId = senderId,
                senderName = senderName,
                senderAvatarColor = session.avatarColor,
                content = cleanText,
                messageType = messageType,
                mediaUri = mediaUri,
                replyToMessage = replyTarget
            )
        }
    }

    fun sendVoiceMessage(filePath: String?, durationSec: Int, waveformCsv: String) {
        val convId = _activeConversationId.value ?: return
        val session = sessionState.value
        val myCanonical = com.example.data.remote.SupabaseAuthRegistry.resolveCanonicalUserId(session.userId, session.email)
        val conv = _activeConversation.value
        if (conv != null && !conv.isGroup) {
            val otherId = conv.participantIds().firstOrNull { 
                it != session.userId && 
                !it.equals(session.email, ignoreCase = true) && 
                !it.equals(myCanonical, ignoreCase = true) 
            }
            val otherContact = allContacts.value.firstOrNull { 
                it.id == otherId || 
                it.email.equals(otherId, ignoreCase = true) || 
                it.displayName.equals(conv.name, ignoreCase = true) 
            }
            if (otherContact?.isBlocked == true) {
                showBanner("Unblock ${otherContact.displayName} to send voice notes")
                return
            }
        }

        val senderId = session.userId.ifBlank { myCanonical }
        val senderName = session.displayName.ifBlank { "You" }
        val replyTarget = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            repository.sendMessage(
                conversationId = convId,
                senderId = senderId,
                senderName = senderName,
                senderAvatarColor = session.avatarColor,
                content = "Voice message",
                messageType = "VOICE",
                mediaUri = filePath,
                voiceDurationSec = durationSec,
                waveformCsv = waveformCsv,
                replyToMessage = replyTarget
            )
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val uid = sessionState.value.userId.ifBlank { "user_me" }
        viewModelScope.launch {
            repository.toggleMessageReaction(messageId, emoji, uid)
        }
    }

    fun toggleStarMessage(messageId: String) {
        viewModelScope.launch {
            repository.toggleMessageStarred(messageId)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
            showBanner("Message deleted")
        }
    }

    fun togglePinConversation(conversationId: String) {
        viewModelScope.launch {
            repository.togglePinConversation(conversationId)
        }
    }

    fun toggleMuteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.toggleMuteConversation(conversationId)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
            showBanner("Conversation deleted")
        }
    }

    fun openDirectChatWithContact(contact: UserProfileEntity, onChatReady: (String) -> Unit) {
        val session = sessionState.value
        val uid = session.userId.ifBlank { "user_me" }
        viewModelScope.launch {
            val convId = repository.startOrOpenDirectChat(uid, contact, session.email)
            openConversation(convId)
            onChatReady(convId)
        }
    }

    fun openSelfChat(onChatReady: (String) -> Unit) {
        val session = sessionState.value
        val uid = session.userId.ifBlank { "user_me" }
        viewModelScope.launch {
            val convId = repository.startOrOpenSelfChat(
                currentUserId = uid,
                currentUserEmail = session.email,
                currentDisplayName = session.displayName.ifBlank { "You" },
                currentAvatarColor = session.avatarColor,
                currentAvatarUri = session.avatarUri
            )
            openConversation(convId)
            onChatReady(convId)
        }
    }

    fun createNewGroup(
        groupName: String,
        description: String,
        avatarColor: String,
        memberIds: List<String>,
        onGroupCreated: (String) -> Unit
    ) {
        if (groupName.isBlank()) {
            showBanner("Please enter a group name")
            return
        }
        val session = sessionState.value
        val uid = session.userId.ifBlank { "user_me" }
        val uName = session.displayName.ifBlank { "You" }
        viewModelScope.launch {
            val convId = repository.createGroupConversation(
                currentUserId = uid,
                currentUserName = uName,
                groupName = groupName,
                description = description,
                avatarColor = avatarColor,
                memberIds = memberIds
            )
            openConversation(convId)
            showBanner("Group \"${groupName.trim()}\" created!")
            onGroupCreated(convId)
        }
    }

    fun updateGroupDetails(
        conversationId: String,
        name: String,
        description: String,
        participantIds: List<String>
    ) {
        viewModelScope.launch {
            repository.updateGroupInfo(conversationId, name, description, participantIds)
            showBanner("Group details updated")
        }
    }

    // --- Status Story Actions ---

    fun createStatusStory(
        content: String,
        caption: String,
        mediaUri: String?,
        backgroundColor: String,
        fontStyle: String,
        onCreated: () -> Unit
    ) {
        if (content.isBlank() && mediaUri == null) {
            showBanner("Add text or a photo to post your status")
            return
        }
        val session = sessionState.value
        viewModelScope.launch {
            repository.createStatusStory(
                userId = session.userId.ifBlank { "user_me" },
                userName = session.displayName.ifBlank { "You" },
                userHandle = session.username.ifBlank { "vibechat_user" },
                userAvatarColor = session.avatarColor,
                content = content.ifBlank { caption.ifBlank { "Photo Status 📸" } },
                caption = caption,
                mediaUri = mediaUri,
                backgroundColor = backgroundColor,
                fontStyle = fontStyle
            )
            showBanner("Status story posted!")
            onCreated()
        }
    }

    fun markStatusViewed(statusId: String) {
        val uid = sessionState.value.userId.ifBlank { "user_me" }
        viewModelScope.launch {
            repository.markStatusViewed(statusId, uid)
        }
    }

    fun reactOrReplyToStatus(
        story: StatusStoryEntity,
        reactionOrReplyText: String
    ) {
        val session = sessionState.value
        val uid = session.userId.ifBlank { "user_me" }
        val uName = session.displayName.ifBlank { "You" }
        viewModelScope.launch {
            repository.addReactionToStatus(story.id, reactionOrReplyText.take(4))
            val targetContact = allContacts.value.firstOrNull { it.id == story.userId }
            if (targetContact != null) {
                val convId = repository.startOrOpenDirectChat(uid, targetContact, session.email)
                repository.sendMessage(
                    conversationId = convId,
                    senderId = uid,
                    senderName = uName,
                    senderAvatarColor = session.avatarColor,
                    content = "Replied to your status \"${story.content.take(35)}…\": $reactionOrReplyText",
                    messageType = "TEXT"
                )
            }
            showBanner("Sent \"$reactionOrReplyText\" to ${story.userName}")
        }
    }

    fun deleteStatusStory(statusId: String) {
        viewModelScope.launch {
            repository.deleteStatusStory(statusId)
            showBanner("Status removed")
        }
    }

    // --- Contacts & Settings Actions ---

    fun addNewContact(
        displayName: String,
        username: String,
        email: String,
        bio: String,
        avatarColor: String
    ) {
        if (displayName.isBlank()) {
            showBanner("Enter a contact name")
            return
        }
        viewModelScope.launch {
            repository.addOrUpdateContact(displayName, username, email, bio, avatarColor)
            showBanner("${displayName.trim()} added to Contacts")
        }
    }

    fun toggleFavoriteContact(userId: String) {
        viewModelScope.launch {
            repository.toggleFavoriteContact(userId)
        }
    }

    fun toggleBlockContact(userId: String) {
        viewModelScope.launch {
            val blocked = repository.toggleBlockContact(userId)
            showBanner(if (blocked) "Contact blocked" else "Contact unblocked")
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        statusText: String,
        avatarColor: String,
        avatarUri: String?
    ) {
        val session = sessionState.value
        viewModelScope.launch {
            val localAvatarUri = if (avatarUri != null && avatarUri.startsWith("content://")) {
                ProfilePictureManager.savePickedImageLocally(
                    appContext,
                    android.net.Uri.parse(avatarUri),
                    session.userId.ifBlank { "user_me" }
                )
            } else {
                avatarUri
            }

            val remoteThumbnail = if (!localAvatarUri.isNullOrBlank()) {
                ProfilePictureManager.createBase64Thumbnail(appContext, localAvatarUri)
            } else null

            prefsRepository.updateProfileDetails(
                displayName = displayName,
                username = username,
                bio = bio,
                statusText = statusText,
                avatarColor = avatarColor,
                avatarUri = localAvatarUri
            )
            repository.updateCurrentUserProfile(
                userId = session.userId.ifBlank { "user_me" },
                email = session.email,
                displayName = displayName,
                username = username,
                bio = bio,
                statusText = statusText,
                avatarColor = avatarColor,
                avatarUri = localAvatarUri,
                remoteAvatarUrl = remoteThumbnail
            )
            showBanner("Profile picture and details updated!")
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch { prefsRepository.setDarkMode(mode) }
    }

    fun setAccentTheme(accentId: String) {
        viewModelScope.launch { prefsRepository.setAccentTheme(accentId) }
    }

    fun setReadReceipts(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setReadReceipts(enabled) }
    }

    fun setOnlineStatusVisible(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setOnlineStatusVisible(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setNotificationsEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setVibrationEnabled(enabled) }
    }

    // --- AI Chat Assist Operations ---

    /**
     * Spell check: debounces typing by ~800ms, only calls when text is 2+ chars.
     * Cancels any in-flight requests when the user continues typing.
     */
    fun onMessageInputChanged(newText: String) {
        spellCheckJob?.cancel()
        rewriteJob?.cancel()
        val clean = newText.trim()
        if (clean.length < 2) {
            _spellCheckState.value = SpellCheckState.Idle
            return
        }

        spellCheckJob = viewModelScope.launch {
            delay(250)
            _spellCheckState.value = SpellCheckState.Checking
            val bounded = clean.take(600)
            when (val res = chatAssistRepository.correct(bounded)) {
                is ChatAssistResult.Success -> {
                    if (res.data.corrections.isNotEmpty()) {
                        _spellCheckState.value = SpellCheckState.Suggestions(
                            originalText = clean,
                            corrections = res.data.corrections,
                            correctedText = res.data.corrected
                        )
                    } else {
                        _spellCheckState.value = SpellCheckState.Idle
                    }
                }
                is ChatAssistResult.Error -> {
                    val local = com.example.util.SpellCheckerEngine.checkText(clean)
                    if (local.corrections.isNotEmpty()) {
                        _spellCheckState.value = SpellCheckState.Suggestions(
                            originalText = clean,
                            corrections = local.corrections,
                            correctedText = local.corrected
                        )
                    } else {
                        _spellCheckState.value = SpellCheckState.Idle
                    }
                }
            }
        }
    }

    fun dismissCorrection(original: String) {
        val current = _spellCheckState.value
        if (current is SpellCheckState.Suggestions) {
            val remaining = current.corrections.filterNot { it.original.equals(original, ignoreCase = true) }
            _spellCheckState.value = if (remaining.isEmpty()) {
                SpellCheckState.Idle
            } else {
                current.copy(corrections = remaining)
            }
        }
    }

    /**
     * "Add to dictionary" calls add_word
     */
    fun addWordToDictionary(word: String) {
        val cleanWord = word.trim()
        if (cleanWord.isBlank()) return
        viewModelScope.launch {
            _addWordState.value = AddWordState.Loading(cleanWord)
            when (val res = chatAssistRepository.addWord(cleanWord)) {
                is ChatAssistResult.Success -> {
                    _addWordState.value = AddWordState.Success(cleanWord)
                    showBanner("\"$cleanWord\" added to custom dictionary")
                    dismissCorrection(cleanWord)
                }
                is ChatAssistResult.Error -> {
                    _addWordState.value = AddWordState.Error(res.message)
                    showBanner(res.message)
                }
            }
        }
    }

    /**
     * Smart-reply: calls replies with last 8 messages.
     */
    fun loadSmartReplies(conversationMessages: List<MessageEntity>, currentUserId: String) {
        smartRepliesJob?.cancel()
        val last8 = conversationMessages
            .filter { it.content.isNotBlank() && it.messageType != "AUDIO" }
            .takeLast(8)
            .map { msg ->
                ChatAssistMessage(
                    from = if (msg.senderId == currentUserId) "me" else "them",
                    text = msg.content
                )
            }
        if (last8.isEmpty()) {
            _smartRepliesState.value = SmartRepliesState.Idle
            return
        }

        smartRepliesJob = viewModelScope.launch {
            _smartRepliesState.value = SmartRepliesState.Loading
            when (val res = chatAssistRepository.replies(last8)) {
                is ChatAssistResult.Success -> {
                    val replies = res.data.take(3)
                    _smartRepliesState.value = if (replies.isNotEmpty()) {
                        SmartRepliesState.Success(replies)
                    } else {
                        SmartRepliesState.Idle
                    }
                }
                is ChatAssistResult.Error -> {
                    _smartRepliesState.value = SmartRepliesState.Error(res.message)
                }
            }
        }
    }

    /**
     * Rewrite text with one of the five tones: casual, formal, friendly, shorter, funny.
     */
    fun rewriteMessage(text: String, tone: String, onApplied: (String) -> Unit) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            showBanner("Type some message text to rewrite with AI")
            return
        }
        rewriteJob?.cancel()
        rewriteJob = viewModelScope.launch {
            _rewriteState.value = RewriteState.Loading(tone)
            when (val res = chatAssistRepository.rewrite(trimmed, tone)) {
                is ChatAssistResult.Success -> {
                    _rewriteState.value = RewriteState.Success(res.data, tone)
                    onApplied(res.data)
                    showBanner("Rewritten with ${tone.replaceFirstChar { it.uppercase() }} tone ✨")
                }
                is ChatAssistResult.Error -> {
                    _rewriteState.value = RewriteState.Error(res.message)
                    showBanner(res.message)
                }
            }
        }
    }

    /**
     * Translate a message to the target language.
     */
    fun translateMessage(message: MessageEntity, targetLang: String) {
        if (message.content.isBlank()) return
        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            _translateState.value = TranslateState.Loading(message.id, targetLang)
            when (val res = chatAssistRepository.translate(message.content, targetLang)) {
                is ChatAssistResult.Success -> {
                    _translateState.value = TranslateState.Success(
                        messageId = message.id,
                        originalText = message.content,
                        translatedText = res.data,
                        targetLang = targetLang
                    )
                }
                is ChatAssistResult.Error -> {
                    _translateState.value = TranslateState.Error(message.id, res.message)
                    showBanner("Translation failed: ${res.message}")
                }
            }
        }
    }

    fun clearTranslation() {
        translateJob?.cancel()
        _translateState.value = TranslateState.Idle
    }

    fun clearSpellCheck() {
        spellCheckJob?.cancel()
        _spellCheckState.value = SpellCheckState.Idle
    }

    fun dismissSmartReplies() {
        smartRepliesJob?.cancel()
        _smartRepliesState.value = SmartRepliesState.Idle
    }

    fun resetRewriteState() {
        rewriteJob?.cancel()
        _rewriteState.value = RewriteState.Idle
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val db = VibeChatDatabase.getInstance(appContext)
                    val repo = VibeChatRepository(db.vibeChatDao())
                    val prefs = UserPreferencesRepository(appContext)
                    val chatAssistRepo = ChatAssistRepository(
                        sessionTokenProvider = {
                            SupabaseClientManager.currentAccessToken ?: prefs.sessionFlow.first().accessToken
                        },
                        sessionRefresher = {
                            val refresh = prefs.sessionFlow.first().refreshToken
                            if (!refresh.isNullOrBlank()) {
                                val res = repo.refreshSession(refresh)
                                res.getOrNull()?.let { (newAccess, newRefresh) ->
                                    prefs.updateAccessToken(newAccess, newRefresh)
                                    newAccess
                                }
                            } else null
                        }
                    )
                    return VibeChatViewModel(appContext, repo, prefs, chatAssistRepo) as T
                }
            }
    }
}
