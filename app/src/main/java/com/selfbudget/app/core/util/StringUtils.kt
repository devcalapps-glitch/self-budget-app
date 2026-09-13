package com.selfbudget.app.core.util

/**
 * Capitalizes the first letter of each word in a string while preserving all whitespace and delimiters.
 * As the user types (e.g. typing a space then the next letter), the first letter of each subsequent word
 * is automatically capitalized.
 */
fun String.toWordTitleCase(): String {
    if (isEmpty()) return this
    val chars = toCharArray()
    var capitalizeNext = true
    for (i in chars.indices) {
        val ch = chars[i]
        if (ch.isWhitespace()) {
            capitalizeNext = true
        } else if (capitalizeNext && ch.isLetter()) {
            chars[i] = ch.titlecaseChar()
            capitalizeNext = false
        } else if (ch.isLetter() || ch.isDigit()) {
            capitalizeNext = false
        }
    }
    return String(chars)
}
