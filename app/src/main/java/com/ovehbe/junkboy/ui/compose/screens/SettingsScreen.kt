package com.ovehbe.junkboy.ui.compose.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovehbe.junkboy.ui.theme.*
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.SmsAppManager
import com.ovehbe.junkboy.utils.SmsDeleter
import com.ovehbe.junkboy.utils.CsvExporter
import com.ovehbe.junkboy.service.NotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

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
    var otpAutoCopy by remember { mutableStateOf(true) }
    var autoReplyStop by remember { mutableStateOf(false) }
    var autoReplyRequireSpam by remember { mutableStateOf(true) }
    val autoReplyLog by remember { database.autoReplyLogDao().getRecent() }.collectAsState(initial = emptyList())
    val stopPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        // READ_CONTACTS is required so known contacts are never auto-replied to
        val ok = granted[Manifest.permission.SEND_SMS] == true && granted[Manifest.permission.READ_CONTACTS] == true
        autoReplyStop = ok
        preferencesManager.setAutoReplyStopEnabled(ok)
    }
    var hubNotifications by remember { mutableStateOf(true) }
    var enabledHubApps by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Hub Settings
    var hubEnabled by remember { mutableStateOf(false) }
    var hubDisplayMode by remember { mutableStateOf("sms_only") }
    var hubDefaultView by remember { mutableStateOf("conversations") }
    
    // SMS App Notification Control
    var smsAppControlEnabled by remember { mutableStateOf(false) }
    var dismissSmsAppNotifications by remember { mutableStateOf(false) }
    var dismissBlockedOnly by remember { mutableStateOf(true) }
    
    // SMS Screen Settings
    var hideJunkInSms by remember { mutableStateOf(false) }
    var showCategoryBadges by remember { mutableStateOf(true) }
    var smsDisplayCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Accessibility Settings
    var keyboardOffset by remember { mutableFloatStateOf(0f) }
    
    // Test auto-delete state
    var isTestingAutoDelete by remember { mutableStateOf(false) }
    var autoDeleteTestResult by remember { mutableStateOf<String?>(null) }
    
    // Individual category notification preferences
    var notifyGeneral by remember { mutableStateOf(true) }
    var notifyPromotion by remember { mutableStateOf(false) }
    var notifyNotification by remember { mutableStateOf(true) }
    var notifyTransaction by remember { mutableStateOf(true) }
    var notifyAllowed by remember { mutableStateOf(true) }
    
    var customKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var customRegexPatterns by remember { mutableStateOf<List<String>>(emptyList()) }
    var allowRules by remember { mutableStateOf<List<com.ovehbe.junkboy.filters.AllowRule>>(emptyList()) }
    var showAddAllowRuleDialog by remember { mutableStateOf(false) }
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
        otpAutoCopy = preferencesManager.isOtpAutoCopyEnabled()
        autoReplyStop = preferencesManager.isAutoReplyStopEnabled()
        autoReplyRequireSpam = preferencesManager.isAutoReplyRequireSpam()
        hubNotifications = preferencesManager.isHubNotificationsEnabled()
        enabledHubApps = preferencesManager.getEnabledHubApps()
        
        // Load Hub settings
        hubEnabled = preferencesManager.isHubEnabled()
        hubDisplayMode = preferencesManager.getHubDisplayMode()
        hubDefaultView = preferencesManager.getHubDefaultView()
        
        // Load individual category notification preferences
        notifyGeneral = preferencesManager.shouldNotifyGeneral()
        notifyPromotion = preferencesManager.shouldNotifyPromotion()
        notifyNotification = preferencesManager.shouldNotifyNotification()
        notifyTransaction = preferencesManager.shouldNotifyTransaction()
        notifyAllowed = preferencesManager.shouldNotifyAllowed()
        
        // Load SMS app notification control preferences
        smsAppControlEnabled = preferencesManager.isSmsAppControlEnabled()
        dismissSmsAppNotifications = preferencesManager.shouldDismissSmsAppNotifications()
        dismissBlockedOnly = preferencesManager.shouldDismissBlockedOnly()
        
        // Load SMS screen settings
        hideJunkInSms = preferencesManager.shouldHideJunkInSms()
        showCategoryBadges = preferencesManager.shouldShowCategoryBadges()
        smsDisplayCategories = preferencesManager.getSmsDisplayCategories()
        
        // Load accessibility settings
        keyboardOffset = preferencesManager.getKeyboardOffset().toFloat()
        
        customKeywords = preferencesManager.getCustomKeywords()
        customRegexPatterns = preferencesManager.getCustomRegexPatterns()
        allowRules = preferencesManager.getAllowRules()
        
        // Load allowed senders
        database.allowedSenderDao().getAllowedSenders().collect { senders ->
            allowedSenders = senders
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignLayout.ContainerPadding),
        verticalArrangement = Arrangement.spacedBy(DesignSpacing.MD)
    ) {
        item {
            // Header
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = DesignColors.Primary
            )
        }
        
        // Hub Settings Section
        item {
            SettingsSection(
                title = "Hub Settings",
                icon = Icons.Default.Inbox
            ) {
                Text(
                    text = "The Hub provides a unified inbox view for all your messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary,
                    modifier = Modifier.padding(bottom = DesignSpacing.SM)
                )
                
                ToggleSettingItem(
                    title = "Enable Hub",
                    subtitle = "Show Hub in bottom navigation (requires app restart)",
                    checked = hubEnabled,
                    onCheckedChange = { enabled ->
                        hubEnabled = enabled
                        preferencesManager.setHubEnabled(enabled)
                    }
                )
                
                if (hubEnabled) {
                    // Display mode dropdown
                    var displayModeExpanded by remember { mutableStateOf(false) }
                    val displayModeOptions = listOf(
                        "all" to "All (SMS + Chats)",
                        "sms_only" to "SMS Only",
                        "chats_only" to "Chats Only"
                    )
                    val currentDisplayLabel = displayModeOptions.find { it.first == hubDisplayMode }?.second ?: "SMS Only"
                    
                    DropdownSettingItem(
                        title = "Display Mode",
                        subtitle = "What to show in the Hub",
                        currentValue = currentDisplayLabel,
                        expanded = displayModeExpanded,
                        onExpandedChange = { displayModeExpanded = it },
                        options = displayModeOptions.map { it.second },
                        onOptionSelected = { label ->
                            val mode = displayModeOptions.find { it.second == label }?.first ?: "sms_only"
                            hubDisplayMode = mode
                            preferencesManager.setHubDisplayMode(mode)
                            displayModeExpanded = false
                        }
                    )
                    
                    // Default view dropdown
                    var defaultViewExpanded by remember { mutableStateOf(false) }
                    val defaultViewOptions = listOf(
                        "conversations" to "Conversations",
                        "messages" to "Individual Messages"
                    )
                    val currentViewLabel = defaultViewOptions.find { it.first == hubDefaultView }?.second ?: "Conversations"
                    
                    DropdownSettingItem(
                        title = "Default View",
                        subtitle = "How messages are displayed",
                        currentValue = currentViewLabel,
                        expanded = defaultViewExpanded,
                        onExpandedChange = { defaultViewExpanded = it },
                        options = defaultViewOptions.map { it.second },
                        onOptionSelected = { label ->
                            val view = defaultViewOptions.find { it.second == label }?.first ?: "conversations"
                            hubDefaultView = view
                            preferencesManager.setHubDefaultView(view)
                            defaultViewExpanded = false
                        }
                    )
                }
            }
        }
        
        // SMS Screen Settings Section (only when Junkboy is default SMS app)
        item {
            SettingsSection(
                title = "SMS App Settings",
                icon = Icons.Default.Message
            ) {
                Text(
                    text = "Settings for when Junkboy is set as default SMS app",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary,
                    modifier = Modifier.padding(bottom = DesignSpacing.SM)
                )
                
                ToggleSettingItem(
                    title = "Hide Junk Messages",
                    subtitle = "Hide blocked/junk messages from the SMS inbox",
                    checked = hideJunkInSms,
                    onCheckedChange = { enabled ->
                        hideJunkInSms = enabled
                        preferencesManager.setHideJunkInSms(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Show Category Badges",
                    subtitle = "Display Junkboy category on messages",
                    checked = showCategoryBadges,
                    onCheckedChange = { enabled ->
                        showCategoryBadges = enabled
                        preferencesManager.setShowCategoryBadges(enabled)
                    }
                )
                
                // SMS Display Categories Selection
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Display Categories",
                        style = MaterialTheme.typography.titleSmall,
                        color = DesignColors.Primary
                    )
                    Text(
                        text = "Choose which message categories to show in the SMS screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignColors.Secondary,
                        modifier = Modifier.padding(bottom = DesignSpacing.SM)
                    )
                    
                    // Category toggles
                    val allCategories = listOf(
                        com.ovehbe.junkboy.database.MessageCategory.GENERAL to "General",
                        com.ovehbe.junkboy.database.MessageCategory.PROMOTION to "Promotion",
                        com.ovehbe.junkboy.database.MessageCategory.NOTIFICATION to "Notification",
                        com.ovehbe.junkboy.database.MessageCategory.TRANSACTION to "Transaction",
                        com.ovehbe.junkboy.database.MessageCategory.JUNK to "Junk",
                        com.ovehbe.junkboy.database.MessageCategory.ALLOWED to "Allowed"
                    )
                    
                    allCategories.forEach { (category, displayName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            getCategoryDisplayColor(category),
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DesignColors.Primary
                                )
                            }
                            Switch(
                                checked = smsDisplayCategories.contains(category.name),
                                onCheckedChange = { checked ->
                                    val newCategories = if (checked) {
                                        smsDisplayCategories + category.name
                                    } else {
                                        smsDisplayCategories - category.name
                                    }
                                    smsDisplayCategories = newCategories
                                    preferencesManager.setSmsDisplayCategories(newCategories)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DesignColors.Primary,
                                    checkedTrackColor = DesignColors.ActiveBackground
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(DesignSpacing.SM))
                
                ToggleSettingItem(
                    title = "Auto-Delete Junk",
                    subtitle = autoDeleteStatus,
                    checked = autoDeleteJunk,
                    onCheckedChange = { enabled ->
                        autoDeleteJunk = enabled
                        preferencesManager.setAutoDeleteJunk(enabled)
                    }
                )
                
                // Test Auto-Delete Button
                if (autoDeleteJunk) {
                    ActionSettingItem(
                        title = "Test Auto-Delete",
                        subtitle = autoDeleteTestResult ?: "Simulate deleting a junk message",
                        icon = Icons.Default.Science,
                        onClick = {
                            isTestingAutoDelete = true
                            autoDeleteTestResult = "Testing..."
                            serviceScope.launch {
                                try {
                                    val canDelete = smsDeleter.canEnableAutoDelete()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        autoDeleteTestResult = if (canDelete) {
                                            "✅ Auto-delete is ready! Junk messages will be removed."
                                        } else {
                                            "❌ Cannot delete: Junkboy is not the default SMS app"
                                        }
                                        isTestingAutoDelete = false
                                    }
                                } catch (e: Exception) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        autoDeleteTestResult = "❌ Error: ${e.message}"
                                        isTestingAutoDelete = false
                                    }
                                }
                            }
                        },
                        isLoading = isTestingAutoDelete
                    )
                }
            }
        }
        
        // Accessibility Settings Section
        item {
            SettingsSection(
                title = "Accessibility",
                icon = Icons.Default.Accessibility
            ) {
                Text(
                    text = "Adjust settings for better compatibility with physical keyboards",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary,
                    modifier = Modifier.padding(bottom = DesignSpacing.SM)
                )
                
                // Keyboard offset slider
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Keyboard Bottom Offset",
                                style = MaterialTheme.typography.titleSmall,
                                color = DesignColors.Primary
                            )
                            Text(
                                text = "Extra space when keyboard is shown (for devices like BlackBerry Key2)",
                                style = MaterialTheme.typography.bodySmall,
                                color = DesignColors.Secondary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(DesignSpacing.SM))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesignColors.Secondary
                        )
                        
                        Slider(
                            value = keyboardOffset,
                            onValueChange = { 
                                keyboardOffset = it
                                preferencesManager.setKeyboardOffset(it.toInt())
                            },
                            valueRange = 0f..200f,
                            steps = 19, // 0, 10, 20, ... 200
                            modifier = Modifier.weight(1f).padding(horizontal = DesignSpacing.SM),
                            colors = SliderDefaults.colors(
                                thumbColor = DesignColors.Accent,
                                activeTrackColor = DesignColors.Accent
                            )
                        )
                        
                        Text(
                            text = "${keyboardOffset.toInt()}dp",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesignColors.Secondary,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                }
            }
        }
        
        // Filter Methods Section
        item {
            SettingsSection(
                title = "Filter Methods",
                icon = Icons.Default.FilterList
            ) {
                ToggleSettingItem(
                    title = "AI Classification",
                    subtitle = "Use machine learning to categorize messages",
                    checked = isMlEnabled,
                    onCheckedChange = { enabled ->
                        isMlEnabled = enabled
                        preferencesManager.setMlFilteringEnabled(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Keyword Filtering",
                    subtitle = "Filter messages based on keywords",
                    checked = isKeywordEnabled,
                    onCheckedChange = { enabled ->
                        isKeywordEnabled = enabled
                        preferencesManager.setKeywordFilteringEnabled(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Regex Filtering",
                    subtitle = "Advanced pattern-based filtering",
                    checked = isRegexEnabled,
                    onCheckedChange = { enabled ->
                        isRegexEnabled = enabled
                        preferencesManager.setRegexFilteringEnabled(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Under Attack Mode",
                    subtitle = "Enhanced protection during spam waves",
                    checked = isUnderAttackMode,
                    onCheckedChange = { enabled ->
                        isUnderAttackMode = enabled
                        preferencesManager.setUnderAttackMode(enabled)
                    }
                )
            }
        }
        
        // Notification Settings Section
        item {
            SettingsSection(
                title = "Notification Settings",
                icon = Icons.Default.Notifications
            ) {
                ToggleSettingItem(
                    title = "General Messages",
                    subtitle = "Show notifications for general messages",
                    checked = notifyGeneral,
                    onCheckedChange = { enabled ->
                        notifyGeneral = enabled
                        preferencesManager.setNotifyGeneral(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Notification Messages",
                    subtitle = "Show notifications for system alerts",
                    checked = notifyNotification,
                    onCheckedChange = { enabled ->
                        notifyNotification = enabled
                        preferencesManager.setNotifyNotification(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Transaction Messages",
                    subtitle = "Show notifications for banking/payment messages",
                    checked = notifyTransaction,
                    onCheckedChange = { enabled ->
                        notifyTransaction = enabled
                        preferencesManager.setNotifyTransaction(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Promotion Messages",
                    subtitle = "Show notifications for promotional messages",
                    checked = notifyPromotion,
                    onCheckedChange = { enabled ->
                        notifyPromotion = enabled
                        preferencesManager.setNotifyPromotion(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Allowed Senders",
                    subtitle = "Show notifications for allowed senders",
                    checked = notifyAllowed,
                    onCheckedChange = { enabled ->
                        notifyAllowed = enabled
                        preferencesManager.setNotifyAllowed(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Blocked Messages",
                    subtitle = "Show notifications for blocked messages",
                    checked = notifyBlockedMessages,
                    onCheckedChange = { enabled ->
                        notifyBlockedMessages = enabled
                        preferencesManager.setNotifyBlockedMessages(enabled)
                    }
                )
            }
        }
        
        // Features Section
        item {
            SettingsSection(
                title = "Features",
                icon = Icons.Default.Settings
            ) {
                ToggleSettingItem(
                    title = "OTP Auto-Copy",
                    subtitle = "Automatically copy OTP codes to clipboard",
                    checked = otpAutoCopy,
                    onCheckedChange = { enabled ->
                        otpAutoCopy = enabled
                        preferencesManager.setOtpAutoCopyEnabled(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Hub Notifications",
                    subtitle = "Show notifications for chat messages",
                    checked = hubNotifications,
                    onCheckedChange = { enabled ->
                        hubNotifications = enabled
                        preferencesManager.setHubNotificationsEnabled(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = "Auto-Delete Junk",
                    subtitle = "Automatically delete junk messages",
                    checked = autoDeleteJunk,
                    onCheckedChange = { enabled ->
                        autoDeleteJunk = enabled
                        preferencesManager.setAutoDeleteJunk(enabled)
                        autoDeleteStatus = smsDeleter.getAutoDeleteStatus()
                    }
                )
                
                if (autoDeleteStatus.isNotEmpty()) {
                    Text(
                        text = "Status: $autoDeleteStatus",
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignColors.Secondary,
                        modifier = Modifier.padding(horizontal = DesignSpacing.MD)
                    )
                }
            }
        }
        
        // Stop2End Auto-Responder Section
        item {
            SettingsSection(
                title = "Stop2End Auto-Responder",
                icon = Icons.Default.Send
            ) {
                ToggleSettingItem(
                    title = "Auto-reply STOP to Marketing SMS",
                    subtitle = "Reply \"STOP\" to promotional messages that ask you to",
                    checked = autoReplyStop,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            autoReplyStop = false
                            preferencesManager.setAutoReplyStopEnabled(false)
                        } else {
                            val missing = listOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS).filter {
                                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                            }
                            if (missing.isEmpty()) {
                                autoReplyStop = true
                                preferencesManager.setAutoReplyStopEnabled(true)
                            } else {
                                stopPermissionLauncher.launch(missing.toTypedArray())
                            }
                        }
                    }
                )
                
                ToggleSettingItem(
                    title = "Also require spam classification",
                    subtitle = "Recommended. Pharmacies, banks and clinics also add opt-out text to real messages",
                    checked = autoReplyRequireSpam,
                    onCheckedChange = { enabled ->
                        autoReplyRequireSpam = enabled
                        preferencesManager.setAutoReplyRequireSpam(enabled)
                    }
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DesignColors.InputBackground)
                ) {
                    Text(
                        text = "Enabling this sends an outbound SMS from your carrier plan, which may incur charges. " +
                            "Replies are only sent to messages that end with an explicit \"Reply STOP\" (or similar) " +
                            "directive, from numbers that are not in your contacts and that you have never texted. " +
                            "At most one reply per number every 24 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignColors.Secondary,
                        modifier = Modifier.padding(DesignSpacing.SM)
                    )
                }
                
                OpenRouterJudgeItem(preferencesManager)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-reply log",
                        style = MaterialTheme.typography.titleSmall,
                        color = DesignColors.Primary
                    )
                    if (autoReplyLog.isNotEmpty()) {
                        TextButton(onClick = { serviceScope.launch { database.autoReplyLogDao().clear() } }) {
                            Text("Clear")
                        }
                    }
                }
                
                if (autoReplyLog.isEmpty()) {
                    Text(
                        text = "No auto-replies sent yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignColors.Secondary
                    )
                } else {
                    autoReplyLog.take(20).forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = entry.senderNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DesignColors.Primary
                                )
                                if (entry.reason.isNotEmpty()) {
                                    Text(
                                        text = entry.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DesignColors.Secondary
                                    )
                                }
                            }
                            Text(
                                text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                    .format(Date(entry.repliedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = DesignColors.Secondary
                            )
                        }
                    }
                }
            }
        }
        
        // SMS App Notification Control Section (Buzzkill-like feature)
        item {
            SettingsSection(
                title = "SMS App Control",
                icon = Icons.Default.NotificationsOff
            ) {
                Text(
                    text = "Control notifications from the default SMS app (${smsAppManager.getDefaultSmsAppName()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary,
                    modifier = Modifier.padding(bottom = DesignSpacing.SM)
                )
                
                ToggleSettingItem(
                    title = "Enable SMS App Control",
                    subtitle = "Allow Junkboy to manage SMS app notifications",
                    checked = smsAppControlEnabled,
                    onCheckedChange = { enabled ->
                        smsAppControlEnabled = enabled
                        preferencesManager.setSmsAppControlEnabled(enabled)
                    }
                )
                
                if (smsAppControlEnabled) {
                    ToggleSettingItem(
                        title = "Dismiss SMS Notifications",
                        subtitle = "Automatically dismiss notifications from SMS app",
                        checked = dismissSmsAppNotifications,
                        onCheckedChange = { enabled ->
                            dismissSmsAppNotifications = enabled
                            preferencesManager.setDismissSmsAppNotifications(enabled)
                        }
                    )
                    
                    if (dismissSmsAppNotifications) {
                        ToggleSettingItem(
                            title = "Blocked Messages Only",
                            subtitle = "Only dismiss notifications for blocked/junk messages",
                            checked = dismissBlockedOnly,
                            onCheckedChange = { enabled ->
                                dismissBlockedOnly = enabled
                                preferencesManager.setDismissBlockedOnly(enabled)
                            }
                        )
                    }
                    
                    ActionSettingItem(
                        title = "Open SMS App Notification Settings",
                        subtitle = "Manually configure SMS app notifications",
                        icon = Icons.Default.OpenInNew,
                        onClick = {
                            smsAppManager.openDefaultSmsAppNotificationSettings()
                        }
                    )
                }
                
                // Show notification listener permission status
                val hasNotificationAccess = remember {
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
                
                if (!hasNotificationAccess) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = DesignSpacing.SM),
                        colors = CardDefaults.cardColors(
                            containerColor = DesignColors.NotificationMessage.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(DesignBorderRadius.SM)
                    ) {
                        Row(
                            modifier = Modifier.padding(DesignSpacing.SM),
                            horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = DesignColors.NotificationMessage,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notification Access Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DesignColors.Primary
                                )
                                Text(
                                    text = "Grant notification access to enable SMS app control",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DesignColors.Secondary
                                )
                            }
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                }
                            ) {
                                Text("Grant")
                            }
                        }
                    }
                }
            }
        }
        
        // Custom Keywords Section
        item {
            SettingsSection(
                title = "Custom Keywords",
                icon = Icons.Default.TextFields
            ) {
                ActionSettingItem(
                    title = "Add Keyword",
                    subtitle = "Add custom spam keywords",
                    icon = Icons.Default.Add,
                    onClick = { showAddKeywordDialog = true }
                )
                
                customKeywords.forEach { keyword ->
                    KeywordItem(
                        keyword = keyword,
                        onRemove = {
                            val newKeywords = customKeywords.toMutableList()
                            newKeywords.remove(keyword)
                            customKeywords = newKeywords
                            preferencesManager.setCustomKeywords(newKeywords)
                        }
                    )
                }
            }
        }
        
        // Custom Regex Patterns Section  
        item {
            SettingsSection(
                title = "Custom Regex Patterns",
                icon = Icons.Default.Code
            ) {
                ActionSettingItem(
                    title = "Add Pattern",
                    subtitle = "Add custom regex patterns",
                    icon = Icons.Default.Add,
                    onClick = { showAddRegexDialog = true }
                )
                
                customRegexPatterns.forEach { pattern ->
                    RegexPatternItem(
                        pattern = pattern,
                        onRemove = {
                            val newPatterns = customRegexPatterns.toMutableList()
                            newPatterns.remove(pattern)
                            customRegexPatterns = newPatterns
                            preferencesManager.setCustomRegexPatterns(newPatterns)
                        }
                    )
                }
            }
        }
        
        // Allowed Content Section
        item {
            SettingsSection(
                title = "Allowed Content",
                icon = Icons.Default.CheckCircle
            ) {
                Text(
                    text = "Messages containing these words or matching these patterns skip all filtering, " +
                        "including Stop2End. Useful for senders that look like spam but aren't.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary
                )
                
                ActionSettingItem(
                    title = "Add Rule",
                    subtitle = "Allow a word, phrase or regex",
                    icon = Icons.Default.Add,
                    onClick = { showAddAllowRuleDialog = true }
                )
                
                allowRules.forEach { rule ->
                    RegexPatternItem(
                        pattern = if (rule.isRegex) "/${rule.pattern}/" else rule.pattern,
                        onRemove = {
                            val newRules = allowRules - rule
                            allowRules = newRules
                            preferencesManager.setAllowRules(newRules)
                        }
                    )
                }
            }
        }
        
        // Allowed Senders Section
        item {
            SettingsSection(
                title = "Allowed Senders",
                icon = Icons.Default.People
            ) {
                ActionSettingItem(
                    title = "Add Sender",
                    subtitle = "Add trusted senders",
                    icon = Icons.Default.Add,
                    onClick = { showAddAllowedSenderDialog = true }
                )
                
                allowedSenders.forEach { sender ->
                    AllowedSenderItem(
                        sender = sender,
                        onRemove = {
                            serviceScope.launch {
                                database.allowedSenderDao().deleteAllowedSender(sender)
                            }
                        }
                    )
                }
            }
        }
        
        // Data Management Section
        item {
            SettingsSection(
                title = "Data Management",
                icon = Icons.Default.Storage
            ) {
                ActionSettingItem(
                    title = "Process Existing Messages",
                    subtitle = "Apply current filters to existing messages",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        isProcessingExistingMessages = true
                        serviceScope.launch {
                            try {
                                val existingProcessor = com.ovehbe.junkboy.utils.ExistingMessagesProcessor(context)
                                val result = existingProcessor.processAllExistingMessages()
                                processingResult = when (result.isSuccess) {
                                    true -> "Successfully processed ${result.getOrNull()} messages"
                                    false -> "Error: ${result.exceptionOrNull()?.message}"
                                }
                                isProcessingExistingMessages = false
                            } catch (e: Exception) {
                                processingResult = "Error: ${e.message}"
                                isProcessingExistingMessages = false
                            }
                        }
                    },
                    isLoading = isProcessingExistingMessages
                )
                
                processingResult?.let { result ->
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignColors.Secondary,
                        modifier = Modifier.padding(horizontal = DesignSpacing.MD)
                    )
                }
                
                ActionSettingItem(
                    title = "Export Data",
                    subtitle = "Export filtered messages to CSV",
                    icon = Icons.Default.FileDownload,
                    onClick = {
                        isExporting = true
                        serviceScope.launch {
                            try {
                                // Get all filtered messages as a list
                                val allMessages = database.filteredMessageDao().getAllMessagesLimited(1000)
                                val messagesList = allMessages.first()
                                val result = csvExporter.exportAndShare(messagesList)
                                exportResult = when (result.isSuccess) {
                                    true -> "Successfully exported ${messagesList.size} messages"
                                    false -> "Error: ${result.exceptionOrNull()?.message}"
                                }
                                isExporting = false
                            } catch (e: Exception) {
                                exportResult = "Error: ${e.message}"
                                isExporting = false
                            }
                        }
                    },
                    isLoading = isExporting
                )
                
                exportResult?.let { result ->
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignColors.Secondary,
                        modifier = Modifier.padding(horizontal = DesignSpacing.MD)
                    )
                }
                
                ActionSettingItem(
                    title = "Clear All Data",
                    subtitle = "Remove all filtered messages",
                    icon = Icons.Default.Delete,
                    onClick = { showClearDataDialog = true },
                    isDestructive = true
                )
            }
        }
    }
    
    // Dialog handlers
    if (showAddKeywordDialog) {
        AddKeywordDialog(
            onDismiss = { showAddKeywordDialog = false },
            onAdd = { keyword ->
                val newKeywords = customKeywords.toMutableList()
                newKeywords.add(keyword)
                customKeywords = newKeywords
                preferencesManager.setCustomKeywords(newKeywords)
                showAddKeywordDialog = false
            }
        )
    }
    
    if (showAddAllowRuleDialog) {
        AddAllowRuleDialog(
            onDismiss = { showAddAllowRuleDialog = false },
            onAdd = { rule ->
                if (rule !in allowRules) {
                    val newRules = allowRules + rule
                    allowRules = newRules
                    preferencesManager.setAllowRules(newRules)
                }
                showAddAllowRuleDialog = false
            }
        )
    }
    
    if (showAddRegexDialog) {
        AddRegexDialog(
            onDismiss = { showAddRegexDialog = false },
            onAdd = { pattern ->
                val newPatterns = customRegexPatterns.toMutableList()
                newPatterns.add(pattern)
                customRegexPatterns = newPatterns
                preferencesManager.setCustomRegexPatterns(newPatterns)
                showAddRegexDialog = false
            }
        )
    }
    
    if (showAddAllowedSenderDialog) {
        AddAllowedSenderDialog(
            onDismiss = { showAddAllowedSenderDialog = false },
            onAdd = { phoneNumber ->
                serviceScope.launch {
                    val sender = com.ovehbe.junkboy.database.AllowedSender(
                        phoneNumber = phoneNumber,
                        displayName = null
                    )
                    database.allowedSenderDao().insertAllowedSender(sender)
                    // Also update all existing messages from this sender to ALLOWED category
                    val normalizedSender = phoneNumber.replace(Regex("[+\\-()\\s]"), "")
                    database.filteredMessageDao().updateSenderCategory(
                        sender = phoneNumber,
                        normalizedSender = normalizedSender,
                        category = com.ovehbe.junkboy.database.MessageCategory.ALLOWED,
                        filterType = com.ovehbe.junkboy.database.FilterType.ALLOWED_SENDER
                    )
                }
                showAddAllowedSenderDialog = false
            }
        )
    }
    
    if (showClearDataDialog) {
        ClearDataDialog(
            onDismiss = { showClearDataDialog = false },
            onConfirm = {
                serviceScope.launch {
                    // Delete ALL filtered messages
                    database.filteredMessageDao().deleteAllMessages()
                    // Also delete chat messages
                    database.chatMessageDao().deleteAllChatMessages()
                }
                // Reset ALL statistics too
                preferencesManager.clearAllData()
                showClearDataDialog = false
            }
        )
    }
}

@Composable
private fun OpenRouterJudgeItem(preferencesManager: PreferencesManager) {
    val scope = rememberCoroutineScope()
    var apiKey by remember { mutableStateOf(preferencesManager.getOpenRouterApiKey()) }
    var model by remember { mutableStateOf(preferencesManager.getOpenRouterModel()) }
    var kind by remember { mutableStateOf(preferencesManager.getOpenRouterModelKind()) }
    var cut by remember { mutableFloatStateOf(preferencesManager.getDecisionCut()) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)) {
        Text(
            text = "Second opinion (OpenRouter)",
            style = MaterialTheme.typography.titleSmall,
            color = DesignColors.Primary
        )
        Text(
            text = "Optional. With an API key, messages that carry an opt-out directive, come from senders you " +
                "have never texted and weren't already flagged as spam are sent to OpenRouter and the model " +
                "provider to decide whether they are spam. Links, email addresses and long numbers are removed " +
                "first; names are not. Leave the key empty to keep everything on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = DesignColors.Secondary
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                preferencesManager.setOpenRouterApiKey(it)
                testResult = null
            },
            label = { Text("OpenRouter API key") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        OpenRouterModelPicker(model, kind) { picked ->
            model = picked.id
            kind = picked.kind
            preferencesManager.setOpenRouterModel(picked.id, picked.kind)
            testResult = null
        }
        if (kind == com.ovehbe.junkboy.llm.ModelKind.DECISION) {
            Text(
                text = "Spam cut-off: ${String.format(java.util.Locale.US, "%.2f", cut)}",
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.Primary
            )
            Slider(
                value = cut,
                onValueChange = { cut = it; testResult = null },
                onValueChangeFinished = { preferencesManager.setDecisionCut(cut) },
                valueRange = 0.3f..0.95f
            )
            Text(
                text = "Decision models score each message. A text counts as spam when its spam score beats its " +
                    "wanted score by at least this much. Higher is more cautious: fewer wanted texts are blocked, " +
                    "but more spam gets through. 0.70 worked well in testing.",
                style = MaterialTheme.typography.bodySmall,
                color = DesignColors.Secondary
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                enabled = apiKey.isNotBlank() && !testing,
                onClick = {
                    testing = true
                    scope.launch {
                        val start = System.currentTimeMillis()
                        testResult = try {
                            // Hard-coded sample, so no real message is sent by the test
                            val result = com.ovehbe.junkboy.llm.OpenRouterJudges
                                .create(apiKey.trim(), model, kind, cut.toDouble())
                                .judgeOrThrow("12345", "Chip in $5 to help us flip the Senate before midnight! STOP2END")
                            val detail = if (result.detail.isNotEmpty()) " (${result.detail})" else ""
                            "Works: sample fundraising text judged ${result.verdict}$detail in ${System.currentTimeMillis() - start} ms"
                        } catch (e: Exception) {
                            "Failed: ${e.message}"
                        }
                        testing = false
                    }
                }
            ) { Text("Test") }
            if (testing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        Text(
            text = testResult ?: if (apiKey.isBlank()) "Off: no API key" else "On",
            style = MaterialTheme.typography.bodySmall,
            color = DesignColors.Secondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenRouterModelPicker(
    model: String,
    kind: com.ovehbe.junkboy.llm.ModelKind,
    onPick: (com.ovehbe.junkboy.llm.OpenRouterModel) -> Unit
) {
    var models by remember { mutableStateOf<List<com.ovehbe.junkboy.llm.OpenRouterModel>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        try {
            // Decision models first (a short list), then chat models, each sorted by id
            models = com.ovehbe.junkboy.llm.OpenRouterModels.load()
                .sortedWith(compareBy({ it.kind != com.ovehbe.junkboy.llm.ModelKind.DECISION }, { it.id }))
        } catch (e: Exception) {
            loadError = "Couldn't load the model list (${e.message})"
        }
    }
    
    val selected = models.firstOrNull { it.id == model }
    
    Column(verticalArrangement = Arrangement.spacedBy(DesignSpacing.XS)) {
        ExposedDropdownMenuBox(
            expanded = expanded && models.isNotEmpty(),
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = model,
                onValueChange = {},
                readOnly = true,
                label = { Text("Model (${kind.label.lowercase()})") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded && models.isNotEmpty(),
                onDismissRequest = { expanded = false }
            ) {
                models.forEach { m ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(m.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${m.kind.label} · ${m.id} · ${m.priceLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DesignColors.Secondary
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onPick(m)
                        }
                    )
                }
            }
        }
        Text(
            text = selected?.let {
                "${it.kind.label} model · ${it.priceLabel} · about ${com.ovehbe.junkboy.llm.formatMoney(it.costPer1000Messages)} per 1,000 messages"
            } ?: loadError ?: (if (models.isEmpty()) "Loading models…"
            else "${models.size} models, decision and chat. Tap the field to choose one"),
            style = MaterialTheme.typography.bodySmall,
            color = DesignColors.Secondary
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DesignColors.Surface
        ),
        shape = RoundedCornerShape(DesignBorderRadius.MD)
    ) {
        Column(
            modifier = Modifier.padding(DesignSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DesignColors.Primary,
                    modifier = Modifier.size(DesignLayout.IconSize)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignColors.Primary
                )
            }
            
            content()
        }
    }
}

@Composable
private fun ToggleSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = DesignColors.Primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DesignColors.Secondary
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DesignColors.ButtonText,
                checkedTrackColor = DesignColors.Accent,
                uncheckedThumbColor = DesignColors.ButtonText,
                uncheckedTrackColor = DesignColors.Secondary
            )
        )
    }
}

@Composable
private fun ActionSettingItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    isDestructive: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignBorderRadius.SM)),
        color = DesignColors.Background,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) DesignColors.Accent else DesignColors.Primary,
                modifier = Modifier.size(DesignLayout.IconSize)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDestructive) DesignColors.Accent else DesignColors.Primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary
                )
            }
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(DesignLayout.IconSize),
                    color = DesignColors.Primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = DesignColors.Secondary,
                    modifier = Modifier.size(DesignLayout.IconSize)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSettingItem(
    title: String,
    subtitle: String,
    currentValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = DesignColors.Primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary
                )
            }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                modifier = Modifier.width(160.dp)
            ) {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .width(160.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignColors.Primary,
                        unfocusedBorderColor = DesignColors.InputBorder,
                        focusedContainerColor = DesignColors.InputBackground,
                        unfocusedContainerColor = DesignColors.InputBackground
                    ),
                    shape = RoundedCornerShape(DesignBorderRadius.SM)
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { onOptionSelected(option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeywordItem(
    keyword: String,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DesignColors.ActiveBackground,
        shape = RoundedCornerShape(DesignBorderRadius.SM)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignSpacing.SM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = keyword,
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.Primary
            )
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(DesignLayout.IconSize)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = DesignColors.Accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RegexPatternItem(
    pattern: String,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DesignColors.ActiveBackground,
        shape = RoundedCornerShape(DesignBorderRadius.SM)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignSpacing.SM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pattern,
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.Primary,
                fontFamily = FontFamily.Monospace
            )
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(DesignLayout.IconSize)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = DesignColors.Accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AllowedSenderItem(
    sender: com.ovehbe.junkboy.database.AllowedSender,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DesignColors.ActiveBackground,
        shape = RoundedCornerShape(DesignBorderRadius.SM)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignSpacing.SM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = sender.displayName ?: sender.phoneNumber,
                    style = MaterialTheme.typography.titleSmall,
                    color = DesignColors.Primary
                )
                if (sender.displayName != null) {
                    Text(
                        text = sender.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignColors.Secondary
                    )
                }
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(DesignLayout.IconSize)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = DesignColors.Accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Dialog composables (simplified versions)
@Composable
private fun AddKeywordDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Keyword") },
        text = {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("Keyword") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (keyword.isNotBlank()) onAdd(keyword) }
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

@Composable
private fun AddAllowRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (com.ovehbe.junkboy.filters.AllowRule) -> Unit
) {
    var pattern by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    val rule = com.ovehbe.junkboy.filters.AllowRule(pattern.trim(), isRegex)
    val error = if (pattern.isBlank()) null else com.ovehbe.junkboy.filters.ContentAllowlist.validate(rule)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Allow Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text(if (isRegex) "Regex" else "Word or phrase") },
                    isError = error != null,
                    supportingText = { if (error != null) Text(error) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRegex, onCheckedChange = { isRegex = it })
                    Text("Regular expression")
                }
                Text(
                    text = if (isRegex) "Matched anywhere in the message, ignoring case."
                    else "Matched as a whole word or phrase, ignoring case.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = pattern.isNotBlank() && error == null,
                onClick = { onAdd(rule) }
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

@Composable
private fun AddRegexDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var pattern by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Regex Pattern") },
        text = {
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text("Pattern") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (pattern.isNotBlank()) onAdd(pattern) }
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

@Composable
private fun AddAllowedSenderDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Allowed Sender") },
        text = {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (phoneNumber.isNotBlank()) onAdd(phoneNumber) }
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

@Composable
private fun ClearDataDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear All Data") },
        text = { Text("This will permanently delete all filtered messages. This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = DesignColors.Accent
                )
            ) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getCategoryDisplayColor(category: com.ovehbe.junkboy.database.MessageCategory): androidx.compose.ui.graphics.Color {
    return when (category) {
        com.ovehbe.junkboy.database.MessageCategory.GENERAL -> DesignColors.GeneralMessage
        com.ovehbe.junkboy.database.MessageCategory.PROMOTION -> DesignColors.PromotionMessage
        com.ovehbe.junkboy.database.MessageCategory.NOTIFICATION -> DesignColors.NotificationMessage
        com.ovehbe.junkboy.database.MessageCategory.TRANSACTION -> DesignColors.TransactionMessage
        com.ovehbe.junkboy.database.MessageCategory.JUNK -> DesignColors.JunkMessage
        com.ovehbe.junkboy.database.MessageCategory.ALLOWED -> DesignColors.AllowedMessage
    }
} 