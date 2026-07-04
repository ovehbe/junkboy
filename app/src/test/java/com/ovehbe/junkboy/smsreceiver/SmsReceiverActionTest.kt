package com.ovehbe.junkboy.smsreceiver

import android.provider.Telephony
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsReceiverActionTest {

    @Test
    fun defaultSmsAppProcessesDeliverAndReceivedForSamsungCompatibility() {
        assertTrue(
            SmsReceiver.shouldProcessAction(
                action = Telephony.Sms.Intents.SMS_DELIVER_ACTION,
                isDefaultSmsApp = true
            )
        )

        assertTrue(
            SmsReceiver.shouldProcessAction(
                action = Telephony.Sms.Intents.SMS_RECEIVED_ACTION,
                isDefaultSmsApp = true
            )
        )
    }

    @Test
    fun nonDefaultSmsAppObservesReceivedOnly() {
        assertTrue(
            SmsReceiver.shouldProcessAction(
                action = Telephony.Sms.Intents.SMS_RECEIVED_ACTION,
                isDefaultSmsApp = false
            )
        )

        assertFalse(
            SmsReceiver.shouldProcessAction(
                action = Telephony.Sms.Intents.SMS_DELIVER_ACTION,
                isDefaultSmsApp = false
            )
        )
    }
}
