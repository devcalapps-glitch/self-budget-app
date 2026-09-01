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
     * Computes realized/posted monthly income from logged income transactions.
     * Following Monarch/Mint standard, net worth and realized income only update when transactions are posted.
     */
    fun computeEffectiveMonthlyIncome(
        loggedIncomeTransactions: List<TransactionEntity>,
        recurringIncomeList: List<RecurringTransactionEntity> = emptyList()
    ): Double {
        return Money.sum(loggedIncomeTransactions.filter { it.type == TransactionType.INCOME }.map { it.amount })
    }

    /**
     * Computes the total expected recurring income for budget planning/forecasting.
     */
    fun computeExpectedMonthlyIncome(
        recurringIncomeList: List<RecurringTransactionEntity>
    ): Double {
        val activeRecurringIncome = recurringIncomeList.filter {
            it.type == TransactionType.INCOME && !it.isArchived
        }
        return Money.sum(activeRecurringIncome.map { rec ->
            RecurringFrequencyNormalizer.toMonthlyAmount(rec.amount, rec.frequency)
        })
    }
}
