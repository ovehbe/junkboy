package com.ovehbe.junkboy.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "allowed_senders")
data class AllowedSender(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val displayName: String? = null,
    val addedAt: Date = Date(),
    val isActive: Boolean = true
) 