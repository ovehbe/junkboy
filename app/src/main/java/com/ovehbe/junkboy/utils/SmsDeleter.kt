package com.ovehbe.junkboy.utils

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.ovehbe.junkboy.database.AppDatabase
import com.ovehbe.junkboy.database.FilteredMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsDeleter(private val context: Context) {
    
    companion object {
        private const val TAG = "SmsDeleter"
    }
    
    private val database = AppDatabase.getDatabase(context)
    private val smsAppManager = SmsAppManager(context)
    
    /**
     * Delete a junk SMS from the system database and archive it locally
     */
    suspend fun deleteJunkSms(
        sender: String, 
        messageBody: String, 
        timestamp: Long
    ): Boolean = withContext(Dispatchers.IO) {
        
        // Only proceed if Junkboy is the default SMS app
        if (!smsAppManager.isJunkboyDefaultSmsApp()) {
            Log.w(TAG, "Cannot delete SMS: Junkboy is not the default SMS app")
            return@withContext false
        }
        
        return@withContext try {
            // Find and delete the SMS from system database
            val deletedCount = deleteSmsFromSystem(sender, messageBody, timestamp)
            
            if (deletedCount > 0) {
                Log.i(TAG, "Successfully deleted junk SMS from system database")
                true
            } else {
                Log.w(TAG, "SMS not found in system database for deletion")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting SMS from system", e)
            false
        }
    }
    
    /**
     * Delete SMS from the system content provider
     */
    private fun deleteSmsFromSystem(
        sender: String, 
        messageBody: String, 
        timestamp: Long
    ): Int {
        val uri = Telephony.Sms.CONTENT_URI
        
        // Build selection criteria to find the exact SMS
        val selection = "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.BODY} = ? AND ${Telephony.Sms.DATE} = ?"
        val selectionArgs = arrayOf(sender, messageBody, timestamp.toString())
        
        return try {
            val deletedRows = context.contentResolver.delete(uri, selection, selectionArgs)
            Log.d(TAG, "Deleted $deletedRows SMS(s) matching criteria")
            deletedRows
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied: Cannot delete SMS. App may not be default SMS app.", e)
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting SMS from content provider", e)
            0
        }
    }
    
    /**
     * Archive a deleted message in the local database with deleted flag
     */
    suspend fun archiveDeletedMessage(filteredMessage: FilteredMessage): Boolean {
        return try {
            // Create an archived version with deleted flag
            val archivedMessage = filteredMessage.copy(
                isBlocked = true, // Mark as blocked since it was junk
                isRead = false    // Keep as unread so user can review
            )
            
            database.filteredMessageDao().insertMessage(archivedMessage)
            Log.d(TAG, "Archived deleted junk message in local database")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error archiving deleted message", e)
            false
        }
    }
    
    /**
     * Get count of deleted messages from archive
     */
    suspend fun getDeletedMessageCount(): Int {
        return try {
            database.filteredMessageDao().getBlockedMessages().toString().length // This is a placeholder
            // TODO: Add proper query for deleted messages when we add deleted flag to schema
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting deleted message count", e)
            0
        }
    }
    
    /**
     * Check if auto-delete can be enabled (requires default SMS app)
     */
    fun canEnableAutoDelete(): Boolean {
        return smsAppManager.isJunkboyDefaultSmsApp()
    }
    
    /**
     * Get status message for auto-delete feature
     */
    fun getAutoDeleteStatus(): String {
        return when {
            smsAppManager.isJunkboyDefaultSmsApp() -> 
                "✅ Auto-delete available - Junkboy is your default SMS app"
            else -> 
                "⚠️ Requires Junkboy as default SMS app to automatically delete junk messages"
        }
    }
} 