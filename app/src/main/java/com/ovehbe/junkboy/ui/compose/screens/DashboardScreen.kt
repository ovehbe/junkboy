package com.ovehbe.junkboy.ui.compose.screens

import android.Manifest
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.ovehbe.junkboy.R
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.ui.theme.*
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.SmsAppManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onRequestPermissions: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isDefaultSmsApp: Boolean,
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
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_SMS", 
            "android.permission.WRITE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_MMS",
            "android.permission.RECEIVE_WAP_PUSH",
            Manifest.permission.POST_NOTIFICATIONS
        ).all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        isUnderAttackMode = preferencesManager.isUnderAttackMode()
        isMlEnabled = preferencesManager.isMlFilteringEnabled()
        
        // Check SMS guidance
        showSmsGuidance = smsAppManager.isNotificationGuidanceNeeded(isDefaultSmsApp)
        smsGuidanceMessage = smsAppManager.getSmsGuidanceMessage(isDefaultSmsApp)
        
        // Auto-enable categorized notifications if guidance is needed
        if (showSmsGuidance && !preferencesManager.shouldNotifyCategorizedMessages()) {
            preferencesManager.setNotifyCategorizedMessages(true)
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignSpacing.XS)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = DesignColors.Primary
                )
                Text(
                    text = stringResource(R.string.dashboard_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DesignColors.Secondary
                )
            }
        }
        
        item {
            // Permissions Card
            if (!hasPermissions) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DesignColors.Surface
                    ),
                    shape = RoundedCornerShape(DesignBorderRadius.MD),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(DesignSpacing.MD),
                        verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = DesignColors.Accent,
                                modifier = Modifier.size(DesignLayout.IconSize)
                            )
                            Spacer(modifier = Modifier.width(DesignSpacing.SM))
                            Text(
                                text = stringResource(R.string.dashboard_permissions_required),
                                style = MaterialTheme.typography.titleMedium,
                                color = DesignColors.Primary
                            )
                        }
                        Text(
                            text = stringResource(R.string.dashboard_permissions_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = DesignColors.Secondary
                        )
                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DesignComponents.Button.Height),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DesignColors.ButtonBackground,
                                contentColor = DesignColors.ButtonText
                            ),
                            shape = RoundedCornerShape(DesignComponents.Button.BorderRadius)
                        ) {
                            Text(
                                stringResource(R.string.dashboard_grant_permissions),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = DesignComponents.Button.FontWeight
                            )
                        }
                    }
                }
            } else {
                // Status Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DesignColors.Surface
                    ),
                    shape = RoundedCornerShape(DesignBorderRadius.MD),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(DesignSpacing.MD),
                        verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = DesignColors.Primary,
                                modifier = Modifier.size(DesignLayout.IconSize)
                            )
                            Spacer(modifier = Modifier.width(DesignSpacing.SM))
                            Text(
                                text = stringResource(R.string.dashboard_filtering_active),
                                style = MaterialTheme.typography.titleMedium,
                                color = DesignColors.Primary
                            )
                        }
                        Text(
                            text = stringResource(R.string.dashboard_filtering_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = DesignColors.Secondary
                        )
                    }
                }
            }
        }

        // SMS Guidance Card
        if (showSmsGuidance) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DesignColors.Surface
                    ),
                    shape = RoundedCornerShape(DesignBorderRadius.MD),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(DesignSpacing.MD),
                        verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = DesignColors.Accent,
                                modifier = Modifier.size(DesignLayout.IconSize)
                            )
                            Spacer(modifier = Modifier.width(DesignSpacing.SM))
                            Text(
                                text = stringResource(R.string.dashboard_sms_guidance),
                                style = MaterialTheme.typography.titleMedium,
                                color = DesignColors.Primary
                            )
                        }
                        Text(
                            text = smsGuidanceMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DesignColors.Secondary
                        )
                    }
                }
            }
        }

        // Quick Actions
        item {
            QuickActionsSection(
                onNavigateToMessages = onNavigateToMessages,
                onNavigateToSettings = onNavigateToSettings,
                isUnderAttackMode = isUnderAttackMode,
                isMlEnabled = isMlEnabled,
                onToggleUnderAttackMode = { 
                    preferencesManager.setUnderAttackMode(!isUnderAttackMode)
                    isUnderAttackMode = !isUnderAttackMode
                }
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onNavigateToMessages: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isUnderAttackMode: Boolean,
    isMlEnabled: Boolean,
    onToggleUnderAttackMode: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
    ) {
        Text(
            text = stringResource(R.string.dashboard_quick_actions),
            style = MaterialTheme.typography.titleMedium,
            color = DesignColors.Primary
        )
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            Button(
                onClick = onNavigateToMessages,
                modifier = Modifier
                    .weight(1f)
                    .height(DesignComponents.Button.Height),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesignColors.ButtonBackground,
                    contentColor = DesignColors.ButtonText
                ),
                shape = RoundedCornerShape(DesignComponents.Button.BorderRadius)
            ) {
                Icon(
                    Icons.Default.Message,
                    contentDescription = null,
                    modifier = Modifier.size(DesignLayout.IconSize)
                )
                Spacer(modifier = Modifier.width(DesignSpacing.XS))
                Text(
                    stringResource(R.string.dashboard_messages),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = DesignComponents.Button.FontWeight
                )
            }
            
            Button(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .weight(1f)
                    .height(DesignComponents.Button.Height),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesignColors.ButtonBackground,
                    contentColor = DesignColors.ButtonText
                ),
                shape = RoundedCornerShape(DesignComponents.Button.BorderRadius)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(DesignLayout.IconSize)
                )
                Spacer(modifier = Modifier.width(DesignSpacing.XS))
                Text(
                    stringResource(R.string.dashboard_settings),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = DesignComponents.Button.FontWeight
                )
            }
        }
        
        // Toggle switches
        Card(
            colors = CardDefaults.cardColors(
                containerColor = DesignColors.Surface
            ),
            shape = RoundedCornerShape(DesignBorderRadius.MD),
            modifier = Modifier.fillMaxWidth()
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
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_under_attack),
                            style = MaterialTheme.typography.titleSmall,
                            color = DesignColors.Primary
                        )
                        Text(
                            text = stringResource(R.string.dashboard_under_attack_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = DesignColors.Secondary
                        )
                    }
                    Switch(
                        checked = isUnderAttackMode,
                        onCheckedChange = { onToggleUnderAttackMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DesignColors.ButtonText,
                            checkedTrackColor = DesignColors.Accent,
                            uncheckedThumbColor = DesignColors.ButtonText,
                            uncheckedTrackColor = DesignColors.Secondary
                        )
                    )
                }
                
                Divider(
                    color = DesignColors.Divider,
                    thickness = DesignComponents.Divider.Thickness
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_ai_classification),
                            style = MaterialTheme.typography.titleSmall,
                            color = DesignColors.Primary
                        )
                        Text(
                            text = if (isMlEnabled) stringResource(R.string.dashboard_ai_enabled) else stringResource(R.string.dashboard_ai_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = DesignColors.Secondary
                        )
                    }
                    Icon(
                        if (isMlEnabled) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isMlEnabled) DesignColors.Accent else DesignColors.Secondary,
                        modifier = Modifier.size(DesignLayout.IconSize)
                    )
                }
            }
        }
    }
} 
