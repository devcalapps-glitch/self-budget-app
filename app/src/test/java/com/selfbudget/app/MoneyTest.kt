package com.selfbudget.app

import com.selfbudget.app.core.util.Money
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies Money's cent-safe arithmetic actually eliminates the binary floating-point drift
 * that plain Double addition/multiplication is prone to.
 */
class MoneyTest {

    @Test
    fun testSumEliminatesFloatingPointDrift() {
        // The classic case: repeatedly adding 0.1 in raw Double math drifts away from the exact
        // decimal answer (0.1 + 0.2 == 0.30000000000000004 in raw IEEE-754 double arithmetic).
        val amounts = List(10) { 0.1 }
        val sum = Money.sum(amounts)
        assertEquals(1.0, sum, 0.0)
    }

    @Test
    fun testRoundHalfUp() {
        assertEquals(19.99, Money.round(19.994999999999997), 0.0)
        assertEquals(20.00, Money.round(19.995), 0.0)
    }

    @Test
    fun testMultiplyRoundsToCents() {
        // Weekly-to-monthly conversion factor used across budgets/recurring math.
        val result = Money.multiply(50.0, 4.333)
        assertEquals(216.65, result, 0.0)
    }

    @Test
    fun testSubtractAndAdd() {
        assertEquals(0.01, Money.subtract(0.03, 0.02), 0.0)
        assertEquals(0.03, Money.add(0.01, 0.02), 0.0)
    }

    @Test
    fun testNegativeAndZeroMoneyBehavior() {
        // Given: Zero amounts and negative debt balances
        assertEquals(0.0, Money.round(0.0), 0.0)
        assertEquals(0.0, Money.add(0.0, 0.0), 0.0)
        assertEquals(-1500.25, Money.round(-1500.246), 0.0)
        assertEquals(-1500.24, Money.round(-1500.244), 0.0)
        assertEquals(-500.0, Money.subtract(500.0, 1000.0), 0.0)
    }

    @Test
    fun testPersistenceRoundTrip_preservesExactCents() {
        // Given: A precise cent dollar amount $1,234.56
        val originalAmount = 1234.56

        // When: Converting Double -> String format -> Double persistence round-trip
        val serializedString = "%.2f".format(originalAmount)
        val deserializedAmount = serializedString.toDoubleOrNull() ?: 0.0
        val roundedAmount = Money.round(deserializedAmount)

        // Then: Cents are preserved with 100% precision
        assertEquals("1234.56", serializedString)
        assertEquals(1234.56, roundedAmount, 0.0)
    }

    @Test
    fun testCentConversionRoundsResidualImprecision() {
        // Given: Imprecise value floating point residual (59.96999999999999)
        val impreciseValue = 59.96999999999999
        val rounded = Money.round(impreciseValue)
        assertEquals(59.97, rounded, 0.0)

        val sumResult = Money.add(impreciseValue, 0.03)
        assertEquals(60.00, sumResult, 0.0)
    }
}
