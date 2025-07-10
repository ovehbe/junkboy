package com.ovehbe.junkboy.smsreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "MmsReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "MMS received, action: ${intent.action}")
        
        // For now, we'll just log MMS messages
        // In a full implementation, you'd process MMS messages here
        // This is mainly needed to qualify as a default SMS app
        
        try {
            when (intent.action) {
                "android.provider.Telephony.WAP_PUSH_DELIVER" -> {
                    Log.d(TAG, "MMS WAP push received")
                    // Handle MMS delivery
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing MMS", e)
        }
    }
} 