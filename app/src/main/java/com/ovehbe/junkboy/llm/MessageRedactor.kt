package com.ovehbe.junkboy.llm

/**
 * Strips what a cloud model doesn't need before a message leaves the device: links, email
 * addresses and long digit runs (phone numbers, codes, account numbers). Names are NOT removed.
 */
object MessageRedactor {

    private const val MAX_LENGTH = 600

    private val EMAIL = Regex("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+")
    private val LINK = Regex("(?i)(?:https?://|www\\.)\\S+|\\b[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.[a-z]{2,}/\\S*")
    private val LONG_DIGITS = Regex("\\d{3,}")

    fun redact(body: String): String =
        body.take(MAX_LENGTH)
            .replace(EMAIL, "<email>")
            .replace(LINK, "<link>")
            .replace(LONG_DIGITS, "#")

    /** The kind of sender, without revealing the number itself. */
    fun senderKind(sender: String): String {
        val digits = sender.count { it.isDigit() }
        return when {
            sender.any { it.isLetter() } -> "alphanumeric name"
            digits <= 8 -> "short code"
            else -> "ordinary phone number"
        }
    }
}
