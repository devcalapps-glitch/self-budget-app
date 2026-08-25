package com.selfbudget.app

import com.selfbudget.app.core.util.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AnalyticsPaceTest {

    @Test
    fun testAnnualPaceExtrapolatesByDaysElapsed() {
        // Given: YTD expense spend of $18,000 on day 180 of a 365-day year
        val ytdExpense = 18000.0
        val dayOfYear = 180
        val daysInYear = 365

        val annualPace = Money.round((ytdExpense / dayOfYear) * daysInYear)

        // $18,000 / 180 * 365 = $36,500.00
        assertEquals(36500.0, annualPace, 0.001)
    }
}
