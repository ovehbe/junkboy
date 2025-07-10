package com.ovehbe.junkboy.ui.compose.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.SmsAppManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onRequestPermissions: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToSettings: () -> Unit,
    refreshTrigger: Int = 0 // Add trigger to refresh permission state
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val smsAppManager = remember { SmsAppManager(context) }
    
    var hasPermissions by remember { mutableStateOf(false) }
    var isUnderAttackMode by remember { mutableStateOf(false) }
    var isMlEnabled by remember { mutableStateOf(true) }
    var showSmsGuidance by remember { mutableStateOf(false) }
    var smsGuidanceMessage by remember { mutableStateOf("") }
    
    // Check permissions - now reactive to refreshTrigger
    LaunchedEffect(refreshTrigger) {
        hasPermissions = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        ).all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        isUnderAttackMode = preferencesManager.isUnderAttackMode()
        isMlEnabled = preferencesManager.isMlFilteringEnabled()
        
        // Check SMS guidance
        showSmsGuidance = smsAppManager.isNotificationGuidanceNeeded()
        smsGuidanceMessage = smsAppManager.getSmsGuidanceMessage()
        
        // Auto-enable categorized notifications if guidance is needed
        if (showSmsGuidance && !preferencesManager.shouldNotifyCategorizedMessages()) {
            preferencesManager.setNotifyCategorizedMessages(true)
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Text(
                text = "Junkboy SMS Filter",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Privacy-first SMS filtering",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        item {
            // Permissions Card
            if (!hasPermissions) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Permissions Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Junkboy needs SMS permissions to filter your messages. Your data stays on your device.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                }
            } else {
                // Status Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SMS Filtering Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your messages are being filtered automatically",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        item {
            // Quick Stats
            TodayStatsCard(preferencesManager)
        }
        
        item {
            // SMS Notification Guidance
            if (showSmsGuidance) {
                SmsGuidanceCard(
                    message = smsGuidanceMessage,
                    defaultAppName = smsAppManager.getDefaultSmsAppName(),
                    onOpenNotificationSettings = {
                        smsAppManager.openDefaultSmsAppNotificationSettings()
                    },
                    onMakeDefaultApp = {
                        smsAppManager.requestDefaultSmsApp()
                    }
                )
            }
        }
        
        item {
            // Under Attack Mode Toggle
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Under Attack Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "More aggressive filtering for heavy spam periods",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isUnderAttackMode,
                            onCheckedChange = { enabled ->
                                isUnderAttackMode = enabled
                                preferencesManager.setUnderAttackMode(enabled)
                            }
                        )
                    }
                }
            }
        }
        
        item {
            // Quick Actions
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToMessages,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Message, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Messages")
                }
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settings")
                }
            }
        }
        
        item {
            // Feature Status
            FeatureStatusCard(
                isMlEnabled = isMlEnabled,
                isKeywordEnabled = preferencesManager.isKeywordFilteringEnabled(),
                isRegexEnabled = preferencesManager.isRegexFilteringEnabled()
            )
        }
    }
}

@Composable
private fun TodayStatsCard(preferencesManager: PreferencesManager) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatsItem(
                    label = "Blocked",
                    value = preferencesManager.getDailyBlockedCount().toString(),
                    icon = Icons.Default.Block,
                    color = MaterialTheme.colorScheme.error
                )
                StatsItem(
                    label = "Junk",
                    value = preferencesManager.getDailyCategoryCount(MessageCategory.JUNK).toString(),
                    icon = Icons.Default.Delete,
                    color = MaterialTheme.colorScheme.error
                )
                StatsItem(
                    label = "Promotion",
                    value = preferencesManager.getDailyCategoryCount(MessageCategory.PROMOTION).toString(),
                    icon = Icons.Default.LocalOffer,
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatsItem(
                    label = "Transaction",
                    value = preferencesManager.getDailyCategoryCount(MessageCategory.TRANSACTION).toString(),
                    icon = Icons.Default.AccountBalance,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatsItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureStatusCard(
    isMlEnabled: Boolean,
    isKeywordEnabled: Boolean,
    isRegexEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filter Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            FeatureStatusRow("AI Classification", isMlEnabled)
            FeatureStatusRow("Keyword Filtering", isKeywordEnabled)
            FeatureStatusRow("Regex Filtering", isRegexEnabled)
        }
    }
}

@Composable
private fun FeatureStatusRow(
    feature: String,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium
        )
        Icon(
            if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SmsGuidanceCard(
    message: String,
    defaultAppName: String,
    onOpenNotificationSettings: () -> Unit,
    onMakeDefaultApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SMS Notification Setup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mute $defaultAppName")
                }
                
                Button(
                    onClick = onMakeDefaultApp,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Use Junkboy")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "💡 Tip: Either mute your current SMS app notifications OR make Junkboy your default SMS app to avoid duplicate notifications.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 