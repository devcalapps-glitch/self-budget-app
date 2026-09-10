package com.selfbudget.app

import com.selfbudget.app.core.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DateUtilsTest {

    @Test
    fun testLocalDateToUtcAndBack_preservesDateInAllTimezones() {
        val testTimezones = listOf("America/Los_Angeles", "America/New_York", "Asia/Tokyo", "Europe/London", "Pacific/Honolulu")
        val originalTz = TimeZone.getDefault()

        try {
            for (tzId in testTimezones) {
                TimeZone.setDefault(TimeZone.getTimeZone(tzId))

                // Given Sep 1, 2026 in local time
                val localCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, 2026)
                    set(Calendar.MONTH, Calendar.SEPTEMBER)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 15)
                    set(Calendar.MINUTE, 30)
                }
                val localMillis = localCal.timeInMillis

                // Convert to DatePicker UTC
                val utcMillis = DateUtils.localDateToUtcMillis(localMillis)

                // Suppose DatePicker returns that UTC millis (or user picked Sep 1)
                val backToLocal = DateUtils.utcMillisToLocalDate(utcMillis, localMillis)

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val resultDateString = sdf.format(backToLocal)

                assertEquals("Timezone $tzId failed to preserve Sep 1st", "2026-09-01", resultDateString)
            }
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }
}
