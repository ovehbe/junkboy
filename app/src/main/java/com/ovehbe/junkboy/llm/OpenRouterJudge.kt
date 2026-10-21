package com.ovehbe.junkboy.llm

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Asks a chat model on OpenRouter (https://openrouter.ai) for a [SpamVerdict]. Messages are redacted first. */
class OpenRouterJudge(
    private val apiKey: String,
    private val model: String
) : DetailedSpamJudge {

    companion object {
        private const val TAG = "OpenRouterJudge"

        internal const val SYSTEM_PROMPT =
            "You judge text messages (SMS) received by one person from senders they have never texted. " +
                "Answer with exactly one word.\n" +
                "SPAM: the sender is trying to get something from the person: money, votes, signatures, " +
                "survey answers, a purchase, a click on a promotion, or a job application. This includes " +
                "political and advocacy campaigns, fundraising, petitions, polls, marketing, recruiting and scams.\n" +
                "WANTED: account, service and transaction messages: welcome, activation, sign-up or " +
                "subscription confirmations, alert setup, login or verification codes, appointments and health " +
                "care, bank or card notices, deliveries, receipts, order updates, waitlists and reservations, " +
                "and service status. These are WANTED even when they mention STOP, HELP or message and data " +
                "rates, because that boilerplate appears in all texts from short codes.\n" +
                "UNSURE: you cannot tell. Also use UNSURE, not SPAM, for feedback requests, review requests, " +
                "satisfaction surveys, coupons and promotions that come from a business the person has visited " +
                "or bought from, since those are mildly annoying but not unwanted spam.\n" +
                "Reply with only SPAM, WANTED or UNSURE."
    }

    override suspend fun judge(sender: String, body: String): SpamVerdict =
        try {
            judgeOrThrow(sender, body).verdict
        } catch (e: Exception) {
            Log.w(TAG, "Judge failed, treating as UNSURE: ${e.message}")
            SpamVerdict.UNSURE
        }

    override suspend fun judgeOrThrow(sender: String, body: String): JudgeResult = withContext(Dispatchers.IO) {
        val messages = JsonArray().apply {
            add(JsonObject().apply { addProperty("role", "system"); addProperty("content", SYSTEM_PROMPT) })
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty(
                    "content",
                    "Sender: ${MessageRedactor.senderKind(sender)}\nMessage: ${MessageRedactor.redact(body)}"
                )
            })
        }
        val request = JsonObject().apply {
            addProperty("model", model)
            addProperty("temperature", 0)
            addProperty("max_tokens", 10)
            add("messages", messages)
        }
        val reply = OpenRouterHttp.post("/api/v1/chat/completions", apiKey, request)
        JudgeResult(parseVerdict(extractContent(reply)))
    }

    private fun extractContent(json: String): String =
        JsonParser.parseString(json).asJsonObject
            .getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
            ?.getAsJsonObject("message")?.get("content")?.takeIf { !it.isJsonNull }?.asString
            ?: throw IOException("Reply had no content")
}
