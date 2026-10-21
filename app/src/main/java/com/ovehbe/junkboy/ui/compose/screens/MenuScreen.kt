package com.ovehbe.junkboy.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ovehbe.junkboy.ui.theme.*
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.SmsAppManager
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onRequestPermissions: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToTest: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    refreshTrigger: Int = 0,
    checkPermissions: () -> Boolean = { true }
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val smsAppManager = remember { SmsAppManager(context) }
    
    var totalFiltered by remember { mutableStateOf(preferencesManager.getTotalMessagesFiltered()) }
    var totalBlocked by remember { mutableStateOf(preferencesManager.getTotalMessagesBlocked()) }
    var otpCount by remember { mutableStateOf(preferencesManager.getOtpCopyCount()) }
    
    // Live permission check
    val hasPermissions by remember(refreshTrigger) {
        mutableStateOf(checkPermissions())
    }
    
    // Check notification listener permission
    val hasNotificationAccess = remember(refreshTrigger) {
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
    
    // Load stats
    LaunchedEffect(refreshTrigger) {
        totalFiltered = preferencesManager.getTotalMessagesFiltered()
        totalBlocked = preferencesManager.getTotalMessagesBlocked()
        otpCount = preferencesManager.getOtpCopyCount()
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DesignSpacing.MD)
            .padding(top = DesignSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(DesignSpacing.MD)
    ) {
        item {
            // Header
            Text(
                text = "Menu",
                style = MaterialTheme.typography.headlineMedium,
                color = DesignColors.Primary
            )
        }
        
        item {
            // Quick Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DesignColors.Surface),
                shape = RoundedCornerShape(DesignBorderRadius.MD)
            ) {
                Column(
                    modifier = Modifier.padding(DesignSpacing.MD),
                    verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
                ) {
                    Text(
                        text = "Quick Stats",
                        style = MaterialTheme.typography.titleMedium,
                        color = DesignColors.Primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatsItem(
                            title = "Filtered",
                            value = totalFiltered.toString(),
                            icon = Icons.Default.FilterList
                        )
                        StatsItem(
                            title = "Blocked",
                            value = totalBlocked.toString(),
                            icon = Icons.Default.Block
                        )
                        StatsItem(
                            title = "OTPs Copied",
                            value = otpCount.toString(),
                            icon = Icons.Default.ContentCopy
                        )
                    }
                }
            }
        }
        
        item {
            // Main Actions
            MenuSection(title = "Main") {
                MenuItem(
                    title = "Dashboard",
                    subtitle = "App overview and status",
                    icon = Icons.Default.Home,
                    onClick = onNavigateToDashboard
                )
                
                MenuItem(
                    title = "Statistics",
                    subtitle = "Detailed filtering statistics",
                    icon = Icons.Default.Assessment,
                    onClick = onNavigateToStats
                )
            }
        }
        
        item {
            // Tools & Settings
            MenuSection(title = "Tools & Settings") {
                MenuItem(
                    title = "Settings",
                    subtitle = "Configure filtering and notifications",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToSettings
                )
                
                MenuItem(
                    title = "Test Filter",
                    subtitle = "Test how messages would be filtered",
                    icon = Icons.Default.Science,
                    onClick = onNavigateToTest
                )
            }
        }
        
        item {
            // App Status
            AppStatusCard(
                smsAppManager = smsAppManager,
                hasPermissions = hasPermissions,
                hasNotificationAccess = hasNotificationAccess,
                onRequestPermissions = onRequestPermissions
            )
        }
        
        // Add some bottom padding
        item {
            Spacer(modifier = Modifier.height(DesignSpacing.MD))
        }
    }
}

@Composable
private fun MenuSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DesignColors.Surface),
        shape = RoundedCornerShape(DesignBorderRadius.MD)
    ) {
        Column(
            modifier = Modifier.padding(DesignSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = DesignColors.Primary
            )
            
            content()
        }
    }
}

@Composable
private fun MenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
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
                .padding(DesignSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = DesignColors.Primary
            )
            
            Column(modifier = Modifier.weight(1f)) {
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
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = DesignColors.Secondary
            )
        }
    }
}

@Composable
private fun StatsItem(
    title: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignSpacing.XS)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = DesignColors.Accent
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = DesignColors.Primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = DesignColors.Secondary
        )
    }
}

@Composable
private fun AppStatusCard(
    smsAppManager: SmsAppManager,
    hasPermissions: Boolean,
    hasNotificationAccess: Boolean,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val isDefaultSmsApp = com.ovehbe.junkboy.ui.compose.rememberIsDefaultSmsApp(smsAppManager)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DesignColors.Surface),
        shape = RoundedCornerShape(DesignBorderRadius.MD)
    ) {
        Column(
            modifier = Modifier.padding(DesignSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            Text(
                text = "App Status",
                style = MaterialTheme.typography.titleMedium,
                color = DesignColors.Primary
            )
            
            // Permissions Status
            StatusRow(
                title = "SMS Permissions",
                subtitle = if (hasPermissions) "All permissions granted" else "Some permissions missing",
                icon = Icons.Default.Security,
                isEnabled = hasPermissions,
                onClick = if (!hasPermissions) onRequestPermissions else null
            )
            
            Divider(color = DesignColors.Divider, thickness = 1.dp)
            
            // Default SMS App Status
            StatusRow(
                title = "Default SMS App",
                subtitle = if (isDefaultSmsApp) "Junkboy is your default SMS app" else "Tap to set as default",
                icon = Icons.Default.Message,
                isEnabled = isDefaultSmsApp,
                onClick = if (!isDefaultSmsApp) {
                    { smsAppManager.requestDefaultSmsApp() }
                } else null
            )
            
            Divider(color = DesignColors.Divider, thickness = 1.dp)
            
            // Notification Access Status
            StatusRow(
                title = "Notification Access",
                subtitle = if (hasNotificationAccess) "Can read chat notifications" else "Tap to enable for Hub",
                icon = Icons.Default.Notifications,
                isEnabled = hasNotificationAccess,
                onClick = if (!hasNotificationAccess) {
                    {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                } else null
            )
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignBorderRadius.SM))
            .clickable { onClick() }
            .padding(DesignSpacing.SM)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(DesignSpacing.SM)
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isEnabled) DesignColors.Primary else DesignColors.Secondary
        )
        
        Column(modifier = Modifier.weight(1f)) {
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
        
        Icon(
            imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isEnabled) DesignColors.GeneralMessage else DesignColors.NotificationMessage
        )
        
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = DesignColors.Secondary
            )
        }
    }
}
