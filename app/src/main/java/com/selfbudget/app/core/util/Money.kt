package com.selfbudget.app.core.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Centralized cent-safe money arithmetic.
 *
 * Amounts are still stored as `Double` throughout the app (see Entities.kt for why a full
 * migration to integer cents was intentionally avoided), but every add/sum/multiply that
 * matters for a dollar total is routed through here first. Raw Double arithmetic (e.g.
 * `50.0 * 4.333`, or summing hundreds of transaction amounts) accumulates binary
 * floating-point error that eventually shows up as an off-by-a-fraction-of-a-cent total.
 * Rounding every result to the nearest cent via BigDecimal after each operation prevents
 * that drift from ever being visible to the user, without touching the storage type.
 */
object Money {

    /** Rounds a raw double amount to the nearest cent using standard half-up rounding. */
    fun round(amount: Double): Double {
        if (amount.isNaN() || amount.isInfinite()) return 0.0
        // BigDecimal.valueOf(Double) goes through Double.toString() first, giving the decimal
        // value a human actually typed (e.g. "19.995"). The BigDecimal(Double) constructor
        // instead exposes the exact binary value of the double (often something like
        // 19.994999999999997335...), which can silently round the WRONG way — precisely the
        // class of bug this utility exists to prevent.
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /** Cent-safe sum of a collection of amounts. */
    fun sum(amounts: Iterable<Double>): Double {
        var totalCents = 0L
        for (amount in amounts) {
            totalCents += toCents(amount)
        }
        return fromCents(totalCents)
    }

    fun sum(vararg amounts: Double): Double = sum(amounts.asIterable())

    /** Cent-safe multiplication, e.g. converting a weekly bill into a monthly-equivalent amount. */
    fun multiply(amount: Double, factor: Double): Double = round(amount * factor)

    /** Cent-safe subtraction. */
    fun subtract(a: Double, b: Double): Double = fromCents(toCents(a) - toCents(b))

    /** Cent-safe addition of exactly two amounts. */
    fun add(a: Double, b: Double): Double = fromCents(toCents(a) + toCents(b))

    private fun toCents(amount: Double): Long {
        if (amount.isNaN() || amount.isInfinite()) return 0L
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun fromCents(cents: Long): Double =
        BigDecimal.valueOf(cents).movePointLeft(2).toDouble()
}
