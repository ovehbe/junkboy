package com.ovehbe.junkboy

import com.ovehbe.junkboy.llm.ModelKind
import com.ovehbe.junkboy.llm.OpenRouterModels
import com.ovehbe.junkboy.llm.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenRouterModelsTest {

    private fun entry(id: String, prompt: String?, completion: String?, input: String = "\"text\"", output: String = "\"text\""): String {
        val pricing = if (prompt == null) "" else """"pricing": {"prompt": "$prompt", "completion": "$completion"},"""
        return """{"id": "$id", "name": "Name of $id", $pricing
            "architecture": {"input_modalities": [$input], "output_modalities": [$output]}}"""
    }

    private val json = """{"data": [
        ${entry("b/model", "0.0000001", "0.0000005", input = "\"text\", \"image\"")},
        ${entry("a/free", "0", "0")},
        ${entry("c/router", "-1", "-1")},
        ${entry("d/draws", "0.000001", "0.000002", output = "\"image\", \"text\"")},
        ${entry("e/no-price", null, null)},
        ${entry("f/audio-in", "0.000001", "0.000002", input = "\"audio\"")},
        ${entry("g/decide", "0.00000001", "0", output = "\"decisions\"")}
    ]}"""

    @Test
    fun keepsPricedTextOnlyModelsSortedById() {
        assertEquals(listOf("a/free", "b/model", "g/decide"), OpenRouterModels.parse(json).map { it.id })
    }

    @Test
    fun tellsChatModelsFromDecisionModels() {
        val kinds = OpenRouterModels.parse(json).associate { it.id to it.kind }
        assertEquals(ModelKind.CHAT, kinds["b/model"])
        assertEquals(ModelKind.DECISION, kinds["g/decide"])
    }

    @Test
    fun convertsPerTokenPricesToPerMillion() {
        val model = OpenRouterModels.parse(json).first { it.id == "b/model" }
        assertEquals(0.1, model.inputPerMillion, 1e-9)
        assertEquals(0.5, model.outputPerMillion, 1e-9)
        assertEquals("Name of b/model", model.name)
    }

    @Test
    fun labelsAndEstimatesCost() {
        val models = OpenRouterModels.parse(json)
        assertEquals("free", models.first { it.id == "a/free" }.priceLabel)
        val haiku = models.first { it.id == "b/model" }
        assertEquals("\$0.10 in / \$0.50 out per M tokens", haiku.priceLabel)
        // 1000 messages x (480 x $0.10 + 6 x $0.50) per million tokens
        assertEquals(0.051, haiku.costPer1000Messages, 1e-9)

        val decide = models.first { it.id == "g/decide" }
        assertEquals("\$0.01 per M input tokens", decide.priceLabel)
        // 1000 messages x 490 tokens x $0.01 per million tokens, no output charge
        assertEquals(0.0049, decide.costPer1000Messages, 1e-9)
    }

    @Test
    fun formatsMoneyCompactly() {
        assertEquals("\$0.10", formatMoney(0.1))
        assertEquals("\$5.00", formatMoney(5.0))
        assertEquals("\$0.05", formatMoney(0.05))
        assertEquals("\$0.038", formatMoney(0.0375))
        assertEquals("\$0.033", formatMoney(0.0325))
    }

    @Test
    fun emptyCatalogParsesToNothing() {
        assertEquals(emptyList<Any>(), OpenRouterModels.parse("""{"data": []}"""))
        assertNull(OpenRouterModels.parse(json).firstOrNull { it.id == "c/router" })
    }
}
