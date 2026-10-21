package com.ovehbe.junkboy

import com.ovehbe.junkboy.filters.AllowRule
import com.ovehbe.junkboy.filters.ContentAllowlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentAllowlistTest {

    private val dsa = listOf(AllowRule("DSA"))

    private fun matches(rules: List<AllowRule>, body: String) = ContentAllowlist.firstMatch(rules, body) != null

    @Test
    fun wordMatchesWholeWordIgnoringCase() {
        assertTrue(matches(dsa, "DSA: join us Saturday. Reply STOP to end"))
        assertTrue(matches(dsa, "from the dsa chapter"))
        assertTrue(matches(dsa, "Message from (DSA)"))
    }

    @Test
    fun wordDoesNotMatchInsideOtherWords() {
        assertTrue(!matches(dsa, "The ADSAB report"))
        assertTrue(!matches(dsa, "dsal"))
        assertTrue(!matches(dsa, "Vote now! Reply STOP to end"))
    }

    @Test
    fun phraseMatchesAndSpecialCharactersAreLiteral() {
        val rules = listOf(AllowRule("Democratic Socialists of America"), AllowRule("a.b"))
        assertTrue(matches(rules, "The Democratic Socialists of America invite you"))
        assertTrue(matches(rules, "see a.b now"))
        assertTrue(!matches(rules, "see axb now"))
    }

    @Test
    fun regexMatchesAnywhereIgnoringCase() {
        val rules = listOf(AllowRule("chapter \\d+", isRegex = true))
        assertTrue(matches(rules, "Join Chapter 12 tonight"))
        assertTrue(!matches(rules, "Join the chapter tonight"))
    }

    @Test
    fun firstMatchReturnsTheMatchingRule() {
        val rules = listOf(AllowRule("foo"), AllowRule("DSA"))
        assertEquals(AllowRule("DSA"), ContentAllowlist.firstMatch(rules, "hello dsa"))
        assertNull(ContentAllowlist.firstMatch(rules, "nothing here"))
    }

    @Test
    fun invalidRegexIsRejectedAndSkipped() {
        val bad = AllowRule("(unclosed", isRegex = true)
        assertNotNull(ContentAllowlist.validate(bad))
        assertNull(ContentAllowlist.validate(AllowRule("ok\\d", isRegex = true)))
        assertNotNull(ContentAllowlist.validate(AllowRule("  ")))
        assertTrue(!matches(listOf(bad), "(unclosed"))
        assertTrue(matches(listOf(bad, AllowRule("DSA")), "DSA meeting"))
    }

    @Test
    fun emptyRuleListMatchesNothing() {
        assertTrue(!matches(emptyList(), "anything"))
    }
}
