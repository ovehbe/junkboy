package com.ovehbe.junkboy.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "filtered_messages")
data class FilteredMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sender: String,
    val messageBody: String,
    val receivedAt: Date,
    val category: MessageCategory,
    val confidence: Float = 0.0f,
    val filterType: FilterType,
    val isBlocked: Boolean = false,
    val isUserOverride: Boolean = false,
    val isRead: Boolean = false
)

enum class MessageCategory {
    GENERAL,
    PROMOTION,
    NOTIFICATION,
    TRANSACTION,
    JUNK,
    ALLOWED  // For messages from allowed senders
}

enum class FilterType {
    ML_CLASSIFICATION,
    KEYWORD_FILTER,
    REGEX_FILTER,
    USER_RULE,
    UNDER_ATTACK_MODE,
    ALLOWED_SENDER,  // Sender is in allowed list
    ALLOWED_CONTENT  // Message matched a word/regex allow rule
} 