package com.ovehbe.junkboy.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat

class SmsAppManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SmsAppManager"
        
        // Common SMS app package names
        private val KNOWN_SMS_APPS = mapOf(
            "com.google.android.apps.messaging" to "Messages",
            "com.samsung.android.messaging" to "Samsung Messages", 
            "com.android.mms" to "Messaging",
            "com.sonyericsson.conversations" to "Conversations",
            "com.htc.sense.mms" to "HTC Messages",
            "com.lge.message" to "LG Messages",
            "com.miui.mms" to "MIUI Messages",
            "com.oneplus.mms" to "OnePlus Messages"
        )
    }
    
    /**
     * Get the package name of the current default SMS app
     */
    fun getDefaultSmsPackage(): String? {
        return try {
            Telephony.Sms.getDefaultSmsPackage(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting default SMS package", e)
            null
        }
    }
    
    /**
     * Get the user-friendly name of the default SMS app
     */
    fun getDefaultSmsAppName(): String {
        val packageName = getDefaultSmsPackage()
        if (packageName == null) {
            return "Unknown SMS App"
        }
        
        // Check if it's a known app
        KNOWN_SMS_APPS[packageName]?.let { return it }
        
        // Try to get the app name from package manager
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Could not get app name for package: $packageName", e)
            "Default SMS App"
        }
    }
    
    /**
     * Check if Junkboy is the default SMS app
     */
    fun isJunkboyDefaultSmsApp(): Boolean {
        val defaultPackage = getDefaultSmsPackage()
        return defaultPackage == context.packageName
    }
    
    /**
     * Open notification settings for the default SMS app
     */
    fun openDefaultSmsAppNotificationSettings(): Boolean {
        val defaultSmsPackage = getDefaultSmsPackage()
        if (defaultSmsPackage == null) {
            Log.w(TAG, "No default SMS package found")
            return false
        }
        
        return try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, defaultSmsPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not open notification settings", e)
            // Fallback to general app settings
            openDefaultSmsAppSettings()
        }
    }
    
    /**
     * Open general app settings for the default SMS app (fallback)
     */
    private fun openDefaultSmsAppSettings(): Boolean {
        val defaultSmsPackage = getDefaultSmsPackage()
        if (defaultSmsPackage == null) {
            return false
        }
        
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$defaultSmsPackage")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not open app settings", e)
            false
        }
    }
    
    /**
     * Request to make Junkboy the default SMS app
     */
    fun requestDefaultSmsApp(): Boolean {
        return try {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not request default SMS app", e)
            false
        }
    }
    
    /**
     * Get SMS guidance message for the user
     */
    fun getSmsGuidanceMessage(): String {
        val defaultAppName = getDefaultSmsAppName()
        return when {
            isJunkboyDefaultSmsApp() -> {
                "✅ Junkboy is your default SMS app. All SMS notifications are handled by Junkboy."
            }
            else -> {
                "To avoid duplicate notifications, please mute notifications from \"$defaultAppName\" and let Junkboy handle all SMS alerts."
            }
        }
    }
    
    /**
     * Check if notification guidance is needed
     */
    fun isNotificationGuidanceNeeded(): Boolean {
        return !isJunkboyDefaultSmsApp()
    }
    
    /**
     * Open SMS conversation for a specific sender
     */
    fun openSmsConversation(senderNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$senderNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not open SMS conversation", e)
            false
        }
    }
} 