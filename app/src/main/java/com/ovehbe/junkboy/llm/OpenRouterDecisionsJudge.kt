package com.ovehbe.junkboy.llm

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/**
 * Asks a "decision" model on OpenRouter's alpha Decisions API. Instead of writing text, these models
 * return a probability for each yes/no question, which makes "unsure" a threshold the user controls.
 *
 * Two questions are asked, whether the text is unsolicited bulk messaging and whether it is about
 * something the person set up or did; the verdict is based on the difference between the two scores.
 */
class OpenRouterDecisionsJudge(
    private val apiKey: String,
    private val model: String,
    private val spamCut: Double = DEFAULT_CUT
) : DetailedSpamJudge {

    companion object {
        private const val TAG = "OpenRouterDecisions"
        const val DEFAULT_CUT = 0.7
        private const val WANTED_CUT = -0.5

        /** SPAM when the spam score beats the wanted score by at least [cut]; WANTED when it trails by half. */
        internal fun decide(spamScore: Double, wantedScore: Double, cut: Double): SpamVerdict {
            val difference = spamScore - wantedScore
            return when {
                difference >= cut -> SpamVerdict.SPAM
                difference <= WANTED_CUT -> SpamVerdict.WANTED
                else -> SpamVerdict.UNSURE
            }
        }

        private fun question(instructions: String, whenTrue: String, whenFalse: String) = JsonObject().apply {
            addProperty("type", "noul")
            addProperty("instructions", instructions)
            add("criteria", JsonObject().apply {
                addProperty("true", whenTrue)
                addProperty("false", whenFalse)
            })
        }

        internal fun buildRequest(model: String, sender: String, body: String) = JsonObject().apply {
            addProperty("model", model)
            add("questions", JsonObject().apply {
                add(
                    "is_spam",
                    question(
                        "A text message was received by one person from a sender they have never texted. " +
                            "Is it unsolicited bulk messaging?",
                        "The sender is trying to get something from the person: money, votes, signatures, " +
                            "survey answers, a purchase, a click on a promotion, or a job. Includes political " +
                            "and advocacy campaigns, fundraising, petitions, polls, marketing, recruiting and scams.",
                        "The message is not an attempt to get money, votes, signatures, survey answers, " +
                            "purchases or clicks from the person."
                    )
                )
                add(
                    "is_wanted",
                    question(
                        "A text message was received by one person from a sender they have never texted. " +
                            "Is it a message about something the person set up or did?",
                        "An account, service or transaction message: welcome or sign-up confirmation, login or " +
                            "verification code, appointment or health care, bank or card notice, delivery, " +
                            "receipt, order update, waitlist or reservation. Wanted even if it mentions STOP, " +
                            "HELP or message and data rates.",
                        "The message is not about an account, service, appointment, delivery or transaction " +
                            "the person set up or did."
                    )
                )
            })
            addProperty("state", "Sender: ${MessageRedactor.senderKind(sender)}\nMessage: ${MessageRedactor.redact(body)}")
        }

        /** Returns (spam score, wanted score) from a Decisions reply. */
        @Throws(IOException::class)
        internal fun scoresFrom(json: String): Pair<Double, Double> {
            val answers = JsonParser.parseString(json).asJsonObject.getAsJsonObject("answers")
                ?: throw IOException("Reply had no answers")
            fun score(name: String) = answers.getAsJsonObject(name)?.get("noul")?.takeIf { it.isJsonPrimitive }?.asDouble
                ?: throw IOException("Reply had no score for $name")
            return score("is_spam") to score("is_wanted")
        }
    }

    override suspend fun judge(sender: String, body: String): SpamVerdict =
        try {
            judgeOrThrow(sender, body).verdict
        } catch (e: Exception) {
            Log.w(TAG, "Judge failed, treating as UNSURE: ${e.message}")
            SpamVerdict.UNSURE
        }

    override suspend fun judgeOrThrow(sender: String, body: String): JudgeResult = withContext(Dispatchers.IO) {
        val reply = OpenRouterHttp.post("/api/alpha/decisions", apiKey, buildRequest(model, sender, body))
        val (spam, wanted) = scoresFrom(reply)
        JudgeResult(decide(spam, wanted, spamCut), String.format(Locale.US, "score %+.2f", spam - wanted))
    }
}
