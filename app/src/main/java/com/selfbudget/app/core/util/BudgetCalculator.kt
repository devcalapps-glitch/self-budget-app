package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.BudgetEntity

/**
 * Core engine for computing effective category budgets for any target month.
 *
 * Implements persistent monthly baseline rules:
 * - When a budget is configured for month M (e.g. "2026-08"), it automatically applies to all subsequent months (M+1, M+2, ...)
 *   unless explicitly overridden or deleted in a later month.
 * - Historical integrity is strictly preserved: past months prior to an edit are never mutated.
 * - If a budget is deleted or set to 0.0 in month M, it ceases to apply for month M and future months, without touching history prior to month M.
 */
object BudgetCalculator {

    /**
     * For a given [targetMonthYear] ("yyyy-MM"), computes the effective budgets across all categories.
     * For each category, finds the most recent budget set on or before [targetMonthYear] (monthYear <= targetMonthYear).
     */
    fun computeBudgetsForMonth(allBudgets: List<BudgetEntity>, targetMonthYear: String): List<BudgetEntity> {
        return allBudgets
            .filter { it.monthYear <= targetMonthYear }
            .groupBy { it.categoryId }
            .mapNotNull { (_, budgetsForCategory) ->
                val latest = budgetsForCategory.maxByOrNull { it.monthYear }
                if (latest != null && latest.amountLimit > 0.0) {
                    latest.copy(monthYear = targetMonthYear)
                } else {
                    null
                }
            }
    }
}
