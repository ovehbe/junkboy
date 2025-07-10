package com.ovehbe.junkboy.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.SmsAppManager
import com.ovehbe.junkboy.utils.SmsDeleter
import com.ovehbe.junkboy.utils.CsvExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val database = remember { com.ovehbe.junkboy.database.AppDatabase.getDatabase(context) }
    val smsAppManager = remember { SmsAppManager(context) }
    val smsDeleter = remember { SmsDeleter(context) }
    val csvExporter = remember { CsvExporter(context) }
    val serviceScope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()) }
    
    var isMlEnabled by remember { mutableStateOf(true) }
    var isKeywordEnabled by remember { mutableStateOf(true) }
    var isRegexEnabled by remember { mutableStateOf(true) }
    var isUnderAttackMode by remember { mutableStateOf(false) }
    var notifyAllFiltered by remember { mutableStateOf(false) }
    var notifyBlockedMessages by remember { mutableStateOf(false) }
    var notifyCategorizedMessages by remember { mutableStateOf(false) }
    var autoDeleteJunk by remember { mutableStateOf(false) }
    var autoDeleteStatus by remember { mutableStateOf("") }
    
    // Individual category notification preferences
    var notifyGeneral by remember { mutableStateOf(true) }
    var notifyPromotion by remember { mutableStateOf(false) }
    var notifyNotification by remember { mutableStateOf(true) }
    var notifyTransaction by remember { mutableStateOf(true) }
    
    var customKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var customRegexPatterns by remember { mutableStateOf<List<String>>(emptyList()) }
    var allowedSenders by remember { mutableStateOf<List<com.ovehbe.junkboy.database.AllowedSender>>(emptyList()) }
    
    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showAddRegexDialog by remember { mutableStateOf(false) }
    var showAddAllowedSenderDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var isProcessingExistingMessages by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var processingResult by remember { mutableStateOf<String?>(null) }
    var exportResult by remember { mutableStateOf<String?>(null) }
    
    // Load preferences
    LaunchedEffect(Unit) {
        isMlEnabled = preferencesManager.isMlFilteringEnabled()
        isKeywordEnabled = preferencesManager.isKeywordFilteringEnabled()
        isRegexEnabled = preferencesManager.isRegexFilteringEnabled()
        isUnderAttackMode = preferencesManager.isUnderAttackMode()
        notifyAllFiltered = preferencesManager.shouldNotifyAllFiltered()
        notifyBlockedMessages = preferencesManager.shouldNotifyBlockedMessages()
        notifyCategorizedMessages = preferencesManager.shouldNotifyCategorizedMessages()
        autoDeleteJunk = preferencesManager.isAutoDeleteJunkEnabled()
        autoDeleteStatus = smsDeleter.getAutoDeleteStatus()
        
        // Load individual category notification preferences
        notifyGeneral = preferencesManager.shouldNotifyGeneral()
        notifyPromotion = preferencesManager.shouldNotifyPromotion()
        notifyNotification = preferencesManager.shouldNotifyNotification()
        notifyTransaction = preferencesManager.shouldNotifyTransaction()
        
        customKeywords = preferencesManager.getCustomKeywords()
        customRegexPatterns = preferencesManager.getCustomRegexPatterns()
        
        // Load allowed senders
        database.allowedSenderDao().getAllowedSenders().collect { senders ->
            allowedSenders = senders
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            // Filter Methods Section
            SettingsSection(title = "Filter Methods") {
                SettingsToggleItem(
                    title = "AI Classification",
                    description = "Use machine learning to classify messages",
                    icon = Icons.Default.Psychology,
                    checked = isMlEnabled,
                    onCheckedChange = { 
                        isMlEnabled = it
                        preferencesManager.setMlFilteringEnabled(it)
                    }
                )
                
                SettingsToggleItem(
                    title = "Keyword Filtering",
                    description = "Filter based on specific words and phrases",
                    icon = Icons.Default.FilterList,
                    checked = isKeywordEnabled,
                    onCheckedChange = { 
                        isKeywordEnabled = it
                        preferencesManager.setKeywordFilteringEnabled(it)
                    }
                )
                
                SettingsToggleItem(
                    title = "Regex Filtering",
                    description = "Advanced pattern-based filtering",
                    icon = Icons.Default.Code,
                    checked = isRegexEnabled,
                    onCheckedChange = { 
                        isRegexEnabled = it
                        preferencesManager.setRegexFilteringEnabled(it)
                    }
                )
            }
        }
        
        item {
            // Attack Mode Section
            SettingsSection(title = "Protection Level") {
                SettingsToggleItem(
                    title = "Under Attack Mode",
                    description = "More aggressive filtering for spam waves",
                    icon = Icons.Default.Shield,
                    checked = isUnderAttackMode,
                    onCheckedChange = { 
                        isUnderAttackMode = it
                        preferencesManager.setUnderAttackMode(it)
                    }
                )
            }
        }
        
        item {
            // Notifications Section
            SettingsSection(title = "Notifications") {
                SettingsToggleItem(
                    title = "Notify All Filtered",
                    description = "Show notifications for all categorized messages",
                    icon = Icons.Default.Notifications,
                    checked = notifyAllFiltered,
                    onCheckedChange = { 
                        notifyAllFiltered = it
                        preferencesManager.setNotifyAllFiltered(it)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingsToggleItem(
                    title = "Notify Blocked Messages",
                    description = "Show notifications for blocked/spam messages",
                    icon = Icons.Default.Block,
                    checked = notifyBlockedMessages,
                    onCheckedChange = { 
                        notifyBlockedMessages = it
                        preferencesManager.setNotifyBlockedMessages(it)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingsToggleItem(
                    title = "Notify Categorized Messages",
                    description = "Show notifications for categorized (non-blocked) messages",
                    icon = Icons.Default.Category,
                    checked = notifyCategorizedMessages,
                    onCheckedChange = { 
                        notifyCategorizedMessages = it
                        preferencesManager.setNotifyCategorizedMessages(it)
                    }
                )
                
                // Show category-specific notification options when categorized notifications are enabled
                if (notifyCategorizedMessages) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Category notification submenu
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Notification Categories",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            SettingsToggleItem(
                                title = "General Messages",
                                description = "Personal and other SMS messages",
                                icon = Icons.Default.Message,
                                checked = notifyGeneral,
                                onCheckedChange = { 
                                    notifyGeneral = it
                                    preferencesManager.setNotifyGeneral(it)
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            SettingsToggleItem(
                                title = "Promotions",
                                description = "Marketing and promotional messages",
                                icon = Icons.Default.LocalOffer,
                                checked = notifyPromotion,
                                onCheckedChange = { 
                                    notifyPromotion = it
                                    preferencesManager.setNotifyPromotion(it)
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            SettingsToggleItem(
                                title = "Notifications",
                                description = "System and app notifications via SMS",
                                icon = Icons.Default.Notifications,
                                checked = notifyNotification,
                                onCheckedChange = { 
                                    notifyNotification = it
                                    preferencesManager.setNotifyNotification(it)
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            SettingsToggleItem(
                                title = "Transactions",
                                description = "Banking and payment messages",
                                icon = Icons.Default.AccountBalance,
                                checked = notifyTransaction,
                                onCheckedChange = { 
                                    notifyTransaction = it
                                    preferencesManager.setNotifyTransaction(it)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        item {
            // Auto-Delete Section
            SettingsSection(title = "Auto-Delete Junk") {
                SettingsToggleItem(
                    title = "Auto-Delete Junk Messages",
                    description = if (smsDeleter.canEnableAutoDelete()) {
                        "Automatically delete junk messages from system SMS database"
                    } else {
                        "Requires Junkboy as default SMS app"
                    },
                    icon = Icons.Default.DeleteForever,
                    checked = autoDeleteJunk,
                    onCheckedChange = { enabled ->
                        if (enabled && !smsDeleter.canEnableAutoDelete()) {
                            // Prompt to become default SMS app
                            smsAppManager.requestDefaultSmsApp()
                        } else {
                            autoDeleteJunk = enabled
                            preferencesManager.setAutoDeleteJunk(enabled)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = autoDeleteStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (smsDeleter.canEnableAutoDelete()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                if (!smsDeleter.canEnableAutoDelete()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { smsAppManager.requestDefaultSmsApp() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Make Junkboy Default SMS App")
                    }
                }
            }
        }
        
        item {
            // Debug Section for troubleshooting
            SettingsSection(title = "Debug - Default SMS App") {
                    var debugOutput by remember { mutableStateOf("") }
                    
                    OutlinedButton(
                        onClick = { 
                            val requirements = smsAppManager.checkDefaultSmsAppRequirements()
                            debugOutput = "Requirements Check:\n" + requirements.map { (req, met) ->
                                "${if (met) "✓" else "✗"} $req"
                            }.joinToString("\n")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check SMS App Requirements")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { 
                            val success = smsAppManager.requestDefaultSmsApp()
                            debugOutput += "\n\nRequest Default SMS App: ${if (success) "Started" else "Failed"}"
                            
                            // If the automatic method doesn't work, show manual instructions
                            if (success) {
                                debugOutput += "\n\nIf dialog doesn't appear, try manual method below:"
                                debugOutput += "\n" + smsAppManager.getManualDefaultSmsInstructions()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Default SMS App Request")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { 
                            debugOutput = "Manual Instructions:\n" + smsAppManager.getManualDefaultSmsInstructions()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Show Manual Instructions")
                    }
                    
                    if (debugOutput.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = debugOutput,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        
        item {
            // Custom Keywords Section
            SettingsSection(title = "Custom Keywords") {
                CustomListSection(
                    items = customKeywords,
                    emptyText = "No custom keywords added",
                    onAddClick = { showAddKeywordDialog = true },
                    onRemoveItem = { keyword ->
                        preferencesManager.removeCustomKeyword(keyword)
                        customKeywords = preferencesManager.getCustomKeywords()
                    }
                )
            }
        }
        
        item {
            // Custom Regex Section
            SettingsSection(title = "Custom Regex Patterns") {
                CustomListSection(
                    items = customRegexPatterns,
                    emptyText = "No custom regex patterns added",
                    onAddClick = { showAddRegexDialog = true },
                    onRemoveItem = { pattern ->
                        preferencesManager.removeCustomRegexPattern(pattern)
                        customRegexPatterns = preferencesManager.getCustomRegexPatterns()
                    }
                )
            }
        }
        
        item {
            // Allowed Senders Section
            SettingsSection(title = "Allowed Senders") {
                AllowedSendersSection(
                    allowedSenders = allowedSenders,
                    onAddClick = { showAddAllowedSenderDialog = true },
                    onRemoveItem = { sender ->
                        serviceScope.launch {
                            database.allowedSenderDao().removeAllowedSender(sender.phoneNumber)
                        }
                    }
                )
            }
        }
        
        item {
            // Data Management Section
            SettingsSection(title = "Data Management") {
                OutlinedButton(
                    onClick = { 
                        serviceScope.launch {
                            if (!isProcessingExistingMessages) {
                                isProcessingExistingMessages = true
                                processingResult = null
                                
                                try {
                                    val processor = com.ovehbe.junkboy.utils.ExistingMessagesProcessor(context)
                                    val result = processor.processAllExistingMessages()
                                    
                                    if (result.isSuccess) {
                                        val count = result.getOrNull() ?: 0
                                        processingResult = "Successfully processed $count existing messages"
                                    } else {
                                        processingResult = "Failed to process existing messages: ${result.exceptionOrNull()?.message}"
                                    }
                                } catch (e: Exception) {
                                    processingResult = "Error: ${e.message}"
                                } finally {
                                    isProcessingExistingMessages = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessingExistingMessages
                ) {
                    if (isProcessingExistingMessages) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(Icons.Default.FilterAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Filter All Existing Messages")
                    }
                }
                
                // Show processing result if available
                processingResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.startsWith("Successfully")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { 
                        serviceScope.launch {
                            if (!isExporting) {
                                isExporting = true
                                exportResult = null
                                
                                try {
                                    // Get all messages from database
                                    database.filteredMessageDao().getAllMessages().collect { messages ->
                                        if (messages.isNotEmpty()) {
                                            val result = csvExporter.exportAndShare(messages)
                                            if (result.isSuccess) {
                                                exportResult = "Successfully exported ${messages.size} messages"
                                            } else {
                                                exportResult = "Export failed: ${result.exceptionOrNull()?.message}"
                                            }
                                        } else {
                                            exportResult = "No messages to export"
                                        }
                                        isExporting = false
                                    }
                                } catch (e: Exception) {
                                    exportResult = "Export error: ${e.message}"
                                    isExporting = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...")
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Filtered Messages")
                    }
                }
                
                // Show export result if available
                exportResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.startsWith("Successfully")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { 
                        showClearDataDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Data")
                }
            }
        }
    }
    
    // Add Keyword Dialog
    if (showAddKeywordDialog) {
        AddTextDialog(
            title = "Add Custom Keyword",
            placeholder = "Enter keyword or phrase",
            onConfirm = { keyword ->
                preferencesManager.addCustomKeyword(keyword)
                customKeywords = preferencesManager.getCustomKeywords()
                showAddKeywordDialog = false
            },
            onDismiss = { showAddKeywordDialog = false }
        )
    }
    
    // Add Regex Dialog
    if (showAddRegexDialog) {
        AddTextDialog(
            title = "Add Regex Pattern",
            placeholder = "Enter regex pattern",
            onConfirm = { pattern ->
                preferencesManager.addCustomRegexPattern(pattern)
                customRegexPatterns = preferencesManager.getCustomRegexPatterns()
                showAddRegexDialog = false
            },
            onDismiss = { showAddRegexDialog = false }
        )
    }
    
    // Add Allowed Sender Dialog
    if (showAddAllowedSenderDialog) {
        AddTextDialog(
            title = "Add Allowed Sender",
            placeholder = "Enter phone number",
            onConfirm = { phoneNumber ->
                serviceScope.launch {
                    val allowedSender = com.ovehbe.junkboy.database.AllowedSender(
                        phoneNumber = phoneNumber,
                        displayName = null
                    )
                    database.allowedSenderDao().insertAllowedSender(allowedSender)
                }
                showAddAllowedSenderDialog = false
            },
            onDismiss = { showAddAllowedSenderDialog = false }
        )
    }
    
    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data") },
            text = { 
                Text("Are you sure you want to clear all data? This will delete:\n\n• All filtered messages\n• Custom keywords and regex patterns\n• Allowed senders\n• Statistics and preferences\n\nThis action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        serviceScope.launch {
                            try {
                                // Clear preferences
                                preferencesManager.clearAllData()
                                
                                // Clear database tables
                                database.clearAllTables()
                                
                                // Reset UI state
                                customKeywords = emptyList()
                                customRegexPatterns = emptyList()
                                processingResult = "All data cleared successfully"
                                exportResult = null
                                
                            } catch (e: Exception) {
                                processingResult = "Error clearing data: ${e.message}"
                            }
                        }
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun CustomListSection(
    items: List<String>,
    emptyText: String,
    onAddClick: () -> Unit,
    onRemoveItem: (String) -> Unit
) {
    Column {
        if (items.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onRemoveItem(item) }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (item != items.last()) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add")
        }
    }
}

@Composable
private fun AllowedSendersSection(
    allowedSenders: List<com.ovehbe.junkboy.database.AllowedSender>,
    onAddClick: () -> Unit,
    onRemoveItem: (com.ovehbe.junkboy.database.AllowedSender) -> Unit
) {
    Column {
        if (allowedSenders.isEmpty()) {
            Text(
                text = "No allowed senders added",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            allowedSenders.forEach { sender ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sender.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (sender.displayName != null) {
                            Text(
                                text = sender.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { onRemoveItem(sender) }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (sender != allowedSenders.last()) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Allowed Sender")
        }
    }
}

@Composable
private fun AddTextDialog(
    title: String,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 