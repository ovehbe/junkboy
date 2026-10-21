package com.ovehbe.junkboy.filters

import java.util.regex.PatternSyntaxException

/** A user-defined content rule: a word/phrase (whole-word, case-insensitive) or a regex. */
data class AllowRule(val pattern: String, val isRegex: Boolean = false)

/** Messages matching any rule bypass all filtering, like messages from an allowed sender. */
object ContentAllowlist {

    private fun compile(rule: AllowRule): Regex {
        val source = if (rule.isRegex) {
            rule.pattern
        } else {
            // Whole word/phrase: not glued to other letters or digits
            "(?<![\\p{L}\\p{N}])${Regex.escape(rule.pattern.trim())}(?![\\p{L}\\p{N}])"
        }
        return Regex(source, RegexOption.IGNORE_CASE)
    }

    /** Returns an error message if the rule can't be used, or null if it's valid. */
    fun validate(rule: AllowRule): String? {
        if (rule.pattern.isBlank()) return "Enter a word or pattern"
        return try {
            compile(rule)
            null
        } catch (e: PatternSyntaxException) {
            e.description ?: "Invalid regular expression"
        }
    }

    /** The first rule matching [body], or null. Rules that fail to compile are skipped. */
    fun firstMatch(rules: List<AllowRule>, body: String): AllowRule? =
        rules.firstOrNull { rule ->
            try {
                compile(rule).containsMatchIn(body)
            } catch (e: PatternSyntaxException) {
                false
            }
        }
}
