package com.ovehbe.junkboy.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [FilteredMessage::class, AllowedSender::class, ChatMessage::class, AutoReplyLog::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun filteredMessageDao(): FilteredMessageDao
    abstract fun allowedSenderDao(): AllowedSenderDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun autoReplyLogDao(): AutoReplyLogDao
    
    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `auto_reply_log` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`senderNumber` TEXT NOT NULL, " +
                        "`repliedAt` INTEGER NOT NULL, " +
                        "`replyText` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_auto_reply_log_senderNumber` ON `auto_reply_log` (`senderNumber`)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "junkboy_database"
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 