package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.RecurringFrequency
import java.util.Calendar

/**
 * Shared scheduling logic for recurring bills/paychecks, used by the manual "Post Now" action in
 * MainViewModel. Posting to the ledger always requires that explicit user action - nothing here
 * (or in BillReminderWorker) ever inserts a transaction automatically. An earlier version did
 * auto-post on a schedule, but that created false/duplicate transactions whenever the real bill
 * amount differed or the payment didn't actually happen exactly as scheduled, so it was removed.
 */
object RecurringScheduler {

    fun computeNextDueDate(currentDueDate: Long, frequency: RecurringFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentDueDate }
        when (frequency) {
            RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.BI_WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            RecurringFrequency.SEMI_MONTHLY -> {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                if (day <= 15) {
                    if (day == 1) {
                        cal.set(Calendar.DAY_OF_MONTH, 15)
                    } else if (day == 15) {
                        cal.add(Calendar.MONTH, 1)
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                    } else {
                        cal.add(Calendar.DAY_OF_MONTH, 15)
                    }
                } else {
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    if (day >= maxDay - 1) {
                        cal.add(Calendar.MONTH, 1)
                        cal.set(Calendar.DAY_OF_MONTH, 15)
                    } else {
                        cal.add(Calendar.MONTH, 1)
                        cal.set(Calendar.DAY_OF_MONTH, (day - 15).coerceAtLeast(1))
                    }
                }
            }
            RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    /**
     * Remaining-occurrences count after one more posting, or null if the item recurs
     * indefinitely (no finite lifespan was set). Never goes below 0.
     */
    fun decrementOccurrences(remaining: Int?): Int? = remaining?.let { (it - 1).coerceAtLeast(0) }

    /** Inverse of [decrementOccurrences], used to undo a posting whose transaction gets deleted. */
    fun incrementOccurrences(remaining: Int?): Int? = remaining?.let { it + 1 }

    /**
     * True once a finite-lifespan recurring item (e.g. "12 more loan payments") has used up all
     * of its remaining occurrences and should be auto-archived so it stops generating reminders
     * and stops counting toward budget/cash-flow projections.
     */
    fun isFinished(remaining: Int?): Boolean = remaining != null && remaining <= 0
}
