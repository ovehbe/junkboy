package com.ovehbe.junkboy.utils

import android.content.Context
import android.content.SharedPreferences
import com.ovehbe.junkboy.database.MessageCategory
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
        private const val KEY_DAILY_BLOCKED_COUNT = "daily_blocked_count"
        
        // First run
        private const val KEY_FIRST_RUN = "first_run"
        private const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
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
                .putInt(KEY_DAILY_BLOCKED_COUNT, 0)
                .apply()
        }
    }
    
    // App state
    fun isFirstRun(): Boolean = prefs.getBoolean(KEY_FIRST_RUN, true)
    fun setFirstRunCompleted() = prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    
    fun arePermissionsGranted(): Boolean = prefs.getBoolean(KEY_PERMISSIONS_GRANTED, false)
    fun setPermissionsGranted(granted: Boolean) = prefs.edit().putBoolean(KEY_PERMISSIONS_GRANTED, granted).apply()
    
    // Reset all data
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
} 