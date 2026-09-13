package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import java.util.Calendar
import java.util.Locale

/**
 * How much of a recurring item's current cycle has actually been posted to the ledger, for a
 * given month - shared by RecurringScreen (list rows, Post Now status) and the Home dashboard's
 * "Upcoming bills" figure, so both agree on what's still outstanding instead of drifting apart.
 */
data class RecurringCyclePaymentSummary(
    val totalPaid: Double,
    val remainingAmount: Double,
    val isFullyPaid: Boolean,
    val isPartiallyPaid: Boolean,
    val matchingTransactions: List<TransactionEntity>,
    val expectedOccurrences: Int = 1,
    val postedOccurrences: Int = 0
)

object RecurringCycleCalculator {

    fun getCyclePaymentSummary(
        item: RecurringTransactionEntity,
        allTransactions: List<TransactionEntity>,
        selectedMonthYear: String
    ): RecurringCyclePaymentSummary {
        val sdf = java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val selectedCal = Calendar.getInstance()
        try {
            val date = sdf.parse(selectedMonthYear)
            if (date != null) {
                selectedCal.time = date
            }
        } catch (e: Exception) {
            // fallback
        }

        val currentYear = selectedCal.get(Calendar.YEAR)
        val currentMonth = selectedCal.get(Calendar.MONTH)

        val now = System.currentTimeMillis()
        val isCurrentMonthSelected = sdf.format(java.util.Date()) == selectedMonthYear
        val nowForWeekly = if (isCurrentMonthSelected) now else {
            val tempCal = selectedCal.clone() as Calendar
            tempCal.set(Calendar.DAY_OF_MONTH, tempCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            tempCal.timeInMillis
        }

        val expectedOccurrences = when (item.frequency) {
            RecurringFrequency.SEMI_MONTHLY -> 2
            RecurringFrequency.BI_WEEKLY -> 2
            RecurringFrequency.WEEKLY -> 4
            RecurringFrequency.MONTHLY -> 1
            RecurringFrequency.YEARLY -> 1
        }
        val targetCycleAmount = when (item.frequency) {
            RecurringFrequency.SEMI_MONTHLY -> item.amount * 2.0
            RecurringFrequency.BI_WEEKLY -> item.amount * 2.0
            RecurringFrequency.WEEKLY -> item.amount * 4.0
            RecurringFrequency.MONTHLY -> item.amount
            RecurringFrequency.YEARLY -> item.amount
        }

        val matches = allTransactions.filter { tx ->
            // A transaction explicitly posted from this recurring item is an exact match,
            // regardless of title/amount edits made to either side since. Otherwise fall back to
            // the old heuristic match for transactions posted before that link existed.
            val isLinkedMatch = tx.linkedRecurringId == item.id
            val isHeuristicMatch = tx.type == item.type &&
                (tx.title.trim().equals(item.title.trim(), ignoreCase = true) || (tx.categoryId == item.categoryId && Math.abs(tx.amount - item.amount) < 0.01))

            (isLinkedMatch || isHeuristicMatch) &&
                when (item.frequency) {
                    RecurringFrequency.WEEKLY -> tx.timestamp >= nowForWeekly - (7 * 24 * 60 * 60 * 1000L) && tx.timestamp <= nowForWeekly
                    RecurringFrequency.BI_WEEKLY, RecurringFrequency.SEMI_MONTHLY, RecurringFrequency.MONTHLY -> {
                        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                        txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth
                    }
                    RecurringFrequency.YEARLY -> {
                        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                        txCal.get(Calendar.YEAR) == currentYear
                    }
                }
        }

        val totalPaid = matches.sumOf { it.amount }
        val postedCount = matches.size
        val remaining = (targetCycleAmount - totalPaid).coerceAtLeast(0.0)
        val isFullyPaid = (totalPaid >= (targetCycleAmount - 0.005) || (expectedOccurrences > 1 && postedCount >= expectedOccurrences)) && matches.isNotEmpty()
        val isPartiallyPaid = (totalPaid > 0.005 || postedCount > 0) && !isFullyPaid

        return RecurringCyclePaymentSummary(
            totalPaid = totalPaid,
            remainingAmount = remaining,
            isFullyPaid = isFullyPaid,
            isPartiallyPaid = isPartiallyPaid,
            matchingTransactions = matches,
            expectedOccurrences = expectedOccurrences,
            postedOccurrences = postedCount
        )
    }
}
