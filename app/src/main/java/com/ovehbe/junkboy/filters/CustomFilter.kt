package com.ovehbe.junkboy.filters

import com.ovehbe.junkboy.database.FilterType
import com.ovehbe.junkboy.database.MessageCategory

data class FilterResult(
    val isBlocked: Boolean,
    val category: MessageCategory,
    val filterType: FilterType,
    val confidence: Float = 1.0f,
    val matchedRule: String? = null
)

object CustomFilter {
    
    // Turkish and English junk keywords
    private val junkKeywords = listOf(
        // English spam keywords
        "win", "winner", "prize", "free", "bonus", "cash", "money", "earn",
        "claim", "urgent", "limited time", "act now", "call now", "click here",
        "congratulations", "selected", "lucky", "offer", "deal", "discount",
        "loan", "credit", "debt", "casino", "bet", "gambling",
        
        // Turkish spam keywords
        "kazan", "kazandınız", "hediye", "ödül", "para", "nakit", "bonus",
        "ücretsiz", "bedava", "çekiliş", "şanslı", "seçildiniz", "tebrikler",
        "acil", "sınırlı", "fırsat", "indirim", "kredi", "borç", "bahis",
        "kumar", "casino", "tıkla", "ara", "hemen", "son gün"
    )
    
    // Promotional keywords
    private val promotionKeywords = listOf(
        "sale", "offer", "discount", "promotion", "deal", "special",
        "satış", "kampanya", "indirim", "özel", "teklif"
    )
    
    // Transaction keywords
    private val transactionKeywords = listOf(
        "bank", "payment", "transfer", "balance", "account", "transaction",
        "banka", "ödeme", "havale", "bakiye", "hesap", "işlem", "atm",
        "card", "kart", "withdraw", "deposit", "çekim", "yatırım"
    )
    
    // Notification keywords
    private val notificationKeywords = listOf(
        "reminder", "appointment", "scheduled", "delivery", "shipped",
        "hatırlatma", "randevu", "teslimat", "kargo", "gönderildi",
        "activation", "verification", "code", "otp",
        "aktivasyon", "doğrulama", "kod"
    )
    
    // Regex patterns for junk detection
    private val junkRegexPatterns = listOf(
        "(?i)\\b(win|won|winner)\\s+(\\$|€|₺|prize|ödül)",
        "(?i)\\b(para|money|cash)\\s+(kazan|win|earn)",
        "(?i)\\b(ücretsiz|free|bedava)\\s+(hediye|gift|bonus)",
        "(?i)\\b(çekiliş|lottery|draw)\\s+(kazandınız|won|winner)",
        "(?i)\\b(tıkla|click)\\s+(kazan|win|here)",
        "(?i)\\b(hemen|urgent|acil)\\s+(ara|call|now)",
        "(?i)\\b(son|last|final)\\s+(gün|day|şans|chance)",
        "(?i)\\b(kumar|gambling|bahis|bet|casino)",
        "(?i)(\\+90|0090)\\s?5\\d{2}\\s?\\d{3}\\s?\\d{2}\\s?\\d{2}", // Turkish phone numbers
        "(?i)\\b\\d{4}\\s?(tl|₺|usd|eur|€)\\b", // Money amounts
        "(?i)\\b(kredi|loan|borç|debt)\\s+(teklif|offer|onay|approval)"
    )
    
    // Compiled regex patterns for performance
    private val compiledJunkRegex = junkRegexPatterns.map { Regex(it) }
    
    fun filterMessage(
        message: String, 
        sender: String,
        isUnderAttackMode: Boolean = false,
        customKeywords: List<String> = emptyList(),
        customRegexPatterns: List<String> = emptyList()
    ): FilterResult {
        
        val messageText = message.lowercase()
        val senderText = sender.lowercase()
        
        // Under Attack Mode - more aggressive filtering
        if (isUnderAttackMode) {
            val attackModeResult = checkUnderAttackMode(messageText, senderText)
            if (attackModeResult.isBlocked) {
                return attackModeResult
            }
        }
        
        // Check custom user patterns first
        val customResult = checkCustomFilters(messageText, customKeywords, customRegexPatterns)
        if (customResult.isBlocked) {
            return customResult
        }
        
        // Check built-in junk patterns
        val junkResult = checkJunkPatterns(messageText, senderText)
        if (junkResult.isBlocked) {
            return junkResult
        }
        
        // Categorize non-junk messages
        return categorizeMessage(messageText)
    }
    
    private fun checkUnderAttackMode(message: String, sender: String): FilterResult {
        // More aggressive patterns for attack mode
        val attackPatterns = listOf(
            "(?i)\\b\\d+\\s?(tl|₺|usd|eur)", // Any money amount
            "(?i)\\b(hediye|gift|bonus|ödül|prize)", // Any gift/prize mention
            "(?i)\\b(kazan|win|earn)", // Any win/earn mention
            "(?i)\\b(tıkla|click|ara|call)", // Any action request
            "(?i)\\b(sınırlı|limited|son|last|acil|urgent)", // Urgency
            "(?i)\\b\\d{4}\\d*\\b" // 4+ digit numbers (suspicious codes)
        )
        
        for (pattern in attackPatterns) {
            if (Regex(pattern).containsMatchIn(message)) {
                return FilterResult(
                    isBlocked = true,
                    category = MessageCategory.JUNK,
                    filterType = FilterType.UNDER_ATTACK_MODE,
                    confidence = 0.9f,
                    matchedRule = pattern
                )
            }
        }
        
        // Block unknown/suspicious senders in attack mode
        if (sender.matches(Regex("\\d+")) && sender.length > 4) {
            return FilterResult(
                isBlocked = true,
                category = MessageCategory.JUNK,
                filterType = FilterType.UNDER_ATTACK_MODE,
                confidence = 0.8f,
                matchedRule = "suspicious_sender"
            )
        }
        
        return FilterResult(false, MessageCategory.GENERAL, FilterType.UNDER_ATTACK_MODE)
    }
    
    private fun checkCustomFilters(
        message: String, 
        customKeywords: List<String>, 
        customRegexPatterns: List<String>
    ): FilterResult {
        
        // Check custom keywords
        for (keyword in customKeywords) {
            if (message.contains(keyword.lowercase())) {
                return FilterResult(
                    isBlocked = true,
                    category = MessageCategory.JUNK,
                    filterType = FilterType.KEYWORD_FILTER,
                    confidence = 1.0f,
                    matchedRule = keyword
                )
            }
        }
        
        // Check custom regex patterns
        for (pattern in customRegexPatterns) {
            try {
                if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(message)) {
                    return FilterResult(
                        isBlocked = true,
                        category = MessageCategory.JUNK,
                        filterType = FilterType.REGEX_FILTER,
                        confidence = 1.0f,
                        matchedRule = pattern
                    )
                }
            } catch (e: Exception) {
                // Invalid regex pattern - skip
                continue
            }
        }
        
        return FilterResult(false, MessageCategory.GENERAL, FilterType.USER_RULE)
    }
    
    private fun checkJunkPatterns(message: String, sender: String): FilterResult {
        
        // Check junk keywords
        for (keyword in junkKeywords) {
            if (message.contains(keyword)) {
                return FilterResult(
                    isBlocked = true,
                    category = MessageCategory.JUNK,
                    filterType = FilterType.KEYWORD_FILTER,
                    confidence = 0.85f,
                    matchedRule = keyword
                )
            }
        }
        
        // Check junk regex patterns
        for (regex in compiledJunkRegex) {
            if (regex.containsMatchIn(message)) {
                return FilterResult(
                    isBlocked = true,
                    category = MessageCategory.JUNK,
                    filterType = FilterType.REGEX_FILTER,
                    confidence = 0.9f,
                    matchedRule = regex.pattern
                )
            }
        }
        
        return FilterResult(false, MessageCategory.GENERAL, FilterType.KEYWORD_FILTER)
    }
    
    private fun categorizeMessage(message: String): FilterResult {
        
        // Check for transaction messages
        val transactionScore = transactionKeywords.count { message.contains(it) }
        if (transactionScore >= 1) {
            return FilterResult(
                isBlocked = false,
                category = MessageCategory.TRANSACTION,
                filterType = FilterType.KEYWORD_FILTER,
                confidence = 0.7f + (transactionScore * 0.1f)
            )
        }
        
        // Check for promotional messages
        val promotionScore = promotionKeywords.count { message.contains(it) }
        if (promotionScore >= 1) {
            return FilterResult(
                isBlocked = false,
                category = MessageCategory.PROMOTION,
                filterType = FilterType.KEYWORD_FILTER,
                confidence = 0.6f + (promotionScore * 0.1f)
            )
        }
        
        // Check for notification messages
        val notificationScore = notificationKeywords.count { message.contains(it) }
        if (notificationScore >= 1) {
            return FilterResult(
                isBlocked = false,
                category = MessageCategory.NOTIFICATION,
                filterType = FilterType.KEYWORD_FILTER,
                confidence = 0.6f + (notificationScore * 0.1f)
            )
        }
        
        // Default to general
        return FilterResult(
            isBlocked = false,
            category = MessageCategory.GENERAL,
            filterType = FilterType.KEYWORD_FILTER,
            confidence = 0.5f
        )
    }
} 