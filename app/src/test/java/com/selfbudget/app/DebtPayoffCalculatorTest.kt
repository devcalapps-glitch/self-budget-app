package com.selfbudget.app

import com.selfbudget.app.core.util.DebtPayoffCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtPayoffCalculatorTest {

    @Test
    fun testZeroBalanceIsAlreadyPaidOff() {
        val result = DebtPayoffCalculator.estimatePayoff(balance = 0.0, aprPercent = 24.99, monthlyPayment = 100.0)
        assertEquals(0, result.monthsToPayoff)
        assertEquals(0.0, result.totalInterestPaid, 0.001)
    }

    @Test
    fun testNoInterestIsSimpleDivision() {
        val result = DebtPayoffCalculator.estimatePayoff(balance = 500.0, aprPercent = 0.0, monthlyPayment = 100.0)
        assertEquals(5, result.monthsToPayoff)
        assertEquals(0.0, result.totalInterestPaid, 0.001)
    }

    @Test
    fun testPaymentBelowInterestNeverPaysOff() {
        // $10,000 balance at 24% APR accrues $200/mo in interest; a $50 payment can't keep up.
        val result = DebtPayoffCalculator.estimatePayoff(balance = 10000.0, aprPercent = 24.0, monthlyPayment = 50.0)
        assertTrue(result.isPaymentTooLow)
    }

    @Test
    fun testTypicalCreditCardPayoffAccruesInterest() {
        val result = DebtPayoffCalculator.estimatePayoff(balance = 1000.0, aprPercent = 24.0, monthlyPayment = 100.0)
        assertTrue("Should take multiple months", result.monthsToPayoff in 10..12)
        assertTrue("Should accrue some interest", result.totalInterestPaid > 0.0)
        assertTrue("Should not report payment as too low", !result.isPaymentTooLow)
    }

    @Test
    fun testZeroOrNegativePaymentIsFlaggedTooLow() {
        val result = DebtPayoffCalculator.estimatePayoff(balance = 500.0, aprPercent = 10.0, monthlyPayment = 0.0)
        assertTrue(result.isPaymentTooLow)
    }

    @Test
    fun testFinalDebtPayment_roundsCorrectly() {
        // Given: Small remaining debt balance of $15.43 at 12% APR with a $20.00 monthly payment
        val result = DebtPayoffCalculator.estimatePayoff(balance = 15.43, aprPercent = 12.0, monthlyPayment = 20.0)

        // Then: Payoff completes in exactly 1 month and interest is rounded to exact cents
        assertEquals(1, result.monthsToPayoff)
        assertTrue("Interest paid must be greater than or equal to 0.0 and rounded to cents", result.totalInterestPaid >= 0.0)
        assertEquals(result.totalInterestPaid, com.selfbudget.app.core.util.Money.round(result.totalInterestPaid), 0.001)
    }

    @Test
    fun testAmortizedMonthlyPayment_standardMortgage() {
        // $300,000 at 6% APR for 30 years (360 months) -> ~$1,798.65
        val pmt = DebtPayoffCalculator.calculateAmortizedMonthlyPayment(
            balance = 300000.0,
            aprPercent = 6.0,
            termMonths = 360
        )
        assertEquals(1798.65, pmt, 0.01)
    }

    @Test
    fun testAmortizedMonthlyPayment_zeroInterest() {
        // $12,000 at 0% APR for 12 months -> $1,000.00
        val pmt = DebtPayoffCalculator.calculateAmortizedMonthlyPayment(
            balance = 12000.0,
            aprPercent = 0.0,
            termMonths = 12
        )
        assertEquals(1000.0, pmt, 0.01)
    }

    @Test
    fun testAmortizedMonthlyPayment_zeroBalanceOrZeroMonths() {
        assertEquals(0.0, DebtPayoffCalculator.calculateAmortizedMonthlyPayment(0.0, 5.0, 36), 0.001)
        assertEquals(0.0, DebtPayoffCalculator.calculateAmortizedMonthlyPayment(10000.0, 5.0, 0), 0.001)
    }
}
