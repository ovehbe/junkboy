package com.ovehbe.junkboy.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Summary data for SMS conversations grouped by sender
 */
data class SmsConversationSummary(
    val sender: String,
    val lastMessage: String,
    val lastMessageDate: Date,
    val messageCount: Int,
    val unreadCount: Int,
    val lastCategory: MessageCategory,
    val hasBlocked: Boolean
)

@Dao
interface FilteredMessageDao {
    
    @Query("SELECT * FROM filtered_messages ORDER BY receivedAt DESC")
    fun getAllMessages(): Flow<List<FilteredMessage>>
    
    @Query("SELECT * FROM filtered_messages ORDER BY receivedAt DESC")
    suspend fun getAllMessagesOnce(): List<FilteredMessage>
    
    @Query("SELECT * FROM filtered_messages ORDER BY receivedAt DESC LIMIT :limit")
    fun getAllMessagesLimited(limit: Int = 100): Flow<List<FilteredMessage>>
    
    @Query("SELECT * FROM filtered_messages WHERE category = :category ORDER BY receivedAt DESC")
    fun getMessagesByCategory(category: MessageCategory): Flow<List<FilteredMessage>>
    
    @Query("SELECT * FROM filtered_messages WHERE category = :category ORDER BY receivedAt DESC LIMIT :limit")
    fun getMessagesByCategoryLimited(category: MessageCategory, limit: Int = 100): Flow<List<FilteredMessage>>
    
    @Query("SELECT * FROM filtered_messages WHERE isBlocked = 1 ORDER BY receivedAt DESC")
    fun getBlockedMessages(): Flow<List<FilteredMessage>>
    
    @Query("SELECT * FROM filtered_messages WHERE isBlocked = 1 ORDER BY receivedAt DESC LIMIT :limit")
    fun getBlockedMessagesLimited(limit: Int = 100): Flow<List<FilteredMessage>>
    
    @Query("SELECT * FROM filtered_messages WHERE isBlocked = 0 ORDER BY receivedAt DESC")
    fun getAllowedMessages(): Flow<List<FilteredMessage>>
    
    @Query("SELECT * FROM filtered_messages WHERE isBlocked = 0 ORDER BY receivedAt DESC LIMIT :limit")
    fun getAllowedMessagesLimited(limit: Int = 100): Flow<List<FilteredMessage>>
    
    @Query("SELECT * FROM filtered_messages WHERE (messageBody LIKE '%' || :query || '%' OR sender LIKE '%' || :query || '%') ORDER BY receivedAt DESC LIMIT :limit")
    fun searchMessagesLimited(query: String, limit: Int = 100): Flow<List<FilteredMessage>>
    
    @Query("SELECT * FROM filtered_messages WHERE receivedAt >= :since ORDER BY receivedAt DESC")
    fun getMessagesAfter(since: Date): Flow<List<FilteredMessage>>

    @Query("""
        SELECT * FROM filtered_messages
        WHERE sender = :sender
            AND messageBody = :messageBody
            AND receivedAt BETWEEN :start AND :end
        ORDER BY receivedAt DESC
        LIMIT 1
    """)
    suspend fun getRecentDuplicate(sender: String, messageBody: String, start: Date, end: Date): FilteredMessage?
    
    @Query("SELECT COUNT(*) FROM filtered_messages WHERE category = :category")
    suspend fun getCountByCategory(category: MessageCategory): Int
    
    @Query("SELECT COUNT(*) FROM filtered_messages WHERE category = :category AND receivedAt >= :since")
    suspend fun getCountByCategoryAfter(category: MessageCategory, since: Date): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: FilteredMessage): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<FilteredMessage>)
    
    @Update
    suspend fun updateMessage(message: FilteredMessage)
    
    @Delete
    suspend fun deleteMessage(message: FilteredMessage)
    
    @Query("DELETE FROM filtered_messages WHERE receivedAt < :before")
    suspend fun deleteMessagesOlderThan(before: Date): Int
    
    @Query("DELETE FROM filtered_messages WHERE category = :category")
    suspend fun deleteMessagesByCategory(category: MessageCategory): Int
    
    @Query("DELETE FROM filtered_messages")
    suspend fun deleteAllMessages(): Int
    
    /**
     * Update all messages from a sender to ALLOWED category
     * Used when adding sender to allowed list
     */
    @Query("UPDATE filtered_messages SET category = :category, filterType = :filterType, isBlocked = 0, isUserOverride = 1 WHERE sender = :sender OR sender LIKE '%' || :normalizedSender || '%'")
    suspend fun updateSenderCategory(sender: String, normalizedSender: String, category: MessageCategory, filterType: FilterType): Int
    
    @Query("UPDATE filtered_messages SET isBlocked = :isBlocked WHERE id = :id")
    suspend fun updateBlockStatus(id: Long, isBlocked: Boolean)
    
    @Query("UPDATE filtered_messages SET isUserOverride = 1, isBlocked = :isBlocked WHERE id = :id")
    suspend fun applyUserOverride(id: Long, isBlocked: Boolean)
    
    @Query("UPDATE filtered_messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
    
    @Query("UPDATE filtered_messages SET isRead = 1 WHERE category = :category")
    suspend fun markCategoryAsRead(category: MessageCategory)

    @Query("UPDATE filtered_messages SET isRead = 1 WHERE sender = :sender")
    suspend fun markSenderAsRead(sender: String)
    
    // Conversation grouping queries
    
    /**
     * Get all SMS conversations grouped by sender (non-blocked)
     */
    @Query("""
        SELECT 
            sender,
            (SELECT messageBody FROM filtered_messages fm2 WHERE fm2.sender = fm.sender ORDER BY receivedAt DESC LIMIT 1) as lastMessage,
            MAX(receivedAt) as lastMessageDate,
            COUNT(*) as messageCount,
            SUM(CASE WHEN isRead = 0 THEN 1 ELSE 0 END) as unreadCount,
            (SELECT category FROM filtered_messages fm2 WHERE fm2.sender = fm.sender ORDER BY receivedAt DESC LIMIT 1) as lastCategory,
            MAX(CASE WHEN isBlocked = 1 THEN 1 ELSE 0 END) as hasBlocked
        FROM filtered_messages fm
        WHERE isBlocked = 0
        GROUP BY sender
        ORDER BY lastMessageDate DESC
    """)
    suspend fun getAllowedConversations(): List<SmsConversationSummary>
    
    /**
     * Get all SMS conversations grouped by sender (all messages)
     */
    @Query("""
        SELECT 
            sender,
            (SELECT messageBody FROM filtered_messages fm2 WHERE fm2.sender = fm.sender ORDER BY receivedAt DESC LIMIT 1) as lastMessage,
            MAX(receivedAt) as lastMessageDate,
            COUNT(*) as messageCount,
            SUM(CASE WHEN isRead = 0 THEN 1 ELSE 0 END) as unreadCount,
            (SELECT category FROM filtered_messages fm2 WHERE fm2.sender = fm.sender ORDER BY receivedAt DESC LIMIT 1) as lastCategory,
            MAX(CASE WHEN isBlocked = 1 THEN 1 ELSE 0 END) as hasBlocked
        FROM filtered_messages fm
        GROUP BY sender
        ORDER BY lastMessageDate DESC
    """)
    suspend fun getAllConversations(): List<SmsConversationSummary>
    
    /**
     * Get blocked SMS conversations grouped by sender
     */
    @Query("""
        SELECT 
            sender,
            (SELECT messageBody FROM filtered_messages fm2 WHERE fm2.sender = fm.sender AND fm2.isBlocked = 1 ORDER BY receivedAt DESC LIMIT 1) as lastMessage,
            MAX(receivedAt) as lastMessageDate,
            COUNT(*) as messageCount,
            SUM(CASE WHEN isRead = 0 THEN 1 ELSE 0 END) as unreadCount,
            (SELECT category FROM filtered_messages fm2 WHERE fm2.sender = fm.sender ORDER BY receivedAt DESC LIMIT 1) as lastCategory,
            1 as hasBlocked
        FROM filtered_messages fm
        WHERE isBlocked = 1
        GROUP BY sender
        ORDER BY lastMessageDate DESC
    """)
    suspend fun getBlockedConversations(): List<SmsConversationSummary>
    
    /**
     * Get messages for a specific sender
     */
    @Query("SELECT * FROM filtered_messages WHERE sender = :sender ORDER BY receivedAt DESC")
    fun getMessagesBySender(sender: String): Flow<List<FilteredMessage>>
    
    /**
     * Get messages for a specific sender (limited)
     */
    @Query("SELECT * FROM filtered_messages WHERE sender = :sender ORDER BY receivedAt DESC LIMIT :limit")
    suspend fun getMessagesBySenderLimited(sender: String, limit: Int = 50): List<FilteredMessage>
}
