package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType

/**
 * Computes Effective Monthly Income per income source rather than a global max.
 *
 * For each recurring income source (e.g. primary paycheck), effective income is max(logged, expected).
 * Ad-hoc or freelance income logged without a corresponding recurring item is added on top.
 */
object IncomeCalculator {

    /**
     * Computes the per-source effective monthly income.
     */
    fun computeEffectiveMonthlyIncome(
        loggedIncomeTransactions: List<TransactionEntity>,
        recurringIncomeList: List<RecurringTransactionEntity>
    ): Double {
        val activeRecurringIncome = recurringIncomeList.filter {
            it.type == TransactionType.INCOME && !it.isArchived
        }

        val recurringCategoryIds = activeRecurringIncome.map { it.categoryId }.toSet()

        // 1. Calculate per recurring income category: max(expected, logged for category)
        val recurringCategoryTotals = activeRecurringIncome.groupBy { it.categoryId }.mapValues { entry ->
            val expectedMonthly = Money.sum(entry.value.map { rec ->
                RecurringFrequencyNormalizer.toMonthlyAmount(rec.amount, rec.frequency)
            })
            val actualLoggedForCat = Money.sum(
                loggedIncomeTransactions.filter { it.categoryId == entry.key }.map { it.amount }
            )
            maxOf(expectedMonthly, actualLoggedForCat)
        }

        val totalRecurringEffective = Money.sum(recurringCategoryTotals.values)

        // 2. Ad-hoc income logged in categories without an active recurring income item (e.g. freelance, bonus)
        val adHocTxs = loggedIncomeTransactions.filter {
            it.type == TransactionType.INCOME && it.categoryId !in recurringCategoryIds
        }
        val totalAdHoc = Money.sum(adHocTxs.map { it.amount })

        return Money.add(totalRecurringEffective, totalAdHoc)
    }
}
