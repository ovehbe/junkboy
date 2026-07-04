package com.ovehbe.junkboy.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovehbe.junkboy.R
import com.ovehbe.junkboy.database.AppDatabase
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.ui.theme.*
import com.ovehbe.junkboy.utils.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    
    var totalFiltered by remember { mutableLongStateOf(0L) }
    var totalBlocked by remember { mutableLongStateOf(0L) }
    var dailyStats by remember { mutableStateOf<Map<MessageCategory, Int>>(emptyMap()) }
    var dailyBlocked by remember { mutableIntStateOf(0) }
    var otpCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        totalFiltered = preferencesManager.getTotalMessagesFiltered()
        totalBlocked = preferencesManager.getTotalMessagesBlocked()
        dailyBlocked = preferencesManager.getDailyBlockedCount()
        otpCount = preferencesManager.getOtpCopyCount()
        
        dailyStats = MessageCategory.values().associateWith { category ->
            preferencesManager.getDailyCategoryCount(category)
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
                text = stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DesignColors.Primary
            )
        }
        
        item {
            // Overall Stats Card
            OverallStatsCard(
                totalFiltered = totalFiltered,
                totalBlocked = totalBlocked,
                otpCount = otpCount
            )
        }
        
        item {
            // Today's Activity Card
            TodayActivityCard(
                dailyStats = dailyStats,
                dailyBlocked = dailyBlocked
            )
        }
        
        item {
            // Category Breakdown Chart
            CategoryBreakdownCard(dailyStats)
        }
        
        item {
            // Performance Metrics
            PerformanceMetricsCard(
                totalFiltered = totalFiltered,
                totalBlocked = totalBlocked,
                dailyStats = dailyStats
            )
        }
        
        item {
            // Filter Effectiveness
            FilterEffectivenessCard(preferencesManager)
        }
    }
}

@Composable
private fun OverallStatsCard(
    totalFiltered: Long,
    totalBlocked: Long,
    otpCount: Int
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
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = DesignColors.Primary,
                    modifier = Modifier.size(DesignLayout.IconSize)
                )
                Text(
                    text = stringResource(R.string.stats_total_statistics),
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignColors.Primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = stringResource(R.string.stats_messages_filtered),
                    value = totalFiltered.toString(),
                    icon = Icons.Default.FilterList,
                    color = DesignColors.Primary
                )
                StatItem(
                    label = stringResource(R.string.stats_junk_blocked),
                    value = totalBlocked.toString(),
                    icon = Icons.Default.Block,
                    color = DesignColors.Accent
                )
                StatItem(
                    label = stringResource(R.string.stats_otp_copied),
                    value = otpCount.toString(),
                    icon = Icons.Default.ContentCopy,
                    color = DesignColors.Secondary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignSpacing.XS)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(DesignLayout.IconSize)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = DesignColors.Secondary
        )
    }
}

@Composable
private fun TodayActivityCard(
    dailyStats: Map<MessageCategory, Int>,
    dailyBlocked: Int
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
                ) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = null,
                        tint = DesignColors.Primary,
                        modifier = Modifier.size(DesignLayout.IconSize)
                    )
                    Text(
                        text = stringResource(R.string.stats_today_activity),
                        style = MaterialTheme.typography.titleMedium,
                        color = DesignColors.Primary
                    )
                }
                val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
                Text(
                    text = dateFormat.format(Date()),
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignColors.Secondary
                )
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
            ) {
                item {
                    CategoryStatCard(
                        category = stringResource(R.string.stats_blocked),
                        count = dailyBlocked,
                        icon = Icons.Default.Block,
                        color = DesignColors.Accent
                    )
                }
                
                items(MessageCategory.values().toList()) { category ->
                    val count = dailyStats[category] ?: 0
                    CategoryStatCard(
                        category = category.name,
                        count = count,
                        icon = getCategoryIcon(category),
                        color = getCategoryColor(category)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryStatCard(
    category: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.width(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(DesignBorderRadius.SM)
    ) {
        Column(
            modifier = Modifier.padding(DesignSpacing.SM),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.XS)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(DesignLayout.IconSize)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = color
            )
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                color = DesignColors.Secondary
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(dailyStats: Map<MessageCategory, Int>) {
    val totalMessages = dailyStats.values.sum()
    
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
                    Icons.Default.PieChart,
                    contentDescription = null,
                    tint = DesignColors.Primary,
                    modifier = Modifier.size(DesignLayout.IconSize)
                )
                Text(
                    text = stringResource(R.string.stats_category_breakdown),
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignColors.Primary
                )
            }
            
            if (totalMessages > 0) {
                dailyStats.entries.sortedByDescending { it.value }.forEach { (category, count) ->
                    if (count > 0) {
                        val percentage = (count.toFloat() / totalMessages * 100).toInt()
                        CategoryPercentageRow(
                            category = category,
                            count = count,
                            percentage = percentage
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.stats_no_messages_today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DesignColors.Secondary,
                    modifier = Modifier.padding(vertical = DesignSpacing.MD)
                )
            }
        }
    }
}

@Composable
private fun CategoryPercentageRow(
    category: MessageCategory,
    count: Int,
    percentage: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesignSpacing.XS)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
            ) {
                Icon(
                    getCategoryIcon(category),
                    contentDescription = null,
                    modifier = Modifier.size(DesignLayout.IconSize),
                    tint = getCategoryColor(category)
                )
                Text(
                                            text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DesignColors.Primary
                )
            }
            Text(
                text = "$count ($percentage%)",
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.Secondary
            )
        }
        
        Spacer(modifier = Modifier.height(DesignSpacing.XS))
        
        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = getCategoryColor(category),
            trackColor = getCategoryColor(category).copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun PerformanceMetricsCard(
    totalFiltered: Long,
    totalBlocked: Long,
    dailyStats: Map<MessageCategory, Int>
) {
    val blockRate = if (totalFiltered > 0) {
        (totalBlocked.toFloat() / totalFiltered * 100).toInt()
    } else 0
    
    val todayTotal = dailyStats.values.sum()
    val todayBlocked = dailyStats[MessageCategory.JUNK] ?: 0
    val todayBlockRate = if (todayTotal > 0) {
        (todayBlocked.toFloat() / todayTotal * 100).toInt()
    } else 0
    
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
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = DesignColors.Primary,
                    modifier = Modifier.size(DesignLayout.IconSize)
                )
                Text(
                    text = stringResource(R.string.stats_performance_metrics),
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignColors.Primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = stringResource(R.string.stats_overall_block_rate),
                    value = "$blockRate%",
                    icon = Icons.Default.Security
                )
                MetricItem(
                    label = stringResource(R.string.stats_today_block_rate),
                    value = "$todayBlockRate%",
                    icon = Icons.Default.Security
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignSpacing.XS)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DesignColors.Accent,
            modifier = Modifier.size(DesignLayout.IconSize)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = DesignColors.Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = DesignColors.Secondary
        )
    }
}

@Composable
private fun FilterEffectivenessCard(preferencesManager: PreferencesManager) {
    val isMlEnabled = preferencesManager.isMlFilteringEnabled()
    val isKeywordEnabled = preferencesManager.isKeywordFilteringEnabled()
    val isRegexEnabled = preferencesManager.isRegexFilteringEnabled()
    val isUnderAttackMode = preferencesManager.isUnderAttackMode()
    
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
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = DesignColors.Primary,
                    modifier = Modifier.size(DesignLayout.IconSize)
                )
                Text(
                    text = stringResource(R.string.stats_filter_status),
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignColors.Primary
                )
            }
            
            FilterStatusRow(stringResource(R.string.stats_filter_ai), isMlEnabled)
            FilterStatusRow(stringResource(R.string.stats_filter_keyword), isKeywordEnabled)
            FilterStatusRow(stringResource(R.string.stats_filter_regex), isRegexEnabled)
            FilterStatusRow(stringResource(R.string.settings_under_attack_mode), isUnderAttackMode)
        }
    }
}

@Composable
private fun FilterStatusRow(
    feature: String,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            color = DesignColors.Primary
        )
        Icon(
            if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isEnabled) DesignColors.Accent else DesignColors.Secondary,
            modifier = Modifier.size(DesignLayout.IconSize)
        )
    }
}

// Helper functions
private fun getCategoryIcon(category: MessageCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        MessageCategory.GENERAL -> Icons.Default.Person
        MessageCategory.PROMOTION -> Icons.Default.LocalOffer
        MessageCategory.NOTIFICATION -> Icons.Default.Notifications
        MessageCategory.TRANSACTION -> Icons.Default.AccountBalance
        MessageCategory.JUNK -> Icons.Default.Delete
        MessageCategory.ALLOWED -> Icons.Default.Verified
    }
}

private fun getCategoryColor(category: MessageCategory): Color {
    return when (category) {
        MessageCategory.GENERAL -> DesignColors.Primary
        MessageCategory.PROMOTION -> DesignColors.Accent
        MessageCategory.NOTIFICATION -> DesignColors.Secondary
        MessageCategory.TRANSACTION -> DesignColors.Primary
        MessageCategory.JUNK -> DesignColors.Accent
        MessageCategory.ALLOWED -> DesignColors.AllowedMessage
    }
} 