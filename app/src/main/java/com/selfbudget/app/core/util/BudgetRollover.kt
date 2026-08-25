package com.selfbudget.app.core.util

/**
 * Computes a category budget's effective limit for the current month when rollover is enabled.
 * Carryover is calculated off the previous month's unclamped effective position so overspending
 * deficits persist across consecutive months until fully repaid rather than resetting after one month.
 */
object BudgetRollover {

    /**
     * Computes the unclamped net budget position for the current month.
     *
     * @param currentBaseLimit this month's configured base limit
     * @param rolloverEnabled whether rollover carryover is enabled
     * @param previousUnclampedPosition last month's unclamped net position (same as previousEffectiveLimit if positive)
     * @param previousSpent last month's actual spend
     */
    fun unclampedPosition(
        currentBaseLimit: Double,
        rolloverEnabled: Boolean,
        previousUnclampedPosition: Double,
        previousSpent: Double
    ): Double {
        if (!rolloverEnabled) return currentBaseLimit
        val carryover = Money.subtract(previousUnclampedPosition, previousSpent)
        return Money.add(currentBaseLimit, carryover)
    }

    /**
     * Computes the effective spending limit for the current month.
     * Floored at 0.0 so spending allowance is never displayed as negative.
     */
    fun effectiveLimit(
        currentLimit: Double,
        rolloverEnabled: Boolean,
        previousLimit: Double,
        previousSpent: Double
    ): Double {
        if (!rolloverEnabled) return currentLimit
        val unclamped = unclampedPosition(currentLimit, rolloverEnabled, previousLimit, previousSpent)
        return unclamped.coerceAtLeast(0.0)
    }
}
