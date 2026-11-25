package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AuthFlowScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsTabScreen
import com.example.ui.screens.ContactsTabScreen
import com.example.ui.screens.ConversationInfoScreen
import com.example.ui.screens.CreateGroupScreen
import com.example.ui.screens.CreateStatusScreen
import com.example.ui.screens.SettingsTabScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StatusStoryViewerScreen
import com.example.ui.screens.StatusTabScreen
import com.example.ui.theme.ChatAccentTheme
import com.example.ui.theme.VibeChatTheme
import com.example.ui.viewmodel.GroupedStatusUser
import com.example.ui.viewmodel.VibeChatViewModel

enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    CHATS("Chats", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_tab_chats"),
    STATUS("Status", Icons.Filled.AutoAwesomeMotion, Icons.Outlined.AutoAwesomeMotion, "nav_tab_status"),
    CONTACTS("Contacts", Icons.Filled.People, Icons.Outlined.PeopleOutline, "nav_tab_contacts"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_tab_settings")
}

private enum class OverlayRoute {
    NONE,
    CHAT_DETAIL,
    CONVERSATION_INFO,
    CREATE_GROUP,
    CREATE_STATUS,
    VIEW_STATUS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeChatRootApp()
        }
    }
}

@Composable
fun VibeChatRootApp() {
    val context = LocalContext.current
    val viewModel: VibeChatViewModel = viewModel(
        factory = VibeChatViewModel.provideFactory(context)
    )

    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val vaultConfig by viewModel.vaultConfig.collectAsStateWithLifecycle()
    val conversations by viewModel.filteredConversations.collectAsStateWithLifecycle()
    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()
    val filteredContacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
    val groupedStatuses by viewModel.groupedStatuses.collectAsStateWithLifecycle()
    val starredMessages by viewModel.starredMessages.collectAsStateWithLifecycle()

    val chatSearchQuery by viewModel.chatSearchQuery.collectAsStateWithLifecycle()
    val chatPeopleSearchResults by viewModel.chatPeopleSearchResults.collectAsStateWithLifecycle()
    val selectedChatFilter by viewModel.selectedChatFilter.collectAsStateWithLifecycle()
    val contactSearchQuery by viewModel.contactSearchQuery.collectAsStateWithLifecycle()
    val selectedContactFilter by viewModel.selectedContactFilter.collectAsStateWithLifecycle()
    val supabaseSearchResults by viewModel.supabaseSearchResults.collectAsStateWithLifecycle()


    val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeConversationMessages.collectAsStateWithLifecycle()
    val replyingToMessage by viewModel.replyingToMessage.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerNotification.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val isSyncingCloud by viewModel.isSyncingCloud.collectAsStateWithLifecycle()

    val spellCheckState by viewModel.spellCheckState.collectAsStateWithLifecycle()
    val smartRepliesState by viewModel.smartRepliesState.collectAsStateWithLifecycle()
    val rewriteState by viewModel.rewriteState.collectAsStateWithLifecycle()
    val translateState by viewModel.translateState.collectAsStateWithLifecycle()
    val addWordState by viewModel.addWordState.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = when (session.darkMode) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemDark
        else -> true
    }
    val accentTheme = ChatAccentTheme.fromId(session.accentThemeId)

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.CHATS) }
    var overlayRoute by rememberSaveable { mutableStateOf(OverlayRoute.NONE) }
    var activeStatusGroup by remember { mutableStateOf<GroupedStatusUser?>(null) }
    var showSplashScreen by rememberSaveable { mutableStateOf(true) }

    val totalUnreadChats = conversations.sumOf { it.unreadCount }
    val unviewedStatusCount = groupedStatuses.count { it.hasUnviewed }

    VibeChatTheme(
        darkTheme = useDarkTheme,
        accentTheme = accentTheme
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!session.isLoggedIn) {
                AuthFlowScreen(
                    isLoading = isAuthLoading,
                    onSignInPassword = { email, password ->
                        viewModel.signInWithPassword(email, password) {
                            overlayRoute = OverlayRoute.NONE
                        }
                    },
                    onSignUpEmail = { email, password, name, username, bio, color, onCodeSent ->
                        viewModel.signUpAndRequestVerification(email, password, name, username, bio, color) {
                            onCodeSent()
                        }
                    },
                    onRequestOtp = { email, name, username, onSent ->
                        viewModel.requestEmailOtp(email, name, username, onSent)
                    },
                    onVerifyOtp = { email, code, name, username, color ->
                        viewModel.verifyEmailOtp(email, code, name, username, color) {
                            overlayRoute = OverlayRoute.NONE
                        }
                    },
                    onSignInGoogle = { idToken, email, name, avatar ->
                        viewModel.signInWithGoogle(idToken, email, name, avatar) {
                            overlayRoute = OverlayRoute.NONE
                        }
                    },
                    onRequestPasswordReset = { email, onSent ->
                        viewModel.requestPasswordReset(email, onSent)
                    },
                    onVerifyPasswordReset = { email, code, newPassword ->
                        viewModel.verifyPasswordReset(email, code, newPassword) {
                            overlayRoute = OverlayRoute.NONE
                        }
                    },
                    onShowMessage = viewModel::showBanner
                )
            } else {
                when (overlayRoute) {
                    OverlayRoute.CHAT_DETAIL -> {
                        val myCanonical = remember(session.userId, session.email) {
                            com.example.data.remote.SupabaseAuthRegistry.resolveCanonicalUserId(session.userId, session.email)
                        }
                        val otherContact = remember(activeConversation, allContacts, session.userId, session.email, myCanonical) {
                            if (activeConversation == null || activeConversation?.isGroup == true) null
                            else {
                                val otherId = activeConversation?.participantIds()?.firstOrNull { 
                                    it != session.userId && 
                                    !it.equals(session.email, ignoreCase = true) &&
                                    !it.equals(myCanonical, ignoreCase = true)
                                }
                                allContacts.firstOrNull { 
                                    it.id == otherId || 
                                    it.email.equals(otherId, ignoreCase = true) || 
                                    it.displayName.equals(activeConversation?.name, ignoreCase = true) 
                                }
                            }
                        }
                        val isContactBlocked = otherContact?.isBlocked == true

                        ChatDetailScreen(
                            conversation = activeConversation,
                            messages = activeMessages,
                            currentUserId = session.userId,
                            currentEmail = session.email,
                            currentCanonicalId = myCanonical,
                            otherContact = otherContact,
                            replyingToMessage = replyingToMessage,
                            spellCheckState = spellCheckState,
                            smartRepliesState = smartRepliesState,
                            rewriteState = rewriteState,
                            translateState = translateState,
                            addWordState = addWordState,
                            isContactBlocked = isContactBlocked,
                            onToggleBlockContact = {
                                val targetId = otherContact?.id ?: activeConversation?.participantIds()?.firstOrNull { it != session.userId }
                                if (targetId != null) {
                                    viewModel.toggleBlockContact(targetId)
                                }
                            },
                            onBack = {
                                viewModel.closeActiveConversation()
                                overlayRoute = OverlayRoute.NONE
                            },
                            onOpenInfo = {
                                overlayRoute = OverlayRoute.CONVERSATION_INFO
                            },
                            onSendText = { text, type, uri ->
                                viewModel.sendTextMessage(text, type, uri)
                            },
                            onSendVoice = { path, dur, wave ->
                                viewModel.sendVoiceMessage(path, dur, wave)
                            },
                            onSelectReply = { viewModel.setReplyingToMessage(it) },
                            onToggleReaction = { msgId, emoji ->
                                viewModel.toggleReaction(msgId, emoji)
                            },
                            onToggleStar = { viewModel.toggleStarMessage(it) },
                            onDeleteMessage = { viewModel.deleteMessage(it) },
                            onMessageInputChanged = viewModel::onMessageInputChanged,
                            onAddWordToDictionary = viewModel::addWordToDictionary,
                            onRewriteText = { text, tone, onApplied ->
                                viewModel.rewriteMessage(text, tone, onApplied)
                            },
                            onTranslateMessage = { msg, targetLang ->
                                viewModel.translateMessage(msg, targetLang)
                            },
                            onClearTranslation = viewModel::clearTranslation,
                            onDismissSmartReplies = viewModel::dismissSmartReplies,
                            onDismissSpellCheck = viewModel::clearSpellCheck
                        )
                    }

                    OverlayRoute.CONVERSATION_INFO -> {
                        val otherContact = remember(activeConversation, allContacts, session.userId) {
                            if (activeConversation == null || activeConversation?.isGroup == true) null
                            else {
                                val otherId = activeConversation?.participantIds()?.firstOrNull { it != session.userId }
                                allContacts.firstOrNull { it.id == otherId || it.displayName.equals(activeConversation?.name, ignoreCase = true) }
                            }
                        }
                        val isContactBlocked = otherContact?.isBlocked == true

                        ConversationInfoScreen(
                            conversation = activeConversation,
                            allContacts = allContacts,
                            currentUserName = session.displayName.ifBlank { "You" },
                            currentUserId = session.userId,
                            isContactBlocked = isContactBlocked,
                            onToggleBlockContact = {
                                val targetId = otherContact?.id ?: activeConversation?.participantIds()?.firstOrNull { it != session.userId }
                                if (targetId != null) {
                                    viewModel.toggleBlockContact(targetId)
                                }
                            },
                            onBack = { overlayRoute = OverlayRoute.CHAT_DETAIL },
                            onUpdateGroup = { id, name, desc, members ->
                                viewModel.updateGroupDetails(id, name, desc, members)
                            },
                            onToggleMute = { viewModel.toggleMuteConversation(it) },
                            onTogglePin = { viewModel.togglePinConversation(it) },
                            onDeleteOrLeave = { id ->
                                viewModel.deleteConversation(id)
                                overlayRoute = OverlayRoute.NONE
                            }
                        )
                    }

                    OverlayRoute.CREATE_GROUP -> {
                        CreateGroupScreen(
                            contacts = allContacts.filter { it.id != session.userId },
                            onBack = { overlayRoute = OverlayRoute.NONE },
                            onCreateGroup = { name, desc, color, members ->
                                viewModel.createNewGroup(name, desc, color, members) {
                                    overlayRoute = OverlayRoute.CHAT_DETAIL
                                }
                            }
                        )
                    }

                    OverlayRoute.CREATE_STATUS -> {
                        CreateStatusScreen(
                            onBack = { overlayRoute = OverlayRoute.NONE },
                            onPostStatus = { text, caption, uri, bg, font ->
                                viewModel.createStatusStory(text, caption, uri, bg, font) {
                                    overlayRoute = OverlayRoute.NONE
                                    selectedTab = MainTab.STATUS
                                }
                            }
                        )
                    }

                    OverlayRoute.VIEW_STATUS -> {
                        val group = activeStatusGroup
                        if (group != null) {
                            StatusStoryViewerScreen(
                                group = group,
                                currentUserId = session.userId,
                                onClose = { overlayRoute = OverlayRoute.NONE },
                                onStoryViewed = { viewModel.markStatusViewed(it) },
                                onReactOrReply = { story, reply ->
                                    viewModel.reactOrReplyToStatus(story, reply)
                                },
                                onDeleteStory = { viewModel.deleteStatusStory(it) }
                            )
                        } else {
                            overlayRoute = OverlayRoute.NONE
                        }
                    }

                    OverlayRoute.NONE -> {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isWideScreen = maxWidth >= 640.dp

                            Scaffold(
                                contentWindowInsets = WindowInsets.safeDrawing,
                                bottomBar = {
                                    if (!isWideScreen) {
                                        NavigationBar(
                                            tonalElevation = 4.dp
                                        ) {
                                            MainTab.entries.forEach { tab ->
                                                val isSelected = selectedTab == tab
                                                val badgeCount = when (tab) {
                                                    MainTab.CHATS -> totalUnreadChats
                                                    MainTab.STATUS -> unviewedStatusCount
                                                    else -> 0
                                                }
                                                NavigationBarItem(
                                                    selected = isSelected,
                                                    onClick = { selectedTab = tab },
                                                    icon = {
                                                        BadgedBox(
                                                            badge = {
                                                                if (badgeCount > 0) {
                                                                    Badge { Text(badgeCount.toString()) }
                                                                }
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                                contentDescription = tab.label
                                                            )
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            text = tab.label,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    modifier = Modifier.testTag(tab.testTag)
                                                )
                                            }
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    if (isWideScreen) {
                                        NavigationRail {
                                            MainTab.entries.forEach { tab ->
                                                val isSelected = selectedTab == tab
                                                val badgeCount = when (tab) {
                                                    MainTab.CHATS -> totalUnreadChats
                                                    MainTab.STATUS -> unviewedStatusCount
                                                    else -> 0
                                                }
                                                NavigationRailItem(
                                                    selected = isSelected,
                                                    onClick = { selectedTab = tab },
                                                    icon = {
                                                        BadgedBox(
                                                            badge = {
                                                                if (badgeCount > 0) {
                                                                    Badge { Text(badgeCount.toString()) }
                                                                }
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                                contentDescription = tab.label
                                                            )
                                                        }
                                                    },
                                                    label = { Text(tab.label) },
                                                    modifier = Modifier.testTag(tab.testTag)
                                                )
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        when (selectedTab) {
                                            MainTab.CHATS -> {
                                                val myCanonical = remember(session.userId, session.email) {
                                                    com.example.data.remote.SupabaseAuthRegistry.resolveCanonicalUserId(session.userId, session.email)
                                                }
                                                ChatsTabScreen(
                                                    conversations = conversations,
                                                    searchQuery = chatSearchQuery,
                                                    selectedFilter = selectedChatFilter,
                                                    peopleSearchResults = chatPeopleSearchResults,
                                                    allContacts = allContacts,
                                                    currentUserId = session.userId,
                                                    currentEmail = session.email,
                                                    currentCanonicalId = myCanonical,
                                                    onSearchQueryChange = viewModel::setChatSearchQuery,
                                                    onFilterChange = viewModel::setChatFilter,
                                                    onOpenConversation = { convId ->
                                                        viewModel.openConversation(convId)
                                                        overlayRoute = OverlayRoute.CHAT_DETAIL
                                                    },
                                                    onStartChatWithContact = { contact ->
                                                        viewModel.openDirectChatWithContact(contact) {
                                                            overlayRoute = OverlayRoute.CHAT_DETAIL
                                                        }
                                                    },
                                                    onCreateGroupClick = {
                                                        overlayRoute = OverlayRoute.CREATE_GROUP
                                                    },
                                                    onNewDirectChatClick = {
                                                        selectedTab = MainTab.CONTACTS
                                                    },
                                                    onOpenSelfChat = {
                                                        viewModel.openSelfChat {
                                                            overlayRoute = OverlayRoute.CHAT_DETAIL
                                                        }
                                                    },
                                                    onTogglePin = viewModel::togglePinConversation,
                                                    onToggleMute = viewModel::toggleMuteConversation,
                                                    onDeleteConversation = viewModel::deleteConversation,
                                                    onSupabaseBadgeClick = {
                                                        selectedTab = MainTab.SETTINGS
                                                    }
                                                )
                                            }

                                            MainTab.STATUS -> {
                                                StatusTabScreen(
                                                    groupedStatuses = groupedStatuses,
                                                    session = session,
                                                    onCreateStatusClick = {
                                                        overlayRoute = OverlayRoute.CREATE_STATUS
                                                    },
                                                    onOpenUserStories = { group ->
                                                        activeStatusGroup = group
                                                        overlayRoute = OverlayRoute.VIEW_STATUS
                                                    }
                                                )
                                            }

                                            MainTab.CONTACTS -> {
                                                ContactsTabScreen(
                                                    contacts = filteredContacts,
                                                    searchQuery = contactSearchQuery,
                                                    selectedFilter = selectedContactFilter,
                                                    supabaseResults = supabaseSearchResults,
                                                    currentUserDisplayName = session.displayName,
                                                    currentUserUsername = session.username,
                                                    onOpenSelfChat = {
                                                        viewModel.openSelfChat {
                                                            overlayRoute = OverlayRoute.CHAT_DETAIL
                                                        }
                                                    },
                                                    onSearchQueryChange = viewModel::setContactSearchQuery,
                                                    onFilterChange = viewModel::setContactFilter,
                                                    onStartChatWithContact = { contact ->
                                                        viewModel.openDirectChatWithContact(contact) {
                                                            overlayRoute = OverlayRoute.CHAT_DETAIL
                                                        }
                                                    },
                                                    onAddSupabaseContact = viewModel::addContactFromSupabase,
                                                    onToggleFavorite = viewModel::toggleFavoriteContact,
                                                    onToggleBlock = viewModel::toggleBlockContact,
                                                    onAddContact = viewModel::addNewContact
                                                )
                                            }

                                            MainTab.SETTINGS -> {
                                                SettingsTabScreen(
                                                    session = session,
                                                    vaultConfig = vaultConfig,
                                                    starredMessages = starredMessages,
                                                    isSyncingCloud = isSyncingCloud,
                                                    onUpdateProfile = viewModel::updateProfile,
                                                    onSaveVaultConfig = viewModel::saveHardwareVaultConfig,
                                                    onClearVaultConfig = viewModel::clearHardwareVaultConfig,
                                                    onSetDarkMode = viewModel::setDarkMode,
                                                    onSetAccentTheme = viewModel::setAccentTheme,
                                                    onSetReadReceipts = viewModel::setReadReceipts,
                                                    onSetOnlineStatus = viewModel::setOnlineStatusVisible,
                                                    onSetNotifications = viewModel::setNotificationsEnabled,
                                                    onSetVibration = viewModel::setVibrationEnabled,
                                                    onSyncSupabase = viewModel::syncWithSupabaseCloud,
                                                    onSignOut = viewModel::signOut
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Banner Toast
            AnimatedVisibility(
                visible = bannerMessage != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Surface(
                    onClick = { viewModel.dismissBanner() },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = bannerMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Animated Splash Screen Overlay
            AnimatedVisibility(
                visible = showSplashScreen,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(450)),
                modifier = Modifier.fillMaxSize()
            ) {
                SplashScreen(
                    onSplashFinished = {
                        showSplashScreen = false
                    }
                )
            }
        }
    }
}
