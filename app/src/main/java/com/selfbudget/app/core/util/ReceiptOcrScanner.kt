package com.selfbudget.app.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
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

    fun createTempReceiptUri(context: Context): Uri {
        val tempFile = File.createTempFile("receipt_", ".jpg", context.cacheDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

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

    fun parseReceiptText(lines: List<String>): OcrScanResult {
        if (lines.isEmpty()) return OcrScanResult(null, null, null)

        val merchantName = extractMerchantName(lines)
        val totalAmount = extractTotalAmount(lines)
        val timestamp = extractDate(lines)

        return OcrScanResult(merchantName, totalAmount, timestamp)
    }

    private val KNOWN_MERCHANT_BRANDS = listOf(
        "Chipotle", "Starbucks", "McDonald's", "Target", "Walmart", "Costco", "Trader Joe's",
        "Whole Foods", "Subway", "Panera Bread", "Chick-fil-A", "Taco Bell", "Wendy's",
        "Burger King", "In-N-Out Burger", "Dunkin", "Panda Express", "CVS", "Walgreens",
        "Home Depot", "Lowe's", "Best Buy", "Apple", "Kroger", "Safeway", "Publix", "HEB",
        "Aldi", "Five Guys", "Shake Shack", "Sweetgreen", "Cava", "Peet's Coffee", "Dutch Bros",
        "Jamba Juice", "Jersey Mike's", "Jimmy John's", "Domino's", "Pizza Hut", "Papa John's",
        "KFC", "Popeyes", "Wingstop", "Waffle House", "IHOP", "Denny's", "Cheesecake Factory",
        "Olive Garden", "Texas Roadhouse", "Outback Steakhouse", "Red Lobster", "Buffalo Wild Wings",
        "Applebee's", "Chili's", "Cracker Barrel", "Sephora", "Ulta", "TJ Maxx", "Marshalls",
        "Ross", "Nordstrom", "Macy's", "Kohl's", "Dollar Tree", "Dollar General", "7-Eleven",
        "Circle K", "Wawa", "Sheetz", "Chevron", "Shell", "Exxon", "Mobil", "BP"
    )

    private val NON_MERCHANT_KEYWORDS = setOf(
        "receipt", "tax invoice", "sales slip", "customer copy", "merchant copy", "duplicate",
        "welcome", "thank you", "thanks for", "cashier", "host", "server", "clerk", "waiter",
        "waitress", "bartender", "operator", "associate", "employee", "manager", "mgr",
        "order", "ticket", "table", "guest", "terminal", "station", "pos", "subtotal",
        "total", "tax", "cash", "change", "visa", "mastercard", "amex", "debit", "credit",
        "balance", "tender", "amount", "phone", "tel", "fax", "www.", ".com", ".net", ".org",
        "deliciousness", "dine in", "dine-in", "to go", "togo", "take out", "takeout", "carry out",
        "carryout", "pickup", "pick up", "delivery", "drive thru", "drive-thru", "mobile order",
        "online order", "catering", "survey", "feedback", "rate your", "tell us", "rewards",
        "loyalty", "points", "received", "satisfaction", "coupon", "discount", "check closed"
    )

    private val STAFF_OR_ORDER_PREFIX = Regex(
        """(?i)^(?:host|server|cashier|clerk|waiter|waitress|employee|mgr|manager|order|ticket|check|table|guest|trans|reg|station|terminal)[\s:#]+"""
    )

    private val PHONE_NUMBER_PATTERN = Regex("""\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}""")
    private val ADDRESS_PATTERN = Regex("""(?i)^\d+\s+[a-z]+(?:\s+[a-z]+)*\s+(?:st|ave|blvd|rd|dr|ln|ct|way|hwy|pkwy|street|avenue|boulevard|road|drive|suite|ste)\b\.?""")
    private val STORE_NUM_PATTERN = Regex("""(?i)\b(?:store|unit|loc|shop|reg|pos)\s*#?\s*\d+""")

    fun extractMerchantName(lines: List<String>): String? {
        val allText = lines.joinToString("\n")

        // 1. Check for known popular merchant brands anywhere on the receipt
        for (brand in KNOWN_MERCHANT_BRANDS) {
            val brandRegex = Regex("""\b${Regex.escape(brand)}\b""", RegexOption.IGNORE_CASE)
            if (brandRegex.containsMatchIn(allText)) {
                // Look for the specific line that contains this brand to capture full store name (e.g. "Chipotle Mexican Grill")
                for (line in lines.take(15)) {
                    var trimmed = line.trim()
                    if (brandRegex.containsMatchIn(trimmed) &&
                        !trimmed.contains("http", ignoreCase = true) &&
                        !trimmed.contains("www.", ignoreCase = true) &&
                        !trimmed.contains("survey", ignoreCase = true) &&
                        !trimmed.contains("feedback", ignoreCase = true)
                    ) {
                        trimmed = trimmed
                            .replace(Regex("""(?i)^(?:welcome\s+to|thanks?\s+for\s+visiting|thank\s+you\s+for\s+visiting|visit\s+us\s+at)\s+"""), "")
                            .replace(STORE_NUM_PATTERN, "")
                            .trim()
                        if (trimmed.length in 3..40) {
                            return trimmed.lowercase().toWordTitleCase()
                        }
                    }
                }
                return brand.toWordTitleCase()
            }
        }

        // 2. Generic top-down search for candidate merchant name
        for (line in lines.take(12)) {
            var candidate = line.trim()
            if (candidate.isBlank()) continue

            // Skip lines with staff / order prefixes (e.g. "Host: Devar", "Server: Sarah")
            if (STAFF_OR_ORDER_PREFIX.containsMatchIn(candidate)) continue

            // Skip lines that contain prices/amounts (these are item rows or totals, not merchants)
            if (AMOUNT_PATTERN.matcher(candidate).find()) continue

            // Strip "Welcome to " or "Thanks for visiting " prefix
            candidate = candidate.replace(Regex("""(?i)^welcome\s+to\s+"""), "")
                .replace(Regex("""(?i)^thanks?\s+for\s+visiting\s+"""), "")
                .trim()

            // Strip store numbers from the merchant line (e.g. "Target Store #0123" -> "Target")
            candidate = candidate.replace(STORE_NUM_PATTERN, "").trim()
            if (candidate.isBlank()) continue

            // Skip phone numbers or street addresses
            if (PHONE_NUMBER_PATTERN.containsMatchIn(candidate)) continue
            if (ADDRESS_PATTERN.containsMatchIn(candidate)) continue

            // Skip lines that contain blacklisted keywords (including slogans & service modes)
            val lower = candidate.lowercase()
            if (NON_MERCHANT_KEYWORDS.any { lower.contains(it) }) continue

            // Skip lines that don't have enough alphabetic content
            val lettersCount = candidate.count { it.isLetter() }
            if (lettersCount < 3) continue
            if (candidate.length !in 3..40) continue

            return candidate.lowercase().toWordTitleCase()
        }
        return null
    }

    private val AMOUNT_PATTERN = Pattern.compile("""\$?\s*(\d{1,5}\.\d{2})""")

    // Lines that represent customer cash payment or change given - NOT the expense amount
    private val TENDER_OR_CHANGE_REGEX = Regex(
        """\b(cash\s*tender(?:ed)?|cash\s*received|cash\s*paid|cash\s*given|paid\s*in\s*cash|change\s*due|change|tender(?:ed)?|cash)\b""",
        RegexOption.IGNORE_CASE
    )

    // Lines that represent discounts, tips, item counts, or subtotal
    private val EXCLUDE_AMOUNT_REGEX = Regex(
        """\b(suggested\s*tip|suggested\s*gratuity|tip\s*guide|discount|you\s*saved|savings|items?\s*count|subtotal|sub-total|sub\s*total)\b""",
        RegexOption.IGNORE_CASE
    )

    fun extractTotalAmount(lines: List<String>): Double? {
        // Priority 1: Check lines explicitly labeled with Grand Total / Total Due / Amount Due
        val grandTotalRegex = Regex(
            """\b(grand\s*total|total\s*due|amount\s*due|balance\s*due|net\s*total|total\s*amount|final\s*total)\b""",
            RegexOption.IGNORE_CASE
        )
        for (line in lines) {
            if (grandTotalRegex.containsMatchIn(line) && !TENDER_OR_CHANGE_REGEX.containsMatchIn(line)) {
                val amounts = extractAmountsFromLine(line)
                if (amounts.isNotEmpty()) return amounts.last()
            }
        }

        // Priority 2: Check lines with "Total" (excluding subtotal and tender/cash/change)
        val totalRegex = Regex("""\btotal\b""", RegexOption.IGNORE_CASE)
        val totalLineAmounts = mutableListOf<Double>()
        for (line in lines) {
            if (totalRegex.containsMatchIn(line) &&
                !EXCLUDE_AMOUNT_REGEX.containsMatchIn(line) &&
                !TENDER_OR_CHANGE_REGEX.containsMatchIn(line)
            ) {
                val amounts = extractAmountsFromLine(line)
                if (amounts.isNotEmpty()) {
                    totalLineAmounts.add(amounts.last())
                }
            }
        }
        if (totalLineAmounts.isNotEmpty()) {
            return totalLineAmounts.last()
        }

        // Priority 3: Subtotal + Tax verification
        var subtotal: Double? = null
        var tax: Double? = null
        val subtotalRegex = Regex("""\b(subtotal|sub-total|sub\s*total)\b""", RegexOption.IGNORE_CASE)
        val taxRegex = Regex("""\b(tax|sales\s*tax|hst|gst|vat)\b""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            if (subtotal == null && subtotalRegex.containsMatchIn(line)) {
                extractAmountsFromLine(line).lastOrNull()?.let { subtotal = it }
            }
            if (tax == null && taxRegex.containsMatchIn(line) && !subtotalRegex.containsMatchIn(line)) {
                extractAmountsFromLine(line).lastOrNull()?.let { tax = it }
            }
        }

        if (subtotal != null && tax != null) {
            val calculatedTotal = Math.round((subtotal!! + tax!!) * 100.0) / 100.0
            val allExtracted = lines.flatMap { extractAmountsFromLine(it) }
            if (allExtracted.any { Math.abs(it - calculatedTotal) < 0.01 }) {
                return calculatedTotal
            }
        }

        // Priority 4: Fallback - pick highest amount among non-tender, non-change, non-tax lines
        val candidates = mutableListOf<Double>()
        for (line in lines) {
            if (!TENDER_OR_CHANGE_REGEX.containsMatchIn(line) &&
                !EXCLUDE_AMOUNT_REGEX.containsMatchIn(line) &&
                !taxRegex.containsMatchIn(line)
            ) {
                candidates.addAll(extractAmountsFromLine(line))
            }
        }

        return candidates.maxOrNull()
    }

    private fun extractAmountsFromLine(line: String): List<Double> {
        val matcher = AMOUNT_PATTERN.matcher(line)
        val list = mutableListOf<Double>()
        while (matcher.find()) {
            matcher.group(1)?.toDoubleOrNull()?.let { list.add(it) }
        }
        return list
    }

    private val DATE_PATTERNS = listOf(
        Pattern.compile("""(\d{1,2})[/\\-](\d{1,2})[/\\-](\d{2,4})"""), // MM/DD/YYYY or MM-DD-YYYY
        Pattern.compile("""(\d{4})[/\\-](\d{1,2})[/\\-](\d{1,2})""")  // YYYY-MM-DD
    )

    fun extractDate(lines: List<String>): Long? {
        for (line in lines) {
            val m1 = DATE_PATTERNS[0].matcher(line)
            if (m1.find()) {
                val month = m1.group(1)?.toIntOrNull() ?: 1
                val day = m1.group(2)?.toIntOrNull() ?: 1
                var year = m1.group(3)?.toIntOrNull() ?: 2026
                if (year < 100) year += 2000

                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
                    set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
                }
                return cal.timeInMillis
            }

            val m2 = DATE_PATTERNS[1].matcher(line)
            if (m2.find()) {
                val year = m2.group(1)?.toIntOrNull() ?: 2026
                val month = m2.group(2)?.toIntOrNull() ?: 1
                val day = m2.group(3)?.toIntOrNull() ?: 1

                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
                    set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
                }
                return cal.timeInMillis
            }
        }
        return null
    }
}
