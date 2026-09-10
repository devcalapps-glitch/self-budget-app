package com.selfbudget.app.core.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Utility functions for timezone conversions between Material 3 DatePicker (which operates in UTC)
 * and the user's local timezone.
 */
object DateUtils {

    /**
     * Converts a local timestamp (in milliseconds) to UTC midnight milliseconds suitable for
     * initializing Material 3's `rememberDatePickerState(initialSelectedDateMillis = ...)`.
     */
    fun localDateToUtcMillis(localTimestamp: Long): Long {
        val localCal = Calendar.getInstance().apply {
            timeInMillis = localTimestamp
        }
        val year = localCal.get(Calendar.YEAR)
        val month = localCal.get(Calendar.MONTH)
        val day = localCal.get(Calendar.DAY_OF_MONTH)

        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }
        return utcCal.timeInMillis
    }

    /**
     * Converts a UTC midnight timestamp (as returned by Material 3 `datePickerState.selectedDateMillis`)
     * into a local timestamp representing the same calendar date in the user's local timezone,
     * preserving the local time-of-day from [currentLocalTimestamp].
     */
    fun utcMillisToLocalDate(utcMillis: Long, currentLocalTimestamp: Long = System.currentTimeMillis()): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val year = utcCal.get(Calendar.YEAR)
        val month = utcCal.get(Calendar.MONTH)
        val day = utcCal.get(Calendar.DAY_OF_MONTH)

        val localCal = Calendar.getInstance().apply {
            timeInMillis = currentLocalTimestamp
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return localCal.timeInMillis
    }
}
