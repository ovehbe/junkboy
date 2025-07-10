package com.ovehbe.junkboy.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import com.ovehbe.junkboy.database.AppDatabase
import com.ovehbe.junkboy.database.FilteredMessage
import com.ovehbe.junkboy.database.MessageCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class MessageFilter {
    ALL, BLOCKED, CATEGORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val serviceScope = remember { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
    
    var selectedFilter by remember { mutableStateOf(MessageFilter.ALL) }
    var selectedCategory by remember { mutableStateOf<MessageCategory?>(null) }
    var messages by remember { mutableStateOf<List<FilteredMessage>>(emptyList()) }
    var blockedMessages by remember { mutableStateOf<List<FilteredMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(selectedFilter, selectedCategory) {
        try {
            isLoading = true
            error = null
            
            // Add a small delay to avoid rapid database queries during quick filter changes
            kotlinx.coroutines.delay(100)
            
            when (selectedFilter) {
                MessageFilter.ALL -> {
                    database.filteredMessageDao().getAllMessagesLimited(100).collect { newMessages ->
                        messages = newMessages
                        isLoading = false
                    }
                }
                MessageFilter.BLOCKED -> {
                    database.filteredMessageDao().getBlockedMessagesLimited(100).collect { newMessages ->
                        messages = newMessages
                        isLoading = false
                    }
                }
                MessageFilter.CATEGORY -> {
                    if (selectedCategory != null) {
                        database.filteredMessageDao().getMessagesByCategoryLimited(selectedCategory!!, 100).collect { newMessages ->
                            messages = newMessages
                            isLoading = false
                        }
                    } else {
                        messages = emptyList()
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
            .padding(16.dp)
    ) {
        Text(
            text = "Filtered Messages",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Category Filter Chips
        CategoryFilterChips(
            selectedFilter = selectedFilter,
            selectedCategory = selectedCategory,
            blockedCount = blockedMessages.size,
            onFilterSelected = { filter, category ->
                selectedFilter = filter
                selectedCategory = category
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content
        when {
            error != null -> {
                ErrorState(error = error!!) {
                    // Trigger reload by toggling the filter
                    selectedFilter = selectedFilter
                }
            }
            isLoading -> {
                LoadingState()
            }
            messages.isEmpty() -> {
                EmptyMessagesState(selectedFilter, selectedCategory)
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageCard(
                            message = message,
                            onAllowSender = { 
                                // Add sender to allowed list
                                serviceScope.launch {
                                    try {
                                        val allowedSender = com.ovehbe.junkboy.database.AllowedSender(
                                            phoneNumber = message.sender,
                                            displayName = null // Could be enhanced to get contact name
                                        )
                                        database.allowedSenderDao().insertAllowedSender(allowedSender)
                                        
                                        // Update message to mark as user override
                                        database.filteredMessageDao().applyUserOverride(message.id, false)
                                        
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterChips(
    selectedFilter: MessageFilter,
    selectedCategory: MessageCategory?,
    blockedCount: Int,
    onFilterSelected: (MessageFilter, MessageCategory?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row: Main filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == MessageFilter.ALL,
                onClick = { onFilterSelected(MessageFilter.ALL, null) },
                label = { Text("All Messages") },
                leadingIcon = {
                    Icon(Icons.Default.Message, contentDescription = null)
                }
            )
            
            FilterChip(
                selected = selectedFilter == MessageFilter.BLOCKED,
                onClick = { onFilterSelected(MessageFilter.BLOCKED, null) },
                label = { Text("Blocked ($blockedCount)") },
                leadingIcon = {
                    Icon(Icons.Default.Block, contentDescription = null)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.error
                )
            )
        }
        
        // Second row: Category filters with flow layout
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MessageCategory.values().forEach { category ->
                CategoryChip(
                    category = category,
                    isSelected = selectedFilter == MessageFilter.CATEGORY && selectedCategory == category,
                    onSelected = { onFilterSelected(MessageFilter.CATEGORY, category) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(
    category: MessageCategory,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val (icon, label) = when (category) {
        MessageCategory.GENERAL -> Icons.Default.Message to "General"
        MessageCategory.PROMOTION -> Icons.Default.LocalOffer to "Promo"
        MessageCategory.NOTIFICATION -> Icons.Default.Notifications to "Alerts"
        MessageCategory.TRANSACTION -> Icons.Default.AccountBalance to "Banking"
        MessageCategory.JUNK -> Icons.Default.Delete to "Junk"
    }
    
    FilterChip(
        selected = isSelected,
        onClick = onSelected,
        label = { Text(label) },
        leadingIcon = {
            Icon(icon, contentDescription = null)
        }
    )
}

@Composable
private fun MessageCard(
    message: FilteredMessage,
    onAllowSender: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    
    // Calculate container color
    val containerColor = if (message.isBlocked) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val formattedDate = remember(message.receivedAt) {
        dateFormat.format(message.receivedAt)
    }
    
    val confidenceText = remember(message.confidence) {
        "${(message.confidence * 100).toInt()}%"
    }
    
    val filterTypeText = remember(message.filterType) {
        message.filterType?.name?.lowercase()?.replace('_', ' ') ?: "unknown"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                CategoryBadge(
                    category = message.category,
                    isBlocked = message.isBlocked
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message content
            Text(
                text = message.messageBody,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Filter info
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Filtered by: $filterTypeText ($confidenceText)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Action buttons
            if (message.isBlocked && !message.isUserOverride) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAllowSender,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Allow Sender")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBadge(
    category: MessageCategory,
    isBlocked: Boolean
) {
    val (color, text, icon) = when {
        isBlocked -> Triple(
            MaterialTheme.colorScheme.error,
            "BLOCKED",
            Icons.Default.Block
        )
        category == MessageCategory.JUNK -> Triple(
            MaterialTheme.colorScheme.error,
            "JUNK",
            Icons.Default.Delete
        )
        category == MessageCategory.PROMOTION -> Triple(
            MaterialTheme.colorScheme.tertiary,
            "PROMO",
            Icons.Default.LocalOffer
        )
        category == MessageCategory.TRANSACTION -> Triple(
            MaterialTheme.colorScheme.primary,
            "TRANSACTION",
            Icons.Default.AccountBalance
        )
        category == MessageCategory.NOTIFICATION -> Triple(
            MaterialTheme.colorScheme.secondary,
            "NOTIFICATION",
            Icons.Default.Notifications
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline,
            "GENERAL",
            Icons.Default.Message
        )
    }
    
    Badge(
        containerColor = color.copy(alpha = 0.1f),
        contentColor = color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading messages...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error loading messages",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyMessagesState(selectedFilter: MessageFilter, selectedCategory: MessageCategory?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Message,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (selectedFilter) {
                MessageFilter.ALL -> "No filtered messages yet"
                MessageFilter.BLOCKED -> "No blocked messages yet"
                MessageFilter.CATEGORY -> if (selectedCategory != null) {
                    "No ${selectedCategory.name.lowercase()} messages yet"
                } else {
                    "No messages for this category"
                }
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Messages will appear here as they are filtered",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 