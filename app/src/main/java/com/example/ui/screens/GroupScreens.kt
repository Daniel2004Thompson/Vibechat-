package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ConversationEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.components.VibeAvatar
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AvatarColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(
    contacts: List<UserProfileEntity>,
    onBack: () -> Unit,
    onCreateGroup: (String, String, String, List<String>) -> Unit
) {
    BackHandler(onBack = onBack)

    var groupName by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedColor by rememberSaveable { mutableStateOf(AvatarColors[2]) }
    val selectedMemberIds = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Create New Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    onCreateGroup(
                        groupName,
                        description,
                        selectedColor,
                        selectedMemberIds.toList()
                    )
                },
                enabled = groupName.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("confirm_create_group_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            VibeAvatar(
                                name = groupName.ifBlank { "Group" },
                                avatarColorHex = selectedColor,
                                size = 64.dp,
                                isGroup = true
                            )
                            Column {
                                Text(
                                    text = "Group Theme Color",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AvatarColors.take(6).forEach { hex ->
                                        val isPicked = selectedColor == hex
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(hex))
                                                .border(
                                                    width = if (isPicked) 2.5.dp else 0.dp,
                                                    color = if (isPicked) Color.White else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedColor = hex }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Group Name") },
                            placeholder = { Text("e.g. Design Sync Crew 🚀") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Group Description") },
                            placeholder = { Text("What is this group about?") },
                            maxLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_description_input")
                        )
                    }
                }
            }

            if (selectedMemberIds.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Selected Members (${selectedMemberIds.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedMemberIds.forEach { id ->
                                val member = contacts.find { it.id == id }
                                if (member != null) {
                                    InputChip(
                                        selected = true,
                                        onClick = { selectedMemberIds.remove(id) },
                                        label = { Text(member.displayName) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove member",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Select Participants",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(contacts, key = { it.id }) { contact ->
                val isChecked = selectedMemberIds.contains(contact.id)
                Surface(
                    onClick = {
                        if (isChecked) selectedMemberIds.remove(contact.id)
                        else selectedMemberIds.add(contact.id)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isChecked) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VibeAvatar(
                            name = contact.displayName,
                            avatarColorHex = contact.avatarColor,
                            avatarUri = contact.avatarUri,
                            size = 44.dp,
                            isOnline = contact.isOnline,
                            showOnlineIndicator = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "@${contact.username} • ${contact.statusText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedMemberIds.add(contact.id)
                                else selectedMemberIds.remove(contact.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationInfoScreen(
    conversation: ConversationEntity?,
    allContacts: List<UserProfileEntity>,
    currentUserName: String,
    currentUserId: String,
    isContactBlocked: Boolean = false,
    onToggleBlockContact: () -> Unit = {},
    onBack: () -> Unit,
    onUpdateGroup: (String, String, String, List<String>) -> Unit,
    onToggleMute: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDeleteOrLeave: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    if (conversation == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onBack) { Text("Back to Chat") }
        }
        return
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showUnblockConfirmDialog by remember { mutableStateOf(false) }
    val participantIds = conversation.participantIds()
    val participants = allContacts.filter { it.id in participantIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = if (conversation.isGroup) "Group Info" else "Contact Info",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (conversation.isGroup) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit group")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VibeAvatar(
                            name = conversation.name,
                            avatarColorHex = conversation.avatarColor,
                            avatarUri = conversation.avatarUri,
                            size = 84.dp,
                            isOnline = true,
                            showOnlineIndicator = !conversation.isGroup,
                            isGroup = conversation.isGroup
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = conversation.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = conversation.description.ifBlank {
                                if (conversation.isGroup) "VibeChat Group" else "Available on VibeChat ✨"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Toggles
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (conversation.isMuted) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Mute Notifications", style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = conversation.isMuted,
                                onCheckedChange = { onToggleMute(conversation.id) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Pin Conversation", style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = conversation.isPinned,
                                onCheckedChange = { onTogglePin(conversation.id) }
                            )
                        }
                    }
                }
            }

            if (conversation.isGroup) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Members (${participants.size + 1})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showAddMemberDialog = true }) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Member")
                        }
                    }
                }

                // Current User item
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VibeAvatar(name = currentUserName, avatarColorHex = "#10B981", size = 42.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$currentUserName (You)", fontWeight = FontWeight.Bold)
                                Text("Group Admin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                items(participants, key = { it.id }) { member ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VibeAvatar(
                                name = member.displayName,
                                avatarColorHex = member.avatarColor,
                                avatarUri = member.avatarUri,
                                size = 42.dp,
                                isOnline = member.isOnline,
                                showOnlineIndicator = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "@${member.username} • ${member.statusText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (!conversation.isGroup) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (isContactBlocked) {
                                showUnblockConfirmDialog = true
                            } else {
                                showBlockConfirmDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("info_block_contact_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isContactBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = if (isContactBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                            contentDescription = null,
                            tint = if (isContactBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isContactBlocked) "Unblock ${conversation.name}" else "Block ${conversation.name}",
                            color = if (isContactBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onDeleteOrLeave(conversation.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (conversation.isGroup) "Leave & Delete Group" else "Delete Conversation",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        var editedName by remember { mutableStateOf(conversation.name) }
        var editedDesc by remember { mutableStateOf(conversation.description) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Group Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Group Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editedDesc,
                        onValueChange = { editedDesc = it },
                        label = { Text("Description") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateGroup(conversation.id, editedName, editedDesc, participantIds)
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddMemberDialog) {
        val nonMembers = allContacts.filter { it.id !in participantIds && it.id != currentUserId }
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Member to Group") },
            text = {
                if (nonMembers.isEmpty()) {
                    Text("All contacts are already in this group!")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        nonMembers.forEach { contact ->
                            Surface(
                                onClick = {
                                    onUpdateGroup(
                                        conversation.id,
                                        conversation.name,
                                        conversation.description,
                                        participantIds + contact.id
                                    )
                                    showAddMemberDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VibeAvatar(
                                        name = contact.displayName,
                                        avatarColorHex = contact.avatarColor,
                                        avatarUri = contact.avatarUri,
                                        size = 36.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(contact.displayName, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMemberDialog = false }) { Text("Close") }
            }
        )
    }

    if (showBlockConfirmDialog) {
        val contactName = conversation.name
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
        val contactName = conversation.name
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
