package com.ovehbe.junkboy

import com.ovehbe.junkboy.filters.Stop2EndDetector
import com.ovehbe.junkboy.filters.Stop2EndDetector.Directive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stop2EndDetectorTest {

    private val positives = listOf(
            "Big sale today! Reply STOP to end",
            "Msg&data rates may apply. Text STOP to cancel.",
            "send stop to unsubscribe",
            "Reply 'STOP' to opt out",
            "Reply \"STOP\" to quit",
            "REPLY   STOP",
            "Save 20%! STOP2END",
            "Save 20%! Stop to unsubscribe",
            "txt STOP 2 cancel",
            "Alon, verify your profile.\n\nPLEASE RESPOND >> x.org/a\n\nSTOPtoEND",
            "Hi! Reply STOP to end.\n"
        )

    private val negatives = listOf(
            "Meet me at the bus stop",
            "Please don't stop by today",
            "Your code is 123456",
            "Non-stop flights from $99",
            "Stopping by at 5",
            "Stop",
            "Send end of month report"
        )

    private fun stopToEnd(text: String) = text.replace("stop", "end", ignoreCase = true)

    @Test
    fun matchesCommonOptOutDirectives() {
        positives.forEach { assertTrue("should match: $it", Stop2EndDetector.matches(it)) }
    }

    @Test
    fun matchesTheSameDirectivesUsingEnd() {
        positives.map(::stopToEnd).forEach { assertTrue("should match: $it", Stop2EndDetector.matches(it)) }
        assertTrue(Stop2EndDetector.matches("Save 20%! END2END"))
    }

    @Test
    fun ignoresOrdinaryMessages() {
        negatives.forEach { assertFalse("should not match: $it", Stop2EndDetector.matches(it)) }
    }

    @Test
    fun ignoresOrdinaryMessagesUsingEnd() {
        negatives.map(::stopToEnd).forEach { assertFalse("should not match: $it", Stop2EndDetector.matches(it)) }
    }

    @Test
    fun directiveAtEndOfMessageIsASpamSignal() {
        assertEquals(Directive.AT_END, Stop2EndDetector.detect("Alon, verify your profile.\n\nSTOPtoEND"))
        assertEquals(Directive.AT_END, Stop2EndDetector.detect("Big sale! Reply STOP to end."))
    }

    @Test
    fun stop2EndElsewhereIsNotASpamSignal() {
        assertEquals(Directive.ANYWHERE, Stop2EndDetector.detect("STOPtoEND to quit. Visit example.com now"))
        assertEquals(Directive.ANYWHERE, Stop2EndDetector.detect("Reply STOP to unsubscribe. Visit example.com now"))
        assertEquals(Directive.ANYWHERE, Stop2EndDetector.detect("The End2End tests passed!"))
    }

    @Test
    fun verbFormElsewhereIsIgnored() {
        assertEquals(Directive.NONE, Stop2EndDetector.detect("Reply STOP. Visit example.com now"))
        assertEquals(Directive.NONE, Stop2EndDetector.detect("Meet me at the bus stop"))
    }
}
