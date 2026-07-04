package com.ovehbe.junkboy.ui.compose.screens

import com.ovehbe.junkboy.database.FilterType
import com.ovehbe.junkboy.database.FilteredMessage
import com.ovehbe.junkboy.database.MessageCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SmsConversationFallbacksTest {

    @Test
    fun createsConversationFromFilteredMessagesWhenSystemProviderHasNoThread() {
        val filteredMessage = filteredMessage(
            sender = "SamsungBank",
            body = "Your login code is 123456",
            timestamp = 2_000L,
            category = MessageCategory.TRANSACTION
        )

        val conversations = SmsConversationFallbacks.merge(
            systemConversations = emptyList(),
            filteredMessages = listOf(filteredMessage)
        )

        assertEquals(1, conversations.size)
        assertEquals("SamsungBank", conversations[0].address)
        assertEquals("Your login code is 123456", conversations[0].snippet)
        assertEquals(MessageCategory.TRANSACTION, conversations[0].category)
        assertTrue(conversations[0].isLocalOnly)
    }

    @Test
    fun doesNotDuplicateFilteredMessageWhenSystemConversationAlreadyExists() {
        val systemConversation = SmsConversation(
            threadId = 42L,
            address = "+1 (555) 010-1234",
            displayName = "Alice",
            snippet = "Provider copy",
            messageCount = 1,
            unreadCount = 0,
            lastMessageDate = Date(1_000L)
        )

        val filteredMessage = filteredMessage(
            sender = "5550101234",
            body = "Room copy",
            timestamp = 2_000L,
            category = MessageCategory.NOTIFICATION
        )

        val conversations = SmsConversationFallbacks.merge(
            systemConversations = listOf(systemConversation),
            filteredMessages = listOf(filteredMessage)
        )

        assertEquals(1, conversations.size)
        assertEquals(42L, conversations[0].threadId)
        assertEquals(MessageCategory.NOTIFICATION, conversations[0].category)
        assertEquals("Provider copy", conversations[0].snippet)
    }

    @Test
    fun buildsMessagesForLocalOnlyConversationFromFilteredMessages() {
        val older = filteredMessage("Bank", "Older", 1_000L, MessageCategory.TRANSACTION)
        val newer = filteredMessage("Bank", "Newer", 2_000L, MessageCategory.TRANSACTION)

        val messages = SmsConversationFallbacks.localMessagesForSender(
            sender = "Bank",
            filteredMessages = listOf(newer, older)
        )

        assertEquals(listOf("Older", "Newer"), messages.map { it.body })
        assertEquals(MessageCategory.TRANSACTION, messages.last().category)
    }

    @Test
    fun excludesBlockedMessagesFromLocalOnlyConversationDetails() {
        val blocked = filteredMessage("Bank", "Blocked", 1_000L, MessageCategory.JUNK).copy(isBlocked = true)
        val allowed = filteredMessage("Bank", "Allowed", 2_000L, MessageCategory.TRANSACTION)

        val messages = SmsConversationFallbacks.localMessagesForSender(
            sender = "Bank",
            filteredMessages = listOf(blocked, allowed)
        )

        assertEquals(listOf("Allowed"), messages.map { it.body })
    }

    @Test
    fun latestCategoryBySenderUsesNewestMessage() {
        val older = filteredMessage("95588", "Older", 1_000L, MessageCategory.PROMOTION)
        val newer = filteredMessage("95588", "Newer", 2_000L, MessageCategory.TRANSACTION)

        val cache = SmsConversationFallbacks.latestCategoryBySender(listOf(newer, older))

        assertEquals(MessageCategory.TRANSACTION, cache["95588"])
    }

    @Test
    fun resolvesCategoryAcrossPhoneNumberFormattingDifferences() {
        val cache = SmsConversationFallbacks.latestCategoryBySender(
            listOf(filteredMessage("5550101234", "Hi", 1_000L, MessageCategory.NOTIFICATION))
        )

        assertEquals(
            MessageCategory.NOTIFICATION,
            SmsConversationFallbacks.resolveCategory("+1 (555) 010-1234", cache)
        )
    }

    private fun filteredMessage(
        sender: String,
        body: String,
        timestamp: Long,
        category: MessageCategory
    ) = FilteredMessage(
        id = timestamp,
        sender = sender,
        messageBody = body,
        receivedAt = Date(timestamp),
        category = category,
        confidence = 1.0f,
        filterType = FilterType.ML_CLASSIFICATION,
        isBlocked = false,
        isUserOverride = false,
        isRead = false
    )
}
