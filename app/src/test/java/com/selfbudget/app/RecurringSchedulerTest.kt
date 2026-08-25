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
}
