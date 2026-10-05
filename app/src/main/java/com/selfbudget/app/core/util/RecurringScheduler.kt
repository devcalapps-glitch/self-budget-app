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

    /**
     * Calculates the effective due date for a recurring item, projecting forward if the stored
     * nextDueDate has fallen behind into a past cycle/month, or projecting to a target month.
     *
     * @param item The recurring transaction entity.
     * @param targetMonthYear Optional target month in "yyyy-MM" format. Defaults to current month.
     * @param isCyclePaid If true, the cycle in targetMonthYear is already fulfilled, so returns the
     *                    due date for the NEXT cycle following targetMonthYear.
     * @param postedOccurrences How many occurrences have already been posted for the target cycle.
     */
    fun computeEffectiveDueDate(
        item: com.selfbudget.app.data.model.RecurringTransactionEntity,
        targetMonthYear: String? = null,
        isCyclePaid: Boolean = false,
        postedOccurrences: Int = 0
    ): Long {
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val currentMonthStr = sdf.format(java.util.Date())
        val effectiveTargetMonth = if (targetMonthYear.isNullOrBlank()) currentMonthStr else targetMonthYear

        val targetCal = Calendar.getInstance()
        try {
            val parsed = sdf.parse(effectiveTargetMonth)
            if (parsed != null) targetCal.time = parsed
        } catch (_: Exception) { }

        val targetYear = targetCal.get(Calendar.YEAR)
        val targetMonth = targetCal.get(Calendar.MONTH)

        val anchorCal = Calendar.getInstance().apply { timeInMillis = item.nextDueDate }

        // If nextDueDate is strictly in a future month beyond targetMonthYear, keep the future date
        val anchorYear = anchorCal.get(Calendar.YEAR)
        val anchorMonth = anchorCal.get(Calendar.MONTH)
        if (anchorYear > targetYear || (anchorYear == targetYear && anchorMonth > targetMonth)) {
            return item.nextDueDate
        }

        val anchorDay = anchorCal.get(Calendar.DAY_OF_MONTH)
        val hour = anchorCal.get(Calendar.HOUR_OF_DAY)
        val minute = anchorCal.get(Calendar.MINUTE)
        val second = anchorCal.get(Calendar.SECOND)
        val millis = anchorCal.get(Calendar.MILLISECOND)

        val resultCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, millis)
        }

        when (item.frequency) {
            RecurringFrequency.MONTHLY -> {
                val maxDay = resultCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                resultCal.set(Calendar.DAY_OF_MONTH, anchorDay.coerceAtMost(maxDay))
                if (isCyclePaid) {
                    resultCal.add(Calendar.MONTH, 1)
                    val nextMaxDay = resultCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    resultCal.set(Calendar.DAY_OF_MONTH, anchorDay.coerceAtMost(nextMaxDay))
                }
            }
            RecurringFrequency.YEARLY -> {
                resultCal.set(Calendar.MONTH, anchorMonth)
                val maxDay = resultCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                resultCal.set(Calendar.DAY_OF_MONTH, anchorDay.coerceAtMost(maxDay))
                if (isCyclePaid) {
                    resultCal.add(Calendar.YEAR, 1)
                }
            }
            RecurringFrequency.SEMI_MONTHLY -> {
                val maxDay = resultCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDay = anchorDay.coerceIn(1, 15)
                val secondDay = if (firstDay == 1) 15 else if (firstDay == 15) maxDay else (firstDay + 14).coerceAtMost(maxDay)

                if (isCyclePaid) {
                    resultCal.add(Calendar.MONTH, 1)
                    resultCal.set(Calendar.DAY_OF_MONTH, firstDay)
                } else if (postedOccurrences >= 1) {
                    resultCal.set(Calendar.DAY_OF_MONTH, secondDay)
                } else {
                    resultCal.set(Calendar.DAY_OF_MONTH, firstDay)
                }
            }
            RecurringFrequency.WEEKLY, RecurringFrequency.BI_WEEKLY -> {
                val intervalDays = if (item.frequency == RecurringFrequency.WEEKLY) 7 else 14
                val cal = Calendar.getInstance().apply { timeInMillis = item.nextDueDate }

                val startOfTarget = resultCal.clone() as Calendar
                startOfTarget.set(Calendar.DAY_OF_MONTH, 1)
                startOfTarget.set(Calendar.HOUR_OF_DAY, 0)
                startOfTarget.set(Calendar.MINUTE, 0)
                startOfTarget.set(Calendar.SECOND, 0)

                val endOfTarget = resultCal.clone() as Calendar
                endOfTarget.set(Calendar.DAY_OF_MONTH, endOfTarget.getActualMaximum(Calendar.DAY_OF_MONTH))
                endOfTarget.set(Calendar.HOUR_OF_DAY, 23)
                endOfTarget.set(Calendar.MINUTE, 59)
                endOfTarget.set(Calendar.SECOND, 59)

                // Advance forward until inside or past target month
                while (cal.timeInMillis < startOfTarget.timeInMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, intervalDays)
                }

                if (isCyclePaid) {
                    while (cal.timeInMillis <= endOfTarget.timeInMillis) {
                        cal.add(Calendar.DAY_OF_YEAR, intervalDays)
                    }
                } else if (postedOccurrences > 0) {
                    repeat(postedOccurrences) {
                        cal.add(Calendar.DAY_OF_YEAR, intervalDays)
                    }
                }
                return cal.timeInMillis
            }
        }

        return resultCal.timeInMillis
    }

    /**
     * Advances [currentDueDate] forward by [frequency] intervals until it is >= [minDateMillis].
     * Preserves the anchor timing while catching up a stale due date from previous months.
     */
    fun advancePastDueDate(
        currentDueDate: Long,
        frequency: RecurringFrequency,
        minDateMillis: Long = System.currentTimeMillis()
    ): Long {
        var due = currentDueDate
        while (due < minDateMillis) {
            val next = computeNextDueDate(due, frequency)
            if (next <= due) break
            due = next
        }
        return due
    }
}

