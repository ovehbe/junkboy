package com.ovehbe.junkboy.ui.compose.screens

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ovehbe.junkboy.R
import com.ovehbe.junkboy.database.AppDatabase
import com.ovehbe.junkboy.database.FilteredMessage
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.ui.theme.DesignColors
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.SmsAppManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class SmsConversation(
    val threadId: Long,
    val address: String,
    val displayName: String,
    val snippet: String,
    val messageCount: Int,
    val unreadCount: Int,
    val lastMessageDate: Date,
    val category: MessageCategory? = null, // Junkboy category if known
    val photoUri: String? = null,
    val isLocalOnly: Boolean = false
)

data class SmsMessage(
    val id: Long,
    val address: String,
    val body: String,
    val date: Date,
    val type: Int, // 1 = received, 2 = sent
    val read: Boolean,
    val status: Int = 0,
    val category: MessageCategory? = null
)

data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

private data class FilteredSmsSnapshot(
    val messages: List<FilteredMessage>,
    val categoryCache: Map<String, MessageCategory>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(
    isDefaultSmsApp: Boolean,
    pendingAddress: String? = null,
    onPendingAddressConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val smsAppManager = remember { SmsAppManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    val serviceScope = remember { CoroutineScope(Dispatchers.IO + SupervisorJob()) }

    var conversations by remember { mutableStateOf<List<SmsConversation>>(emptyList()) }
    var selectedConversation by remember { mutableStateOf<SmsConversation?>(null) }
    var conversationMessages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showNewMessageScreen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    // Category lookup cache
    var categoryCache by remember { mutableStateOf<Map<String, MessageCategory>>(emptyMap()) }
    var filteredMessages by remember { mutableStateOf<List<FilteredMessage>>(emptyList()) }

    fun refreshConversations(showLoading: Boolean = false) {
        serviceScope.launch {
            if (showLoading) {
                withContext(Dispatchers.Main) {
                    isLoading = true
                    error = null
                }
            }

            try {
                val snapshot = loadFilteredSmsSnapshot(database)

                withContext(Dispatchers.Main) {
                    filteredMessages = snapshot.messages
                    categoryCache = snapshot.categoryCache
                }

                loadAllConversations(
                    contentResolver = context.contentResolver,
                    categoryCache = snapshot.categoryCache,
                    filteredMessages = snapshot.messages
                ) { result ->
                    conversations = result
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("SmsScreen", "Error refreshing conversations", e)
                withContext(Dispatchers.Main) {
                    error = e.message
                    isLoading = false
                }
            }
        }
    }
    // Auto-navigate to conversation when pendingAddress is provided
    LaunchedEffect(pendingAddress, conversations) {
        if (pendingAddress != null && conversations.isNotEmpty()) {
            val matchedConversation = conversations.find { conv ->
                conv.address.contains(pendingAddress, ignoreCase = true) ||
                pendingAddress.contains(conv.address, ignoreCase = true) ||
                conv.displayName.contains(pendingAddress, ignoreCase = true)
            }
            if (matchedConversation != null) {
                selectedConversation = matchedConversation
                onPendingAddressConsumed()
            }
        }
    }

    // Load conversations
    LaunchedEffect(isDefaultSmsApp) {
        if (isDefaultSmsApp) {
            refreshConversations(showLoading = true)
        } else {
            conversations = emptyList()
            conversationMessages = emptyList()
            selectedConversation = null
            isLoading = false
        }
    }
    
    // Refresh conversations periodically
    LaunchedEffect(isDefaultSmsApp) {
        if (isDefaultSmsApp) {
            while (true) {
                delay(10000) // Refresh every 10 seconds
                if (selectedConversation == null && !showNewMessageScreen) {
                    refreshConversations()
                }
            }
        }
    }
    
    // Load messages for selected conversation
    LaunchedEffect(selectedConversation, filteredMessages) {
        selectedConversation?.let { conversation ->
            if (conversation.isLocalOnly) {
                conversationMessages = SmsConversationFallbacks.localMessagesForSender(
                    sender = conversation.address,
                    filteredMessages = filteredMessages
                )
                serviceScope.launch {
                    database.filteredMessageDao().markSenderAsRead(conversation.address)
                    val snapshot = loadFilteredSmsSnapshot(database)
                    withContext(Dispatchers.Main) {
                        filteredMessages = snapshot.messages
                        categoryCache = snapshot.categoryCache
                    }
                }
            } else {
                loadConversationMessages(context.contentResolver, conversation.threadId, categoryCache) { messages ->
                    conversationMessages = messages
                    // Mark as read
                    markConversationAsRead(context.contentResolver, conversation.threadId)
                }
            }
        }
    }
    
    // Filter conversations by search query
    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { conv ->
                conv.displayName.contains(searchQuery, ignoreCase = true) ||
                conv.address.contains(searchQuery, ignoreCase = true) ||
                conv.snippet.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    when {
        showNewMessageScreen -> {
            NewMessageScreen(
                onBack = { showNewMessageScreen = false },
                onMessageSent = { recipient, message ->
                    serviceScope.launch {
                        val success = sendSmsWithDelivery(context, recipient, message)
                        if (success) {
                            saveSentMessage(context.contentResolver, recipient, message)
                            withContext(Dispatchers.Main) {
                                showNewMessageScreen = false
                                refreshConversations()
                            }
                        }
                    }
                }
            )
        }
        selectedConversation != null -> {
            ChatScreen(
                conversation = selectedConversation!!,
                messages = conversationMessages,
                showCategoryBadges = preferencesManager.shouldShowCategoryBadges(),
                onBack = { 
                    selectedConversation = null
                    refreshConversations()
                },
                onSendMessage = { message ->
                    serviceScope.launch {
                        val success = sendSmsWithDelivery(context, selectedConversation!!.address, message)
                        if (success) {
                            saveSentMessage(context.contentResolver, selectedConversation!!.address, message)
                            if (selectedConversation!!.isLocalOnly) {
                                refreshConversations()
                            } else {
                                loadConversationMessages(context.contentResolver, selectedConversation!!.threadId, categoryCache) { messages ->
                                    conversationMessages = messages
                                }
                            }
                        }
                    }
                },
                onDeleteConversation = {
                    serviceScope.launch {
                        if (!selectedConversation!!.isLocalOnly) {
                            deleteConversation(context.contentResolver, selectedConversation!!.threadId)
                        }
                        withContext(Dispatchers.Main) {
                            selectedConversation = null
                            refreshConversations()
                        }
                    }
                }
            )
        }
        else -> {
            Scaffold(
                topBar = {
                    if (isSearching) {
                        SearchTopBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onClose = {
                                isSearching = false
                                searchQuery = ""
                            }
                        )
                    } else {
                        TopAppBar(
                            title = { Text(stringResource(R.string.sms_messages)) },
                            actions = {
                                IconButton(onClick = { isSearching = true }) {
                                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.sms_search))
                                }
                            }
                        )
                    }
                },
                floatingActionButton = {
                    if (isDefaultSmsApp && !isLoading) {
                        FloatingActionButton(
                            onClick = { showNewMessageScreen = true },
                            containerColor = DesignColors.Accent
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.sms_new_message),
                                tint = Color.White
                            )
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when {
                        !isDefaultSmsApp -> {
                            NotDefaultSmsAppState(
                                onMakeDefault = { smsAppManager.requestDefaultSmsApp() }
                            )
                        }
                        error != null -> {
                            ErrorState(error = error!!) {
                                error = null
                                refreshConversations(showLoading = true)
                            }
                        }
                        isLoading -> {
                            LoadingState()
                        }
                        filteredConversations.isEmpty() -> {
                            if (searchQuery.isNotBlank()) {
                                NoSearchResultsState(searchQuery)
                            } else {
                                EmptySmsState()
                            }
                        }
                        else -> {
                            ConversationsList(
                                conversations = filteredConversations,
                                showCategoryBadges = preferencesManager.shouldShowCategoryBadges(),
                                onConversationClick = { conversation ->
                                    selectedConversation = conversation
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.sms_search_conversations_hint)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.sms_close_search))
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.sms_clear))
                }
            }
        }
    )
}

@Composable
private fun ConversationsList(
    conversations: List<SmsConversation>,
    showCategoryBadges: Boolean,
    onConversationClick: (SmsConversation) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(conversations, key = { it.threadId }) { conversation ->
            ConversationItem(
                conversation = conversation,
                showCategoryBadge = showCategoryBadges,
                onClick = { onConversationClick(conversation) }
            )
            Divider(color = DesignColors.Surface, thickness = 1.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationItem(
    conversation: SmsConversation,
    showCategoryBadge: Boolean,
    onClick: () -> Unit
) {
    val hasUnread = conversation.unreadCount > 0
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    when (conversation.category) {
                        MessageCategory.JUNK -> Color(0xFFE53935)
                        MessageCategory.PROMOTION -> Color(0xFFFFA726)
                        MessageCategory.TRANSACTION -> Color(0xFF66BB6A)
                        MessageCategory.NOTIFICATION -> Color(0xFF42A5F5)
                        else -> if (hasUnread) DesignColors.Accent else DesignColors.Surface
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = conversation.displayName.firstOrNull()?.uppercase() ?: "#",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = conversation.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                        color = DesignColors.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Category badge
                    if (showCategoryBadge && conversation.category != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        CategoryBadge(category = conversation.category)
                    }
                }
                
                Text(
                    text = formatConversationTime(conversation.lastMessageDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasUnread) DesignColors.Accent else DesignColors.Secondary
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasUnread) DesignColors.Primary else DesignColors.Secondary,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (hasUnread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = DesignColors.Accent,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: MessageCategory) {
    val (color, label) = when (category) {
        MessageCategory.JUNK -> Color(0xFFE53935) to stringResource(R.string.sms_category_junk)
        MessageCategory.PROMOTION -> Color(0xFFFFA726) to stringResource(R.string.sms_category_promo)
        MessageCategory.TRANSACTION -> Color(0xFF66BB6A) to stringResource(R.string.sms_category_bank)
        MessageCategory.NOTIFICATION -> Color(0xFF42A5F5) to stringResource(R.string.sms_category_alert)
        MessageCategory.GENERAL -> Color(0xFF78909C) to stringResource(R.string.sms_category_gen)
        MessageCategory.ALLOWED -> Color(0xFF00BCD4) to stringResource(R.string.sms_category_allowed)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            fontSize = 9.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    conversation: SmsConversation,
    messages: List<SmsMessage>,
    showCategoryBadges: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDeleteConversation: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.sms_delete_conversation)) },
            text = { Text(stringResource(R.string.sms_delete_conversation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConversation()
                    }
                ) {
                    Text(stringResource(R.string.sms_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.sms_cancel))
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = conversation.displayName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (showCategoryBadges && conversation.category != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CategoryBadge(category = conversation.category)
                            }
                        }
                        if (conversation.displayName != conversation.address) {
                            Text(
                                text = conversation.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = DesignColors.Secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.sms_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.sms_more_options))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sms_delete_conversation_action)) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val groupedMessages = messages.groupBy { message ->
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(message.date)
                }
                
                groupedMessages.forEach { (dateStr, dayMessages) ->
                    item {
                        DateSeparator(dateStr)
                    }
                    
                    items(dayMessages) { message ->
                        MessageBubble(message = message, showCategoryBadge = showCategoryBadges)
                    }
                }
            }
            
            MessageInput(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun DateSeparator(dateStr: String) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
        Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
    )

    val displayDate = when (dateStr) {
        today -> stringResource(R.string.sms_today)
        yesterday -> stringResource(R.string.sms_yesterday)
        else -> {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date!!)
            } catch (e: Exception) {
                dateStr
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DesignColors.Surface
        ) {
            Text(
                text = displayDate,
                style = MaterialTheme.typography.labelSmall,
                color = DesignColors.Secondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MessageBubble(message: SmsMessage, showCategoryBadge: Boolean) {
    val isReceived = message.type == 1
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isReceived) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isReceived) 4.dp else 16.dp,
                bottomEnd = if (isReceived) 16.dp else 4.dp
            ),
            color = if (isReceived) DesignColors.Surface else DesignColors.Accent
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Category badge for received messages
                if (isReceived && showCategoryBadge && message.category != null) {
                    CategoryBadge(category = message.category)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isReceived) DesignColors.Primary else Color.White
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(message.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isReceived) 
                            DesignColors.Secondary 
                        else 
                            Color.White.copy(alpha = 0.7f)
                    )
                    
                    if (!isReceived) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = when (message.status) {
                                0 -> Icons.Default.DoneAll
                                32 -> Icons.Default.Schedule
                                64 -> Icons.Default.Error
                                else -> Icons.Default.Done
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = DesignColors.Background,
        shadowElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(stringResource(R.string.sms_message_hint)) },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignColors.Accent,
                        unfocusedBorderColor = DesignColors.Surface,
                        focusedContainerColor = DesignColors.Surface,
                        unfocusedContainerColor = DesignColors.Surface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FilledIconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = DesignColors.Accent,
                        contentColor = Color.White,
                        disabledContainerColor = DesignColors.Surface,
                        disabledContentColor = DesignColors.Secondary
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = stringResource(R.string.sms_send))
                }
            }
            
            // Note: Keyboard offset is handled globally in JunkboyApp
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMessageScreen(
    onBack: () -> Unit,
    onMessageSent: (String, String) -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showContactPicker by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var contactSearchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val serviceScope = remember { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
    
    // Contact permission launcher
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            serviceScope.launch {
                val loadedContacts = loadContacts(context.contentResolver)
                withContext(Dispatchers.Main) {
                    contacts = loadedContacts
                    showContactPicker = true
                }
            }
        } else {
            Toast.makeText(context, context.getString(R.string.sms_contact_permission_required), Toast.LENGTH_SHORT).show()
        }
    }
    
    // Filter contacts by search
    val filteredContacts = remember(contacts, contactSearchQuery) {
        if (contactSearchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(contactSearchQuery, ignoreCase = true) ||
                contact.phoneNumber.contains(contactSearchQuery)
            }
        }
    }
    
    if (showContactPicker) {
        AlertDialog(
            onDismissRequest = { showContactPicker = false },
            title = { Text(stringResource(R.string.sms_select_contact)) },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = contactSearchQuery,
                        onValueChange = { contactSearchQuery = it },
                        placeholder = { Text(stringResource(R.string.sms_search_contacts_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn {
                        items(filteredContacts) { contact ->
                            ListItem(
                                headlineContent = { Text(contact.name) },
                                supportingContent = { Text(contact.phoneNumber) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(DesignColors.Surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.firstOrNull()?.uppercase() ?: "?",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    recipient = contact.phoneNumber
                                    showContactPicker = false
                                    contactSearchQuery = ""
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactPicker = false }) {
                    Text(stringResource(R.string.sms_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sms_new_message_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.sms_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Recipient field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text(stringResource(R.string.sms_to_label)) },
                    placeholder = { Text(stringResource(R.string.sms_phone_number_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Contact picker button
                IconButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            serviceScope.launch {
                                val loadedContacts = loadContacts(context.contentResolver)
                                withContext(Dispatchers.Main) {
                                    contacts = loadedContacts
                                    showContactPicker = true
                                }
                            }
                        } else {
                            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Contacts,
                        contentDescription = stringResource(R.string.sms_select_contact_icon),
                        tint = DesignColors.Accent
                    )
                }
            }
            
            Divider()
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Message input
            MessageInput(
                value = message,
                onValueChange = { message = it },
                onSend = {
                    if (recipient.isNotBlank() && message.isNotBlank()) {
                        onMessageSent(recipient, message)
                    } else {
                        Toast.makeText(context, context.getString(R.string.sms_enter_recipient_message), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun NotDefaultSmsAppState(onMakeDefault: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Message,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = DesignColors.Accent
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.sms_set_default),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DesignColors.Primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.sms_set_default_message),
            style = MaterialTheme.typography.bodyMedium,
            color = DesignColors.Secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onMakeDefault,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = DesignColors.Accent
            )
        ) {
            Icon(Icons.Default.PhoneAndroid, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.sms_set_default))
        }
    }
}

@Composable
private fun EmptySmsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = DesignColors.Secondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.sms_no_messages),
            style = MaterialTheme.typography.titleLarge,
            color = DesignColors.Primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.sms_no_messages_message),
            style = MaterialTheme.typography.bodyMedium,
            color = DesignColors.Secondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoSearchResultsState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = DesignColors.Secondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.sms_no_results, query),
            style = MaterialTheme.typography.titleMedium,
            color = DesignColors.Primary
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = DesignColors.Accent)
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.sms_error_loading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = DesignColors.Secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text(stringResource(R.string.sms_retry))
        }
    }
}

// Helper functions
private suspend fun loadFilteredSmsSnapshot(database: AppDatabase): FilteredSmsSnapshot {
    val messages = database.filteredMessageDao().getAllMessagesOnce()
    return FilteredSmsSnapshot(
        messages = messages,
        categoryCache = SmsConversationFallbacks.latestCategoryBySender(messages)
    )
}

private suspend fun loadAllConversations(
    contentResolver: ContentResolver,
    categoryCache: Map<String, MessageCategory>,
    filteredMessages: List<FilteredMessage>,
    onResult: (List<SmsConversation>) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val conversationsMap = mutableMapOf<Long, SmsConversation>()
        
        // Query all SMS messages using thread_id for proper grouping
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ
        )
        
        val cursor = contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )
        
        cursor?.use { c ->
            val threadIdIndex = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIndex = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val readIndex = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            
            while (c.moveToNext()) {
                val threadId = c.getLong(threadIdIndex)
                val address = c.getString(addressIndex) ?: continue
                val body = c.getString(bodyIndex) ?: ""
                val date = c.getLong(dateIndex)
                val type = c.getInt(typeIndex)
                val read = c.getInt(readIndex) == 1
                
                // Get category from cache
                val category = SmsConversationFallbacks.resolveCategory(address, categoryCache)
                
                if (!conversationsMap.containsKey(threadId)) {
                    val displayName = getContactName(contentResolver, address)
                    conversationsMap[threadId] = SmsConversation(
                        threadId = threadId,
                        address = address,
                        displayName = displayName,
                        snippet = body,
                        messageCount = 1,
                        unreadCount = if (!read && type == 1) 1 else 0,
                        lastMessageDate = Date(date),
                        category = category
                    )
                } else {
                    val existing = conversationsMap[threadId]!!
                    conversationsMap[threadId] = existing.copy(
                        messageCount = existing.messageCount + 1,
                        unreadCount = existing.unreadCount + (if (!read && type == 1) 1 else 0)
                    )
                }
            }
        }
        
        val conversations = SmsConversationFallbacks.merge(
            systemConversations = conversationsMap.values.toList(),
            filteredMessages = filteredMessages
        )
            .sortedByDescending { it.lastMessageDate }
            .toList()
        
        Log.d("SmsScreen", "Loaded ${conversations.size} conversations")
        
        withContext(Dispatchers.Main) {
            onResult(conversations)
        }
    } catch (e: Exception) {
        Log.e("SmsScreen", "Error loading conversations", e)
        withContext(Dispatchers.Main) {
            onResult(emptyList())
        }
    }
}

private suspend fun loadConversationMessages(
    contentResolver: ContentResolver,
    threadId: Long,
    categoryCache: Map<String, MessageCategory>,
    onResult: (List<SmsMessage>) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val messages = mutableListOf<SmsMessage>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.STATUS
        )
        
        val cursor = contentResolver.query(
            uri,
            projection,
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} ASC"
        )
        
        cursor?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms._ID))
                val address = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                val body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val date = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val type = c.getInt(c.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val read = c.getInt(c.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                val status = c.getInt(c.getColumnIndexOrThrow(Telephony.Sms.STATUS))
                
                val category = if (type == 1) {
                    SmsConversationFallbacks.resolveCategory(address, categoryCache)
                } else {
                    null
                }
                
                messages.add(
                    SmsMessage(
                        id = id,
                        address = address,
                        body = body,
                        date = Date(date),
                        type = type,
                        read = read,
                        status = status,
                        category = category
                    )
                )
            }
        }
        
        withContext(Dispatchers.Main) {
            onResult(messages)
        }
    } catch (e: Exception) {
        Log.e("SmsScreen", "Error loading messages", e)
        withContext(Dispatchers.Main) {
            onResult(emptyList())
        }
    }
}

private suspend fun loadContacts(contentResolver: ContentResolver): List<Contact> = withContext(Dispatchers.IO) {
    val contacts = mutableListOf<Contact>()
    
    try {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        
        val cursor = contentResolver.query(
            uri,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        
        cursor?.use { c ->
            val seenNumbers = mutableSetOf<String>()
            
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID))
                val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: continue
                val number = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: continue
                val photoUri = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
                
                // Normalize number to avoid duplicates
                val normalizedNumber = number.replace(Regex("[^0-9+]"), "")
                if (normalizedNumber !in seenNumbers) {
                    seenNumbers.add(normalizedNumber)
                    contacts.add(Contact(id, name, number, photoUri))
                }
            }
        }
    } catch (e: Exception) {
        Log.e("SmsScreen", "Error loading contacts", e)
    }
    
    contacts
}

private fun markConversationAsRead(contentResolver: ContentResolver, threadId: Long) {
    try {
        val values = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
        }
        contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
            arrayOf(threadId.toString())
        )
    } catch (e: Exception) {
        Log.e("SmsScreen", "Error marking conversation as read", e)
    }
}

private fun deleteConversation(contentResolver: ContentResolver, threadId: Long) {
    try {
        contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString())
        )
    } catch (e: Exception) {
        Log.e("SmsScreen", "Error deleting conversation", e)
    }
}

private fun getContactName(contentResolver: ContentResolver, address: String): String {
    try {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
            Uri.encode(address)
        )
        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )
        
        cursor?.use { c ->
            if (c.moveToFirst()) {
                return c.getString(0) ?: address
            }
        }
    } catch (e: Exception) {
        Log.e("SmsScreen", "Error getting contact name", e)
    }
    
    return address
}

private suspend fun sendSmsWithDelivery(
    context: Context,
    recipient: String,
    message: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        
        val parts = smsManager.divideMessage(message)
        
        if (parts.size == 1) {
            smsManager.sendTextMessage(recipient, null, message, null, null)
        } else {
            smsManager.sendMultipartTextMessage(recipient, null, parts, null, null)
        }
        
        Log.d("SmsScreen", "SMS sent to $recipient")
        true
    } catch (e: Exception) {
        Log.e("SmsScreen", "Error sending SMS", e)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.toast_send_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
        false
    }
}

private fun saveSentMessage(
    contentResolver: ContentResolver,
    recipient: String,
    message: String
) {
    try {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, recipient)
            put(Telephony.Sms.BODY, message)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            put(Telephony.Sms.READ, 1)
        }
        
        contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        Log.d("SmsScreen", "Saved sent message to database")
    } catch (e: Exception) {
        Log.e("SmsScreen", "Error saving sent message", e)
    }
}

private fun formatConversationTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
    val messageDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    
    return when {
        today == messageDay -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        diff < 7 * 24 * 60 * 60 * 1000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}
