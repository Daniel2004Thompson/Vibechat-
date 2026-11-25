package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.StatusStoryEntity
import com.example.data.repository.UserSessionPrefs
import com.example.ui.components.StoryRingAvatar
import com.example.ui.components.VibeAvatar
import com.example.ui.components.parseHexColor
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.StatusBackgroundColors
import com.example.ui.viewmodel.GroupedStatusUser
import com.example.util.DateUtils
import kotlinx.coroutines.delay

@Composable
fun StatusTabScreen(
    groupedStatuses: List<GroupedStatusUser>,
    session: UserSessionPrefs,
    onCreateStatusClick: () -> Unit,
    onOpenUserStories: (GroupedStatusUser) -> Unit
) {
    val myGroup = groupedStatuses.find {
        it.userId == session.userId ||
        it.userId == "user_me" ||
        it.userName == "My Status" ||
        (session.email.isNotBlank() && it.userId.equals(session.email, ignoreCase = true))
    }
    val otherGroups = groupedStatuses.filter { it != myGroup }
    val recentUpdates = otherGroups.filter { it.hasUnviewed }
    val viewedUpdates = otherGroups.filter { !it.hasUnviewed }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Status Updates",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Share 24-hour text vibes & photo stories with your contacts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // My Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable {
                            if (myGroup != null && myGroup.stories.isNotEmpty()) {
                                onOpenUserStories(myGroup)
                            } else {
                                onCreateStatusClick()
                            }
                        }
                        .testTag("my_status_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            if (myGroup != null) {
                                StoryRingAvatar(
                                    name = session.displayName.ifBlank { "You" },
                                    avatarColorHex = session.avatarColor,
                                    avatarUri = session.avatarUri,
                                    segmentCount = myGroup.stories.size,
                                    hasUnviewed = true,
                                    size = 58.dp,
                                    onClick = { onOpenUserStories(myGroup) }
                                )
                            } else {
                                VibeAvatar(
                                    name = session.displayName.ifBlank { "You" },
                                    avatarColorHex = session.avatarColor,
                                    avatarUri = session.avatarUri,
                                    size = 56.dp
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add status",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "My Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (myGroup != null) {
                                    "${myGroup.stories.size} active update(s) • Tap to view"
                                } else {
                                    "Tap to add a text or photo status update"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onCreateStatusClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("add_status_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Post")
                        }
                    }
                }
            }

            if (otherGroups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No status updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Status updates from people you have added to your contacts will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (recentUpdates.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Updates",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(recentUpdates, key = { it.userId }) { group ->
                    StatusUserRow(group = group, onClick = { onOpenUserStories(group) })
                }
            }

            if (viewedUpdates.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Viewed Updates",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(viewedUpdates, key = { it.userId }) { group ->
                    StatusUserRow(group = group, onClick = { onOpenUserStories(group) })
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateStatusClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_status_fab")
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Create Status")
        }
    }
}

@Composable
private fun StatusUserRow(
    group: GroupedStatusUser,
    onClick: () -> Unit
) {
    val latest = group.stories.last()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("status_user_${group.userId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoryRingAvatar(
                name = group.userName,
                avatarColorHex = group.userAvatarColor,
                avatarUri = group.userAvatarUri,
                segmentCount = group.stories.size,
                hasUnviewed = group.hasUnviewed,
                size = 56.dp,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = latest.content.replace("\n", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = DateUtils.formatRelativeTime(latest.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateStatusScreen(
    onBack: () -> Unit,
    onPostStatus: (String, String, String?, String, String) -> Unit
) {
    BackHandler(onBack = onBack)

    var statusText by rememberSaveable { mutableStateOf("") }
    var caption by rememberSaveable { mutableStateOf("") }
    var mediaUri by rememberSaveable { mutableStateOf<String?>(null) }
    var colorIndex by rememberSaveable { mutableIntStateOf(1) }
    var fontIndex by rememberSaveable { mutableIntStateOf(0) }

    val fontStyles = listOf("DISPLAY", "MODERN", "MONO")
    val currentBgHex = StatusBackgroundColors[colorIndex % StatusBackgroundColors.size]
    val currentFontStyle = fontStyles[fontIndex % fontStyles.size]

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaUri = uri.toString()
        }
    }

    val bgColor = parseHexColor(currentBgHex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgColor, bgColor.copy(alpha = 0.8f), Color(0xFF090D16))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick photo", tint = Color.White)
                }
                IconButton(onClick = { fontIndex = (fontIndex + 1) % fontStyles.size }) {
                    Icon(Icons.Default.TextFields, contentDescription = "Cycle font", tint = Color.White)
                }
                IconButton(onClick = { colorIndex = (colorIndex + 1) % StatusBackgroundColors.size }) {
                    Icon(Icons.Default.ColorLens, contentDescription = "Cycle background color", tint = Color.White)
                }
            }
        }

        // Center Status Composer
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!mediaUri.isNullOrBlank()) {
                AsyncImage(
                    model = mediaUri,
                    contentDescription = "Selected status photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedTextField(
                value = statusText,
                onValueChange = { statusText = it },
                placeholder = {
                    Text(
                        text = "Type your status vibe…",
                        color = Color.White.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontFamily = when (currentFontStyle) {
                        "DISPLAY" -> OutfitFontFamily
                        "MONO" -> FontFamily.Monospace
                        else -> PlusJakartaFontFamily
                    },
                    fontWeight = FontWeight.Bold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("status_text_input")
            )
        }

        // Bottom Caption & Post Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                placeholder = { Text("Add a caption…", color = Color.White.copy(alpha = 0.7f)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.weight(1f)
            )

            FloatingActionButton(
                onClick = {
                    onPostStatus(
                        statusText,
                        caption,
                        mediaUri,
                        currentBgHex,
                        currentFontStyle
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("publish_status_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post status")
            }
        }
    }
}

@Composable
fun StatusStoryViewerScreen(
    group: GroupedStatusUser,
    currentUserId: String,
    onClose: () -> Unit,
    onStoryViewed: (String) -> Unit,
    onReactOrReply: (StatusStoryEntity, String) -> Unit,
    onDeleteStory: (String) -> Unit
) {
    BackHandler(onBack = onClose)

    val stories = group.stories
    if (stories.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, stories.lastIndex)
    val currentStory = stories[safeIndex]
    var replyInput by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentStory.id) {
        onStoryViewed(currentStory.id)
        progress = 0f
        val totalDurationMs = 5000L
        val tickMs = 50L
        var elapsed = 0L
        while (elapsed < totalDurationMs) {
            delay(tickMs)
            elapsed += tickMs
            progress = (elapsed.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        }
        if (safeIndex < stories.lastIndex) {
            currentIndex = safeIndex + 1
        } else {
            onClose()
        }
    }

    val bgColor = parseHexColor(currentStory.backgroundColor)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = "story_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgColor, bgColor.copy(alpha = 0.85f), Color(0xFF070B12))
                )
            )
            .pointerInput(safeIndex, stories.size) {
                detectTapGestures { offset ->
                    val width = size.width
                    if (offset.x < width * 0.35f) {
                        if (safeIndex > 0) currentIndex = safeIndex - 1
                    } else {
                        if (safeIndex < stories.lastIndex) currentIndex = safeIndex + 1
                        else onClose()
                    }
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Segmented Progress Bars + Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stories.forEachIndexed { idx, _ ->
                    val segProgress = when {
                        idx < safeIndex -> 1f
                        idx == safeIndex -> animatedProgress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VibeAvatar(
                        name = currentStory.userName,
                        avatarColorHex = currentStory.userAvatarColor,
                        avatarUri = group.userAvatarUri,
                        size = 42.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentStory.userName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DateUtils.formatRelativeTime(currentStory.createdAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                Row {
                    if (currentStory.userId == currentUserId || currentStory.userId == "user_me") {
                        IconButton(
                            onClick = {
                                onDeleteStory(currentStory.id)
                                onClose()
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete status", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close viewer", tint = Color.White)
                    }
                }
            }
        }

        // Story Body Content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!currentStory.mediaUri.isNullOrBlank()) {
                AsyncImage(
                    model = currentStory.mediaUri,
                    contentDescription = "Story image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = currentStory.content,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    lineHeight = 36.sp,
                    fontFamily = when (currentStory.fontStyle) {
                        "DISPLAY" -> OutfitFontFamily
                        "MONO" -> FontFamily.Monospace
                        else -> PlusJakartaFontFamily
                    }
                ),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (currentStory.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = currentStory.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom Views / Reactions & Reply Composer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${currentStory.viewerIds().size} views",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                if (currentStory.reactionsList().isNotEmpty()) {
                    Text(
                        text = "• ${currentStory.reactionsList().joinToString(" ")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Emoji Reaction Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("🔥", "❤️", "😂", "🙌", "🎉", "😍").forEach { emoji ->
                    Surface(
                        onClick = { onReactOrReply(currentStory, emoji) },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyInput,
                    onValueChange = { replyInput = it },
                    placeholder = { Text("Reply to ${currentStory.userName}…", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                        focusedBorderColor = Color.White.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (replyInput.isNotBlank()) {
                            onReactOrReply(currentStory, replyInput)
                            replyInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send status reply",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
