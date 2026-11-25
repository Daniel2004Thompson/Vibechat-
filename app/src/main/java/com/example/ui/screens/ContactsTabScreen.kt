package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.UserProfileEntity
import com.example.ui.components.VibeAvatar
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AvatarColors
import com.example.ui.viewmodel.ContactFilterType

@Composable
fun ContactsTabScreen(
    contacts: List<UserProfileEntity>,
    searchQuery: String,
    selectedFilter: ContactFilterType,
    supabaseResults: List<UserProfileEntity> = emptyList(),
    currentUserDisplayName: String = "",
    currentUserUsername: String = "",
    onOpenSelfChat: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (ContactFilterType) -> Unit,
    onStartChatWithContact: (UserProfileEntity) -> Unit,
    onAddSupabaseContact: (UserProfileEntity) -> Unit = {},
    onToggleFavorite: (String) -> Unit,
    onToggleBlock: (String) -> Unit,
    onAddContact: (String, String, String, String, String) -> Unit
) {
    var showAddContactDialog by remember { mutableStateOf(false) }
    var selectedContactDetail by remember { mutableStateOf<UserProfileEntity?>(null) }

    val onlineContacts = contacts.filter { it.isOnline && !it.isBlocked }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Contacts",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${contacts.size} people • ${onlineContacts.size} online now",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAddContactDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("add_contact_top_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add")
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by name, @username, or email…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("contact_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ContactFilterType.entries) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.label) },
                        shape = RoundedCornerShape(50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // WhatsApp-style "Message yourself" entry
                val cleanQuery = searchQuery.trim().removePrefix("@").lowercase()
                val showSelfCard = cleanQuery.isBlank() ||
                    cleanQuery.contains("you") ||
                    cleanQuery.contains("self") ||
                    cleanQuery.contains("note") ||
                    (currentUserDisplayName.isNotBlank() && currentUserDisplayName.lowercase().contains(cleanQuery)) ||
                    (currentUserUsername.isNotBlank() && currentUserUsername.lowercase().contains(cleanQuery))

                if (showSelfCard) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenSelfChat() }
                                .testTag("message_yourself_item"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = "Message yourself",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (currentUserDisplayName.isNotBlank()) "$currentUserDisplayName (You)" else "You",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Message yourself • Notes, links & reminders 📝",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = onOpenSelfChat,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Notes", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                if (onlineContacts.isNotEmpty() && selectedFilter == ContactFilterType.ALL && searchQuery.isBlank()) {
                    item {
                        Text(
                            text = "Online Right Now",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(onlineContacts, key = { "online_${it.id}" }) { contact ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onStartChatWithContact(contact) }
                                        .padding(4.dp)
                                ) {
                                    VibeAvatar(
                                        name = contact.displayName,
                                        avatarColorHex = contact.avatarColor,
                                        avatarUri = contact.avatarUri,
                                        size = 52.dp,
                                        isOnline = true,
                                        showOnlineIndicator = true
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = contact.displayName.substringBefore(" "),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (contacts.isEmpty() && supabaseResults.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No contacts matching \"$searchQuery\"" else "No Contacts Added Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "To chat with someone, search their @username using the search bar above or tap Add.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (searchQuery.isNotBlank() && supabaseResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "Found on Cloud (${supabaseResults.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                    }

                    items(supabaseResults, key = { "cloud_user_${it.id}" }) { remoteUser ->
                        val isAlreadySaved = contacts.any { it.id == remoteUser.id }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VibeAvatar(
                                    name = remoteUser.displayName,
                                    avatarColorHex = remoteUser.avatarColor,
                                    avatarUri = remoteUser.avatarUri,
                                    size = 48.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = remoteUser.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (remoteUser.email.isNotBlank()) "@${remoteUser.username} • ${remoteUser.email}" else "@${remoteUser.username}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = remoteUser.bio,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (!isAlreadySaved) {
                                        IconButton(
                                            onClick = { onAddSupabaseContact(remoteUser) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = "Add to contacts",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onStartChatWithContact(remoteUser) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Chat", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                items(contacts, key = { it.id }) { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { selectedContactDetail = contact }
                            .testTag("contact_card_${contact.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VibeAvatar(
                                name = contact.displayName,
                                avatarColorHex = contact.avatarColor,
                                avatarUri = contact.avatarUri,
                                size = 50.dp,
                                isOnline = contact.isOnline,
                                showOnlineIndicator = true
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = contact.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (contact.isBlocked) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Blocked",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Text(
                                    text = "@${contact.username} • ${contact.statusText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { onToggleFavorite(contact.id) }) {
                                Icon(
                                    imageVector = if (contact.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite contact",
                                    tint = if (contact.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { onStartChatWithContact(contact) },
                                modifier = Modifier.testTag("message_contact_${contact.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Message ${contact.displayName}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddContactDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_contact_fab")
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
        }
    }

    if (showAddContactDialog) {
        var name by remember { mutableStateOf("") }
        var handle by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var bio by remember { mutableStateOf("Hey there! I'm on VibeChat ✨") }
        var colorHex by remember { mutableStateOf(AvatarColors.random()) }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Add New Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (handle.isBlank()) handle = it.lowercase().replace(" ", "_")
                        },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.testTag("new_contact_name_input")
                    )
                    OutlinedTextField(
                        value = handle,
                        onValueChange = { handle = it },
                        label = { Text("Username") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Status / Bio") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AvatarColors.take(6).forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .clickable { colorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddContact(name, handle, email, bio, colorHex)
                        showAddContactDialog = false
                    },
                    modifier = Modifier.testTag("confirm_add_contact_button")
                ) {
                    Text("Save Contact")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) { Text("Cancel") }
            }
        )
    }

    selectedContactDetail?.let { contact ->
        AlertDialog(
            onDismissRequest = { selectedContactDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VibeAvatar(
                        name = contact.displayName,
                        avatarColorHex = contact.avatarColor,
                        avatarUri = contact.avatarUri,
                        size = 48.dp,
                        isOnline = contact.isOnline,
                        showOnlineIndicator = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(contact.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "@${contact.username}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(contact.bio, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Email: ${contact.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Status: ${contact.statusText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedContactDetail = null
                        onStartChatWithContact(contact)
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Message")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onToggleBlock(contact.id)
                        selectedContactDetail = null
                    }
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (contact.isBlocked) "Unblock" else "Block",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}
