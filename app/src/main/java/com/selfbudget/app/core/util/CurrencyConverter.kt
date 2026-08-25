package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.ExchangeRateEntity

/**
 * Converts an amount between two currency codes using the user's manually-entered exchange
 * rates (this app is offline-first and does not call a live FX API). If no rate is on file and
 * the currencies differ, the original amount is returned unconverted rather than crashing or
 * silently zeroing it out — callers that need to distinguish "converted" from "no rate found"
 * should check `hasRate` first.
 */
object CurrencyConverter {

    fun hasRate(from: String, to: String, rates: List<ExchangeRateEntity>): Boolean {
        if (from.equals(to, ignoreCase = true)) return true
        return rates.any {
            it.fromCurrency.equals(from, ignoreCase = true) && it.toCurrency.equals(to, ignoreCase = true)
        } || rates.any {
            it.fromCurrency.equals(to, ignoreCase = true) && it.toCurrency.equals(from, ignoreCase = true)
        }
    }

    fun convert(amount: Double, from: String, to: String, rates: List<ExchangeRateEntity>): Double {
        if (from.equals(to, ignoreCase = true)) return amount

        val direct = rates.firstOrNull {
            it.fromCurrency.equals(from, ignoreCase = true) && it.toCurrency.equals(to, ignoreCase = true)
        }
        if (direct != null) return Money.multiply(amount, direct.rate)

        val inverse = rates.firstOrNull {
            it.fromCurrency.equals(to, ignoreCase = true) && it.toCurrency.equals(from, ignoreCase = true)
        }
        if (inverse != null && inverse.rate != 0.0) return Money.multiply(amount, 1.0 / inverse.rate)

        // No rate on file — return face value rather than dropping the amount from totals.
        return amount
    }
}
