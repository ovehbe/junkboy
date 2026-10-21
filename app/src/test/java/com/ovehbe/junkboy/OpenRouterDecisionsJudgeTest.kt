package com.ovehbe.junkboy

import com.google.gson.JsonParser
import com.ovehbe.junkboy.llm.ModelKind
import com.ovehbe.junkboy.llm.OpenRouterDecisionsJudge
import com.ovehbe.junkboy.llm.OpenRouterJudges
import com.ovehbe.junkboy.llm.OpenRouterModels
import com.ovehbe.junkboy.llm.SpamVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class OpenRouterDecisionsJudgeTest {

    @Test
    fun verdictFollowsTheDifferenceBetweenTheTwoScores() {
        // spam score 0.86 vs wanted 0.03: difference 0.83
        assertEquals(SpamVerdict.SPAM, OpenRouterDecisionsJudge.decide(0.86, 0.03, 0.7))
        // spam 0.21 vs wanted 0.75: clearly wanted
        assertEquals(SpamVerdict.WANTED, OpenRouterDecisionsJudge.decide(0.21, 0.75, 0.7))
        // in between is unsure, so nothing happens
        assertEquals(SpamVerdict.UNSURE, OpenRouterDecisionsJudge.decide(0.5, 0.2, 0.7))
    }

    @Test
    fun theCutOffMovesTheSpamBoundary() {
        assertEquals(SpamVerdict.UNSURE, OpenRouterDecisionsJudge.decide(0.8, 0.2, 0.7))
        assertEquals(SpamVerdict.SPAM, OpenRouterDecisionsJudge.decide(0.8, 0.2, 0.5))
        assertEquals(SpamVerdict.UNSURE, OpenRouterDecisionsJudge.decide(0.8, 0.2, 0.9))
    }

    @Test
    fun readsBothScoresFromAReply() {
        val reply = """{"model":"m","answers":{"is_spam":{"type":"noul","noul":0.859},"is_wanted":{"type":"noul","noul":0.031}}}"""
        val (spam, wanted) = OpenRouterDecisionsJudge.scoresFrom(reply)
        assertEquals(0.859, spam, 1e-9)
        assertEquals(0.031, wanted, 1e-9)
    }

    @Test
    fun malformedRepliesAreReportedNotGuessed() {
        assertThrows(IOException::class.java) { OpenRouterDecisionsJudge.scoresFrom("""{"answers":{}}""") }
        assertThrows(IOException::class.java) { OpenRouterDecisionsJudge.scoresFrom("""{"model":"m"}""") }
        assertThrows(IOException::class.java) {
            OpenRouterDecisionsJudge.scoresFrom("""{"answers":{"is_spam":{"noul":0.9}}}""")
        }
    }

    @Test
    fun requestHasTheTwoQuestionsAndARedactedState() {
        val request = OpenRouterDecisionsJudge.buildRequest(
            "respan/span-02", "+12029894279", "Hi Alon, call 8005551234 or see https://x.example.com/a. STOP2END"
        )
        assertEquals("respan/span-02", request.get("model").asString)
        val questions = request.getAsJsonObject("questions")
        assertEquals(setOf("is_spam", "is_wanted"), questions.keySet())
        for (name in questions.keySet()) {
            val q = questions.getAsJsonObject(name)
            assertEquals("noul", q.get("type").asString)
            assertEquals(setOf("true", "false"), q.getAsJsonObject("criteria").keySet())
        }
        val state = request.get("state").asString
        assertTrue(state.contains("ordinary phone number"))
        assertFalse(state.contains("+12029894279"))
        assertFalse(state.contains("8005551234"))
        assertFalse(state.contains("example.com"))
        // the whole request must be valid JSON
        JsonParser.parseString(request.toString())
    }

    @Test
    fun pickingAJudgeFollowsTheModelKind() {
        assertTrue(OpenRouterJudges.create("k", "respan/span-02", ModelKind.DECISION, 0.7) is OpenRouterDecisionsJudge)
        assertFalse(OpenRouterJudges.create("k", "anthropic/claude-haiku-5.5", ModelKind.CHAT, 0.7) is OpenRouterDecisionsJudge)
    }

    @Test
    fun storedModelResolution() {
        // nothing stored: the default decision model
        assertEquals(OpenRouterModels.DEFAULT_MODEL to ModelKind.DECISION, OpenRouterJudges.resolve(null, null))
        assertEquals(OpenRouterModels.DEFAULT_MODEL to ModelKind.DECISION, OpenRouterJudges.resolve("  ", null))
        // an install from before decision models existed stored only a chat model id
        assertEquals("anthropic/claude-haiku-5.5" to ModelKind.CHAT, OpenRouterJudges.resolve("anthropic/claude-haiku-5.5", null))
        // the kind is respected when stored
        assertEquals("respan/span-01" to ModelKind.DECISION, OpenRouterJudges.resolve("respan/span-01", "decision"))
        assertEquals("some/chat" to ModelKind.CHAT, OpenRouterJudges.resolve("some/chat", "chat"))
        // the default id without a kind is still a decision model
        assertEquals(OpenRouterModels.DEFAULT_MODEL to ModelKind.DECISION, OpenRouterJudges.resolve(OpenRouterModels.DEFAULT_MODEL, null))
    }
}
