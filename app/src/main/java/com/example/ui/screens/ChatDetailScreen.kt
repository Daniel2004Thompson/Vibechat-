package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import com.example.util.FileUtils
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import android.widget.Toast
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.ui.viewmodel.AddWordState
import com.example.ui.viewmodel.RewriteState
import com.example.ui.viewmodel.SmartRepliesState
import com.example.ui.viewmodel.SpellCheckState
import com.example.ui.viewmodel.TranslateState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.components.LiveRecordingWaveform
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VoiceWaveformPlayer
import com.example.ui.components.parseHexColor
import com.example.util.DateUtils
import com.example.util.VoiceRecorderHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    conversation: ConversationEntity?,
    messages: List<MessageEntity>,
    currentUserId: String,
    currentEmail: String = "",
    currentCanonicalId: String = "",
    otherContact: UserProfileEntity? = null,
    replyingToMessage: MessageEntity?,
    spellCheckState: SpellCheckState = SpellCheckState.Idle,
    smartRepliesState: SmartRepliesState = SmartRepliesState.Idle,
    rewriteState: RewriteState = RewriteState.Idle,
    translateState: TranslateState = TranslateState.Idle,
    addWordState: AddWordState = AddWordState.Idle,
    isContactBlocked: Boolean = false,
    onToggleBlockContact: () -> Unit = {},
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onSendText: (String, String, String?) -> Unit,
    onSendVoice: (String?, Int, String) -> Unit,
    onSelectReply: (MessageEntity?) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onToggleStar: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onMessageInputChanged: (String) -> Unit = {},
    onAddWordToDictionary: (String) -> Unit = {},
    onRewriteText: (String, String, (String) -> Unit) -> Unit = { _, _, _ -> },
    onTranslateMessage: (MessageEntity, String) -> Unit = { _, _ -> },
    onClearTranslation: () -> Unit = {},
    onDismissSmartReplies: () -> Unit = {},
    onDismissSpellCheck: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val voiceRecorderHelper = remember { VoiceRecorderHelper(context) }
    val listState = rememberLazyListState()

    var messageInput by rememberSaveable { mutableStateOf("") }
    var showEmojiStrip by rememberSaveable { mutableStateOf(false) }
    var showRewriteMenu by remember { mutableStateOf(false) }
    var showLanguagePickerDialog by remember { mutableStateOf(false) }
    var messageForTranslation by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageForActions by remember { mutableStateOf<MessageEntity?>(null) }
    var activeCallMode by remember { mutableStateOf<String?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showUnblockConfirmDialog by remember { mutableStateOf(false) }

    val spellCheckVisualTransformation = remember(spellCheckState, messageInput) {
        if (spellCheckState is SpellCheckState.Suggestions && spellCheckState.corrections.isNotEmpty()) {
            VisualTransformation { text ->
                val annotated = buildAnnotatedString {
                    append(text.text)
                    for (correction in spellCheckState.corrections) {
                        val original = correction.original.trim()
                        if (original.isEmpty()) continue
                        val regex = "\\b${Regex.escape(original)}\\b".toRegex(RegexOption.IGNORE_CASE)
                        regex.findAll(text.text).forEach { match ->
                            addStyle(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = Color(0xFFE53935)
                                ),
                                start = match.range.first,
                                end = match.range.last + 1
                            )
                        }
                    }
                }
                TransformedText(annotated, OffsetMapping.Identity)
            }
        } else {
            VisualTransformation.None
        }
    }

    // Voice Recording state
    var isRecordingVoice by remember { mutableStateOf(false) }
    val recordingSeconds by voiceRecorderHelper.elapsedSeconds.collectAsState()
    val liveAmplitude by voiceRecorderHelper.liveAmplitude.collectAsState()

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Whether physical mic permission is granted or running in headless emulator, start recorder
        voiceRecorderHelper.startRecording()
        isRecordingVoice = true
    }

    val coroutineScope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSendText(
                messageInput.ifBlank { "Shared a photo 📸" },
                "IMAGE",
                uri.toString()
            )
            messageInput = ""
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val metadata = FileUtils.savePickedFileLocally(context, uri)
                onSendText(
                    metadata.displayName,
                    "FILE",
                    metadata.localFilePath
                )
            }
        }
    }



    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorderHelper.cancelRecording()
            voiceRecorderHelper.stopPlayback()
        }
    }

    val isSelfChat = conversation?.id?.startsWith("conv_self_") == true ||
        conversation?.name?.endsWith("(You)") == true ||
        conversation?.name?.contains("(Message yourself)") == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Conversation Header
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("chat_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chats"
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenInfo)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayName = if (isSelfChat) {
                        conversation?.name ?: "You (Message yourself)"
                    } else if (conversation?.isGroup == true) {
                        conversation.name
                    } else {
                        otherContact?.displayName ?: conversation?.name ?: "Conversation"
                    }
                    val displayAvatarColor = if (isSelfChat) {
                        conversation?.avatarColor ?: "#10B981"
                    } else if (conversation?.isGroup == true) {
                        conversation.avatarColor
                    } else {
                        otherContact?.avatarColor ?: conversation?.avatarColor ?: "#10B981"
                    }
                    val displayAvatarUri = if (isSelfChat) {
                        conversation?.avatarUri
                    } else if (conversation?.isGroup == true) {
                        conversation.avatarUri
                    } else {
                        otherContact?.avatarUri ?: conversation?.avatarUri
                    }

                    VibeAvatar(
                        name = displayName,
                        avatarColorHex = displayAvatarColor,
                        avatarUri = displayAvatarUri,
                        size = 42.dp,
                        isOnline = if (isSelfChat) true else (otherContact?.isOnline ?: true),
                        showOnlineIndicator = conversation?.isGroup == false && !isSelfChat,
                        isGroup = conversation?.isGroup == true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isSelfChat) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        val subtitle = if (isSelfChat) {
                            "Message yourself • Notes, links & reminders 📝"
                        } else if (conversation?.isGroup == true) {
                            "${conversation.participantIds().size} members • Tap for group info"
                        } else if (isContactBlocked) {
                            "Blocked"
                        } else {
                            "${otherContact?.statusText ?: "Online"} • Active now"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isContactBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!isSelfChat) {
                    IconButton(onClick = {
                        if (isContactBlocked) {
                            showUnblockConfirmDialog = true
                        } else {
                            activeCallMode = "Voice Call"
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice Call",
                            tint = if (isContactBlocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        if (isContactBlocked) {
                            showUnblockConfirmDialog = true
                        } else {
                            activeCallMode = "Video Call"
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = if (isContactBlocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(
                    onClick = onOpenInfo,
                    modifier = Modifier.testTag("chat_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Conversation Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // WhatsApp 3-dots Overflow Menu with Block / Unblock
                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.testTag("chat_more_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View contact") },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showOverflowMenu = false
                                onOpenInfo()
                            }
                        )

                        if (conversation?.isGroup != true && !isSelfChat) {
                            HorizontalDivider()
                            if (isContactBlocked) {
                                DropdownMenuItem(
                                    text = { Text("Unblock") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showUnblockConfirmDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_unblock_contact")
                                )
                            } else {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Block",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showBlockConfirmDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_block_contact")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSelfChat) {
                item(key = "self_chat_info_card") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Message yourself",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Send notes, links, photos or voice memos to yourself. These messages are private and accessible across all devices you sign in with.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                val prevMsg = messages.getOrNull(index - 1)
                val currHeader = DateUtils.formatDateHeader(msg.createdAtMillis)
                val prevHeader = prevMsg?.let { DateUtils.formatDateHeader(it.createdAtMillis) }

                if (currHeader != prevHeader) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                        ) {
                            Text(
                                text = currHeader,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                val isOutgoing = msg.senderId == currentUserId ||
                    (currentEmail.isNotBlank() && msg.senderId.equals(currentEmail, ignoreCase = true)) ||
                    (currentCanonicalId.isNotBlank() && msg.senderId.equals(currentCanonicalId, ignoreCase = true))
                MessageBubbleItem(
                    message = msg,
                    isGroup = conversation?.isGroup == true,
                    isOutgoing = isOutgoing,
                    voiceRecorderHelper = voiceRecorderHelper,
                    translateState = translateState,
                    onLongPress = { selectedMessageForActions = msg },
                    onQuickReaction = { emoji -> onToggleReaction(msg.id, emoji) }
                )
            }

            if (isContactBlocked) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = { showUnblockConfirmDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "You blocked this contact. Tap to unblock.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Emoji Picker Strip
        AnimatedVisibility(visible = !isContactBlocked && showEmojiStrip) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("🔥", "✨", "❤️", "😂", "🚀", "🙌", "🎉", "🎧", "💯").forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { messageInput += emoji }
                                .padding(6.dp)
                        )
                    }
                }
            }
        }

        // Reply Preview Banner
        AnimatedVisibility(visible = !isContactBlocked && replyingToMessage != null) {
            replyingToMessage?.let { replyMsg ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${replyMsg.senderName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = replyMsg.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onSelectReply(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel reply")
                        }
                    }
                }
            }
        }

        // Bottom Message / Voice Note Composer
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Feature B: Three Smart-Reply Chips above keyboard
                if (smartRepliesState is SmartRepliesState.Success && smartRepliesState.replies.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(smartRepliesState.replies.take(3)) { reply ->
                                    SuggestionChip(
                                        onClick = {
                                            messageInput = reply
                                            onMessageInputChanged(reply)
                                        },
                                        label = {
                                            Text(
                                                text = reply,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismissSmartReplies,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss smart replies",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Feature A: Spell Check suggestions card
                if (spellCheckState is SpellCheckState.Suggestions && spellCheckState.corrections.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Spellcheck,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Spelling Suggestions",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (spellCheckState.correctedText.isNotBlank() && spellCheckState.correctedText != messageInput) {
                                        Button(
                                            onClick = {
                                                messageInput = spellCheckState.correctedText
                                                onMessageInputChanged(messageInput)
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("auto_correct_all_button")
                                        ) {
                                            Text(
                                                "Auto-Correct All",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    IconButton(
                                        onClick = onDismissSpellCheck,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(spellCheckState.corrections) { correction ->
                                    SuggestionChip(
                                        onClick = {
                                            // Tap to apply fix
                                            val updated = messageInput.replace(
                                                "\\b${Regex.escape(correction.original)}\\b".toRegex(RegexOption.IGNORE_CASE),
                                                correction.suggestion
                                            )
                                            messageInput = updated
                                            onMessageInputChanged(updated)
                                        },
                                        label = {
                                            Text("${correction.original} → ${correction.suggestion}")
                                        },
                                        icon = {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )

                                    AssistChip(
                                        onClick = { onAddWordToDictionary(correction.original) },
                                        label = { Text("Add \"${correction.original}\" to dict") }
                                    )
                                }
                            }
                        }
                    }
                } else if (spellCheckState is SpellCheckState.Checking) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Checking spelling…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isContactBlocked) {
                    Surface(
                        onClick = { showUnblockConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("blocked_chat_footer_bar"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "You blocked this contact. Tap to unblock.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (isRecordingVoice) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                voiceRecorderHelper.cancelRecording()
                                isRecordingVoice = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Cancel recording",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        LiveRecordingWaveform(
                            durationSec = recordingSeconds,
                            currentAmplitude = liveAmplitude,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                val result = voiceRecorderHelper.stopRecording()
                                isRecordingVoice = false
                                onSendVoice(
                                    result.filePath,
                                    result.durationSec.coerceAtLeast(recordingSeconds.coerceAtLeast(1)),
                                    result.waveformCsv
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("send_voice_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Finish and send voice note",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.testTag("attach_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach file or document",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("attach_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Attach photo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { showEmojiStrip = !showEmojiStrip }
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEmotions,
                                contentDescription = "Toggle emojis",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Feature C: Rewrite Menu button with 5 tones
                        Box {
                            IconButton(
                                onClick = { showRewriteMenu = true },
                                modifier = Modifier.testTag("ai_rewrite_button")
                            ) {
                                if (rewriteState is RewriteState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Rewrite with AI",
                                        tint = if (messageInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showRewriteMenu,
                                onDismissRequest = { showRewriteMenu = false }
                            ) {
                                val tones = listOf(
                                    Triple("casual", "🕶️ Casual", "Relaxed & conversational"),
                                    Triple("formal", "💼 Formal", "Polite & professional"),
                                    Triple("friendly", "😊 Friendly", "Warm & supportive"),
                                    Triple("shorter", "✂️ Shorter", "Concise & punchy"),
                                    Triple("funny", "😄 Funny", "Witty & humorous")
                                )
                                tones.forEach { (toneKey, toneLabel, toneDesc) ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(toneLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(toneDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            showRewriteMenu = false
                                            onRewriteText(messageInput, toneKey) { rewritten ->
                                                messageInput = rewritten
                                                onMessageInputChanged(rewritten)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = {
                                messageInput = it
                                onMessageInputChanged(it)
                            },
                            placeholder = { Text("Message…") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            visualTransformation = spellCheckVisualTransformation,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        if (messageInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onSendText(messageInput, "TEXT", null)
                                    messageInput = ""
                                    onMessageInputChanged("")
                                    onDismissSpellCheck()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val hasMicPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasMicPerm) {
                                        voiceRecorderHelper.startRecording()
                                        isRecordingVoice = true
                                    } else {
                                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .testTag("record_voice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record voice note",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Message Action & Reaction Dialog
    selectedMessageForActions?.let { targetMsg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForActions = null },
            title = { Text("Message Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("❤️", "😂", "🔥", "👍", "🎉", "😮").forEach { emoji ->
                            Surface(
                                onClick = {
                                    onToggleReaction(targetMsg.id, emoji)
                                    selectedMessageForActions = null
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            messageForTranslation = targetMsg
                            showLanguagePickerDialog = true
                            selectedMessageForActions = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("translate_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translate with AI", color = MaterialTheme.colorScheme.primary)
                    }

                    TextButton(
                        onClick = {
                            onSelectReply(targetMsg)
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reply to Message")
                    }

                    TextButton(
                        onClick = {
                            onToggleStar(targetMsg.id)
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (targetMsg.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (targetMsg.isStarred) "Remove Star" else "Star Message")
                    }

                    TextButton(
                        onClick = {
                            onDeleteMessage(targetMsg.id)
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Message", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForActions = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Language Picker Dialog for AI Translate
    if (showLanguagePickerDialog && messageForTranslation != null) {
        val targetMsg = messageForTranslation
        var customLang by remember { mutableStateOf("") }
        val popularLanguages = listOf(
            "French", "Spanish", "German", "Italian", "Portuguese",
            "Japanese", "Chinese", "Korean", "Arabic", "Hindi", "Russian"
        )

        AlertDialog(
            onDismissRequest = {
                showLanguagePickerDialog = false
                messageForTranslation = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Translate Message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose target language for AI translation:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${targetMsg?.content?.take(70) ?: ""}${if ((targetMsg?.content?.length ?: 0) > 70) "…" else ""}\"",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "Quick Select:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(popularLanguages) { lang ->
                            SuggestionChip(
                                onClick = {
                                    targetMsg?.let { onTranslateMessage(it, lang) }
                                    showLanguagePickerDialog = false
                                    messageForTranslation = null
                                },
                                label = { Text(lang) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (lang == "French") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("lang_chip_$lang")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customLang,
                        onValueChange = { customLang = it },
                        label = { Text("Other language") },
                        placeholder = { Text("e.g. Dutch, Turkish, Swedish") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_lang_input")
                    )
                }
            },
            confirmButton = {
                if (customLang.isNotBlank()) {
                    TextButton(
                        onClick = {
                            targetMsg?.let { onTranslateMessage(it, customLang.trim()) }
                            showLanguagePickerDialog = false
                            messageForTranslation = null
                        },
                        modifier = Modifier.testTag("confirm_translate_button")
                    ) {
                        Text("Translate", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLanguagePickerDialog = false
                        messageForTranslation = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Translation Result Dialog
    if (translateState is TranslateState.Success) {
        AlertDialog(
            onDismissRequest = onClearTranslation,
            icon = {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "AI Translation (${translateState.targetLang})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Original:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = translateState.originalText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Translated:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = translateState.translatedText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(translateState.translatedText))
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    TextButton(
                        onClick = {
                            messageInput = translateState.translatedText
                            onMessageInputChanged(translateState.translatedText)
                            onClearTranslation()
                        }
                    ) {
                        Text("Insert in Draft")
                    }
                    TextButton(onClick = onClearTranslation) {
                        Text("Done")
                    }
                }
            }
        )
    }

    activeCallMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { activeCallMode = null },
            icon = {
                Icon(
                    imageVector = if (mode.contains("Video")) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("$mode • ${conversation?.name ?: ""}") },
            text = {
                Text(
                    "Encrypted WebRTC signaling channel ready. Calling ${conversation?.name ?: "participant"}…"
                )
            },
            confirmButton = {
                TextButton(onClick = { activeCallMode = null }) {
                    Text("End Call", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showBlockConfirmDialog) {
        val contactName = conversation?.name ?: "this contact"
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Block $contactName?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Blocked contacts will no longer be able to call you or send you messages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockConfirmDialog = false
                        onToggleBlockContact()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_block_button")
                ) {
                    Text("Block", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBlockConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_block_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUnblockConfirmDialog) {
        val contactName = conversation?.name ?: "this contact"
        AlertDialog(
            onDismissRequest = { showUnblockConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Unblock $contactName?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Unblock $contactName to send a message or make calls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnblockConfirmDialog = false
                        onToggleBlockContact()
                    },
                    modifier = Modifier.testTag("confirm_unblock_button")
                ) {
                    Text("Unblock", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnblockConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_unblock_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleItem(
    message: MessageEntity,
    isGroup: Boolean,
    isOutgoing: Boolean,
    voiceRecorderHelper: VoiceRecorderHelper,
    translateState: TranslateState = TranslateState.Idle,
    onLongPress: () -> Unit,
    onQuickReaction: (String) -> Unit
) {
    val context = LocalContext.current
    if (message.messageType == "VIBE_CARD") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        return
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    val bubbleContainerColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val bubbleTextColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 110.dp, max = 310.dp)
                .clip(bubbleShape)
                .combinedClickable(
                    onClick = onLongPress,
                    onLongClick = onLongPress
                ),
            shape = bubbleShape,
            colors = CardDefaults.cardColors(containerColor = bubbleContainerColor)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isGroup && !isOutgoing) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = parseHexColor(message.senderAvatarColor),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Reply Quote Preview inside bubble
                if (!message.replyToPreview.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = message.replyToSenderName ?: "Reply",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = message.replyToPreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = bubbleTextColor.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Image Attachment
                if (message.messageType == "IMAGE" && !message.mediaUri.isNullOrBlank()) {
                    AsyncImage(
                        model = message.mediaUri,
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // File Attachment Card
                if (message.messageType == "FILE") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOutgoing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                FileUtils.openFile(context, message.mediaUri ?: "")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = "Document",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.content.ifBlank { "Attached File" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = bubbleTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap to open document 📄",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = bubbleTextColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Voice Note Player or Text Content
                if (message.messageType == "VOICE") {
                    VoiceWaveformPlayer(
                        mediaUri = message.mediaUri,
                        durationSec = message.voiceDurationSec,
                        amplitudes = message.waveformAmplitudes(),
                        isOutgoing = isOutgoing,
                        voiceRecorderHelper = voiceRecorderHelper,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = bubbleTextColor
                    )
                }

                // Inline AI Translation indicator/result
                if (translateState is TranslateState.Loading && translateState.messageId == message.id) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Translating to ${translateState.targetLang}…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (translateState is TranslateState.Success && translateState.messageId == message.id) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${translateState.targetLang} Translation",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = translateState.translatedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = bubbleTextColor
                            )
                        }
                    }
                } else if (translateState is TranslateState.Error && translateState.messageId == message.id) {
                    Text(
                        text = "Translation failed: ${translateState.message}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.isStarred) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = DateUtils.formatMessageTime(message.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = bubbleTextColor.copy(alpha = 0.65f)
                    )
                    if (isOutgoing) {
                        if (message.deliveryStatus == "READ") {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read by recipient",
                                tint = Color(0xFF34B7F1), // WhatsApp double blue check
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent - Waiting for recipient to open",
                                    tint = bubbleTextColor.copy(alpha = 0.65f),
                                    modifier = Modifier.size(14.dp)
                                )
                                // WhatsApp-style special indicator for unopened message:
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25D366))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reaction Pills below bubble
        val reactions = message.parsedReactions()
        if (reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                reactions.forEach { (emoji, users) ->
                    Surface(
                        onClick = { onQuickReaction(emoji) },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = "$emoji ${users.size}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
