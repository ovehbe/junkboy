package com.ovehbe.junkboy.llm

enum class SpamVerdict { SPAM, WANTED, UNSURE }

/** A second opinion on whether a text from an unknown sender is unsolicited bulk messaging. */
interface SpamJudge {
    /** Never throws: anything that goes wrong is [SpamVerdict.UNSURE]. */
    suspend fun judge(sender: String, body: String): SpamVerdict
}

/** A verdict plus a short human-readable detail (for example the score), for the settings test. */
data class JudgeResult(val verdict: SpamVerdict, val detail: String = "")

/** A judge that can also report failures, so the settings screen can show what went wrong. */
interface DetailedSpamJudge : SpamJudge {
    @Throws(java.io.IOException::class)
    suspend fun judgeOrThrow(sender: String, body: String): JudgeResult
}

/** Reads the first verdict word in a model reply; anything unrecognizable is UNSURE. */
fun parseVerdict(reply: String): SpamVerdict {
    val upper = reply.uppercase()
    return listOf("SPAM" to SpamVerdict.SPAM, "WANTED" to SpamVerdict.WANTED, "UNSURE" to SpamVerdict.UNSURE)
        .map { (word, verdict) -> upper.indexOf(word) to verdict }
        .filter { (index, _) -> index >= 0 }
        .minByOrNull { (index, _) -> index }
        ?.second ?: SpamVerdict.UNSURE
}
