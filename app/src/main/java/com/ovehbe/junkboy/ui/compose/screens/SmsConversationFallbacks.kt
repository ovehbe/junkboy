package com.ovehbe.junkboy.ui.compose.screens

import android.provider.Telephony
import com.ovehbe.junkboy.database.FilteredMessage
import java.util.Date
import kotlin.math.abs

object SmsConversationFallbacks {
    fun latestCategoryBySender(
        filteredMessages: List<FilteredMessage>
    ): Map<String, com.ovehbe.junkboy.database.MessageCategory> {
        return filteredMessages
            .groupBy { normalizeSender(it.sender) }
            .mapValues { (_, messages) -> messages.maxBy { it.receivedAt.time }.category }
    }

    fun resolveCategory(
        sender: String,
        categoryCache: Map<String, com.ovehbe.junkboy.database.MessageCategory>
    ): com.ovehbe.junkboy.database.MessageCategory? {
        return categoryCache[normalizeSender(sender)]
    }

    fun merge(
        systemConversations: List<SmsConversation>,
        filteredMessages: List<FilteredMessage>
    ): List<SmsConversation> {
        val conversationsBySender = systemConversations.associateBy { normalizeSender(it.address) }
        val latestFilteredBySender = filteredMessages
            .filter { !it.isBlocked }
            .groupBy { normalizeSender(it.sender) }
            .mapValues { (_, messages) -> messages.maxBy { it.receivedAt.time } }

        val enhancedSystemConversations = systemConversations.map { conversation ->
            val latestFiltered = latestFilteredBySender[normalizeSender(conversation.address)]
            if (latestFiltered != null) {
                conversation.copy(category = latestFiltered.category)
            } else {
                conversation
            }
        }

        val localOnlyConversations = latestFilteredBySender
            .filterKeys { normalizedSender -> normalizedSender !in conversationsBySender }
            .values
            .map { message ->
                val senderMessages = filteredMessages.filter {
                    !it.isBlocked && normalizeSender(it.sender) == normalizeSender(message.sender)
                }
                SmsConversation(
                    threadId = syntheticThreadId(message.sender),
                    address = message.sender,
                    displayName = message.sender,
                    snippet = message.messageBody,
                    messageCount = senderMessages.size,
                    unreadCount = senderMessages.count { !it.isRead },
                    lastMessageDate = message.receivedAt,
                    category = message.category,
                    isLocalOnly = true
                )
            }

        return (enhancedSystemConversations + localOnlyConversations)
            .sortedByDescending { it.lastMessageDate.time }
    }

    fun localMessagesForSender(
        sender: String,
        filteredMessages: List<FilteredMessage>
    ): List<SmsMessage> {
        val normalizedSender = normalizeSender(sender)
        return filteredMessages
            .filter { !it.isBlocked && normalizeSender(it.sender) == normalizedSender }
            .sortedBy { it.receivedAt.time }
            .map { message ->
                SmsMessage(
                    id = -abs(message.id),
                    address = message.sender,
                    body = message.messageBody,
                    date = message.receivedAt,
                    type = Telephony.Sms.MESSAGE_TYPE_INBOX,
                    read = message.isRead,
                    category = message.category
                )
            }
    }

    fun syntheticThreadId(sender: String): Long {
        return -abs(sender.hashCode().toLong()) - 1L
    }

    private fun normalizeSender(sender: String): String {
        val digits = sender.filter { it.isDigit() }
        return if (digits.length >= 7) {
            digits.takeLast(10)
        } else {
            sender.trim().uppercase()
        }
    }
}
