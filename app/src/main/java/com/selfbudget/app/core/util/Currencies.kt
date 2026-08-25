package com.selfbudget.app.core.util

/**
 * Supported currencies, mapping an ISO code (stored on each account) to the display symbol
 * already used throughout the UI (the existing "Preferred Currency" picker in Settings).
 */
object Currencies {
    data class Currency(val code: String, val symbol: String, val label: String)

    val SUPPORTED = listOf(
        Currency("USD", "$", "US Dollar"),
        Currency("EUR", "€", "Euro"),
        Currency("GBP", "£", "British Pound"),
        Currency("INR", "₹", "Indian Rupee"),
        Currency("JPY", "¥", "Japanese Yen"),
        Currency("AUD", "A$", "Australian Dollar")
    )

    fun symbolFor(code: String): String =
        SUPPORTED.firstOrNull { it.code.equals(code, ignoreCase = true) }?.symbol ?: code

    fun codeForSymbol(symbol: String): String =
        SUPPORTED.firstOrNull { it.symbol == symbol }?.code ?: "USD"
}
