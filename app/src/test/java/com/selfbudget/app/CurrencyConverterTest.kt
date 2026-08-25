package com.selfbudget.app

import com.selfbudget.app.core.util.CurrencyConverter
import com.selfbudget.app.data.model.ExchangeRateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConverterTest {

    private val rates = listOf(
        ExchangeRateEntity(id = "EUR-USD", userId = "u1", fromCurrency = "EUR", toCurrency = "USD", rate = 1.10)
    )

    @Test
    fun testSameCurrencyIsUnchanged() {
        assertEquals(100.0, CurrencyConverter.convert(100.0, "USD", "USD", rates), 0.001)
    }

    @Test
    fun testDirectRateConversion() {
        assertEquals(110.0, CurrencyConverter.convert(100.0, "EUR", "USD", rates), 0.001)
    }

    @Test
    fun testInverseRateConversion() {
        // Only EUR->USD is on file; converting USD->EUR should use the inverse.
        val result = CurrencyConverter.convert(110.0, "USD", "EUR", rates)
        assertEquals(100.0, result, 0.01)
    }

    @Test
    fun testMissingRateReturnsFaceValue() {
        val result = CurrencyConverter.convert(100.0, "GBP", "USD", rates)
        assertEquals(100.0, result, 0.001)
    }

    @Test
    fun testHasRateDetection() {
        assertTrue(CurrencyConverter.hasRate("EUR", "USD", rates))
        assertTrue(CurrencyConverter.hasRate("USD", "EUR", rates))
        assertTrue(CurrencyConverter.hasRate("USD", "USD", rates))
        assertFalse(CurrencyConverter.hasRate("GBP", "USD", rates))
    }

    @Test
    fun testCurrencyConversionRoundingAndHistoricalRateBehavior() {
        // Given: EUR 100.00 and an exchange rate of 1 EUR = 1.0853 USD
        val ratev1 = listOf(ExchangeRateEntity(id = "EUR-USD", userId = "u1", fromCurrency = "EUR", toCurrency = "USD", rate = 1.0853))
        val convertedv1 = CurrencyConverter.convert(100.0, "EUR", "USD", ratev1)

        // Then: Result converts to 108.53 USD with exact cent rounding
        assertEquals(108.53, com.selfbudget.app.core.util.Money.round(convertedv1), 0.0)

        // When: Historical rate is updated to 1 EUR = 1.1200 USD
        val ratev2 = listOf(ExchangeRateEntity(id = "EUR-USD", userId = "u1", fromCurrency = "EUR", toCurrency = "USD", rate = 1.1200))
        val convertedv2 = CurrencyConverter.convert(100.0, "EUR", "USD", ratev2)

        // Then: Conversion updates to 112.00 USD
        assertEquals(112.00, com.selfbudget.app.core.util.Money.round(convertedv2), 0.0)
    }
}
