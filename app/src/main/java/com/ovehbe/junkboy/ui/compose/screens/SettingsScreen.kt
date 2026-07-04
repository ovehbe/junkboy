package com.ovehbe.junkboy.ui.compose.screens

import android.content.Intent
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
import androidx.compose.ui.res.stringResource
import com.ovehbe.junkboy.ui.theme.*
import com.ovehbe.junkboy.R
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
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DesignColors.Primary
            )
        }
        
        // Hub Settings Section
        item {
            SettingsSection(
                title = stringResource(R.string.settings_section_hub),
                icon = Icons.Default.Inbox
            ) {
                Text(
                    text = stringResource(R.string.settings_hub_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary,
                    modifier = Modifier.padding(bottom = DesignSpacing.SM)
                )

                ToggleSettingItem(
                    title = stringResource(R.string.settings_enable_hub),
                    subtitle = stringResource(R.string.settings_enable_hub_subtitle),
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
                        "all" to stringResource(R.string.settings_display_mode_all),
                        "sms_only" to stringResource(R.string.settings_display_mode_sms),
                        "chats_only" to stringResource(R.string.settings_display_mode_chats)
                    )
                    val currentDisplayLabel = displayModeOptions.find { it.first == hubDisplayMode }?.second ?: stringResource(R.string.settings_display_mode_sms)

                    DropdownSettingItem(
                        title = stringResource(R.string.settings_display_mode),
                        subtitle = stringResource(R.string.settings_display_mode_subtitle),
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
                        "conversations" to stringResource(R.string.settings_default_view_conversations),
                        "messages" to stringResource(R.string.settings_default_view_messages)
                    )
                    val currentViewLabel = defaultViewOptions.find { it.first == hubDefaultView }?.second ?: stringResource(R.string.settings_default_view_conversations)

                    DropdownSettingItem(
                        title = stringResource(R.string.settings_default_view),
                        subtitle = stringResource(R.string.settings_default_view_subtitle),
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
                title = stringResource(R.string.settings_section_sms_app),
                icon = Icons.Default.Message
            ) {
                Text(
                    text = stringResource(R.string.settings_sms_app_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary,
                    modifier = Modifier.padding(bottom = DesignSpacing.SM)
                )

                ToggleSettingItem(
                    title = stringResource(R.string.settings_hide_junk),
                    subtitle = stringResource(R.string.settings_hide_junk_subtitle),
                    checked = hideJunkInSms,
                    onCheckedChange = { enabled ->
                        hideJunkInSms = enabled
                        preferencesManager.setHideJunkInSms(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_show_category_badges),
                    subtitle = stringResource(R.string.settings_show_category_badges_subtitle),
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
                        text = stringResource(R.string.settings_display_categories),
                        style = MaterialTheme.typography.titleSmall,
                        color = DesignColors.Primary
                    )
                    Text(
                        text = stringResource(R.string.settings_display_categories_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignColors.Secondary,
                        modifier = Modifier.padding(bottom = DesignSpacing.SM)
                    )

                    // Category toggles
                    val allCategories = listOf(
                        com.ovehbe.junkboy.database.MessageCategory.GENERAL to stringResource(R.string.settings_category_general),
                        com.ovehbe.junkboy.database.MessageCategory.PROMOTION to stringResource(R.string.settings_category_promotion),
                        com.ovehbe.junkboy.database.MessageCategory.NOTIFICATION to stringResource(R.string.settings_category_notification),
                        com.ovehbe.junkboy.database.MessageCategory.TRANSACTION to stringResource(R.string.settings_category_transaction),
                        com.ovehbe.junkboy.database.MessageCategory.JUNK to stringResource(R.string.settings_category_junk),
                        com.ovehbe.junkboy.database.MessageCategory.ALLOWED to stringResource(R.string.settings_category_allowed)
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
                    title = stringResource(R.string.settings_auto_delete_junk),
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
                        title = stringResource(R.string.settings_test_auto_delete),
                        subtitle = autoDeleteTestResult ?: stringResource(R.string.settings_test_auto_delete_subtitle),
                        icon = Icons.Default.Science,
                        onClick = {
                            isTestingAutoDelete = true
                            autoDeleteTestResult = context.getString(R.string.settings_testing)
                            serviceScope.launch {
                                try {
                                    val canDelete = smsDeleter.canEnableAutoDelete()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        autoDeleteTestResult = if (canDelete) {
                                            context.getString(R.string.settings_auto_delete_ready)
                                        } else {
                                            context.getString(R.string.settings_auto_delete_cannot)
                                        }
                                        isTestingAutoDelete = false
                                    }
                                } catch (e: Exception) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        autoDeleteTestResult = context.getString(R.string.settings_process_error, e.message ?: "")
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
                title = stringResource(R.string.settings_section_accessibility),
                icon = Icons.Default.Accessibility
            ) {
                Text(
                    text = stringResource(R.string.settings_accessibility_description),
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
                                text = stringResource(R.string.settings_keyboard_offset),
                                style = MaterialTheme.typography.titleSmall,
                                color = DesignColors.Primary
                            )
                            Text(
                                text = stringResource(R.string.settings_keyboard_offset_subtitle),
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
                title = stringResource(R.string.settings_section_filter_methods),
                icon = Icons.Default.FilterList
            ) {
                ToggleSettingItem(
                    title = stringResource(R.string.settings_ai_classification),
                    subtitle = stringResource(R.string.settings_ai_classification_subtitle),
                    checked = isMlEnabled,
                    onCheckedChange = { enabled ->
                        isMlEnabled = enabled
                        preferencesManager.setMlFilteringEnabled(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_keyword_filtering),
                    subtitle = stringResource(R.string.settings_keyword_filtering_subtitle),
                    checked = isKeywordEnabled,
                    onCheckedChange = { enabled ->
                        isKeywordEnabled = enabled
                        preferencesManager.setKeywordFilteringEnabled(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_regex_filtering),
                    subtitle = stringResource(R.string.settings_regex_filtering_subtitle),
                    checked = isRegexEnabled,
                    onCheckedChange = { enabled ->
                        isRegexEnabled = enabled
                        preferencesManager.setRegexFilteringEnabled(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_under_attack_mode),
                    subtitle = stringResource(R.string.settings_under_attack_mode_subtitle),
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
                title = stringResource(R.string.settings_section_notifications),
                icon = Icons.Default.Notifications
            ) {
                ToggleSettingItem(
                    title = stringResource(R.string.settings_notify_general),
                    subtitle = stringResource(R.string.settings_notify_general_subtitle),
                    checked = notifyGeneral,
                    onCheckedChange = { enabled ->
                        notifyGeneral = enabled
                        preferencesManager.setNotifyGeneral(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_notify_notification),
                    subtitle = stringResource(R.string.settings_notify_notification_subtitle),
                    checked = notifyNotification,
                    onCheckedChange = { enabled ->
                        notifyNotification = enabled
                        preferencesManager.setNotifyNotification(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_notify_transaction),
                    subtitle = stringResource(R.string.settings_notify_transaction_subtitle),
                    checked = notifyTransaction,
                    onCheckedChange = { enabled ->
                        notifyTransaction = enabled
                        preferencesManager.setNotifyTransaction(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_notify_promotion),
                    subtitle = stringResource(R.string.settings_notify_promotion_subtitle),
                    checked = notifyPromotion,
                    onCheckedChange = { enabled ->
                        notifyPromotion = enabled
                        preferencesManager.setNotifyPromotion(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_notify_allowed),
                    subtitle = stringResource(R.string.settings_notify_allowed_subtitle),
                    checked = notifyAllowed,
                    onCheckedChange = { enabled ->
                        notifyAllowed = enabled
                        preferencesManager.setNotifyAllowed(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_notify_blocked),
                    subtitle = stringResource(R.string.settings_notify_blocked_subtitle),
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
                title = stringResource(R.string.settings_section_features),
                icon = Icons.Default.Settings
            ) {
                ToggleSettingItem(
                    title = stringResource(R.string.settings_otp_auto_copy),
                    subtitle = stringResource(R.string.settings_otp_auto_copy_subtitle),
                    checked = otpAutoCopy,
                    onCheckedChange = { enabled ->
                        otpAutoCopy = enabled
                        preferencesManager.setOtpAutoCopyEnabled(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_hub_notifications),
                    subtitle = stringResource(R.string.settings_hub_notifications_subtitle),
                    checked = hubNotifications,
                    onCheckedChange = { enabled ->
                        hubNotifications = enabled
                        preferencesManager.setHubNotificationsEnabled(enabled)
                    }
                )
                
                ToggleSettingItem(
                    title = stringResource(R.string.settings_auto_delete_feature),
                    subtitle = stringResource(R.string.settings_auto_delete_feature_subtitle),
                    checked = autoDeleteJunk,
                    onCheckedChange = { enabled ->
                        autoDeleteJunk = enabled
                        preferencesManager.setAutoDeleteJunk(enabled)
                        autoDeleteStatus = smsDeleter.getAutoDeleteStatus()
                    }
                )

                if (autoDeleteStatus.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_status, autoDeleteStatus),
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignColors.Secondary,
                        modifier = Modifier.padding(horizontal = DesignSpacing.MD)
                    )
                }
            }
        }
        
        // SMS App Notification Control Section (Buzzkill-like feature)
        item {
            SettingsSection(
                title = stringResource(R.string.settings_section_sms_control),
                icon = Icons.Default.NotificationsOff
            ) {
                Text(
                    text = stringResource(R.string.settings_sms_control_description, smsAppManager.getDefaultSmsAppName()),
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary,
                    modifier = Modifier.padding(bottom = DesignSpacing.SM)
                )

                ToggleSettingItem(
                    title = stringResource(R.string.settings_enable_sms_control),
                    subtitle = stringResource(R.string.settings_enable_sms_control_subtitle),
                    checked = smsAppControlEnabled,
                    onCheckedChange = { enabled ->
                        smsAppControlEnabled = enabled
                        preferencesManager.setSmsAppControlEnabled(enabled)
                    }
                )
                
                if (smsAppControlEnabled) {
                    ToggleSettingItem(
                        title = stringResource(R.string.settings_dismiss_sms_notifications),
                        subtitle = stringResource(R.string.settings_dismiss_sms_notifications_subtitle),
                        checked = dismissSmsAppNotifications,
                        onCheckedChange = { enabled ->
                            dismissSmsAppNotifications = enabled
                            preferencesManager.setDismissSmsAppNotifications(enabled)
                        }
                    )
                    
                    if (dismissSmsAppNotifications) {
                        ToggleSettingItem(
                            title = stringResource(R.string.settings_blocked_messages_only),
                            subtitle = stringResource(R.string.settings_blocked_messages_only_subtitle),
                            checked = dismissBlockedOnly,
                            onCheckedChange = { enabled ->
                                dismissBlockedOnly = enabled
                                preferencesManager.setDismissBlockedOnly(enabled)
                            }
                        )
                    }
                    
                    ActionSettingItem(
                        title = stringResource(R.string.settings_open_sms_notification_settings),
                        subtitle = stringResource(R.string.settings_open_sms_notification_settings_subtitle),
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
                                    text = stringResource(R.string.settings_notification_access_required),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DesignColors.Primary
                                )
                                Text(
                                    text = stringResource(R.string.settings_notification_access_message),
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
                                Text(stringResource(R.string.settings_grant))
                            }
                        }
                    }
                }
            }
        }
        
        // Custom Keywords Section
        item {
            SettingsSection(
                title = stringResource(R.string.settings_section_custom_keywords),
                icon = Icons.Default.TextFields
            ) {
                ActionSettingItem(
                    title = stringResource(R.string.settings_add_keyword),
                    subtitle = stringResource(R.string.settings_add_keyword_subtitle),
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
                title = stringResource(R.string.settings_section_custom_regex),
                icon = Icons.Default.Code
            ) {
                ActionSettingItem(
                    title = stringResource(R.string.settings_add_pattern),
                    subtitle = stringResource(R.string.settings_add_pattern_subtitle),
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
        
        // Allowed Senders Section
        item {
            SettingsSection(
                title = stringResource(R.string.settings_section_allowed_senders),
                icon = Icons.Default.People
            ) {
                ActionSettingItem(
                    title = stringResource(R.string.settings_add_sender),
                    subtitle = stringResource(R.string.settings_add_sender_subtitle),
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
                title = stringResource(R.string.settings_section_data_management),
                icon = Icons.Default.Storage
            ) {
                ActionSettingItem(
                    title = stringResource(R.string.settings_process_existing),
                    subtitle = stringResource(R.string.settings_process_existing_subtitle),
                    icon = Icons.Default.Refresh,
                    onClick = {
                        isProcessingExistingMessages = true
                        serviceScope.launch {
                            try {
                                val existingProcessor = com.ovehbe.junkboy.utils.ExistingMessagesProcessor(context)
                                val result = existingProcessor.processAllExistingMessages()
                                processingResult = when (result.isSuccess) {
                                    true -> context.getString(R.string.settings_process_success, result.getOrNull() ?: 0)
                                    false -> context.getString(R.string.settings_process_error, result.exceptionOrNull()?.message ?: "")
                                }
                                isProcessingExistingMessages = false
                            } catch (e: Exception) {
                                processingResult = context.getString(R.string.settings_process_error, e.message ?: "")
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
                    title = stringResource(R.string.settings_export_data),
                    subtitle = stringResource(R.string.settings_export_data_subtitle),
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
                                    true -> context.getString(R.string.settings_export_success, messagesList.size)
                                    false -> context.getString(R.string.settings_process_error, result.exceptionOrNull()?.message ?: "")
                                }
                                isExporting = false
                            } catch (e: Exception) {
                                exportResult = context.getString(R.string.settings_process_error, e.message ?: "")
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
                    title = stringResource(R.string.settings_clear_all_data),
                    subtitle = stringResource(R.string.settings_clear_all_data_subtitle),
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
                    contentDescription = stringResource(R.string.settings_remove),
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
                    contentDescription = stringResource(R.string.settings_remove),
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
                    contentDescription = stringResource(R.string.settings_remove),
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
        title = { Text(stringResource(R.string.settings_add_keyword_title)) },
        text = {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text(stringResource(R.string.settings_keyword_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (keyword.isNotBlank()) onAdd(keyword) }
            ) {
                Text(stringResource(R.string.settings_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
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
        title = { Text(stringResource(R.string.settings_add_regex_title)) },
        text = {
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text(stringResource(R.string.settings_pattern_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (pattern.isNotBlank()) onAdd(pattern) }
            ) {
                Text(stringResource(R.string.settings_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
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
        title = { Text(stringResource(R.string.settings_add_sender_title)) },
        text = {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text(stringResource(R.string.settings_phone_number_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (phoneNumber.isNotBlank()) onAdd(phoneNumber) }
            ) {
                Text(stringResource(R.string.settings_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
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
        title = { Text(stringResource(R.string.settings_clear_data_title)) },
        text = { Text(stringResource(R.string.settings_clear_data_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = DesignColors.Accent
                )
            ) {
                Text(stringResource(R.string.settings_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
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