package com.ovehbe.junkboy.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ovehbe.junkboy.R
import com.ovehbe.junkboy.classifier.SmsClassifier
import com.ovehbe.junkboy.database.AppDatabase
import com.ovehbe.junkboy.database.FilteredMessage
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.filters.CustomFilter
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.NotificationHelper
import com.ovehbe.junkboy.utils.SmsDeleter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*

class SmsFilterService : Service() {
    
    companion object {
        private const val TAG = "SmsFilterService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_filter_service"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var smsClassifier: SmsClassifier
    private lateinit var smsDeleter: SmsDeleter
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SmsFilterService created")
        
        // Initialize components
        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        notificationHelper = NotificationHelper(this)
        smsClassifier = SmsClassifier.getInstance()
        smsDeleter = SmsDeleter(this)
        
        // Initialize ML classifier
        serviceScope.launch {
            if (!smsClassifier.initialize(this@SmsFilterService)) {
                Log.w(TAG, "ML classifier initialization failed, using rule-based filtering only")
            }
        }
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with intent: $intent")
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createServiceNotification())
        
        // Process the SMS if intent contains message data
        intent?.let { processIncomingSms(it) }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun processIncomingSms(intent: Intent) {
        val sender = intent.getStringExtra("sender") ?: return
        val message = intent.getStringExtra("message") ?: return
        val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
        val isObviousJunk = intent.getBooleanExtra("is_obvious_junk", false)
        
        serviceScope.launch {
            try {
                Log.d(TAG, "Processing SMS from $sender")
                
                // Check if sender is in allowed list - if so, don't filter
                val isAllowed = database.allowedSenderDao().isAllowedSender(sender)
                if (isAllowed) {
                    Log.d(TAG, "Sender $sender is in allowed list, skipping filtering")
                    
                    // Create record but don't block
                    val filteredMessage = FilteredMessage(
                        sender = sender,
                        messageBody = message,
                        receivedAt = Date(timestamp),
                        category = MessageCategory.GENERAL,
                        confidence = 0.0f,
                        filterType = com.ovehbe.junkboy.database.FilterType.KEYWORD_FILTER,
                        isBlocked = false,
                        isUserOverride = true, // Mark as user override since it's manually allowed
                        isRead = false
                    )
                    
                    database.filteredMessageDao().insertMessage(filteredMessage)
                    stopSelf()
                    return@launch
                }
                
                // Get user preferences
                val isUnderAttackMode = preferencesManager.isUnderAttackMode()
                val isMlEnabled = preferencesManager.isMlFilteringEnabled()
                val isKeywordEnabled = preferencesManager.isKeywordFilteringEnabled()
                val isRegexEnabled = preferencesManager.isRegexFilteringEnabled()
                val customKeywords = preferencesManager.getCustomKeywords()
                val customRegexPatterns = preferencesManager.getCustomRegexPatterns()
                
                // If already marked as obvious junk by receiver, create blocked result
                var filterResult = if (isObviousJunk) {
                    com.ovehbe.junkboy.filters.FilterResult(
                        isBlocked = true,
                        category = MessageCategory.JUNK,
                        filterType = com.ovehbe.junkboy.database.FilterType.KEYWORD_FILTER,
                        confidence = 0.95f,
                        matchedRule = "obvious_junk_receiver"
                    )
                } else {
                    // Determine primary classification method based on user preferences and implement hierarchy
                    when {
                        isMlEnabled -> {
                            // ML is enabled - use ML as primary classifier
                            val mlResult = smsClassifier.classify(message)
                            
                            // Also run rule-based filtering for comparison if enabled
                            val ruleResult = if (isKeywordEnabled || isRegexEnabled) {
                                CustomFilter.filterMessage(
                                    message = message,
                                    sender = sender,
                                    isUnderAttackMode = isUnderAttackMode,
                                    customKeywords = if (isKeywordEnabled) customKeywords else emptyList(),
                                    customRegexPatterns = if (isRegexEnabled) customRegexPatterns else emptyList()
                                )
                            } else null
                            
                            // Implement hierarchy: ML has precedence, rules can only enhance confidence
                            if (ruleResult != null) {
                                // When ML is enabled, always preserve ML_CLASSIFICATION FilterType
                                // But respect blocking decisions from both methods
                                when {
                                    // If either method wants to block, respect the blocking decision
                                    mlResult.isBlocked || ruleResult.isBlocked -> {
                                        // Use the blocking result with higher confidence, but keep ML FilterType
                                        if (mlResult.isBlocked && ruleResult.isBlocked) {
                                            // Both want to block - use higher confidence
                                            if (mlResult.confidence >= ruleResult.confidence) {
                                                mlResult.copy(
                                                    matchedRule = "ml_primary_block:${mlResult.matchedRule ?: "ml_default"}"
                                                )
                                            } else {
                                                mlResult.copy(
                                                    isBlocked = true,
                                                    category = ruleResult.category,
                                                    confidence = ruleResult.confidence,
                                                    matchedRule = "ml_with_rule_block:${ruleResult.matchedRule}"
                                                )
                                            }
                                        } else if (mlResult.isBlocked) {
                                            // Only ML wants to block
                                            mlResult.copy(
                                                matchedRule = "ml_block:${mlResult.matchedRule ?: "ml_default"}"
                                            )
                                        } else {
                                            // Only rule wants to block - use rule decision but mark as ML classification
                                            mlResult.copy(
                                                isBlocked = true,
                                                category = ruleResult.category,
                                                confidence = ruleResult.confidence,
                                                matchedRule = "ml_enhanced_by_rule_block:${ruleResult.matchedRule}"
                                            )
                                        }
                                    }
                                    
                                    // Neither wants to block - use ML result for categorization but consider rule confidence
                                    else -> {
                                        // For non-blocking categorization, use the result with higher confidence
                                        // but always preserve ML FilterType
                                        if (mlResult.confidence >= ruleResult.confidence) {
                                            mlResult.copy(
                                                matchedRule = "ml_primary:${mlResult.matchedRule ?: "ml_default"}"
                                            )
                                        } else {
                                            // Rule has better categorization confidence - use it but mark as ML
                                            mlResult.copy(
                                                category = ruleResult.category,
                                                confidence = ruleResult.confidence,
                                                matchedRule = "ml_with_rule_category:${ruleResult.matchedRule}"
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Only ML is enabled, use ML result
                                mlResult
                            }
                        }
                        isKeywordEnabled || isRegexEnabled -> {
                            // ML disabled, use rule-based filtering only
                            CustomFilter.filterMessage(
                                message = message,
                                sender = sender,
                                isUnderAttackMode = isUnderAttackMode,
                                customKeywords = if (isKeywordEnabled) customKeywords else emptyList(),
                                customRegexPatterns = if (isRegexEnabled) customRegexPatterns else emptyList()
                            )
                        }
                        else -> {
                            // No filtering enabled, neutral result
                            com.ovehbe.junkboy.filters.FilterResult(
                                isBlocked = false,
                                category = MessageCategory.GENERAL,
                                filterType = com.ovehbe.junkboy.database.FilterType.KEYWORD_FILTER,
                                confidence = 0.0f
                            )
                        }
                    }
                }
                
                // Create filtered message record
                val filteredMessage = FilteredMessage(
                    sender = sender,
                    messageBody = message,
                    receivedAt = Date(timestamp),
                    category = filterResult.category,
                    confidence = filterResult.confidence,
                    filterType = filterResult.filterType,
                    isBlocked = filterResult.isBlocked,
                    isUserOverride = false,
                    isRead = false
                )
                
                // Save to database
                val messageId = database.filteredMessageDao().insertMessage(filteredMessage)
                
                Log.d(TAG, "Message classified as ${filterResult.category} (blocked: ${filterResult.isBlocked}, confidence: ${filterResult.confidence})")
                
                // Show notification - NotificationHelper now handles all category-specific logic
                notificationHelper.showSmsNotification(
                    filteredMessage.copy(id = messageId),
                    filterResult.matchedRule
                )
                
                // Auto-delete junk if enabled and app is default SMS app
                if (filterResult.isBlocked && preferencesManager.isAutoDeleteJunkEnabled()) {
                    try {
                        val deleted = smsDeleter.deleteJunkSms(sender, message, timestamp)
                        if (deleted) {
                            Log.i(TAG, "Auto-deleted junk SMS from system database")
                            // Archive the deleted message
                            smsDeleter.archiveDeletedMessage(filteredMessage.copy(id = messageId))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during auto-delete", e)
                    }
                }
                
                // Log the blocking action for obvious junk
                if (isObviousJunk) {
                    Log.i(TAG, "Successfully blocked obvious junk message from $sender - broadcast aborted")
                }
                
                // Update statistics
                updateFilteringStats(filterResult.category, filterResult.isBlocked)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            } finally {
                // Stop service after processing
                stopSelf()
            }
        }
    }
    
    private fun updateFilteringStats(category: MessageCategory, isBlocked: Boolean) {
        serviceScope.launch {
            try {
                // Update daily statistics
                preferencesManager.incrementCategoryCount(category)
                if (isBlocked) {
                    preferencesManager.incrementBlockedCount()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating stats", e)
            }
        }
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SMS Filter Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background SMS filtering service"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createServiceNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Junkboy SMS Filter")
            .setContentText("Filtering incoming SMS messages")
            .setSmallIcon(R.drawable.ic_filter_list)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SmsFilterService destroyed")
    }
} 