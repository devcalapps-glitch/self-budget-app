package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.TransactionType
import java.util.regex.Pattern

data class ParsedVoiceTransaction(
    val title: String,
    val amount: Double,
    val type: TransactionType
)

object VoiceParser {

    // Matches numbers with optional currency symbols and words like "$15", "15.00", "15 dollars", "$10.50"
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:\$|£|€|¥|₹)?\s*(\d+(?:\.\d{1,2})?)\s*(?:dollars?|bucks?|cents?|\$|£|€|¥|₹)?""",
        Pattern.CASE_INSENSITIVE
    )

    // Action verbs, filler words, and generic classification labels
    private val STOP_WORDS_REGEX = Regex(
        """\b(spent|spend|spending|paid|pay|paying|bought|buy|buying|cost|costs|earned|earn|earning|received|receive|receiving|income|expense|dollars?|bucks?|cents?)\b""",
        RegexOption.IGNORE_CASE
    )

    // Leading and trailing prepositions/articles after amount and action verb removal
    private val LEADING_PREPOSITIONS = Regex("""^(?:(?:for|on|at|in|to|from|a|an|the)\s+)+""", RegexOption.IGNORE_CASE)
    private val TRAILING_PREPOSITIONS = Regex("""(?:\s+(?:for|on|at|in|to|from|a|an|the))+$""", RegexOption.IGNORE_CASE)

    fun parseSpokenText(text: String): ParsedVoiceTransaction? {
        if (text.isBlank()) return null

        val trimmed = text.trim()

        // 1. Extract Amount using currency-aware pattern
        val matcher = AMOUNT_PATTERN.matcher(trimmed)
        var amount = 0.0
        var cleaned = trimmed

        if (matcher.find()) {
            amount = matcher.group(1)?.toDoubleOrNull() ?: 0.0
            cleaned = trimmed.removeRange(matcher.start(), matcher.end())
        }

        if (amount <= 0.0) return null

        // 2. Determine Transaction Type
        val lowerText = trimmed.lowercase()
        val isIncome = Regex("""\b(income|salary|deposit|earned|earn|refund|paycheck|received|dividend)\b""").containsMatchIn(lowerText)
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // 3. Clean isolated currency symbols, punctuation, and action verbs
        cleaned = cleaned
            .replace(Regex("""[\$£€¥₹,;!?]"""), " ")
            .replace(STOP_WORDS_REGEX, " ")

        // 4. Collapse extra whitespace
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()

        // 5. Strip dangling prepositions/articles at start and end
        cleaned = cleaned
            .replace(LEADING_PREPOSITIONS, "")
            .replace(TRAILING_PREPOSITIONS, "")
            .trim()

        // 6. Format title using design system Word Title Case
        val finalTitle = if (cleaned.isNotBlank()) {
            cleaned.toWordTitleCase()
        } else {
            if (isIncome) "Voice Income" else "Voice Expense"
        }

        return ParsedVoiceTransaction(title = finalTitle, amount = amount, type = type)
    }
}
