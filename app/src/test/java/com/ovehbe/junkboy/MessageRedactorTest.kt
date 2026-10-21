package com.ovehbe.junkboy

import com.ovehbe.junkboy.llm.MessageRedactor
import com.ovehbe.junkboy.llm.SpamVerdict
import com.ovehbe.junkboy.llm.parseVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageRedactorTest {

    @Test
    fun longNumbersAreRemovedButShortOnesStay() {
        val out = MessageRedactor.redact("Code 482913, call 8005551234, chip in $5 by 10/23 at 4:00 PM")
        assertFalse(out.contains("482913"))
        assertFalse(out.contains("8005551234"))
        assertTrue(out.contains("$5"))
        assertTrue(out.contains("4:00 PM"))
    }

    @Test
    fun linksAndEmailAddressesAreRemoved() {
        val out = MessageRedactor.redact(
            "Donate: https://secure.example.com/l/8HxfK5 or www.foo.org/x or action.dscc.org/1p08 mail me@x.org"
        )
        assertFalse(out.contains("example.com"))
        assertFalse(out.contains("foo.org"))
        assertFalse(out.contains("dscc"))
        assertFalse(out.contains("me@x.org"))
        assertTrue(out.contains("<link>"))
        assertTrue(out.contains("<email>"))
    }

    @Test
    fun plainTextIsKeptAndLongMessagesAreTruncated() {
        assertEquals("Hi Alon, your appointment is confirmed.", MessageRedactor.redact("Hi Alon, your appointment is confirmed."))
        assertEquals(600, MessageRedactor.redact("a".repeat(2000)).length)
    }

    @Test
    fun senderKindHidesTheNumber() {
        assertEquals("short code", MessageRedactor.senderKind("21862"))
        assertEquals("ordinary phone number", MessageRedactor.senderKind("+12029894279"))
        assertEquals("alphanumeric name", MessageRedactor.senderKind("BANKNAME"))
    }

    @Test
    fun verdictIsTheFirstRecognizedWord() {
        assertEquals(SpamVerdict.SPAM, parseVerdict("SPAM"))
        assertEquals(SpamVerdict.WANTED, parseVerdict(" wanted\n"))
        assertEquals(SpamVerdict.UNSURE, parseVerdict("UNSURE."))
        assertEquals(SpamVerdict.SPAM, parseVerdict("I think SPAM, not WANTED"))
        assertEquals(SpamVerdict.UNSURE, parseVerdict("maybe"))
        assertEquals(SpamVerdict.UNSURE, parseVerdict(""))
    }
}
