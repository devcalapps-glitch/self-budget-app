package com.selfbudget.app.core.util

import kotlin.math.ceil
import kotlin.math.ln

/**
 * Standard amortization math for credit-card / loan accounts (AccountType.CREDIT_CARD,
 * AccountType.LOAN), using the account's balance, APR, and a monthly payment amount.
 */
object DebtPayoffCalculator {

    data class PayoffEstimate(
        val monthsToPayoff: Int,
        val totalInterestPaid: Double,
        val isPaymentTooLow: Boolean
    )

    /**
     * @param balance current amount owed (positive number)
     * @param aprPercent annual percentage rate, e.g. 24.99 for 24.99%
     * @param monthlyPayment fixed amount paid toward the balance each month
     */
    fun estimatePayoff(balance: Double, aprPercent: Double, monthlyPayment: Double): PayoffEstimate {
        if (balance <= 0.0) return PayoffEstimate(0, 0.0, false)
        if (monthlyPayment <= 0.0) return PayoffEstimate(0, 0.0, true)

        val monthlyRate = (aprPercent / 100.0) / 12.0

        // No interest: simple division.
        if (monthlyRate == 0.0) {
            val months = ceil(balance / monthlyPayment).toInt()
            return PayoffEstimate(months, 0.0, false)
        }

        val interestOnlyPayment = balance * monthlyRate
        if (monthlyPayment <= interestOnlyPayment) {
            // Payment doesn't even cover monthly interest — balance never shrinks.
            return PayoffEstimate(Int.MAX_VALUE, Double.POSITIVE_INFINITY, true)
        }

        // Standard amortization formula: n = -ln(1 - (P*r)/M) / ln(1+r)
        val months = ceil(
            -ln(1 - (balance * monthlyRate) / monthlyPayment) / ln(1 + monthlyRate)
        ).toInt()

        val totalPaid = Money.multiply(monthlyPayment, months.toDouble())
        val totalInterest = Money.subtract(totalPaid, balance).coerceAtLeast(0.0)

        return PayoffEstimate(months, totalInterest, false)
    }

    /**
     * Calculates the standard fixed monthly payment for an amortized loan (e.g. mortgage, auto loan, student loan).
     * Formula: M = P * (r * (1+r)^n) / ((1+r)^n - 1)
     *
     * @param balance current principal balance (positive number)
     * @param aprPercent annual percentage rate, e.g. 6.5 for 6.5%
     * @param termMonths duration of the loan in months
     * @return calculated monthly payment
     */
    fun calculateAmortizedMonthlyPayment(balance: Double, aprPercent: Double, termMonths: Int): Double {
        if (balance <= 0.0 || termMonths <= 0) return 0.0
        val monthlyRate = (aprPercent / 100.0) / 12.0
        if (monthlyRate <= 0.0) {
            return Money.round(balance / termMonths.toDouble())
        }
        val factor = Math.pow(1.0 + monthlyRate, termMonths.toDouble())
        if (factor == 1.0) return Money.round(balance / termMonths.toDouble())
        val monthlyPayment = balance * (monthlyRate * factor) / (factor - 1.0)
        return if (monthlyPayment.isFinite()) Money.round(monthlyPayment) else 0.0
    }
}
