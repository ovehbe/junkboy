package com.ovehbe.junkboy.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "auto_reply_log", indices = [Index("senderNumber")])
data class AutoReplyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderNumber: String,
    val repliedAt: Long,
    val replyText: String,
    // Why Junkboy decided to reply, so decisions can be audited
    val reason: String = ""
)

@Dao
interface AutoReplyLogDao {

    @Insert
    suspend fun insert(log: AutoReplyLog): Long

    @Query("SELECT MAX(repliedAt) FROM auto_reply_log WHERE senderNumber = :senderNumber")
    suspend fun getLastReplyAt(senderNumber: String): Long?

    @Query("SELECT * FROM auto_reply_log ORDER BY repliedAt DESC LIMIT 200")
    fun getRecent(): Flow<List<AutoReplyLog>>

    @Query("DELETE FROM auto_reply_log")
    suspend fun clear()
}
