package com.ovehbe.junkboy.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ovehbe.junkboy.R
import com.ovehbe.junkboy.database.FilteredMessage
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.ui.MainActivity
import kotlin.random.Random

class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID_BLOCKED = "blocked_messages"
        private const val CHANNEL_ID_TRANSACTION = "transaction_messages"
        private const val CHANNEL_ID_PROMOTION = "promotion_messages"
        private const val CHANNEL_ID_NOTIFICATION = "notification_messages"
        private const val CHANNEL_ID_GENERAL = "general_messages"
        private const val CHANNEL_ID_STATS = "daily_stats"
    }
    
    init {
        createNotificationChannels()
    }
    
    fun showSmsNotification(message: FilteredMessage, matchedRule: String?) {
        // Check for notification permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        // Get preferences manager to check individual category settings
        val preferencesManager = PreferencesManager(context)
        
        // Check if notifications should be shown for this specific category
        val shouldShowForCategory = when {
            message.isBlocked -> {
                // For blocked messages, check blocked message notification setting
                preferencesManager.shouldNotifyBlockedMessages()
            }
            else -> {
                // For categorized (non-blocked) messages, check both general and specific category settings
                val notifyCategorizedEnabled = preferencesManager.shouldNotifyCategorizedMessages()
                val notifyAllEnabled = preferencesManager.shouldNotifyAllFiltered()
                
                // If general categorized notifications are enabled, check specific category preference
                if (notifyCategorizedEnabled) {
                    when (message.category) {
                        MessageCategory.GENERAL -> preferencesManager.shouldNotifyGeneral()
                        MessageCategory.PROMOTION -> preferencesManager.shouldNotifyPromotion()
                        MessageCategory.NOTIFICATION -> preferencesManager.shouldNotifyNotification()
                        MessageCategory.TRANSACTION -> preferencesManager.shouldNotifyTransaction()
                        MessageCategory.JUNK -> false // Junk should use blocked message setting instead
                    }
                } else {
                    // If categorized notifications are disabled, check if "notify all" is enabled
                    notifyAllEnabled
                }
            }
        }
        
        // Only proceed if notifications are enabled for this category
        if (!shouldShowForCategory) {
            return
        }
        
        val categoryInfo = getCategoryInfo(message.category, message.isBlocked)
        val channelId = categoryInfo.channelId
        
        // Create intent to open SMS conversation in default SMS app
        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:${message.sender}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val smsPendingIntent = PendingIntent.getActivity(
            context, 
            Random.nextInt(),
            smsIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create intent to open Junkboy app
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "messages")
            putExtra("category", message.category.name)
        }
        
        val appPendingIntent = PendingIntent.getActivity(
            context, 
            Random.nextInt(),
            appIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Format message content
        val shortContent = if (message.messageBody.length > 40) {
            message.messageBody.take(37) + "..."
        } else {
            message.messageBody
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(categoryInfo.icon)
            .setContentTitle(categoryInfo.title)
            .setContentText("${message.sender}: $shortContent")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(message.messageBody)
                .setSummaryText("From: ${message.sender}"))
            .setPriority(categoryInfo.priority)
            .setContentIntent(smsPendingIntent) // Main action: open SMS app
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(message.receivedAt.time)
            .setOnlyAlertOnce(true)
            .apply {
                // Add action to open Junkboy app
                addAction(
                    R.drawable.ic_filter_list, 
                    "Open Junkboy", 
                    appPendingIntent
                )
                
                // Add category-specific actions
                when {
                    message.isBlocked -> {
                        // Allow sender action for blocked messages
                        val allowIntent = Intent(context, MainActivity::class.java).apply {
                            putExtra("action", "allow_sender")
                            putExtra("sender", message.sender)
                            putExtra("message_id", message.id)
                        }
                        val allowPendingIntent = PendingIntent.getActivity(
                            context,
                            Random.nextInt(),
                            allowIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        addAction(R.drawable.ic_check, "Allow Sender", allowPendingIntent)
                    }
                    message.category == MessageCategory.TRANSACTION -> {
                        // Mark as read action for important transactions
                        addAction(R.drawable.ic_check, "Mark Read", appPendingIntent)
                    }
                }
                
                // Add confidence info for debugging
                if (message.confidence > 0) {
                    setSubText("${(message.confidence * 100).toInt()}% confidence")
                }
            }
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            if (areNotificationsEnabled()) {
                notify(Random.nextInt(), notification)
            }
        }
    }
    
    fun showDailyStatsNotification(
        totalFiltered: Int,
        totalBlocked: Int,
        categoryBreakdown: Map<MessageCategory, Int>
    ) {
        if (totalFiltered == 0) return
        
        // Check for notification permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "stats")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(buildString {
                appendLine("📊 Today's SMS filtering summary:")
                appendLine()
                appendLine("🛡️ Total filtered: $totalFiltered")
                appendLine("🚫 Blocked (junk): $totalBlocked")
                appendLine()
                categoryBreakdown.forEach { (category, count) ->
                    if (count > 0) {
                        val emoji = getCategoryEmoji(category)
                        val name = getCategoryDisplayName(category)
                        appendLine("$emoji $name: $count")
                    }
                }
            })
            .setSummaryText("Daily filtering report")
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_STATS)
            .setSmallIcon(R.drawable.ic_analytics)
            .setContentTitle("Junkboy Daily Report")
            .setContentText("Filtered $totalFiltered messages, blocked $totalBlocked junk")
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            if (areNotificationsEnabled()) {
                notify(9999, notification) // Fixed ID for daily stats
            }
        }
    }
    
    private fun createNotificationChannels() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Blocked messages channel (minimal priority)
        val blockedChannel = NotificationChannel(
            CHANNEL_ID_BLOCKED,
            "Blocked Messages",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Junk messages that were blocked"
            enableVibration(false)
            setShowBadge(false)
            setSound(null, null)
        }
        
        // Transaction messages channel (high priority)
        val transactionChannel = NotificationChannel(
            CHANNEL_ID_TRANSACTION,
            "Transaction Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Banking and payment messages"
            enableVibration(true)
            setShowBadge(true)
        }
        
        // Promotion messages channel (low priority)
        val promotionChannel = NotificationChannel(
            CHANNEL_ID_PROMOTION,
            "Promotion Messages",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Marketing and promotional messages"
            enableVibration(false)
            setShowBadge(false)
        }
        
        // Notification messages channel (default priority)
        val notificationChannel = NotificationChannel(
            CHANNEL_ID_NOTIFICATION,
            "Notification Messages",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "System and app notifications via SMS"
            enableVibration(true)
            setShowBadge(true)
        }
        
        // General messages channel (default priority)
        val generalChannel = NotificationChannel(
            CHANNEL_ID_GENERAL,
            "General Messages",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Personal and other SMS messages"
            enableVibration(true)
            setShowBadge(true)
        }
        
        // Daily stats channel
        val statsChannel = NotificationChannel(
            CHANNEL_ID_STATS,
            "Daily Statistics",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Daily filtering statistics and reports"
            enableVibration(false)
            setShowBadge(false)
            setSound(null, null)
        }
        
        notificationManager.createNotificationChannels(listOf(
            blockedChannel, 
            transactionChannel, 
            promotionChannel, 
            notificationChannel, 
            generalChannel, 
            statsChannel
        ))
    }
    
    private fun getCategoryInfo(category: MessageCategory, isBlocked: Boolean): CategoryInfo {
        return when {
            isBlocked -> CategoryInfo(
                channelId = CHANNEL_ID_BLOCKED,
                title = "Blocked: Junk SMS",
                icon = R.drawable.ic_block,
                priority = NotificationCompat.PRIORITY_MIN
            )
            category == MessageCategory.TRANSACTION -> CategoryInfo(
                channelId = CHANNEL_ID_TRANSACTION,
                title = "New Transaction Message",
                icon = R.drawable.ic_account_balance,
                priority = NotificationCompat.PRIORITY_HIGH
            )
            category == MessageCategory.PROMOTION -> CategoryInfo(
                channelId = CHANNEL_ID_PROMOTION,
                title = "New Promotion",
                icon = R.drawable.ic_local_offer,
                priority = NotificationCompat.PRIORITY_LOW
            )
            category == MessageCategory.NOTIFICATION -> CategoryInfo(
                channelId = CHANNEL_ID_NOTIFICATION,
                title = "New Notification",
                icon = R.drawable.ic_notifications,
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
            else -> CategoryInfo(
                channelId = CHANNEL_ID_GENERAL,
                title = "New Message",
                icon = R.drawable.ic_message,
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
        }
    }

    private fun getCategoryDisplayName(category: MessageCategory): String {
        return when (category) {
            MessageCategory.GENERAL -> "General"
            MessageCategory.PROMOTION -> "Promotion"
            MessageCategory.NOTIFICATION -> "Notification"
            MessageCategory.TRANSACTION -> "Transaction"
            MessageCategory.JUNK -> "Junk"
        }
    }
    
    private fun getCategoryEmoji(category: MessageCategory): String {
        return when (category) {
            MessageCategory.GENERAL -> "💬"
            MessageCategory.PROMOTION -> "🏷️"
            MessageCategory.NOTIFICATION -> "🔔"
            MessageCategory.TRANSACTION -> "💳"
            MessageCategory.JUNK -> "🗑️"
        }
    }
    
    private data class CategoryInfo(
        val channelId: String,
        val title: String,
        val icon: Int,
        val priority: Int
    )
} 