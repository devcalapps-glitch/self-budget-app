package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.TransactionType
import java.util.regex.Pattern

data class ParsedVoiceTransaction(
    val title: String,
    val amount: Double,
    val type: TransactionType
)

object VoiceParser {

    fun parseSpokenText(text: String): ParsedVoiceTransaction? {
        if (text.isBlank()) return null

        val lowerText = text.lowercase().trim()

        // Extract amount using regex
        val numberPattern = Pattern.compile("(\\d+(\\.\\d{1,2})?)")
        val matcher = numberPattern.matcher(lowerText)

        val amount = if (matcher.find()) {
            matcher.group(1)?.toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        // Determine transaction type
        val isIncome = lowerText.contains("income") ||
                lowerText.contains("salary") ||
                lowerText.contains("deposit") ||
                lowerText.contains("earned") ||
                lowerText.contains("received") ||
                lowerText.contains("refund")

        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // Clean title
        var title = lowerText
            .replace(numberPattern.pattern().toRegex(), "")
            .replace("dollars", "")
            .replace("dollar", "")
            .replace("bucks", "")
            .replace("spent", "")
            .replace("paid", "")
            .replace("for", "")
            .replace("on", "")
            .replace("earned", "")
            .replace("received", "")
            .replace("income", "")
            .replace("expense", "")
            .trim()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        if (title.isBlank()) {
            title = if (isIncome) "Voice Income" else "Voice Expense"
        }

        return if (amount > 0.0) {
            ParsedVoiceTransaction(title = title, amount = amount, type = type)
        } else {
            null
        }
    }
}
