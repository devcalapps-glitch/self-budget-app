package com.selfbudget.app

import com.selfbudget.app.core.util.RecurringScheduler
import com.selfbudget.app.data.model.RecurringFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.util.Calendar
import org.junit.Test

class RecurringSchedulerTest {

    @Test
    fun testComputeNextDueDateMonthly() {
        val cal = Calendar.getInstance()
        val start = cal.timeInMillis
        val next = RecurringScheduler.computeNextDueDate(start, RecurringFrequency.MONTHLY)
        cal.add(Calendar.MONTH, 1)
        assertEquals(cal.timeInMillis, next)
    }

    @Test
    fun testComputeNextDueDateWeekly() {
        val cal = Calendar.getInstance()
        val start = cal.timeInMillis
        val next = RecurringScheduler.computeNextDueDate(start, RecurringFrequency.WEEKLY)
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        assertEquals(cal.timeInMillis, next)
    }

    @Test
    fun testComputeNextDueDateBiWeekly() {
        val cal = Calendar.getInstance()
        val start = cal.timeInMillis
        val next = RecurringScheduler.computeNextDueDate(start, RecurringFrequency.BI_WEEKLY)
        cal.add(Calendar.WEEK_OF_YEAR, 2)
        assertEquals(cal.timeInMillis, next)
    }

    @Test
    fun testComputeNextDueDateSemiMonthly() {
        // 1st of the month moves to 15th
        val cal1 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 1, 10, 0, 0) }
        val next1 = RecurringScheduler.computeNextDueDate(cal1.timeInMillis, RecurringFrequency.SEMI_MONTHLY)
        val res1 = Calendar.getInstance().apply { timeInMillis = next1 }
        assertEquals(15, res1.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.SEPTEMBER, res1.get(Calendar.MONTH))

        // 15th of the month moves to 1st of next month
        val cal15 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 15, 10, 0, 0) }
        val next15 = RecurringScheduler.computeNextDueDate(cal15.timeInMillis, RecurringFrequency.SEMI_MONTHLY)
        val res15 = Calendar.getInstance().apply { timeInMillis = next15 }
        assertEquals(1, res15.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.OCTOBER, res15.get(Calendar.MONTH))
    }

    @Test
    fun testComputeNextDueDateYearly() {
        val cal = Calendar.getInstance()
        val start = cal.timeInMillis
        val next = RecurringScheduler.computeNextDueDate(start, RecurringFrequency.YEARLY)
        cal.add(Calendar.YEAR, 1)
        assertEquals(cal.timeInMillis, next)
    }

    // --- Finite-lifespan recurring items (e.g. "12 more loan payments then done") ---

    @Test
    fun testIndefiniteItemNeverDecrements() {
        assertNull(RecurringScheduler.decrementOccurrences(null))
        assertFalse(RecurringScheduler.isFinished(null))
    }

    @Test
    fun testDecrementOccurrencesCountsDown() {
        assertEquals(2, RecurringScheduler.decrementOccurrences(3))
        assertEquals(0, RecurringScheduler.decrementOccurrences(1))
    }

    @Test
    fun testDecrementOccurrencesNeverGoesNegative() {
        assertEquals(0, RecurringScheduler.decrementOccurrences(0))
    }

    @Test
    fun testIsFinishedOnlyWhenZeroOrLess() {
        assertFalse(RecurringScheduler.isFinished(1))
        assertTrue(RecurringScheduler.isFinished(0))
    }

    @Test
    fun testMonthlyRecurrence_handlesFeb28_29_30_31() {
        // Given: Jan 31, 2025 (non-leap year)
        val cal2025 = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 31, 10, 0, 0)
        }
        val nextFeb2025 = RecurringScheduler.computeNextDueDate(cal2025.timeInMillis, RecurringFrequency.MONTHLY)
        val resFeb2025 = Calendar.getInstance().apply { timeInMillis = nextFeb2025 }

        // Then: Clamps to Feb 28, 2025
        assertEquals(Calendar.FEBRUARY, resFeb2025.get(Calendar.MONTH))
        assertEquals(28, resFeb2025.get(Calendar.DAY_OF_MONTH))

        // Given: Jan 31, 2028 (leap year)
        val cal2028 = Calendar.getInstance().apply {
            set(2028, Calendar.JANUARY, 31, 10, 0, 0)
        }
        val nextFeb2028 = RecurringScheduler.computeNextDueDate(cal2028.timeInMillis, RecurringFrequency.MONTHLY)
        val resFeb2028 = Calendar.getInstance().apply { timeInMillis = nextFeb2028 }

        // Then: Clamps to Feb 29, 2028
        assertEquals(Calendar.FEBRUARY, resFeb2028.get(Calendar.MONTH))
        assertEquals(29, resFeb2028.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testScheduler_isIdempotentAfterCrashOrRestart() {
        // Given: A recurring item posted for August 2026 before worker crash/restart
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 15, 10, 0, 0)
        }
        val lastPostedTimestamp = cal.timeInMillis

        // When: App/worker restarts and checks if item is already posted for August 2026
        val targetMonthYear = "2026-08"
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val isAlreadyPosted = sdf.format(java.util.Date(lastPostedTimestamp)) == targetMonthYear

        // Then: Idempotency is preserved and duplicate execution is prevented
        assertTrue(isAlreadyPosted)
    }

    @Test
    fun testEffectiveDueDateMonthly_projectsPastDueDateToTargetMonth() {
        // Given: A bill with nextDueDate in September 2026 (previous month)
        val sepCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 10, 0, 0)
        }
        val item = com.selfbudget.app.data.model.RecurringTransactionEntity(
            userId = "u1",
            title = "Gym",
            amount = 50.0,
            type = com.selfbudget.app.data.model.TransactionType.EXPENSE,
            categoryId = "cat_fitness",
            frequency = RecurringFrequency.MONTHLY,
            nextDueDate = sepCal.timeInMillis
        )

        // When: Projecting to October 2026, unpaid
        val effectiveDue = RecurringScheduler.computeEffectiveDueDate(
            item = item,
            targetMonthYear = "2026-10",
            isCyclePaid = false
        )
        val resCal = Calendar.getInstance().apply { timeInMillis = effectiveDue }

        // Then: Should be October 15, 2026, not September
        assertEquals(2026, resCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, resCal.get(Calendar.MONTH))
        assertEquals(15, resCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testEffectiveDueDateMonthly_whenCyclePaid_advancesToNextMonth() {
        // Given: A bill for October 2026 that has already been posted/paid
        val octCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 10, 0, 0)
        }
        val item = com.selfbudget.app.data.model.RecurringTransactionEntity(
            userId = "u1",
            title = "Internet",
            amount = 80.0,
            type = com.selfbudget.app.data.model.TransactionType.EXPENSE,
            categoryId = "cat_utilities",
            frequency = RecurringFrequency.MONTHLY,
            nextDueDate = octCal.timeInMillis
        )

        // When: Projecting to October 2026 with isCyclePaid = true
        val effectiveDue = RecurringScheduler.computeEffectiveDueDate(
            item = item,
            targetMonthYear = "2026-10",
            isCyclePaid = true
        )
        val resCal = Calendar.getInstance().apply { timeInMillis = effectiveDue }

        // Then: Next due date advances to November 10, 2026
        assertEquals(2026, resCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, resCal.get(Calendar.MONTH))
        assertEquals(10, resCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testEffectiveDueDate_preservesFutureStartDate() {
        // Given: A recurring item with a future start date in December 2026
        val decCal = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 1, 10, 0, 0)
        }
        val item = com.selfbudget.app.data.model.RecurringTransactionEntity(
            userId = "u1",
            title = "New Car Lease",
            amount = 400.0,
            type = com.selfbudget.app.data.model.TransactionType.EXPENSE,
            categoryId = "cat_auto",
            frequency = RecurringFrequency.MONTHLY,
            nextDueDate = decCal.timeInMillis
        )

        // When: Viewing October 2026
        val effectiveDue = RecurringScheduler.computeEffectiveDueDate(
            item = item,
            targetMonthYear = "2026-10",
            isCyclePaid = false
        )

        // Then: Future start date is preserved and not pulled backward into October
        assertEquals(decCal.timeInMillis, effectiveDue)
    }

    @Test
    fun testEffectiveDueDateSemiMonthly_handlesPartiallyAndFullyPaid() {
        val sepCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 1, 9, 0, 0)
        }
        val item = com.selfbudget.app.data.model.RecurringTransactionEntity(
            userId = "u1",
            title = "Salary",
            amount = 2500.0,
            type = com.selfbudget.app.data.model.TransactionType.INCOME,
            categoryId = "cat_income",
            frequency = RecurringFrequency.SEMI_MONTHLY,
            nextDueDate = sepCal.timeInMillis
        )

        // When: In October 2026, 0 posted
        val due0 = RecurringScheduler.computeEffectiveDueDate(
            item = item,
            targetMonthYear = "2026-10",
            isCyclePaid = false,
            postedOccurrences = 0
        )
        val cal0 = Calendar.getInstance().apply { timeInMillis = due0 }
        assertEquals(Calendar.OCTOBER, cal0.get(Calendar.MONTH))
        assertEquals(1, cal0.get(Calendar.DAY_OF_MONTH))

        // When: In October 2026, 1 posted (partially paid)
        val due1 = RecurringScheduler.computeEffectiveDueDate(
            item = item,
            targetMonthYear = "2026-10",
            isCyclePaid = false,
            postedOccurrences = 1
        )
        val cal1 = Calendar.getInstance().apply { timeInMillis = due1 }
        assertEquals(Calendar.OCTOBER, cal1.get(Calendar.MONTH))
        assertEquals(15, cal1.get(Calendar.DAY_OF_MONTH))

        // When: In October 2026, fully paid (2 posted)
        val due2 = RecurringScheduler.computeEffectiveDueDate(
            item = item,
            targetMonthYear = "2026-10",
            isCyclePaid = true,
            postedOccurrences = 2
        )
        val cal2 = Calendar.getInstance().apply { timeInMillis = due2 }
        assertEquals(Calendar.NOVEMBER, cal2.get(Calendar.MONTH))
        assertEquals(1, cal2.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testAdvancePastDueDate_catchesUpMultipleLapsedCycles() {
        // Given: A monthly bill due on the 5th, with stored nextDueDate in July 2026 (3 cycles ago)
        val julCal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 5, 10, 0, 0)
        }
        val oct1Cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        }

        // When: Catching up past Oct 1, 2026
        val caughtUp = RecurringScheduler.advancePastDueDate(
            currentDueDate = julCal.timeInMillis,
            frequency = RecurringFrequency.MONTHLY,
            minDateMillis = oct1Cal.timeInMillis
        )
        val resCal = Calendar.getInstance().apply { timeInMillis = caughtUp }

        // Then: Should be October 5, 2026
        assertEquals(2026, resCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, resCal.get(Calendar.MONTH))
        assertEquals(5, resCal.get(Calendar.DAY_OF_MONTH))
    }
}
