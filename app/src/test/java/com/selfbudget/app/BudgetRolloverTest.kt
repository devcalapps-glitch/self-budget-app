package com.selfbudget.app

import com.selfbudget.app.core.util.BudgetRollover
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetRolloverTest {

    @Test
    fun testRolloverDisabledUsesOwnLimitOnly() {
        val effective = BudgetRollover.effectiveLimit(
            currentLimit = 300.0,
            rolloverEnabled = false,
            previousLimit = 500.0,
            previousSpent = 100.0
        )
        assertEquals(300.0, effective, 0.001)
    }

    @Test
    fun testUnderspendCarriesOverAsBonus() {
        // Last month: $500 limit, only $350 spent -> $150 leftover carries forward.
        val effective = BudgetRollover.effectiveLimit(
            currentLimit = 400.0,
            rolloverEnabled = true,
            previousLimit = 500.0,
            previousSpent = 350.0
        )
        assertEquals(550.0, effective, 0.001)
    }

    @Test
    fun testOverspendReducesThisMonthsLimit() {
        // Last month: $300 limit, $380 spent -> $80 overage reduces this month's limit.
        val effective = BudgetRollover.effectiveLimit(
            currentLimit = 300.0,
            rolloverEnabled = true,
            previousLimit = 300.0,
            previousSpent = 380.0
        )
        assertEquals(220.0, effective, 0.001)
    }

    @Test
    fun testSevereOverspendNeverGoesNegative() {
        val effective = BudgetRollover.effectiveLimit(
            currentLimit = 100.0,
            rolloverEnabled = true,
            previousLimit = 100.0,
            previousSpent = 1000.0
        )
        assertEquals(0.0, effective, 0.001)
    }

    @Test
    fun testNoPreviousBudgetTreatedAsZero() {
        val effective = BudgetRollover.effectiveLimit(
            currentLimit = 200.0,
            rolloverEnabled = true,
            previousLimit = 0.0,
            previousSpent = 0.0
        )
        assertEquals(200.0, effective, 0.001)
    }

    @Test
    fun testRolloverWorksAcrossDecToJan() {
        // Given: December budget limit of $500, December spent of $300 ($200 underspend bonus)
        val decLimit = 500.0
        val decSpent = 300.0

        // When: Calculating January effective limit with rollover enabled and January base limit of $500
        val janBaseLimit = 500.0
        val janEffectiveLimit = BudgetRollover.effectiveLimit(
            currentLimit = janBaseLimit,
            rolloverEnabled = true,
            previousLimit = decLimit,
            previousSpent = decSpent
        )

        // Then: January effective limit receives full $200 December bonus -> $700
        assertEquals(700.0, janEffectiveLimit, 0.001)
    }

    @Test
    fun test3ConsecutiveMonthsDeficitPersistsAcrossMonths() {
        // Given: Base monthly limit = $1,000
        val baseLimit = 1000.0

        // Month 1: Base $1,000, spent $2,500 ($1,500 overspend)
        val m1Unclamped = BudgetRollover.unclampedPosition(baseLimit, true, 0.0, 0.0)
        val m1Effective = BudgetRollover.effectiveLimit(baseLimit, true, 0.0, 0.0)
        assertEquals(1000.0, m1Effective, 0.001)

        // Month 2: Base $1,000, spent $0. Carryover from M1 is (1000 - 2500 = -1500).
        // Unclamped M2 position = 1000 + (-1500) = -500.
        val m2Unclamped = BudgetRollover.unclampedPosition(baseLimit, true, m1Unclamped, 2500.0)
        val m2Effective = BudgetRollover.effectiveLimit(baseLimit, true, m1Unclamped, 2500.0)
        assertEquals(-500.0, m2Unclamped, 0.001)
        assertEquals(0.0, m2Effective, 0.001) // Spending limit floored at 0

        // Month 3: Base $1,000, spent $0. Carryover from M2 unclamped is (-500 - 0 = -500).
        // Unclamped M3 position = 1000 + (-500) = 500.
        val m3Unclamped = BudgetRollover.unclampedPosition(baseLimit, true, m2Unclamped, 0.0)
        val m3Effective = BudgetRollover.effectiveLimit(baseLimit, true, m2Unclamped, 0.0)
        assertEquals(500.0, m3Effective, 0.001) // Deficit persisted and reduced by Month 2's $1,000 credit, leaving $500
    }
}
