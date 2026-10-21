package com.ovehbe.junkboy.llm

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** CHAT models write text; DECISION models return a probability per yes/no question (alpha Decisions API). */
enum class ModelKind(val stored: String, val label: String) {
    CHAT("chat", "Chat"),
    DECISION("decision", "Decision");

    companion object {
        fun fromStored(value: String?): ModelKind? = entries.firstOrNull { it.stored == value }
    }
}

/** A model from OpenRouter's public catalog, with prices in dollars per million tokens. */
data class OpenRouterModel(
    val id: String,
    val name: String,
    val inputPerMillion: Double,
    val outputPerMillion: Double,
    val kind: ModelKind = ModelKind.CHAT
) {
    /** Measured on real messages: about 480 tokens in and 6 out for a chat judge, about 490 in for a decision judge. */
    val costPer1000Messages: Double
        get() = when (kind) {
            ModelKind.CHAT -> 1000 * (CHAT_INPUT_TOKENS * inputPerMillion + CHAT_OUTPUT_TOKENS * outputPerMillion) / 1_000_000
            ModelKind.DECISION -> 1000 * DECISION_INPUT_TOKENS * inputPerMillion / 1_000_000
        }

    val priceLabel: String
        get() = when {
            inputPerMillion == 0.0 && outputPerMillion == 0.0 -> "free"
            kind == ModelKind.DECISION -> "${formatMoney(inputPerMillion)} per M input tokens"
            else -> "${formatMoney(inputPerMillion)} in / ${formatMoney(outputPerMillion)} out per M tokens"
        }

    companion object {
        private const val CHAT_INPUT_TOKENS = 480
        private const val CHAT_OUTPUT_TOKENS = 6
        private const val DECISION_INPUT_TOKENS = 490
    }
}

fun formatMoney(dollars: Double): String {
    val text = String.format(Locale.US, if (dollars >= 0.1) "%.2f" else "%.3f", dollars)
    // Keep at least cents, but drop a pointless third decimal (0.050 -> 0.05)
    return "$" + if (text.endsWith("0") && text.substringAfter('.').length == 3) text.dropLast(1) else text
}

object OpenRouterModels {

    // "all" so that decision models, which return scores instead of text, are included
    private const val ENDPOINT = "https://openrouter.ai/api/v1/models?output_modalities=all"

    const val DEFAULT_MODEL = "respan/span-02"
    val DEFAULT_KIND = ModelKind.DECISION

    @Volatile
    private var cached: List<OpenRouterModel>? = null

    /** The catalog is public: no API key needed. Cached for the life of the process. */
    suspend fun load(): List<OpenRouterModel> = cached ?: fetch().also { cached = it }

    private suspend fun fetch(): List<OpenRouterModel> = withContext(Dispatchers.IO) {
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 10_000
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            parse(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Keeps models that take text and either return only text (chat) or only decisions, and have a fixed
     * price. Router entries with a negative ("varies") price are dropped. Sorted by id.
     */
    fun parse(json: String): List<OpenRouterModel> {
        val data = JsonParser.parseString(json).asJsonObject.getAsJsonArray("data") ?: return emptyList()
        return data.mapNotNull { element ->
            val model = element.asJsonObject
            val id = model.get("id")?.asString ?: return@mapNotNull null
            val pricing = model.getAsJsonObject("pricing") ?: return@mapNotNull null
            val input = pricing.get("prompt")?.toPerMillion() ?: return@mapNotNull null
            val output = pricing.get("completion")?.toPerMillion() ?: return@mapNotNull null
            if (input < 0 || output < 0) return@mapNotNull null

            val architecture = model.getAsJsonObject("architecture")
            val takesText = architecture?.getAsJsonArray("input_modalities")?.any { it.asString == "text" } ?: false
            val outputs = architecture?.getAsJsonArray("output_modalities")?.map { it.asString }
            val kind = when (outputs) {
                listOf("text") -> ModelKind.CHAT
                listOf("decisions") -> ModelKind.DECISION
                else -> return@mapNotNull null
            }
            if (!takesText) return@mapNotNull null

            OpenRouterModel(id, model.get("name")?.asString ?: id, input, output, kind)
        }.sortedBy { it.id }
    }

    private fun JsonElement.toPerMillion(): Double? =
        runCatching { asString.toDouble() * 1_000_000 }.getOrNull()
}
