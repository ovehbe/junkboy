package com.ovehbe.junkboy.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.ovehbe.junkboy.database.AppDatabase
import com.ovehbe.junkboy.database.AutoReplyLog
import com.ovehbe.junkboy.database.AutoReplyLogDao
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sends a "STOP" SMS to marketing senders, guarded by the user setting, a sender
 * sanity check and a per-sender 24h cooldown persisted in Room.
 */
class AutoReplyEngine(
    private val dao: AutoReplyLogDao,
    private val isEnabled: () -> Boolean,
    private val hasSendPermission: () -> Boolean,
    private val dispatch: (number: String, text: String) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis
) {

    companion object {
        private const val TAG = "AutoReplyEngine"
        const val REPLY_TEXT = "STOP"
        const val COOLDOWN_MS = 24L * 60 * 60 * 1000

        // One lock for the process so concurrent messages can't both pass the cooldown check.
        private val lock = Mutex()

        // Phone numbers and numeric short codes only; alphanumeric sender IDs can't receive SMS.
        private val VALID_TARGET = Regex("^\\+?\\d{3,15}$")

        /** Strips formatting; returns null if [sender] is not a number or numeric short code. */
        fun normalizeTarget(sender: String): String? {
            val cleaned = sender.trim().replace(Regex("[\\s\\-().]"), "")
            return cleaned.takeIf { VALID_TARGET.matches(it) }
        }

        fun isCoolingDown(lastReplyAt: Long?, now: Long): Boolean =
            lastReplyAt != null && now - lastReplyAt < COOLDOWN_MS

        fun create(context: Context): AutoReplyEngine {
            val appContext = context.applicationContext
            val prefs = PreferencesManager(appContext)
            return AutoReplyEngine(
                dao = AppDatabase.getDatabase(appContext).autoReplyLogDao(),
                isEnabled = { prefs.isAutoReplyStopEnabled() },
                hasSendPermission = {
                    ContextCompat.checkSelfPermission(appContext, Manifest.permission.SEND_SMS) ==
                        PackageManager.PERMISSION_GRANTED
                },
                dispatch = { number, text ->
                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        appContext.getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }
                    smsManager.sendTextMessage(number, null, text, null, null)
                }
            )
        }
    }

    /** Returns true if a reply was sent. */
    suspend fun sendStopReply(senderNumber: String, reason: String = ""): Boolean {
        // 1. Settings and permission
        if (!isEnabled() || !hasSendPermission()) return false

        // 2. Sender sanity
        val target = normalizeTarget(senderNumber) ?: run {
            Log.d(TAG, "Not replying: $senderNumber is not a number or short code")
            return false
        }

        return lock.withLock {
            // 3. 24h cooldown
            val now = clock()
            if (isCoolingDown(dao.getLastReplyAt(target), now)) {
                Log.d(TAG, "Not replying: $target already answered within 24h")
                return@withLock false
            }

            // 4. Dispatch, then log
            try {
                dispatch(target, REPLY_TEXT)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send STOP reply to $target", e)
                return@withLock false
            }
            dao.insert(AutoReplyLog(senderNumber = target, repliedAt = now, replyText = REPLY_TEXT, reason = reason))
            true
        }
    }
}
