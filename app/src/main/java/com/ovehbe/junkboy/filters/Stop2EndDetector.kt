package com.ovehbe.junkboy.filters

/**
 * Detects commercial opt-out directives ("Reply STOP to end", "STOP2END", "END2END", ...).
 *
 * Deliberately stricter than a bare `stop`/`end` match so ordinary text ("the bus stop is closed")
 * never qualifies. A directive that closes the message is a spam signal; the "STOP2END" form
 * elsewhere in a message is not, so it needs independent spam classification to justify a reply.
 */
object Stop2EndDetector {

    private val VERBS = "reply|respond|text|txt|send"
    private val KEYWORDS = "stop|end"
    private val ACTIONS = "end|cancel|quit|unsubscribe"
    private const val QUOTE = "[\"'”’]?"

    // "Reply STOP", optionally followed by a short tail such as "to opt out"
    private val VERB_FORM =
        "(?:$VERBS)\\s+[\"'“‘]?(?:$KEYWORDS)$QUOTE(?:\\s*(?:to|2)\\s+[a-z]+(?:\\s+[a-z]+){0,2})?"

    // "STOP2END", "STOP to unsubscribe"
    private val BARE_FORM = "(?:$KEYWORDS)\\s*(?:2|to)\\s*(?:$ACTIONS)"

    // Only closing punctuation and whitespace may follow the directive
    private const val TRAILER = "[\\s.!?\"'”’)]*$"

    // Closes the message: counts as a spam signal
    private val AT_END = Regex("\\b(?:$VERB_FORM|$BARE_FORM)$TRAILER", RegexOption.IGNORE_CASE)

    // The "Stop2End" form anywhere in the message: not a spam signal by itself
    private val ANYWHERE = Regex("\\b$BARE_FORM\\b", RegexOption.IGNORE_CASE)

    enum class Directive { NONE, ANYWHERE, AT_END }

    fun detect(body: String): Directive = when {
        AT_END.containsMatchIn(body) -> Directive.AT_END
        ANYWHERE.containsMatchIn(body) -> Directive.ANYWHERE
        else -> Directive.NONE
    }

    /** True for any directive, anywhere in the message. */
    fun matches(body: String): Boolean = detect(body) != Directive.NONE
}
