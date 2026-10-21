package com.ovehbe.junkboy

import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.filters.Stop2EndDetector.Directive
import com.ovehbe.junkboy.filters.Stop2EndPolicy
import com.ovehbe.junkboy.filters.Stop2EndPolicy.Action
import com.ovehbe.junkboy.llm.SpamJudge
import com.ovehbe.junkboy.llm.SpamVerdict
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class Stop2EndPolicyTest {

    private class FakeJudge(private val verdict: SpamVerdict) : SpamJudge {
        var calls = 0
        override suspend fun judge(sender: String, body: String): SpamVerdict {
            calls++
            return verdict
        }
    }

    private fun decide(
        directive: Directive,
        category: MessageCategory = MessageCategory.GENERAL,
        flagged: Boolean = false,
        otp: Boolean = false,
        requireSpam: Boolean = true,
        judge: SpamJudge? = null
    ) = runBlocking {
        Stop2EndPolicy.decide(directive, category, flagged, otp, requireSpam, judge, "12345", "body")
    }

    @Test
    fun noDirectiveOrAnOtpMeansNothingAndNoJudgeCall() {
        val judge = FakeJudge(SpamVerdict.SPAM)
        assertEquals(Action.NONE, decide(Directive.NONE, flagged = true, judge = judge).action)
        assertEquals(Action.NONE, decide(Directive.AT_END, flagged = true, otp = true, judge = judge).action)
        assertEquals(0, judge.calls)
    }

    @Test
    fun classifierFlaggedMessagesDoNotNeedTheJudge() {
        val judge = FakeJudge(SpamVerdict.WANTED)
        assertEquals(Action.SUPPRESS_AND_REPLY, decide(Directive.AT_END, flagged = true, judge = judge).action)
        assertEquals(Action.REPLY_ONLY, decide(Directive.ANYWHERE, flagged = true, judge = judge).action)
        assertEquals(0, judge.calls)
    }

    @Test
    fun judgeDecidesWhenTheClassifierDidNotFlag() {
        val spam = FakeJudge(SpamVerdict.SPAM)
        assertEquals(Action.SUPPRESS_AND_REPLY, decide(Directive.AT_END, judge = spam).action)
        assertEquals(Action.REPLY_ONLY, decide(Directive.ANYWHERE, judge = spam).action)
        assertEquals("Judged SPAM", decide(Directive.AT_END, judge = spam).reason)

        for (verdict in listOf(SpamVerdict.WANTED, SpamVerdict.UNSURE)) {
            assertEquals(Action.NONE, decide(Directive.AT_END, judge = FakeJudge(verdict)).action)
            assertEquals(Action.NONE, decide(Directive.ANYWHERE, judge = FakeJudge(verdict)).action)
        }
    }

    @Test
    fun judgeIsConsultedEvenForTransactionAndNotificationCategories() {
        val judge = FakeJudge(SpamVerdict.SPAM)
        assertEquals(Action.SUPPRESS_AND_REPLY, decide(Directive.AT_END, MessageCategory.TRANSACTION, judge = judge).action)
        assertEquals(Action.SUPPRESS_AND_REPLY, decide(Directive.AT_END, MessageCategory.NOTIFICATION, judge = judge).action)
        assertEquals(2, judge.calls)
    }

    @Test
    fun withoutAJudgeWantedLookingCategoriesAreLeftAlone() {
        assertEquals(Action.NONE, decide(Directive.AT_END, MessageCategory.TRANSACTION, requireSpam = false).action)
        assertEquals(Action.NONE, decide(Directive.AT_END, MessageCategory.NOTIFICATION, requireSpam = false).action)
    }

    @Test
    fun withoutAJudgeTheRequireSpamSettingDecidesClosingDirectives() {
        assertEquals(Action.NONE, decide(Directive.AT_END, requireSpam = true).action)
        val trusted = decide(Directive.AT_END, requireSpam = false)
        assertEquals(Action.SUPPRESS_AND_REPLY, trusted.action)
        assertEquals("Closing opt-out directive", trusted.reason)
        // A directive elsewhere is never trusted on its own
        assertEquals(Action.NONE, decide(Directive.ANYWHERE, requireSpam = false).action)
    }
}
