package com.ovehbe.junkboy.ui.compose.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log
import com.ovehbe.junkboy.R
import com.ovehbe.junkboy.database.AppDatabase
import com.ovehbe.junkboy.database.ChatMessage
import com.ovehbe.junkboy.database.FilteredMessage
import com.ovehbe.junkboy.database.SmsConversationSummary
import com.ovehbe.junkboy.database.ConversationSummary
import com.ovehbe.junkboy.database.HubConversation
import com.ovehbe.junkboy.database.HubAppSection
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.service.NotificationListenerService
import com.ovehbe.junkboy.ui.theme.*
import com.ovehbe.junkboy.utils.AppLauncher
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.SmsAppManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable

sealed class HubItem {
    data class SmsMessage(val message: FilteredMessage) : HubItem()
    data class ChatNotification(val message: ChatMessage) : HubItem()
}

enum class HubFilter {
    ALL, SMS, CHATS
}

enum class ViewMode {
    CONVERSATIONS, MESSAGES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    permissionRefreshTrigger: Int = 0,
    isDefaultSmsApp: Boolean = false,
    onOpenSmsConversation: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val serviceScope = remember { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
    val appLauncher = remember { AppLauncher(context) }
    val smsAppManager = remember { SmsAppManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }
    
    // Load preferences for display mode and default view
    val displayMode = remember { preferencesManager.getHubDisplayMode() }
    val defaultView = remember { preferencesManager.getHubDefaultView() }
    
    // Set initial filter based on display mode preference
    val initialFilter = when (displayMode) {
        "sms_only" -> HubFilter.SMS
        "chats_only" -> HubFilter.CHATS
        else -> HubFilter.ALL
    }
    
    // Set initial view mode based on preference
    val initialViewMode = if (defaultView == "messages") ViewMode.MESSAGES else ViewMode.CONVERSATIONS
    
    var hubFilter by remember { mutableStateOf(initialFilter) }
    var viewMode by remember { mutableStateOf(initialViewMode) }
    var smsMessages by remember { mutableStateOf<List<FilteredMessage>>(emptyList()) }
    var smsConversations by remember { mutableStateOf<List<SmsConversationSummary>>(emptyList()) }
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Check if notification listener permission is granted
    val hasNotificationPermission = remember(permissionRefreshTrigger) {
        try {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            enabledServices?.contains(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }
    
    // Load SMS messages/conversations
    LaunchedEffect(hubFilter, viewMode, searchQuery, permissionRefreshTrigger) {
        try {
            isLoading = true
            error = null
            
            kotlinx.coroutines.delay(100)
            
            when (hubFilter) {
                HubFilter.ALL, HubFilter.SMS -> {
                    if (viewMode == ViewMode.CONVERSATIONS) {
                        // Load conversation summaries
                        val conversations = database.filteredMessageDao().getAllowedConversations()
                        smsConversations = if (searchQuery.isNotBlank()) {
                            conversations.filter { 
                                it.sender.contains(searchQuery, ignoreCase = true) ||
                                it.lastMessage.contains(searchQuery, ignoreCase = true)
                            }
                        } else {
                            conversations
                        }
                    } else {
                        // Load individual messages
                        database.filteredMessageDao().getAllowedMessagesLimited(100).collect { messages ->
                            smsMessages = if (searchQuery.isNotBlank()) {
                                messages.filter { 
                                    it.messageBody.contains(searchQuery, ignoreCase = true) ||
                                    it.sender.contains(searchQuery, ignoreCase = true)
                                }
                            } else {
                                messages
                            }
                        }
                    }
                    isLoading = false
                }
                HubFilter.CHATS -> {
                    smsMessages = emptyList()
                    smsConversations = emptyList()
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            error = "Error loading messages: ${e.message}"
            isLoading = false
            Log.e("HubScreen", "Error loading messages", e)
        }
    }
    
    // Load chat messages separately
    LaunchedEffect(hubFilter, searchQuery, permissionRefreshTrigger) {
        if (hubFilter == HubFilter.ALL || hubFilter == HubFilter.CHATS) {
            try {
                if (searchQuery.isNotBlank()) {
                    database.chatMessageDao().searchChatMessagesLimited(searchQuery, 100).collect { messages ->
                        chatMessages = messages
                    }
                } else {
                    database.chatMessageDao().getAllChatMessagesLimited(100).collect { messages ->
                        chatMessages = messages
                    }
                }
            } catch (e: Exception) {
                Log.e("HubScreen", "Error loading chat messages", e)
            }
        }
    }
    
    // Combine items based on filter
    val combinedItems = remember(smsMessages, chatMessages, hubFilter, viewMode) {
        when {
            viewMode == ViewMode.CONVERSATIONS && hubFilter != HubFilter.CHATS -> {
                // In conversation mode, don't combine - use smsConversations separately
                emptyList<HubItem>()
            }
            hubFilter == HubFilter.SMS -> {
                smsMessages.map { HubItem.SmsMessage(it) }
            }
            hubFilter == HubFilter.CHATS -> {
                chatMessages.map { HubItem.ChatNotification(it) }
            }
            else -> {
                // Combine and sort by timestamp
                val smsList = smsMessages.map { HubItem.SmsMessage(it) }
                val chatList = chatMessages.map { HubItem.ChatNotification(it) }
                (smsList + chatList).sortedByDescending { item ->
                    when (item) {
                        is HubItem.SmsMessage -> item.message.receivedAt.time
                        is HubItem.ChatNotification -> item.message.receivedAt.time
                    }
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DesignSpacing.MD)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_hub),
                style = MaterialTheme.typography.headlineMedium,
                color = DesignColors.Primary
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(DesignSpacing.XS)) {
                // View mode toggle
                IconButton(onClick = { 
                    viewMode = if (viewMode == ViewMode.CONVERSATIONS) ViewMode.MESSAGES else ViewMode.CONVERSATIONS
                }) {
                    Icon(
                        if (viewMode == ViewMode.CONVERSATIONS) Icons.Default.ViewList else Icons.Default.Forum,
                        contentDescription = if (viewMode == ViewMode.CONVERSATIONS) stringResource(R.string.hub_show_messages) else stringResource(R.string.hub_show_conversations),
                        tint = DesignColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = stringResource(R.string.hub_info),
                        tint = DesignColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(DesignSpacing.SM))
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.hub_search_hint), color = DesignColors.Secondary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = DesignColors.Secondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.hub_clear),
                            tint = DesignColors.Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(DesignBorderRadius.MD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DesignColors.Primary,
                unfocusedBorderColor = DesignColors.InputBorder,
                focusedContainerColor = DesignColors.InputBackground,
                unfocusedContainerColor = DesignColors.InputBackground
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(DesignSpacing.SM))
        
        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            FilterChip(
                selected = hubFilter == HubFilter.ALL,
                onClick = { hubFilter = HubFilter.ALL },
                label = { Text(stringResource(R.string.hub_tab_all), style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (hubFilter == HubFilter.ALL) {
                    { Icon(Icons.Default.Inbox, null, Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DesignColors.Primary,
                    selectedLabelColor = DesignColors.ButtonText,
                    selectedLeadingIconColor = DesignColors.ButtonText,
                    containerColor = DesignColors.Surface,
                    labelColor = DesignColors.Secondary
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(32.dp)
            )
            
            FilterChip(
                selected = hubFilter == HubFilter.SMS,
                onClick = { hubFilter = HubFilter.SMS },
                label = { 
                    val count = if (viewMode == ViewMode.CONVERSATIONS) smsConversations.size else smsMessages.size
                    Text(stringResource(R.string.hub_tab_sms, count), style = MaterialTheme.typography.labelMedium)
                },
                leadingIcon = if (hubFilter == HubFilter.SMS) {
                    { Icon(Icons.Default.Sms, null, Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DesignColors.Accent,
                    selectedLabelColor = DesignColors.ButtonText,
                    selectedLeadingIconColor = DesignColors.ButtonText,
                    containerColor = DesignColors.Surface,
                    labelColor = DesignColors.Secondary
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(32.dp)
            )
            
            FilterChip(
                selected = hubFilter == HubFilter.CHATS,
                onClick = { hubFilter = HubFilter.CHATS },
                label = { Text(stringResource(R.string.hub_tab_chats, chatMessages.size), style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (hubFilter == HubFilter.CHATS) {
                    { Icon(Icons.Default.Chat, null, Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DesignColors.Primary,
                    selectedLabelColor = DesignColors.ButtonText,
                    selectedLeadingIconColor = DesignColors.ButtonText,
                    containerColor = DesignColors.Surface,
                    labelColor = DesignColors.Secondary
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(DesignSpacing.MD))
        
        // Permission status for chats
        if (!hasNotificationPermission && (hubFilter == HubFilter.ALL || hubFilter == HubFilter.CHATS)) {
            PermissionCard(
                onRequestPermission = {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("HubScreen", "Error opening notification settings", e)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(DesignSpacing.MD))
        }
        
        // Content
        when {
            error != null -> {
                ErrorState(error = error!!) {
                    hubFilter = HubFilter.ALL
                    searchQuery = ""
                }
            }
            isLoading -> {
                LoadingState()
            }
            viewMode == ViewMode.CONVERSATIONS && hubFilter != HubFilter.CHATS && smsConversations.isEmpty() && chatMessages.isEmpty() -> {
                EmptyState(hubFilter)
            }
            viewMode == ViewMode.MESSAGES && combinedItems.isEmpty() -> {
                EmptyState(hubFilter)
            }
            viewMode == ViewMode.CONVERSATIONS && hubFilter != HubFilter.CHATS -> {
                // Conversation view for SMS
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(smsConversations.size) { index ->
                        val conversation = smsConversations[index]
                        SmsConversationCard(
                            conversation = conversation,
                            onClick = {
                                if (isDefaultSmsApp && onOpenSmsConversation != null) {
                                    onOpenSmsConversation(conversation.sender)
                                } else {
                                    openExternalSmsApp(context, conversation.sender)
                                }
                            }
                        )
                    }
                    
                    // Also show chat messages if in ALL filter
                    if (hubFilter == HubFilter.ALL && chatMessages.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.hub_chat_notifications),
                                style = MaterialTheme.typography.titleSmall,
                                color = DesignColors.Secondary,
                                modifier = Modifier.padding(vertical = DesignSpacing.SM)
                            )
                        }
                        
                        items(chatMessages.size) { index ->
                            ChatMessageCard(
                                message = chatMessages[index],
                                onClick = {
                                    try {
                                        appLauncher.openApp(chatMessages[index].packageName, chatMessages[index].appName)
                                    } catch (e: Exception) {
                                        Log.e("HubScreen", "Error launching app", e)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            else -> {
                // Flat message view
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(combinedItems.size) { index ->
                        when (val item = combinedItems[index]) {
                            is HubItem.SmsMessage -> {
                                SmsMessageCard(
                                    message = item.message,
                                    onClick = {
                                        if (isDefaultSmsApp && onOpenSmsConversation != null) {
                                            onOpenSmsConversation(item.message.sender)
                                        } else {
                                            openExternalSmsApp(context, item.message.sender)
                                        }
                                    }
                                )
                            }
                            is HubItem.ChatNotification -> {
                                ChatMessageCard(
                                    message = item.message,
                                    onClick = {
                                        try {
                                            appLauncher.openApp(item.message.packageName, item.message.appName)
                                        } catch (e: Exception) {
                                            Log.e("HubScreen", "Error launching app", e)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Info dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = DesignColors.Accent) },
            title = { Text(stringResource(R.string.hub_about_title)) },
            text = {
                Text(stringResource(R.string.hub_about_message))
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.hub_got_it))
                }
            }
        )
    }
}

@Composable
private fun SmsConversationCard(
    conversation: SmsConversationSummary,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DesignColors.Surface),
        shape = RoundedCornerShape(DesignBorderRadius.MD)
    ) {
        Row(
            modifier = Modifier.padding(DesignSpacing.MD),
            horizontalArrangement = Arrangement.spacedBy(DesignSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            Surface(
                color = getCategoryColor(conversation.lastCategory).copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = conversation.sender.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = getCategoryColor(conversation.lastCategory),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.sender,
                        style = MaterialTheme.typography.titleSmall,
                        color = DesignColors.Primary,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = formatTimestamp(context, conversation.lastMessageDate.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (conversation.unreadCount > 0) DesignColors.Accent else DesignColors.Secondary
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (conversation.unreadCount > 0) DesignColors.OnSurface else DesignColors.Secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DesignSpacing.XS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Message count badge
                        if (conversation.messageCount > 1) {
                            Surface(
                                color = DesignColors.Surface,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${conversation.messageCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DesignColors.Secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        // Unread badge
                        if (conversation.unreadCount > 0) {
                            Surface(
                                color = DesignColors.Accent,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${conversation.unreadCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DesignColors.ButtonText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        // Open indicator
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.hub_open),
                            tint = DesignColors.Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsMessageCard(
    message: FilteredMessage,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DesignColors.Surface),
        shape = RoundedCornerShape(DesignBorderRadius.MD)
    ) {
        Column(
            modifier = Modifier.padding(DesignSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SMS indicator
                    Surface(
                        color = DesignColors.Accent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Sms,
                            contentDescription = stringResource(R.string.hub_sms),
                            tint = DesignColors.Accent,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                    
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.titleSmall,
                        color = DesignColors.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category badge
                    Surface(
                        color = getCategoryColor(message.category).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = message.category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = getCategoryColor(message.category),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Open indicator
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = stringResource(R.string.hub_open_in_sms_app),
                        tint = DesignColors.Secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Text(
                text = message.messageBody,
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = formatTimestamp(context, message.receivedAt.time),
                style = MaterialTheme.typography.bodySmall,
                color = DesignColors.Secondary
            )
        }
    }
}

@Composable
private fun ChatMessageCard(
    message: ChatMessage,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DesignColors.Surface),
        shape = RoundedCornerShape(DesignBorderRadius.MD)
    ) {
        Column(
            modifier = Modifier.padding(DesignSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat indicator
                    Surface(
                        color = DesignColors.Primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = stringResource(R.string.hub_chat),
                            tint = DesignColors.Primary,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                    
                    Text(
                        text = message.appName,
                        style = MaterialTheme.typography.titleSmall,
                        color = DesignColors.Primary
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(context, message.receivedAt.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignColors.Secondary
                    )

                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = stringResource(R.string.hub_open_app),
                        tint = DesignColors.Secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Text(
                text = message.messageContent,
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DesignColors.NotificationMessage.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(DesignBorderRadius.MD)
    ) {
        Row(
            modifier = Modifier.padding(DesignSpacing.MD),
            horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = DesignColors.NotificationMessage,
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.hub_notification_access_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = DesignColors.Primary
                )
                Text(
                    text = stringResource(R.string.hub_notification_access_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary
                )
            }

            TextButton(onClick = onRequestPermission) {
                Text(stringResource(R.string.hub_grant))
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.MD)
        ) {
            CircularProgressIndicator(color = DesignColors.Accent)
            Text(
                text = stringResource(R.string.hub_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.Secondary
            )
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.MD)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = DesignColors.JunkMessage,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.Secondary
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.hub_retry))
            }
        }
    }
}

@Composable
private fun EmptyState(filter: HubFilter) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.MD)
        ) {
            Icon(
                when (filter) {
                    HubFilter.ALL -> Icons.Default.Inbox
                    HubFilter.SMS -> Icons.Default.Sms
                    HubFilter.CHATS -> Icons.Default.Chat
                },
                contentDescription = null,
                tint = DesignColors.Secondary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = when (filter) {
                    HubFilter.ALL -> stringResource(R.string.hub_empty_all)
                    HubFilter.SMS -> stringResource(R.string.hub_empty_sms)
                    HubFilter.CHATS -> stringResource(R.string.hub_empty_chats)
                },
                style = MaterialTheme.typography.titleMedium,
                color = DesignColors.Secondary
            )
            Text(
                text = when (filter) {
                    HubFilter.ALL -> stringResource(R.string.hub_empty_hint_all)
                    HubFilter.SMS -> stringResource(R.string.hub_empty_hint_sms)
                    HubFilter.CHATS -> stringResource(R.string.hub_empty_hint_chats)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.Secondary.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getCategoryColor(category: MessageCategory): androidx.compose.ui.graphics.Color {
    return when (category) {
        MessageCategory.GENERAL -> DesignColors.GeneralMessage
        MessageCategory.PROMOTION -> DesignColors.PromotionMessage
        MessageCategory.NOTIFICATION -> DesignColors.NotificationMessage
        MessageCategory.TRANSACTION -> DesignColors.TransactionMessage
        MessageCategory.JUNK -> DesignColors.JunkMessage
        MessageCategory.ALLOWED -> DesignColors.AllowedMessage
    }
}

/**
 * Open SMS conversation in the external default SMS app.
 * Only safe to call when this app is NOT the default SMS app,
 * otherwise it causes an infinite navigation loop.
 */
private fun openExternalSmsApp(context: android.content.Context, sender: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$sender")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("HubScreen", "Error opening SMS app", e)
    }
}

private fun formatTimestamp(context: android.content.Context, timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> context.getString(R.string.time_just_now)
        diff < 3600_000 -> context.getString(R.string.time_minutes_ago, diff / 60_000)
        diff < 86400_000 -> context.getString(R.string.time_hours_ago, diff / 3600_000)
        diff < 604800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
