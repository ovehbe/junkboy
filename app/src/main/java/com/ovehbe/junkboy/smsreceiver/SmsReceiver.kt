package com.ovehbe.junkboy.smsreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.ovehbe.junkboy.service.SmsFilterService
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.SmsAppManager

class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"

        fun shouldProcessAction(action: String?, isDefaultSmsApp: Boolean): Boolean {
            return when (action) {
                Telephony.Sms.Intents.SMS_DELIVER_ACTION -> isDefaultSmsApp
                Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> true
                else -> false
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS received, action: ${intent.action}")
        
        val action = intent.action
        val isDefaultSmsApp = SmsAppManager(context).isJunkboyDefaultSmsApp()
        if (!shouldProcessAction(action, isDefaultSmsApp)) {
            return
        }
        
        try {
            // Extract SMS messages (PDU parts) from intent
            val messageParts = extractSmsMessages(intent)
            val preferencesManager = PreferencesManager(context)
            
            if (messageParts.isNotEmpty()) {
                // Combine all PDU parts into a single message
                // Multipart SMS messages arrive as multiple PDUs in the same broadcast
                val sender = messageParts[0].originatingAddress ?: "Unknown"
                val timestamp = messageParts[0].timestampMillis
                
                // Concatenate all message parts in order
                val fullMessageBody = messageParts
                    .sortedBy { it.indexOnIcc } // Sort by index to ensure correct order
                    .mapNotNull { it.messageBody }
                    .joinToString("") // Join without separator - parts are already properly split
                
                Log.d(TAG, "Processing SMS from $sender (${messageParts.size} parts): ${fullMessageBody.take(50)}...")
                
                // Quick junk detection for immediate blocking
                var shouldBlockBroadcast = false
                if (isObviousJunk(fullMessageBody, preferencesManager)) {
                    Log.d(TAG, "Blocking obvious junk message from $sender")
                    shouldBlockBroadcast = true
                }
                
                // Start the filter service to process this message ONCE with the complete body
                val serviceIntent = Intent(context, SmsFilterService::class.java).apply {
                    putExtra("sender", sender)
                    putExtra("message", fullMessageBody)
                    putExtra("timestamp", timestamp)
                    putExtra("is_obvious_junk", shouldBlockBroadcast)
                    putExtra("broadcast_action", action)
                }
                
                // Start as foreground service for reliable processing
                context.startForegroundService(serviceIntent)
                
                // Block the broadcast to prevent other apps from receiving obvious junk
                if (shouldBlockBroadcast && action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                    Log.d(TAG, "Aborting SMS broadcast to prevent notification from other apps")
                    abortBroadcast()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
        }
    }
    
    private fun extractSmsMessages(intent: Intent): Array<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        try {
            // Get PDUs from intent
            val pdus = intent.extras?.get("pdus") as? Array<*>
            val format = intent.extras?.getString("format")
            
            pdus?.forEach { pdu ->
                val smsMessage = if (format != null) {
                    SmsMessage.createFromPdu(pdu as ByteArray, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }
                
                if (smsMessage != null) {
                    messages.add(smsMessage)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting SMS messages", e)
        }
        
        return messages.toTypedArray()
    }
    
    /**
     * Quick junk detection for immediate blocking in the receiver.
     * This catches only obvious junk to prevent notification spam.
     * More complex classification is done in the service.
     */
    private fun isObviousJunk(messageBody: String, preferencesManager: PreferencesManager): Boolean {
        // Only block if filtering is enabled
        if (!preferencesManager.isKeywordFilteringEnabled() && !preferencesManager.isMlFilteringEnabled()) {
            return false
        }
        
        val lowerText = messageBody.lowercase()
        
        // Very high confidence junk indicators
        val obviousJunkPatterns = listOf(
            // Turkish spam patterns
            "tıklayınız", "tıkla ve kazan", "hediye kazan", "ödül kazan",
            "çekiliş", "şanslısın", "seçildin", "kazandın",
            "ücretsiz para", "para kazan", "bonus kazan",
            
            // English spam patterns  
            "click to win", "you have won", "congratulations winner",
            "claim your prize", "free money", "cash prize",
            "urgent action required", "act now", "limited time offer",
            
            // Suspicious URLs with common spam domains
            "bit.ly", "tinyurl.com", "t.co",
            
            // Casino/gambling terms
            "casino", "kumar", "bahis", "slot",
            
            // Get rich quick schemes
            "earn money fast", "hızlı para", "kolay kazanç"
        )
        
        // Check for multiple spam indicators (higher confidence)
        var spamScore = 0
        for (pattern in obviousJunkPatterns) {
            if (lowerText.contains(pattern)) {
                spamScore++
            }
        }
        
        // Block if message contains 2+ obvious spam indicators
        // or 1 very specific spam phrase
        val veryObviousSpam = listOf(
            "tıklayınız", "click to win", "you have won", "kazandın",
            "congratulations winner", "ücretsiz para", "free money"
        )
        
        return spamScore >= 2 || veryObviousSpam.any { lowerText.contains(it) }
    }
} 
