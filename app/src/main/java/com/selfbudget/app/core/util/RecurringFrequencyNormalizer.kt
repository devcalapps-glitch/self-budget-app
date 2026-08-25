package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.RecurringFrequency

/**
 * Normalizes recurring transaction amounts to their exact monthly-equivalent figure.
 * Uses exact mathematical ratios (52/12 for weekly, 26/12 for bi-weekly) routed through
 * cent-safe rounding, replacing truncated constants (4.333 / 2.166).
 */
object RecurringFrequencyNormalizer {

    /**
     * Converts a recurring amount to its exact monthly-equivalent figure.
     */
    fun toMonthlyAmount(amount: Double, frequency: RecurringFrequency): Double {
        val raw = when (frequency) {
            RecurringFrequency.WEEKLY -> amount * 52.0 / 12.0
            RecurringFrequency.BI_WEEKLY -> amount * 26.0 / 12.0
            RecurringFrequency.MONTHLY -> amount
            RecurringFrequency.YEARLY -> amount / 12.0
        }
        return Money.round(raw)
    }
}
