package com.ovehbe.junkboy.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat

/** Decides whether a sender is someone the user knows: a contact, or someone they have texted. */
class SenderHistoryChecker(private val context: Context) {

    companion object {
        private const val TAG = "SenderHistoryChecker"
    }

    fun isContact(sender: String): Boolean {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return true // can't verify: fail safe
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(sender))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Contact lookup failed", e)
            true
        }
    }

    fun hasOutboundHistory(sender: String): Boolean {
        if (!hasPermission(Manifest.permission.READ_SMS)) return true // can't verify: fail safe
        val digits = sender.filter { it.isDigit() }
        if (digits.isEmpty()) return false
        return try {
            context.contentResolver.query(
                Telephony.Sms.Sent.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                "${Telephony.Sms.ADDRESS} LIKE ?",
                arrayOf("%${digits.takeLast(10)}"),
                null
            )?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Sent history lookup failed", e)
            true
        }
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
