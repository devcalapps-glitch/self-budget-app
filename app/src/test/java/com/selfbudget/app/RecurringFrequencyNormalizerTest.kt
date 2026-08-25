package com.selfbudget.app

import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import com.selfbudget.app.data.model.RecurringFrequency
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringFrequencyNormalizerTest {

    @Test
    fun testWeeklyNormalization_ExactFraction() {
        // Given: $500/week bill
        val weeklyAmount = 500.0

        // When: Normalizing to monthly (52 / 12 = 4.333333...)
        val monthlyAmount = RecurringFrequencyNormalizer.toMonthlyAmount(weeklyAmount, RecurringFrequency.WEEKLY)

        // Then: Must equal $2,166.67 (not $2,166.50 from 4.333 truncation)
        assertEquals(2166.67, monthlyAmount, 0.001)
    }

    @Test
    fun testBiWeeklyNormalization_ExactFraction() {
        // Given: $1,000 bi-weekly paycheck
        val biWeeklyAmount = 1000.0

        // When: Normalizing to monthly (26 / 12 = 2.166666...)
        val monthlyAmount = RecurringFrequencyNormalizer.toMonthlyAmount(biWeeklyAmount, RecurringFrequency.BI_WEEKLY)

        // Then: Must equal $2,166.67
        assertEquals(2166.67, monthlyAmount, 0.001)
    }
}
