package com.selfbudget.app.core.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class OcrScanResult(
    val merchantName: String?,
    val totalAmount: Double?,
    val timestamp: Long?
)

object ReceiptOcrScanner {

    fun scanReceipt(
        context: Context,
        imageUri: Uri,
        onSuccess: (OcrScanResult) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val textLines = visionText.textBlocks.flatMap { block -> block.lines.map { it.text } }
                    val result = parseReceiptText(textLines)
                    onSuccess(result)
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun parseReceiptText(lines: List<String>): OcrScanResult {
        if (lines.isEmpty()) return OcrScanResult(null, null, null)

        var merchantName: String? = null
        var totalAmount: Double? = null
        var timestamp: Long? = null

        // 1. Extract Merchant Name (first non-generic top line)
        val genericKeywords = listOf("receipt", "tax invoice", "welcome", "thank you", "cashier", "order", "table", "copy", "customer")
        for (line in lines.take(5)) {
            val clean = line.trim()
            if (clean.length in 3..35 && genericKeywords.none { clean.lowercase().contains(it) }) {
                merchantName = clean
                break
            }
        }

        // 2. Extract Total Amount using Regex
        val amountPattern = Pattern.compile("(?:total|amount|due|balance|subtotal)?\\s*\\$?(\\d{1,5}\\.\\d{2})", Pattern.CASE_INSENSITIVE)
        val extractedAmounts = mutableListOf<Double>()

        for (line in lines) {
            val matcher = amountPattern.matcher(line)
            while (matcher.find()) {
                val group = matcher.group(1)
                group?.toDoubleOrNull()?.let { extractedAmounts.add(it) }
            }
        }

        if (extractedAmounts.isNotEmpty()) {
            totalAmount = extractedAmounts.maxOrNull()
        }

        // 3. Extract Date using Regex
        val datePattern = Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{2,4})")
        for (line in lines) {
            val matcher = datePattern.matcher(line)
            if (matcher.find()) {
                val month = matcher.group(1)?.toIntOrNull() ?: 1
                val day = matcher.group(2)?.toIntOrNull() ?: 1
                var year = matcher.group(3)?.toIntOrNull() ?: 2026
                if (year < 100) year += 2000

                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
                    set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
                }
                timestamp = cal.timeInMillis
                break
            }
        }

        return OcrScanResult(merchantName, totalAmount, timestamp)
    }
}
