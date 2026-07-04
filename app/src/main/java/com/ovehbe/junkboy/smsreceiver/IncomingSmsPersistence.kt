package com.ovehbe.junkboy.smsreceiver

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.Telephony
import android.util.Log

object IncomingSmsPersistence {
    private const val TAG = "IncomingSmsPersistence"
    private const val DUPLICATE_WINDOW_MS = 5 * 60 * 1000L

    fun shouldPersistToSystemInbox(
        action: String?,
        isBlocked: Boolean,
        autoDeleteBlockedMessages: Boolean
    ): Boolean {
        if (action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return false
        }

        return !(isBlocked && autoDeleteBlockedMessages)
    }

    fun persistToSystemInboxIfNeeded(
        contentResolver: ContentResolver,
        action: String?,
        sender: String,
        message: String,
        timestamp: Long,
        isBlocked: Boolean,
        autoDeleteBlockedMessages: Boolean
    ) {
        if (!shouldPersistToSystemInbox(action, isBlocked, autoDeleteBlockedMessages)) {
            return
        }

        try {
            if (messageAlreadyExists(contentResolver, sender, message, timestamp)) {
                Log.d(TAG, "Incoming SMS already exists in system inbox for $sender")
                return
            }

            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, sender)
                put(Telephony.Sms.BODY, message)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
            }

            val uri = contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            Log.d(TAG, "Persisted incoming SMS to system inbox: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting incoming SMS to system inbox", e)
        }
    }

    private fun messageAlreadyExists(
        contentResolver: ContentResolver,
        sender: String,
        message: String,
        timestamp: Long
    ): Boolean {
        val start = timestamp - DUPLICATE_WINDOW_MS
        val end = timestamp + DUPLICATE_WINDOW_MS
        val selection = """
            ${Telephony.Sms.ADDRESS} = ?
            AND ${Telephony.Sms.BODY} = ?
            AND ${Telephony.Sms.TYPE} = ?
            AND ${Telephony.Sms.DATE} BETWEEN ? AND ?
        """.trimIndent()
        val args = arrayOf(
            sender,
            message,
            Telephony.Sms.MESSAGE_TYPE_INBOX.toString(),
            start.toString(),
            end.toString()
        )

        return contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            selection,
            args,
            null
        )?.use { cursor ->
            cursor.moveToFirst()
        } ?: false
    }
}
