package com.ovehbe.junkboy.ui.compose.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.ovehbe.junkboy.database.FilteredMessage
import com.ovehbe.junkboy.database.SmsConversationSummary
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable

enum class MessageFilter {
    ALL, BLOCKED, CATEGORY
}

enum class FilteredViewMode {
    CONVERSATIONS, MESSAGES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    isDefaultSmsApp: Boolean = false,
    onOpenSmsConversation: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val serviceScope = remember { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
    
    var selectedFilter by remember { mutableStateOf(MessageFilter.ALL) }
    var selectedCategory by remember { mutableStateOf<MessageCategory?>(null) }
    var viewMode by remember { mutableStateOf(FilteredViewMode.CONVERSATIONS) }
    var messages by remember { mutableStateOf<List<FilteredMessage>>(emptyList()) }
    var conversations by remember { mutableStateOf<List<SmsConversationSummary>>(emptyList()) }
    var blockedMessages by remember { mutableStateOf<List<FilteredMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Load messages or conversations
    LaunchedEffect(selectedFilter, selectedCategory, searchQuery, viewMode) {
        try {
            isLoading = true
            error = null
            
            kotlinx.coroutines.delay(100)
            
            when {
                searchQuery.isNotBlank() -> {
                    // Always show flat messages when searching
                    database.filteredMessageDao().searchMessagesLimited(searchQuery, 100).collect { newMessages ->
                        messages = newMessages
                        isLoading = false
                    }
                }
                viewMode == FilteredViewMode.CONVERSATIONS && selectedFilter != MessageFilter.CATEGORY -> {
                    // Conversation view
                    val convos = when (selectedFilter) {
                        MessageFilter.BLOCKED -> database.filteredMessageDao().getBlockedConversations()
                        else -> database.filteredMessageDao().getAllConversations()
                    }
                    conversations = convos
                    isLoading = false
                }
                selectedFilter == MessageFilter.BLOCKED -> {
                    database.filteredMessageDao().getBlockedMessagesLimited(100).collect { newMessages ->
                        messages = newMessages
                        isLoading = false
                    }
                }
                selectedFilter == MessageFilter.CATEGORY && selectedCategory != null -> {
                    database.filteredMessageDao().getMessagesByCategoryLimited(selectedCategory!!, 100).collect { newMessages ->
                        messages = newMessages
                        isLoading = false
                    }
                }
                else -> {
                    database.filteredMessageDao().getAllMessagesLimited(100).collect { newMessages ->
                        messages = newMessages
                        isLoading = false
                    }
                }
            }
        } catch (e: Exception) {
            error = "Error loading messages: ${e.message}"
            isLoading = false
        }
    }
    
    LaunchedEffect(Unit) {
        try {
            database.filteredMessageDao().getBlockedMessages().collect {
                blockedMessages = it
            }
        } catch (e: Exception) {
            // Handle blocked messages error silently
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
                text = stringResource(R.string.filtered_messages),
                style = MaterialTheme.typography.headlineMedium,
                color = DesignColors.Primary
            )
            
            // View mode toggle
            IconButton(onClick = { 
                viewMode = if (viewMode == FilteredViewMode.CONVERSATIONS) FilteredViewMode.MESSAGES else FilteredViewMode.CONVERSATIONS
            }) {
                Icon(
                    if (viewMode == FilteredViewMode.CONVERSATIONS) Icons.Default.ViewList else Icons.Default.Forum,
                    contentDescription = if (viewMode == FilteredViewMode.CONVERSATIONS) stringResource(R.string.messages_show_messages) else stringResource(R.string.messages_show_conversations),
                    tint = DesignColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(DesignSpacing.SM))
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.messages_search_hint), color = DesignColors.Secondary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = DesignColors.Secondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.messages_clear),
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
        
        // Filter chips - horizontally scrollable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            // All messages
            FilterChip(
                selected = selectedFilter == MessageFilter.ALL,
                onClick = { 
                    selectedFilter = MessageFilter.ALL
                    selectedCategory = null
                    searchQuery = ""
                },
                label = { Text(stringResource(R.string.messages_tab_all), style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (selectedFilter == MessageFilter.ALL) {
                    { Icon(Icons.Default.AllInbox, null, Modifier.size(16.dp)) }
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
            
            // Blocked messages
            FilterChip(
                selected = selectedFilter == MessageFilter.BLOCKED,
                onClick = { 
                    selectedFilter = MessageFilter.BLOCKED
                    selectedCategory = null
                    searchQuery = ""
                },
                label = { 
                    val count = if (viewMode == FilteredViewMode.CONVERSATIONS) {
                        conversations.count { it.hasBlocked }
                    } else {
                        blockedMessages.size
                    }
                    Text(stringResource(R.string.messages_tab_blocked, count), style = MaterialTheme.typography.labelMedium)
                },
                leadingIcon = if (selectedFilter == MessageFilter.BLOCKED) {
                    { Icon(Icons.Default.Block, null, Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DesignColors.JunkMessage,
                    selectedLabelColor = DesignColors.ButtonText,
                    selectedLeadingIconColor = DesignColors.ButtonText,
                    containerColor = DesignColors.Surface,
                    labelColor = DesignColors.Secondary
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(32.dp)
            )
            
            // Category chips
            MessageCategory.values().forEach { category ->
                FilterChip(
                    selected = selectedFilter == MessageFilter.CATEGORY && selectedCategory == category,
                    onClick = { 
                        selectedFilter = MessageFilter.CATEGORY
                        selectedCategory = category
                        searchQuery = ""
                        // Switch to messages view when filtering by category
                        viewMode = FilteredViewMode.MESSAGES
                    },
                    label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = if (selectedFilter == MessageFilter.CATEGORY && selectedCategory == category) {
                        {
                            Icon(
                                getCategoryIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getCategoryColor(category),
                        selectedLabelColor = DesignColors.ButtonText,
                        selectedLeadingIconColor = DesignColors.ButtonText,
                        containerColor = DesignColors.Surface,
                        labelColor = DesignColors.Secondary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(DesignSpacing.MD))
        
        // Content
        when {
            error != null -> {
                ErrorState(error = error!!) {
                    selectedFilter = MessageFilter.ALL
                    selectedCategory = null
                }
            }
            isLoading -> {
                LoadingState()
            }
            searchQuery.isNotBlank() && messages.isEmpty() -> {
                EmptyMessagesState(selectedFilter, selectedCategory, isSearching = true)
            }
            viewMode == FilteredViewMode.CONVERSATIONS && selectedFilter != MessageFilter.CATEGORY && conversations.isEmpty() -> {
                EmptyMessagesState(selectedFilter, selectedCategory, isSearching = false)
            }
            viewMode == FilteredViewMode.MESSAGES && messages.isEmpty() -> {
                EmptyMessagesState(selectedFilter, selectedCategory, isSearching = false)
            }
            viewMode == FilteredViewMode.CONVERSATIONS && selectedFilter != MessageFilter.CATEGORY && searchQuery.isBlank() -> {
                // Conversation view
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(conversations.size) { index ->
                        val conversation = conversations[index]
                        ConversationCard(
                            conversation = conversation,
                            context = context,
                            onClick = {
                                if (isDefaultSmsApp && onOpenSmsConversation != null) {
                                    onOpenSmsConversation(conversation.sender)
                                } else {
                                    openExternalSmsApp(context, conversation.sender)
                                }
                            },
                            onAllowSender = {
                                serviceScope.launch {
                                    try {
                                        val allowedSender = com.ovehbe.junkboy.database.AllowedSender(
                                            phoneNumber = conversation.sender,
                                            displayName = null
                                        )
                                        database.allowedSenderDao().insertAllowedSender(allowedSender)
                                        // Update all existing messages from this sender to ALLOWED category
                                        val normalizedSender = conversation.sender.replace(Regex("[+\\-()\\s]"), "")
                                        database.filteredMessageDao().updateSenderCategory(
                                            sender = conversation.sender,
                                            normalizedSender = normalizedSender,
                                            category = com.ovehbe.junkboy.database.MessageCategory.ALLOWED,
                                            filterType = com.ovehbe.junkboy.database.FilterType.ALLOWED_SENDER
                                        )
                                        Log.d("MessagesScreen", "Added ${conversation.sender} to allowed senders")
                                    } catch (e: Exception) {
                                        Log.e("MessagesScreen", "Error adding allowed sender", e)
                                    }
                                }
                            }
                        )
                    }
                }
            }
            else -> {
                // Flat messages view
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageCard(
                            message = message,
                            context = context,
                            onClick = {
                                if (isDefaultSmsApp && onOpenSmsConversation != null) {
                                    onOpenSmsConversation(message.sender)
                                } else {
                                    openExternalSmsApp(context, message.sender)
                                }
                            },
                            onAllowSender = { 
                                serviceScope.launch {
                                    try {
                                        val allowedSender = com.ovehbe.junkboy.database.AllowedSender(
                                            phoneNumber = message.sender,
                                            displayName = null
                                        )
                                        database.allowedSenderDao().insertAllowedSender(allowedSender)
                                        // Update all existing messages from this sender to ALLOWED category
                                        val normalizedSender = message.sender.replace(Regex("[+\\-()\\s]"), "")
                                        database.filteredMessageDao().updateSenderCategory(
                                            sender = message.sender,
                                            normalizedSender = normalizedSender,
                                            category = com.ovehbe.junkboy.database.MessageCategory.ALLOWED,
                                            filterType = com.ovehbe.junkboy.database.FilterType.ALLOWED_SENDER
                                        )
                                        Log.d("MessagesScreen", "Added ${message.sender} to allowed senders")
                                    } catch (e: Exception) {
                                        Log.e("MessagesScreen", "Error adding allowed sender", e)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: SmsConversationSummary,
    context: Context,
    onClick: () -> Unit,
    onAllowSender: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DesignSpacing.XS),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = conversation.sender,
                            style = MaterialTheme.typography.titleSmall,
                            color = DesignColors.Primary,
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Blocked indicator
                        if (conversation.hasBlocked) {
                            Surface(
                                color = DesignColors.JunkMessage.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = stringResource(R.string.messages_blocked),
                                    tint = DesignColors.JunkMessage,
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(12.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = formatTimestamp(conversation.lastMessageDate.time, context),
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
                        // Category badge
                        Surface(
                            color = getCategoryColor(conversation.lastCategory).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = conversation.lastCategory.name.take(4),
                                style = MaterialTheme.typography.labelSmall,
                                color = getCategoryColor(conversation.lastCategory),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        
                        // Message count
                        if (conversation.messageCount > 1) {
                            Text(
                                text = "${conversation.messageCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DesignColors.Secondary
                            )
                        }
                        
                        // More options
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.messages_more_options),
                                    tint = DesignColors.Secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.messages_open_in_sms_app)) },
                                    leadingIcon = { Icon(Icons.Default.OpenInNew, null) },
                                    onClick = {
                                        showMenu = false
                                        onClick()
                                    }
                                )
                                if (conversation.hasBlocked) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.messages_allow_sender)) },
                                        leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                                        onClick = {
                                            showMenu = false
                                            onAllowSender()
                                        }
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

@Composable
private fun MessageCard(
    message: FilteredMessage,
    context: Context,
    onClick: () -> Unit,
    onAllowSender: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DesignColors.Surface
        ),
        shape = RoundedCornerShape(DesignBorderRadius.MD)
    ) {
        Column(
            modifier = Modifier.padding(DesignSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
                    modifier = Modifier.weight(1f)
                ) {
                    // Category icon
                    Icon(
                        getCategoryIcon(message.category),
                        contentDescription = null,
                        tint = getCategoryColor(message.category),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    // Sender
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.titleSmall,
                        color = DesignColors.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Blocked indicator
                    if (message.isBlocked) {
                        Surface(
                            color = DesignColors.JunkMessage.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = stringResource(R.string.messages_blocked),
                                tint = DesignColors.JunkMessage,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(12.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignSpacing.XS),
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
                    
                    // More options
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.messages_more_options),
                                tint = DesignColors.Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.messages_open_in_sms_app)) },
                                leadingIcon = { Icon(Icons.Default.OpenInNew, null) },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                }
                            )
                            if (message.isBlocked) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.messages_allow_sender)) },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                                    onClick = {
                                        showMenu = false
                                        onAllowSender()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Message content
            Text(
                text = message.messageBody,
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Footer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(message.receivedAt.time, context),
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary
                )
                
                // Open in SMS indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.messages_tap_to_open),
                        style = MaterialTheme.typography.labelSmall,
                        color = DesignColors.Secondary.copy(alpha = 0.7f)
                    )
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = stringResource(R.string.messages_open_in_sms_app),
                        tint = DesignColors.Secondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                }
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
                text = stringResource(R.string.messages_loading),
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
                Text(stringResource(R.string.messages_retry))
            }
        }
    }
}

@Composable
private fun EmptyMessagesState(
    filter: MessageFilter,
    category: MessageCategory?,
    isSearching: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.MD)
        ) {
            Icon(
                when {
                    isSearching -> Icons.Default.SearchOff
                    filter == MessageFilter.BLOCKED -> Icons.Default.Block
                    category != null -> getCategoryIcon(category)
                    else -> Icons.Default.Inbox
                },
                contentDescription = null,
                tint = DesignColors.Secondary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = when {
                    isSearching -> stringResource(R.string.messages_empty_search)
                    filter == MessageFilter.BLOCKED -> stringResource(R.string.messages_empty_blocked)
                    category != null -> stringResource(R.string.messages_empty_category, category.name.lowercase())
                    else -> stringResource(R.string.messages_empty_none)
                },
                style = MaterialTheme.typography.titleMedium,
                color = DesignColors.Secondary
            )
            Text(
                text = when {
                    isSearching -> stringResource(R.string.messages_empty_hint_search)
                    filter == MessageFilter.BLOCKED -> stringResource(R.string.messages_empty_hint_blocked)
                    else -> stringResource(R.string.messages_empty_hint_none)
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

private fun getCategoryIcon(category: MessageCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        MessageCategory.GENERAL -> Icons.Default.Email
        MessageCategory.PROMOTION -> Icons.Default.LocalOffer
        MessageCategory.NOTIFICATION -> Icons.Default.Notifications
        MessageCategory.TRANSACTION -> Icons.Default.AccountBalance
        MessageCategory.JUNK -> Icons.Default.Delete
        MessageCategory.ALLOWED -> Icons.Default.Verified
    }
}

private fun formatTimestamp(timestamp: Long, context: Context): String {
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

/**
 * Open SMS conversation in the external default SMS app.
 * Only safe to call when this app is NOT the default SMS app,
 * otherwise it causes an infinite navigation loop.
 */
private fun openExternalSmsApp(context: Context, sender: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$sender")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("MessagesScreen", "Error opening SMS app", e)
    }
}
