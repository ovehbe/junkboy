package com.ovehbe.junkboy.smsreceiver

import android.provider.Telephony
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingSmsPersistenceTest {

    @Test
    fun persistsDefaultSmsDeliverMessagesThatRemainInInbox() {
        assertTrue(
            IncomingSmsPersistence.shouldPersistToSystemInbox(
                action = Telephony.Sms.Intents.SMS_DELIVER_ACTION,
                isBlocked = false,
                autoDeleteBlockedMessages = false
            )
        )

        assertTrue(
            IncomingSmsPersistence.shouldPersistToSystemInbox(
                action = Telephony.Sms.Intents.SMS_DELIVER_ACTION,
                isBlocked = true,
                autoDeleteBlockedMessages = false
            )
        )
    }

    @Test
    fun skipsPersistenceForObservedBroadcastsAndAutoDeletedBlockedMessages() {
        assertFalse(
            IncomingSmsPersistence.shouldPersistToSystemInbox(
                action = Telephony.Sms.Intents.SMS_RECEIVED_ACTION,
                isBlocked = false,
                autoDeleteBlockedMessages = false
            )
        )

        assertFalse(
            IncomingSmsPersistence.shouldPersistToSystemInbox(
                action = Telephony.Sms.Intents.SMS_DELIVER_ACTION,
                isBlocked = true,
                autoDeleteBlockedMessages = true
            )
        )
    }
}
