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
import com.ovehbe.junkboy.filters.ContentAllowlist
import com.ovehbe.junkboy.filters.CustomFilter
import com.ovehbe.junkboy.filters.FilterResult
import com.ovehbe.junkboy.filters.Stop2EndDetector
import com.ovehbe.junkboy.filters.Stop2EndPolicy
import com.ovehbe.junkboy.llm.OpenRouterJudges
import com.ovehbe.junkboy.utils.AutoReplyEngine
import com.ovehbe.junkboy.utils.SenderHistoryChecker
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.NotificationHelper
import com.ovehbe.junkboy.utils.SmsDeleter
import com.ovehbe.junkboy.utils.OtpHelper
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
    private lateinit var otpHelper: OtpHelper
    private lateinit var autoReplyEngine: AutoReplyEngine
    private lateinit var senderHistoryChecker: SenderHistoryChecker
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SmsFilterService created")
        
        // Initialize components
        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        notificationHelper = NotificationHelper(this)
        smsClassifier = SmsClassifier.getInstance()
        smsDeleter = SmsDeleter(this)
        otpHelper = OtpHelper(this)
        autoReplyEngine = AutoReplyEngine.create(this)
        senderHistoryChecker = SenderHistoryChecker(this)
        
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
                // Use flexible matching: case-insensitive for text senders, and normalized for phone numbers
                val allowedBySender = isAllowedSenderFlexible(sender)
                val allowRule = if (allowedBySender) null else ContentAllowlist.firstMatch(preferencesManager.getAllowRules(), message)
                if (allowedBySender || allowRule != null) {
                    Log.d(TAG, if (allowedBySender) "Sender $sender is in allowed list, skipping filtering"
                        else "Message from $sender matches allow rule '${allowRule?.pattern}', skipping filtering")
                    
                    // Create record but don't block - use ALLOWED category
                    val filteredMessage = FilteredMessage(
                        sender = sender,
                        messageBody = message,
                        receivedAt = Date(timestamp),
                        category = MessageCategory.ALLOWED,
                        confidence = 1.0f,
                        filterType = if (allowedBySender) com.ovehbe.junkboy.database.FilterType.ALLOWED_SENDER
                            else com.ovehbe.junkboy.database.FilterType.ALLOWED_CONTENT,
                        isBlocked = false,
                        isUserOverride = true, // Mark as user override since it's manually allowed
                        isRead = false
                    )
                    
                    // Save to database and get message ID
                    val messageId = database.filteredMessageDao().insertMessage(filteredMessage)
                    
                    // IMPORTANT: Show notification for allow-listed senders
                    // They should bypass filtering but still show notifications
                    notificationHelper.showSmsNotification(
                        filteredMessage.copy(id = messageId),
                        if (allowedBySender) "allowed_sender" else "allowed_content"
                    )
                    
                    // Detect and copy OTP if present (works for allow-listed senders too)
                    otpHelper.detectAndCopyOtp(message, "SMS")
                    
                    Log.d(TAG, "Showed notification for allow-listed sender: $sender")
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
                
                // Stop2End: a closing "Reply STOP" directive marks marketing spam (stored silently);
                // a "STOP2END" elsewhere only triggers the reply if the message is independently spam
                val stop2End = evaluateStop2End(sender, message, filterResult)
                val isStop2End = stop2End.action == Stop2EndPolicy.Action.SUPPRESS_AND_REPLY
                if (isStop2End) {
                    filterResult = filterResult.copy(
                        isBlocked = true,
                        category = if (filterResult.category == MessageCategory.JUNK) MessageCategory.JUNK else MessageCategory.PROMOTION
                    )
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
                
                if (isStop2End) {
                    Log.i(TAG, "Stop2End spam from $sender stored silently")
                    autoReplyEngine.sendStopReply(sender, stop2End.reason)
                } else {
                    // Show notification - NotificationHelper now handles all category-specific logic
                    notificationHelper.showSmsNotification(
                        filteredMessage.copy(id = messageId),
                        filterResult.matchedRule
                    )
                    
                    // Detect and copy OTP if present (works regardless of category)
                    otpHelper.detectAndCopyOtp(message, "SMS")
                    
                    if (stop2End.action == Stop2EndPolicy.Action.REPLY_ONLY) {
                        autoReplyEngine.sendStopReply(sender, stop2End.reason)
                    }
                }
                
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
    
    /**
     * Stop2End pipeline: (1) known senders pass, (2) the message needs an opt-out directive and
     * must not contain a code, (3) the spam decision: the classifier if it flagged the message,
     * otherwise the OpenRouter second opinion if an API key is set. See [Stop2EndPolicy].
     */
    private suspend fun evaluateStop2End(sender: String, message: String, result: FilterResult): Stop2EndPolicy.Decision {
        val none = Stop2EndPolicy.Decision(Stop2EndPolicy.Action.NONE, "")
        if (!preferencesManager.isAutoReplyStopEnabled()) return none
        
        // 1. Contacts or anyone the user has texted (except numbers we only auto-replied to)
        if (senderHistoryChecker.isContact(sender)) return none
        val autoRepliedBefore = AutoReplyEngine.normalizeTarget(sender)
            ?.let { database.autoReplyLogDao().getLastReplyAt(it) } != null
        if (!autoRepliedBefore && senderHistoryChecker.hasOutboundHistory(sender)) return none
        
        // 2 and 3
        val apiKey = preferencesManager.getOpenRouterApiKey()
        return Stop2EndPolicy.decide(
            directive = Stop2EndDetector.detect(message),
            category = result.category,
            flaggedAsSpam = result.isBlocked ||
                result.category == MessageCategory.JUNK ||
                result.category == MessageCategory.PROMOTION,
            hasOtp = otpHelper.testOtpDetection(message) != null,
            requireSpamClassification = preferencesManager.isAutoReplyRequireSpam(),
            judge = if (apiKey.isBlank()) null else OpenRouterJudges.create(
                apiKey,
                preferencesManager.getOpenRouterModel(),
                preferencesManager.getOpenRouterModelKind(),
                preferencesManager.getDecisionCut().toDouble()
            ),
            sender = sender,
            body = message
        )
    }
    
    /**
     * Check if sender is in allowed list with flexible matching:
     * - Case-insensitive matching for text senders (e.g., "BANKNAME" matches "bankname")
     * - Normalized phone number matching (removes +, -, spaces, parentheses)
     * - Partial matching for phone numbers (last 10 digits)
     */
    private suspend fun isAllowedSenderFlexible(sender: String): Boolean {
        val dao = database.allowedSenderDao()
        
        // First try exact match
        if (dao.isAllowedSender(sender)) {
            return true
        }
        
        // Try case-insensitive match (for text senders like "BANKNAME")
        if (dao.isAllowedSenderCaseInsensitive(sender)) {
            return true
        }
        
        // For phone numbers, try normalized matching
        val normalizedSender = normalizePhoneNumber(sender)
        if (normalizedSender != sender && dao.isAllowedSenderCaseInsensitive(normalizedSender)) {
            return true
        }
        
        // Get all allowed senders and check with various normalizations
        val allowedSenders = dao.getAllowedSendersList()
        for (allowed in allowedSenders) {
            val normalizedAllowed = normalizePhoneNumber(allowed.phoneNumber)
            
            // Check if normalized versions match
            if (normalizedSender == normalizedAllowed) {
                return true
            }
            
            // Check if sender contains the allowed number or vice versa (partial match)
            if (normalizedSender.length >= 10 && normalizedAllowed.length >= 10) {
                val senderLast10 = normalizedSender.takeLast(10)
                val allowedLast10 = normalizedAllowed.takeLast(10)
                if (senderLast10 == allowedLast10) {
                    return true
                }
            }
            
            // Case-insensitive text match
            if (sender.equals(allowed.phoneNumber, ignoreCase = true)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Normalize phone number by removing common formatting characters
     */
    private fun normalizePhoneNumber(phone: String): String {
        return phone
            .replace(Regex("[+\\-()\\s]"), "")  // Remove +, -, (, ), spaces
            .replace(Regex("^0+"), "")           // Remove leading zeros
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