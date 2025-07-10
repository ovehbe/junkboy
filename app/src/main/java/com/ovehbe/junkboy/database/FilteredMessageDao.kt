package com.ovehbe.junkboy.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface FilteredMessageDao {
    
    @Query("SELECT * FROM filtered_messages ORDER BY receivedAt DESC")
    fun getAllMessages(): Flow<List<FilteredMessage>>
    
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
    
    @Query("SELECT * FROM filtered_messages WHERE receivedAt >= :since ORDER BY receivedAt DESC")
    fun getMessagesAfter(since: Date): Flow<List<FilteredMessage>>
    
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
    
    @Query("UPDATE filtered_messages SET isBlocked = :isBlocked WHERE id = :id")
    suspend fun updateBlockStatus(id: Long, isBlocked: Boolean)
    
    @Query("UPDATE filtered_messages SET isUserOverride = 1, isBlocked = :isBlocked WHERE id = :id")
    suspend fun applyUserOverride(id: Long, isBlocked: Boolean)
    
    @Query("UPDATE filtered_messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
    
    @Query("UPDATE filtered_messages SET isRead = 1 WHERE category = :category")
    suspend fun markCategoryAsRead(category: MessageCategory)
} 