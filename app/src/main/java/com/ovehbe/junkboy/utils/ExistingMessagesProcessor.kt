package com.ovehbe.junkboy.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.ovehbe.junkboy.classifier.SmsClassifier
import com.ovehbe.junkboy.database.AppDatabase
import com.ovehbe.junkboy.database.FilteredMessage
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.filters.CustomFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class ExistingMessagesProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "ExistingMessagesProcessor"
    }
    
    private val database = AppDatabase.getDatabase(context)
    private val preferencesManager = PreferencesManager(context)
    private val smsClassifier = SmsClassifier.getInstance()
    
    suspend fun processAllExistingMessages(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting to process all existing messages")
            
            // Initialize ML classifier
            if (!smsClassifier.initialize(context)) {
                Log.w(TAG, "ML classifier initialization failed, using rule-based filtering only")
            }
            
            // Get all SMS messages from device
            val existingMessages = readAllSmsMessages()
            Log.d(TAG, "Found ${existingMessages.size} existing messages")
            
            var processedCount = 0
            
            // Process each message
            for (smsMessage in existingMessages) {
                try {
                    // Process message through filtering logic
                    val filteredMessage = processMessage(smsMessage)
                    database.filteredMessageDao().insertMessage(filteredMessage)
                    
                    // Update statistics for this message
                    updateStatistics(filteredMessage.category, filteredMessage.isBlocked)
                    
                    processedCount++
                    Log.d(TAG, "Processed message from ${smsMessage.sender}: ${smsMessage.body.take(50)}")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing message from ${smsMessage.sender}", e)
                }
            }
            
            Log.d(TAG, "Finished processing $processedCount existing messages")
            Result.success(processedCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing existing messages", e)
            Result.failure(e)
        }
    }
    
    private suspend fun readAllSmsMessages(): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        
        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )
        
        cursor?.use { c ->
            val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIndex = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            
            while (c.moveToNext()) {
                val address = c.getString(addressIndex) ?: "Unknown"
                val body = c.getString(bodyIndex) ?: ""
                val date = c.getLong(dateIndex)
                val type = c.getInt(typeIndex)
                
                // Only process received messages (type 1)
                if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                    messages.add(SmsMessage(address, body, date))
                }
            }
        }
        
        return messages
    }
    
    private suspend fun processMessage(smsMessage: SmsMessage): FilteredMessage {
        // Check if sender is in allowed list
        val isAllowed = database.allowedSenderDao().isAllowedSender(smsMessage.sender)
        if (isAllowed) {
            return FilteredMessage(
                sender = smsMessage.sender,
                messageBody = smsMessage.body,
                receivedAt = Date(smsMessage.timestamp),
                category = MessageCategory.GENERAL,
                confidence = 0.0f,
                filterType = com.ovehbe.junkboy.database.FilterType.KEYWORD_FILTER,
                isBlocked = false,
                isUserOverride = true,
                isRead = false
            )
        }
        
        // Get user preferences
        val isUnderAttackMode = preferencesManager.isUnderAttackMode()
        val isMlEnabled = preferencesManager.isMlFilteringEnabled()
        val isKeywordEnabled = preferencesManager.isKeywordFilteringEnabled()
        val isRegexEnabled = preferencesManager.isRegexFilteringEnabled()
        val customKeywords = preferencesManager.getCustomKeywords()
        val customRegexPatterns = preferencesManager.getCustomRegexPatterns()
        
        // Apply filtering logic (same hierarchy as SmsFilterService)
        val filterResult = when {
            isMlEnabled -> {
                // ML is enabled - use ML as primary classifier
                val mlResult = smsClassifier.classify(smsMessage.body)
                
                // Also run rule-based filtering for comparison if enabled
                val ruleResult = if (isKeywordEnabled || isRegexEnabled) {
                    CustomFilter.filterMessage(
                        message = smsMessage.body,
                        sender = smsMessage.sender,
                        isUnderAttackMode = isUnderAttackMode,
                        customKeywords = if (isKeywordEnabled) customKeywords else emptyList(),
                        customRegexPatterns = if (isRegexEnabled) customRegexPatterns else emptyList()
                    )
                } else null
                
                // Implement same hierarchy as SmsFilterService
                if (ruleResult != null) {
                    // When ML is enabled, always preserve ML_CLASSIFICATION FilterType
                    when {
                        // If either method wants to block, respect the blocking decision
                        mlResult.isBlocked || ruleResult.isBlocked -> {
                            if (mlResult.isBlocked && ruleResult.isBlocked) {
                                // Both want to block - use higher confidence
                                if (mlResult.confidence >= ruleResult.confidence) {
                                    mlResult.copy(matchedRule = "ml_primary_block")
                                } else {
                                    mlResult.copy(
                                        isBlocked = true,
                                        category = ruleResult.category,
                                        confidence = ruleResult.confidence,
                                        matchedRule = "ml_with_rule_block"
                                    )
                                }
                            } else if (mlResult.isBlocked) {
                                mlResult.copy(matchedRule = "ml_block")
                            } else {
                                mlResult.copy(
                                    isBlocked = true,
                                    category = ruleResult.category,
                                    confidence = ruleResult.confidence,
                                    matchedRule = "ml_enhanced_by_rule_block"
                                )
                            }
                        }
                        
                        // Neither wants to block - use ML result for categorization
                        else -> {
                            if (mlResult.confidence >= ruleResult.confidence) {
                                mlResult.copy(matchedRule = "ml_primary")
                            } else {
                                mlResult.copy(
                                    category = ruleResult.category,
                                    confidence = ruleResult.confidence,
                                    matchedRule = "ml_with_rule_category"
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
                    message = smsMessage.body,
                    sender = smsMessage.sender,
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
        
        return FilteredMessage(
            sender = smsMessage.sender,
            messageBody = smsMessage.body,
            receivedAt = Date(smsMessage.timestamp),
            category = filterResult.category,
            confidence = filterResult.confidence,
            filterType = filterResult.filterType,
            isBlocked = filterResult.isBlocked,
            isUserOverride = false,
            isRead = false
        )
    }
    
    private fun updateStatistics(category: MessageCategory, isBlocked: Boolean) {
        try {
            // Update total and category statistics
            preferencesManager.incrementCategoryCount(category)
            if (isBlocked) {
                preferencesManager.incrementBlockedCount()
            }
            
            Log.d(TAG, "Updated statistics for category: $category, blocked: $isBlocked")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating statistics", e)
        }
    }
    
    data class SmsMessage(
        val sender: String,
        val body: String,
        val timestamp: Long
    )
} 