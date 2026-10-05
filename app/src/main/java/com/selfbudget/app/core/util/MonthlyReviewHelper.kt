package com.selfbudget.app.core.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Helper and preference manager for the First-of-the-Month Account Balance Review.
 *
 * Checks if the current calendar day is the 1st of the month and tracks whether the user
 * has already reviewed and updated their account balances for that month.
 */
object MonthlyReviewHelper {
    private const val PREFS_NAME = "monthly_account_review_prefs"
    private const val KEY_LAST_REVIEWED_MONTH = "last_reviewed_month_year"

    /**
     * Returns true if the given calendar (or current wall-clock date) is the 1st day of the month.
     */
    fun isFirstDayOfMonth(calendar: Calendar = Calendar.getInstance()): Boolean {
        return calendar.get(Calendar.DAY_OF_MONTH) == 1
    }

    /**
     * Formats the given calendar date as "yyyy-MM" (e.g. "2026-10").
     */
    fun getMonthYearKey(calendar: Calendar = Calendar.getInstance()): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    /**
     * Returns a human-friendly label for the 1st of the given month (e.g. "October 1").
     */
    fun formatFirstOfMonthLabel(calendar: Calendar = Calendar.getInstance()): String {
        val sdf = SimpleDateFormat("MMMM 1", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    /**
     * Checks if the user has already completed the balance review for the given month.
     */
    fun hasReviewedMonth(context: Context, monthYearKey: String = getMonthYearKey()): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_REVIEWED_MONTH, null) == monthYearKey
    }

    /**
     * Marks the given month as reviewed.
     */
    fun markMonthReviewed(context: Context, monthYearKey: String = getMonthYearKey()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_REVIEWED_MONTH, monthYearKey).apply()
    }

    /**
     * Clears the reviewed month record (useful for testing or re-triggering review).
     */
    fun clearReviewedMonth(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LAST_REVIEWED_MONTH).apply()
    }

    /**
     * Determines whether the monthly review pop-up should be automatically displayed:
     * - Today is the 1st day of the month
     * - The user has not yet completed the review for this month
     */
    fun shouldPromptMonthlyReview(
        context: Context,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        val monthKey = getMonthYearKey(calendar)
        return isFirstDayOfMonth(calendar) && !hasReviewedMonth(context, monthKey)
    }
}
