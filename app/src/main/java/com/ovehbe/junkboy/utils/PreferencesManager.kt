package com.ovehbe.junkboy.utils

import android.content.Context
import android.content.SharedPreferences
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.filters.AllowRule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "junkboy_prefs"
        
        // Feature toggles
        private const val KEY_UNDER_ATTACK_MODE = "under_attack_mode"
        private const val KEY_ML_FILTERING_ENABLED = "ml_filtering_enabled"
        private const val KEY_KEYWORD_FILTERING_ENABLED = "keyword_filtering_enabled"
        private const val KEY_REGEX_FILTERING_ENABLED = "regex_filtering_enabled"
        private const val KEY_NOTIFY_ALL_FILTERED = "notify_all_filtered"
        private const val KEY_NOTIFY_BLOCKED_MESSAGES = "notify_blocked_messages"
        private const val KEY_NOTIFY_CATEGORIZED_MESSAGES = "notify_categorized_messages"
        private const val KEY_AUTO_DELETE_JUNK = "auto_delete_junk"
        
        // Individual category notification preferences
        private const val KEY_NOTIFY_GENERAL = "notify_general"
        private const val KEY_NOTIFY_PROMOTION = "notify_promotion"
        private const val KEY_NOTIFY_NOTIFICATION = "notify_notification"
        private const val KEY_NOTIFY_TRANSACTION = "notify_transaction"
        private const val KEY_NOTIFY_ALLOWED = "notify_allowed"
        
        // Content allowlist (words/regexes that bypass all filtering)
        private const val KEY_CONTENT_ALLOW_RULES = "content_allow_rules"
        
        // Custom filters
        private const val KEY_CUSTOM_KEYWORDS = "custom_keywords"
        private const val KEY_CUSTOM_REGEX_PATTERNS = "custom_regex_patterns"
        
        // Statistics
        private const val KEY_TOTAL_MESSAGES_FILTERED = "total_messages_filtered"
        private const val KEY_TOTAL_MESSAGES_BLOCKED = "total_messages_blocked"
        private const val KEY_DAILY_STATS_DATE = "daily_stats_date"
        private const val KEY_DAILY_GENERAL_COUNT = "daily_general_count"
        private const val KEY_DAILY_PROMOTION_COUNT = "daily_promotion_count"
        private const val KEY_DAILY_NOTIFICATION_COUNT = "daily_notification_count"
        private const val KEY_DAILY_TRANSACTION_COUNT = "daily_transaction_count"
        private const val KEY_DAILY_JUNK_COUNT = "daily_junk_count"
        private const val KEY_DAILY_ALLOWED_COUNT = "daily_allowed_count"
        private const val KEY_DAILY_BLOCKED_COUNT = "daily_blocked_count"
        
        // First run
        private const val KEY_FIRST_RUN = "first_run"
        private const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
        
        // OTP Auto-Copy feature
        private const val KEY_OTP_AUTO_COPY_ENABLED = "otp_auto_copy_enabled"
        private const val KEY_OTP_COPY_COUNT = "otp_copy_count"
        
        // Stop2End auto-responder
        private const val KEY_AUTO_REPLY_STOP_ENABLED = "auto_reply_stop_enabled"
        // "_v2": the first version defaulted to off, which was unsafe; ignore any value stored under it
        private const val KEY_AUTO_REPLY_REQUIRE_SPAM = "auto_reply_require_spam_v2"
        
        // Hub configuration
        private const val KEY_HUB_ENABLED = "hub_enabled"
        private const val KEY_HUB_DISPLAY_MODE = "hub_display_mode" // "all", "sms_only", "chats_only"
        private const val KEY_HUB_DEFAULT_VIEW = "hub_default_view" // "conversations", "messages"
        private const val KEY_HUB_NOTIFICATIONS_ENABLED = "hub_notifications_enabled"
        private const val KEY_ENABLED_HUB_APPS = "enabled_hub_apps"
        
        // SMS App Notification Control (Buzzkill-like features)
        private const val KEY_DISMISS_SMS_APP_NOTIFICATIONS = "dismiss_sms_app_notifications"
        private const val KEY_MUTE_SMS_APP_NOTIFICATIONS = "mute_sms_app_notifications"
        private const val KEY_DISMISS_BLOCKED_ONLY = "dismiss_blocked_only"
        private const val KEY_SMS_APP_CONTROL_ENABLED = "sms_app_control_enabled"
        
        // SMS Screen Settings
        private const val KEY_SMS_HIDE_JUNK = "sms_hide_junk"
        private const val KEY_SMS_SHOW_CATEGORY_BADGES = "sms_show_category_badges"
        private const val KEY_SMS_DISPLAY_CATEGORIES = "sms_display_categories" // Set of categories to display
        
        // Accessibility Settings
        private const val KEY_KEYBOARD_OFFSET = "keyboard_offset" // Extra bottom padding for keyboards
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Feature toggles
    fun isUnderAttackMode(): Boolean = prefs.getBoolean(KEY_UNDER_ATTACK_MODE, false)
    fun setUnderAttackMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_UNDER_ATTACK_MODE, enabled).apply()
    
    fun isMlFilteringEnabled(): Boolean = prefs.getBoolean(KEY_ML_FILTERING_ENABLED, true)
    fun setMlFilteringEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ML_FILTERING_ENABLED, enabled).apply()
    
    fun isKeywordFilteringEnabled(): Boolean = prefs.getBoolean(KEY_KEYWORD_FILTERING_ENABLED, true)
    fun setKeywordFilteringEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_KEYWORD_FILTERING_ENABLED, enabled).apply()
    
    fun isRegexFilteringEnabled(): Boolean = prefs.getBoolean(KEY_REGEX_FILTERING_ENABLED, true)
    fun setRegexFilteringEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_REGEX_FILTERING_ENABLED, enabled).apply()
    
    fun shouldNotifyAllFiltered(): Boolean = prefs.getBoolean(KEY_NOTIFY_ALL_FILTERED, false)
    fun setNotifyAllFiltered(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFY_ALL_FILTERED, enabled).apply()
    
    fun shouldNotifyBlockedMessages(): Boolean = prefs.getBoolean(KEY_NOTIFY_BLOCKED_MESSAGES, false)
    fun setNotifyBlockedMessages(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFY_BLOCKED_MESSAGES, enabled).apply()
    
    fun shouldNotifyCategorizedMessages(): Boolean = prefs.getBoolean(KEY_NOTIFY_CATEGORIZED_MESSAGES, false)
    fun setNotifyCategorizedMessages(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFY_CATEGORIZED_MESSAGES, enabled).apply()
    
    // Individual category notification preferences
    fun shouldNotifyGeneral(): Boolean = prefs.getBoolean(KEY_NOTIFY_GENERAL, true)
    fun setNotifyGeneral(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFY_GENERAL, enabled).apply()
    
    fun shouldNotifyPromotion(): Boolean = prefs.getBoolean(KEY_NOTIFY_PROMOTION, false)
    fun setNotifyPromotion(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFY_PROMOTION, enabled).apply()
    
    fun shouldNotifyNotification(): Boolean = prefs.getBoolean(KEY_NOTIFY_NOTIFICATION, true)
    fun setNotifyNotification(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFY_NOTIFICATION, enabled).apply()
    
    fun shouldNotifyTransaction(): Boolean = prefs.getBoolean(KEY_NOTIFY_TRANSACTION, true)
    fun setNotifyTransaction(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFY_TRANSACTION, enabled).apply()
    
    fun shouldNotifyAllowed(): Boolean = prefs.getBoolean(KEY_NOTIFY_ALLOWED, true)
    fun setNotifyAllowed(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFY_ALLOWED, enabled).apply()
    
    fun isAutoDeleteJunkEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_DELETE_JUNK, false)
    fun setAutoDeleteJunk(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_DELETE_JUNK, enabled).apply()
    
    // Custom filters
    fun getCustomKeywords(): List<String> {
        val json = prefs.getString(KEY_CUSTOM_KEYWORDS, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun setCustomKeywords(keywords: List<String>) {
        val json = gson.toJson(keywords)
        prefs.edit().putString(KEY_CUSTOM_KEYWORDS, json).apply()
    }
    
    fun addCustomKeyword(keyword: String) {
        val current = getCustomKeywords().toMutableList()
        if (!current.contains(keyword.trim()) && keyword.trim().isNotEmpty()) {
            current.add(keyword.trim())
            setCustomKeywords(current)
        }
    }
    
    fun removeCustomKeyword(keyword: String) {
        val current = getCustomKeywords().toMutableList()
        current.remove(keyword)
        setCustomKeywords(current)
    }
    
    // Content allowlist
    fun getAllowRules(): List<AllowRule> {
        val json = prefs.getString(KEY_CONTENT_ALLOW_RULES, "[]")
        val type = object : TypeToken<List<AllowRule>>() {}.type
        return gson.fromJson<List<AllowRule>?>(json, type) ?: emptyList()
    }
    
    fun setAllowRules(rules: List<AllowRule>) {
        prefs.edit().putString(KEY_CONTENT_ALLOW_RULES, gson.toJson(rules)).apply()
    }
    
    fun getCustomRegexPatterns(): List<String> {
        val json = prefs.getString(KEY_CUSTOM_REGEX_PATTERNS, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun setCustomRegexPatterns(patterns: List<String>) {
        val json = gson.toJson(patterns)
        prefs.edit().putString(KEY_CUSTOM_REGEX_PATTERNS, json).apply()
    }
    
    fun addCustomRegexPattern(pattern: String) {
        val current = getCustomRegexPatterns().toMutableList()
        if (!current.contains(pattern.trim()) && pattern.trim().isNotEmpty()) {
            current.add(pattern.trim())
            setCustomRegexPatterns(current)
        }
    }
    
    fun removeCustomRegexPattern(pattern: String) {
        val current = getCustomRegexPatterns().toMutableList()
        current.remove(pattern)
        setCustomRegexPatterns(current)
    }
    
    // Statistics
    fun getTotalMessagesFiltered(): Long = prefs.getLong(KEY_TOTAL_MESSAGES_FILTERED, 0)
    fun getTotalMessagesBlocked(): Long = prefs.getLong(KEY_TOTAL_MESSAGES_BLOCKED, 0)
    
    fun incrementCategoryCount(category: MessageCategory) {
        checkDailyStatsReset()
        
        val key = when (category) {
            MessageCategory.GENERAL -> KEY_DAILY_GENERAL_COUNT
            MessageCategory.PROMOTION -> KEY_DAILY_PROMOTION_COUNT
            MessageCategory.NOTIFICATION -> KEY_DAILY_NOTIFICATION_COUNT
            MessageCategory.TRANSACTION -> KEY_DAILY_TRANSACTION_COUNT
            MessageCategory.JUNK -> KEY_DAILY_JUNK_COUNT
            MessageCategory.ALLOWED -> KEY_DAILY_ALLOWED_COUNT
        }
        
        val current = prefs.getInt(key, 0)
        prefs.edit()
            .putInt(key, current + 1)
            .putLong(KEY_TOTAL_MESSAGES_FILTERED, getTotalMessagesFiltered() + 1)
            .apply()
    }
    
    fun incrementBlockedCount() {
        checkDailyStatsReset()
        
        val current = prefs.getInt(KEY_DAILY_BLOCKED_COUNT, 0)
        prefs.edit()
            .putInt(KEY_DAILY_BLOCKED_COUNT, current + 1)
            .putLong(KEY_TOTAL_MESSAGES_BLOCKED, getTotalMessagesBlocked() + 1)
            .apply()
    }
    
    fun getDailyCategoryCount(category: MessageCategory): Int {
        checkDailyStatsReset()
        
        val key = when (category) {
            MessageCategory.GENERAL -> KEY_DAILY_GENERAL_COUNT
            MessageCategory.PROMOTION -> KEY_DAILY_PROMOTION_COUNT
            MessageCategory.NOTIFICATION -> KEY_DAILY_NOTIFICATION_COUNT
            MessageCategory.TRANSACTION -> KEY_DAILY_TRANSACTION_COUNT
            MessageCategory.JUNK -> KEY_DAILY_JUNK_COUNT
            MessageCategory.ALLOWED -> KEY_DAILY_ALLOWED_COUNT
        }
        
        return prefs.getInt(key, 0)
    }
    
    fun getDailyBlockedCount(): Int {
        checkDailyStatsReset()
        return prefs.getInt(KEY_DAILY_BLOCKED_COUNT, 0)
    }
    
    private fun checkDailyStatsReset() {
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24) // Days since epoch
        val lastStatsDate = prefs.getLong(KEY_DAILY_STATS_DATE, 0)
        
        if (today != lastStatsDate) {
            // Reset daily stats
            prefs.edit()
                .putLong(KEY_DAILY_STATS_DATE, today)
                .putInt(KEY_DAILY_GENERAL_COUNT, 0)
                .putInt(KEY_DAILY_PROMOTION_COUNT, 0)
                .putInt(KEY_DAILY_NOTIFICATION_COUNT, 0)
                .putInt(KEY_DAILY_TRANSACTION_COUNT, 0)
                .putInt(KEY_DAILY_JUNK_COUNT, 0)
                .putInt(KEY_DAILY_ALLOWED_COUNT, 0)
                .putInt(KEY_DAILY_BLOCKED_COUNT, 0)
                .apply()
        }
    }
    
    // App state
    fun isFirstRun(): Boolean = prefs.getBoolean(KEY_FIRST_RUN, true)
    fun setFirstRunCompleted() = prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    
    fun arePermissionsGranted(): Boolean = prefs.getBoolean(KEY_PERMISSIONS_GRANTED, false)
    fun setPermissionsGranted(granted: Boolean) = prefs.edit().putBoolean(KEY_PERMISSIONS_GRANTED, granted).apply()
    
    // Stop2End auto-responder (off by default: sends SMS on the user's plan)
    fun isAutoReplyStopEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_REPLY_STOP_ENABLED, false)
    fun setAutoReplyStopEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_REPLY_STOP_ENABLED, enabled).apply()
    
    // When true, the classifier must also flag the message as promotional/junk before replying.
    // On by default: pharmacies, banks and clinics also end real messages with "STOP2END".
    fun isAutoReplyRequireSpam(): Boolean = prefs.getBoolean(KEY_AUTO_REPLY_REQUIRE_SPAM, true)
    fun setAutoReplyRequireSpam(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_REPLY_REQUIRE_SPAM, enabled).apply()
    
    // OTP Auto-Copy
    fun isOtpAutoCopyEnabled(): Boolean = prefs.getBoolean(KEY_OTP_AUTO_COPY_ENABLED, true)
    fun setOtpAutoCopyEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_OTP_AUTO_COPY_ENABLED, enabled).apply()
    
    fun getOtpCopyCount(): Int = prefs.getInt(KEY_OTP_COPY_COUNT, 0)
    fun incrementOtpCopyCount() = prefs.edit().putInt(KEY_OTP_COPY_COUNT, getOtpCopyCount() + 1).apply()
    
    // Hub Configuration
    fun isHubEnabled(): Boolean = prefs.getBoolean(KEY_HUB_ENABLED, false) // Disabled by default
    fun setHubEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_HUB_ENABLED, enabled).apply()
    
    // Hub display mode: "all", "sms_only", "chats_only"
    fun getHubDisplayMode(): String = prefs.getString(KEY_HUB_DISPLAY_MODE, "sms_only") ?: "sms_only"
    fun setHubDisplayMode(mode: String) = prefs.edit().putString(KEY_HUB_DISPLAY_MODE, mode).apply()
    
    // Hub default view: "conversations", "messages"
    fun getHubDefaultView(): String = prefs.getString(KEY_HUB_DEFAULT_VIEW, "conversations") ?: "conversations"
    fun setHubDefaultView(view: String) = prefs.edit().putString(KEY_HUB_DEFAULT_VIEW, view).apply()
    
    fun isHubNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_HUB_NOTIFICATIONS_ENABLED, true)
    fun setHubNotificationsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_HUB_NOTIFICATIONS_ENABLED, enabled).apply()
    
    fun getEnabledHubApps(): Set<String> {
        val defaultApps = setOf(
            "com.whatsapp",
            "org.telegram.messenger", 
            "org.thoughtcrime.securesms",
            "com.facebook.orca",
            "com.instagram.android",
            "com.google.android.gm",
            "com.microsoft.office.outlook"
        )
        val appsJson = prefs.getString(KEY_ENABLED_HUB_APPS, null)
        return if (appsJson != null) {
            try {
                gson.fromJson(appsJson, Array<String>::class.java).toSet()
            } catch (e: Exception) {
                defaultApps
            }
        } else {
            defaultApps
        }
    }
    
    fun setEnabledHubApps(apps: Set<String>) {
        val appsJson = gson.toJson(apps.toTypedArray())
        prefs.edit().putString(KEY_ENABLED_HUB_APPS, appsJson).apply()
    }
    
    fun isAppEnabledForHub(packageName: String): Boolean {
        return getEnabledHubApps().contains(packageName)
    }
    
    // SMS App Notification Control (Buzzkill-like features)
    fun isSmsAppControlEnabled(): Boolean = prefs.getBoolean(KEY_SMS_APP_CONTROL_ENABLED, false)
    fun setSmsAppControlEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SMS_APP_CONTROL_ENABLED, enabled).apply()
    
    fun shouldDismissSmsAppNotifications(): Boolean = prefs.getBoolean(KEY_DISMISS_SMS_APP_NOTIFICATIONS, false)
    fun setDismissSmsAppNotifications(enabled: Boolean) = prefs.edit().putBoolean(KEY_DISMISS_SMS_APP_NOTIFICATIONS, enabled).apply()
    
    fun shouldMuteSmsAppNotifications(): Boolean = prefs.getBoolean(KEY_MUTE_SMS_APP_NOTIFICATIONS, false)
    fun setMuteSmsAppNotifications(enabled: Boolean) = prefs.edit().putBoolean(KEY_MUTE_SMS_APP_NOTIFICATIONS, enabled).apply()
    
    fun shouldDismissBlockedOnly(): Boolean = prefs.getBoolean(KEY_DISMISS_BLOCKED_ONLY, true)
    fun setDismissBlockedOnly(enabled: Boolean) = prefs.edit().putBoolean(KEY_DISMISS_BLOCKED_ONLY, enabled).apply()
    
    // SMS Screen Settings
    fun shouldHideJunkInSms(): Boolean = prefs.getBoolean(KEY_SMS_HIDE_JUNK, false)
    fun setHideJunkInSms(enabled: Boolean) = prefs.edit().putBoolean(KEY_SMS_HIDE_JUNK, enabled).apply()
    
    fun shouldShowCategoryBadges(): Boolean = prefs.getBoolean(KEY_SMS_SHOW_CATEGORY_BADGES, true)
    fun setShowCategoryBadges(enabled: Boolean) = prefs.edit().putBoolean(KEY_SMS_SHOW_CATEGORY_BADGES, enabled).apply()
    
    // SMS Display Categories - which categories to show on SMS screen
    fun getSmsDisplayCategories(): Set<String> {
        val defaultCategories = setOf(
            MessageCategory.GENERAL.name,
            MessageCategory.PROMOTION.name,
            MessageCategory.NOTIFICATION.name,
            MessageCategory.TRANSACTION.name,
            MessageCategory.ALLOWED.name
            // JUNK excluded by default
        )
        val categoriesJson = prefs.getString(KEY_SMS_DISPLAY_CATEGORIES, null)
        return if (categoriesJson != null) {
            try {
                gson.fromJson(categoriesJson, Array<String>::class.java).toSet()
            } catch (e: Exception) {
                defaultCategories
            }
        } else {
            defaultCategories
        }
    }
    
    fun setSmsDisplayCategories(categories: Set<String>) {
        val categoriesJson = gson.toJson(categories.toTypedArray())
        prefs.edit().putString(KEY_SMS_DISPLAY_CATEGORIES, categoriesJson).apply()
    }
    
    fun isCategoryDisplayedInSms(category: MessageCategory): Boolean {
        return getSmsDisplayCategories().contains(category.name)
    }
    
    fun setCategoryDisplayedInSms(category: MessageCategory, displayed: Boolean) {
        val current = getSmsDisplayCategories().toMutableSet()
        if (displayed) {
            current.add(category.name)
        } else {
            current.remove(category.name)
        }
        setSmsDisplayCategories(current)
    }
    
    // Accessibility Settings
    fun getKeyboardOffset(): Int = prefs.getInt(KEY_KEYBOARD_OFFSET, 0)
    fun setKeyboardOffset(offset: Int) = prefs.edit().putInt(KEY_KEYBOARD_OFFSET, offset).apply()
    
    // Reset all data
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
} 