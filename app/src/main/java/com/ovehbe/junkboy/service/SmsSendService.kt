package com.ovehbe.junkboy.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class SmsSendService : Service() {
    
    companion object {
        private const val TAG = "SmsSendService"
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SMS send service started")
        
        // This service is required for default SMS app qualification
        // It handles "respond via message" functionality
        // For now, we'll just log and stop the service
        
        try {
            when (intent?.action) {
                "android.intent.action.RESPOND_VIA_MESSAGE" -> {
                    Log.d(TAG, "Respond via message request received")
                    // In a full implementation, you'd handle quick replies here
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in SMS send service", e)
        } finally {
            stopSelf()
        }
        
        return START_NOT_STICKY
    }
} 