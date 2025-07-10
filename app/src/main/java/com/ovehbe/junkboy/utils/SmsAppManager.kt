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
            Log.d(TAG, "Attempting to request default SMS app for package: ${context.packageName}")
            Log.d(TAG, "Android version: ${android.os.Build.VERSION.SDK_INT}")
            Log.d(TAG, "Device manufacturer: ${android.os.Build.MANUFACTURER}")
            Log.d(TAG, "Device model: ${android.os.Build.MODEL}")
            
            // Check if the device supports default SMS app selection
            if (!isDefaultSmsAppChangeSupported()) {
                Log.e(TAG, "Default SMS app change not supported on this device")
                return false
            }
            
            // Check if all requirements are met
            val requirements = checkDefaultSmsAppRequirements()
            val unmetRequirements = requirements.filter { !it.value }
            if (unmetRequirements.isNotEmpty()) {
                Log.w(TAG, "Some requirements not met: ${unmetRequirements.keys}")
                // Continue anyway - the system will show an error if needed
            }
            
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Verify the intent can be resolved
            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo == null) {
                Log.e(TAG, "No activity found to handle default SMS app change intent")
                return false
            }
            
            Log.d(TAG, "Starting default SMS app selection activity")
            Log.d(TAG, "Intent details: $intent")
            Log.d(TAG, "Intent action: ${intent.action}")
            Log.d(TAG, "Intent package: ${intent.getStringExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME)}")
            
            context.startActivity(intent)
            Log.d(TAG, "Default SMS app selection activity started successfully")
            
            // Also try the alternative manual approach
            tryManualDefaultSmsSettings()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not request default SMS app", e)
            false
        }
    }
    
    /**
     * Try alternative method - open default app settings manually
     */
    private fun tryManualDefaultSmsSettings(): Boolean {
        return try {
            Log.d(TAG, "Trying manual default SMS app settings")
            
            // Method 1: Try opening default apps settings
            val defaultAppsIntent = Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (context.packageManager.resolveActivity(defaultAppsIntent, 0) != null) {
                Log.d(TAG, "Opening default apps settings")
                context.startActivity(defaultAppsIntent)
                return true
            }
            
            // Method 2: Try opening apps settings
            val appsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (context.packageManager.resolveActivity(appsIntent, 0) != null) {
                Log.d(TAG, "Opening apps settings")
                context.startActivity(appsIntent)
                return true
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error opening manual settings", e)
            false
        }
    }
    
    /**
     * Check if the device supports changing default SMS app
     */
    private fun isDefaultSmsAppChangeSupported(): Boolean {
        return try {
            // This method is available from API 19+
            Telephony.Sms.getDefaultSmsPackage(context)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Default SMS app functionality not available", e)
            false
        }
    }
    
    /**
     * Check if Junkboy meets all requirements to be a default SMS app
     */
    fun checkDefaultSmsAppRequirements(): Map<String, Boolean> {
        val requirements = mutableMapOf<String, Boolean>()
        
        try {
            val packageManager = context.packageManager
            val packageName = context.packageName
            
            // Check for required permissions
            requirements["RECEIVE_SMS"] = ContextCompat.checkSelfPermission(context, "android.permission.RECEIVE_SMS") == PackageManager.PERMISSION_GRANTED
            requirements["READ_SMS"] = ContextCompat.checkSelfPermission(context, "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED
            requirements["WRITE_SMS"] = ContextCompat.checkSelfPermission(context, "android.permission.WRITE_SMS") == PackageManager.PERMISSION_GRANTED
            requirements["SEND_SMS"] = ContextCompat.checkSelfPermission(context, "android.permission.SEND_SMS") == PackageManager.PERMISSION_GRANTED
            requirements["RECEIVE_MMS"] = ContextCompat.checkSelfPermission(context, "android.permission.RECEIVE_MMS") == PackageManager.PERMISSION_GRANTED
            requirements["RECEIVE_WAP_PUSH"] = ContextCompat.checkSelfPermission(context, "android.permission.RECEIVE_WAP_PUSH") == PackageManager.PERMISSION_GRANTED
            
            // Check for required components by querying intents
            val smsReceiverIntent = Intent("android.provider.Telephony.SMS_RECEIVED")
            val smsReceivers = packageManager.queryBroadcastReceivers(smsReceiverIntent, PackageManager.MATCH_ALL)
            requirements["SMS_RECEIVER"] = smsReceivers.any { it.activityInfo.packageName == packageName }
            
            val mmsReceiverIntent = Intent("android.provider.Telephony.WAP_PUSH_DELIVER").apply {
                type = "application/vnd.wap.mms-message"
            }
            val mmsReceivers = packageManager.queryBroadcastReceivers(mmsReceiverIntent, PackageManager.MATCH_ALL)
            requirements["MMS_RECEIVER"] = mmsReceivers.any { it.activityInfo.packageName == packageName }
            
            val respondViaMessageIntent = Intent("android.intent.action.RESPOND_VIA_MESSAGE").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                data = android.net.Uri.parse("sms:")
            }
            val respondServices = packageManager.queryIntentServices(respondViaMessageIntent, PackageManager.MATCH_ALL)
            requirements["RESPOND_VIA_MESSAGE_SERVICE"] = respondServices.any { it.serviceInfo.packageName == packageName }
            
            // Check for main activity with required intent filters
            val viewSmsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("sms:")
            }
            val viewActivities = packageManager.queryIntentActivities(viewSmsIntent, 0)
            requirements["VIEW_SMS_ACTIVITY"] = viewActivities.any { it.activityInfo.packageName == packageName }
            
            val sendToSmsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("sms:")
            }
            val sendToActivities = packageManager.queryIntentActivities(sendToSmsIntent, 0)
            requirements["SENDTO_SMS_ACTIVITY"] = sendToActivities.any { it.activityInfo.packageName == packageName }
            
            Log.d(TAG, "Default SMS app requirements check:")
            requirements.forEach { (requirement, met) ->
                Log.d(TAG, "  $requirement: ${if (met) "✓" else "✗"}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default SMS app requirements", e)
        }
        
        return requirements
    }
    
    /**
     * Get manual instructions for setting default SMS app
     */
    fun getManualDefaultSmsInstructions(): String {
        return """
            Since the automatic method didn't work, please set Junkboy as default SMS app manually:
            
            📱 Method 1 - Android Settings:
            1. Open Android Settings
            2. Go to Apps → Default Apps → SMS app
            3. Select "Junkboy SMS Filter"
            
            📱 Method 2 - App Info:
            1. Open Android Settings
            2. Go to Apps → Junkboy SMS Filter
            3. Tap "Set as default" or "Open by default"
            4. Enable SMS handling
            
            📱 Method 3 - Search Settings:
            1. Open Android Settings
            2. Search for "default SMS" or "messaging app"
            3. Select Junkboy from the list
            
            💡 Note: The exact steps may vary by device manufacturer.
        """.trimIndent()
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