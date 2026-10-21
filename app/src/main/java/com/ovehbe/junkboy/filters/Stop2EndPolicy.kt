package com.ovehbe.junkboy.filters

import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.llm.SpamJudge
import com.ovehbe.junkboy.llm.SpamVerdict

/**
 * Decides what to do with a message from an unknown sender once it is known to carry (or not carry)
 * an opt-out directive. Pure logic so it can be unit tested; the caller supplies the facts.
 */
object Stop2EndPolicy {

    enum class Action { NONE, REPLY_ONLY, SUPPRESS_AND_REPLY }

    data class Decision(val action: Action, val reason: String)

    private val NO = Decision(Action.NONE, "")

    suspend fun decide(
        directive: Stop2EndDetector.Directive,
        category: MessageCategory,
        flaggedAsSpam: Boolean,
        hasOtp: Boolean,
        requireSpamClassification: Boolean,
        judge: SpamJudge?,
        sender: String,
        body: String
    ): Decision {
        if (directive == Stop2EndDetector.Directive.NONE) return NO
        // Codes are never touched, whatever else the message says
        if (hasOtp) return NO

        val reason: String? = when {
            // The classifier already calls it spam: no need to ask anyone else
            flaggedAsSpam -> "Classifier flagged it"
            // Not flagged: ask the second opinion if one is configured
            judge != null -> {
                val verdict = judge.judge(sender, body)
                if (verdict == SpamVerdict.SPAM) "Judged SPAM" else null
            }
            // No second opinion: wanted-looking categories are left alone, and a closing directive
            // is trusted on its own only if the user turned the classification requirement off
            category == MessageCategory.TRANSACTION || category == MessageCategory.NOTIFICATION -> null
            directive == Stop2EndDetector.Directive.AT_END && !requireSpamClassification ->
                "Closing opt-out directive"
            else -> null
        }
        if (reason == null) return NO

        val action = if (directive == Stop2EndDetector.Directive.AT_END) Action.SUPPRESS_AND_REPLY else Action.REPLY_ONLY
        return Decision(action, reason)
    }
}
