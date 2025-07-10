package com.ovehbe.junkboy.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovehbe.junkboy.database.AppDatabase
import com.ovehbe.junkboy.database.MessageCategory
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
    
    LaunchedEffect(Unit) {
        totalFiltered = preferencesManager.getTotalMessagesFiltered()
        totalBlocked = preferencesManager.getTotalMessagesBlocked()
        dailyBlocked = preferencesManager.getDailyBlockedCount()
        
        dailyStats = MessageCategory.values().associateWith { category ->
            preferencesManager.getDailyCategoryCount(category)
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
                text = "Statistics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            // Overall Stats Card
            OverallStatsCard(
                totalFiltered = totalFiltered,
                totalBlocked = totalBlocked
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
    totalBlocked: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Total Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Messages Filtered",
                    value = totalFiltered.toString(),
                    icon = Icons.Default.FilterList,
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "Junk Blocked",
                    value = totalBlocked.toString(),
                    icon = Icons.Default.Block,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun TodayActivityCard(
    dailyStats: Map<MessageCategory, Int>,
    dailyBlocked: Int
) {
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
                Text(
                    text = "Today's Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                Text(
                    text = dateFormat.format(Date()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    CategoryStatCard(
                        category = "Blocked",
                        count = dailyBlocked,
                        icon = Icons.Default.Block,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                items(MessageCategory.values().toList()) { category ->
                    val count = dailyStats[category] ?: 0
                    CategoryStatCard(
                        category = category.name.lowercase().replaceFirstChar { it.uppercase() },
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
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(dailyStats: Map<MessageCategory, Int>) {
    val totalMessages = dailyStats.values.sum()
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
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
                    text = "No messages to analyze today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
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
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    getCategoryIcon(category),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = getCategoryColor(category)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "$count ($percentage%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = getCategoryColor(category),
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        )
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PerformanceMetricsCard(
    totalFiltered: Long,
    totalBlocked: Long,
    dailyStats: Map<MessageCategory, Int>
) {
    val junkDetectionRate = if (totalFiltered > 0) {
        (totalBlocked.toFloat() / totalFiltered * 100).toInt()
    } else 0
    
    val todayTotal = dailyStats.values.sum()
    val todayJunk = dailyStats[MessageCategory.JUNK] ?: 0
    val todayDetectionRate = if (todayTotal > 0) {
        (todayJunk.toFloat() / todayTotal * 100).toInt()
    } else 0
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Overall Junk Rate",
                    value = "$junkDetectionRate%",
                    icon = Icons.Default.Analytics,
                    color = MaterialTheme.colorScheme.primary
                )
                MetricItem(
                    label = "Today's Junk Rate",
                    value = "$todayDetectionRate%",
                    icon = Icons.Default.TrendingUp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun FilterEffectivenessCard(preferencesManager: PreferencesManager) {
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
            Spacer(modifier = Modifier.height(16.dp))
            
            val filters = listOf(
                "AI Classification" to preferencesManager.isMlFilteringEnabled(),
                "Keyword Filtering" to preferencesManager.isKeywordFilteringEnabled(),
                "Regex Filtering" to preferencesManager.isRegexFilteringEnabled(),
                "Under Attack Mode" to preferencesManager.isUnderAttackMode()
            )
            
            filters.forEach { (name, enabled) ->
                FilterStatusRow(name, enabled)
            }
        }
    }
}

@Composable
private fun FilterStatusRow(name: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (enabled) "Active" else "Disabled",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
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
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun getCategoryIcon(category: MessageCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        MessageCategory.GENERAL -> Icons.Default.Message
        MessageCategory.PROMOTION -> Icons.Default.LocalOffer
        MessageCategory.NOTIFICATION -> Icons.Default.Notifications
        MessageCategory.TRANSACTION -> Icons.Default.AccountBalance
        MessageCategory.JUNK -> Icons.Default.Delete
    }
}

@Composable
private fun getCategoryColor(category: MessageCategory): Color {
    return when (category) {
        MessageCategory.GENERAL -> MaterialTheme.colorScheme.outline
        MessageCategory.PROMOTION -> MaterialTheme.colorScheme.tertiary
        MessageCategory.NOTIFICATION -> MaterialTheme.colorScheme.secondary
        MessageCategory.TRANSACTION -> MaterialTheme.colorScheme.primary
        MessageCategory.JUNK -> MaterialTheme.colorScheme.error
    }
} 