package com.selfbudget.app

import com.selfbudget.app.core.util.toWordTitleCase
import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilsTest {

    @Test
    fun testEmptyString() {
        assertEquals("", "".toWordTitleCase())
    }

    @Test
    fun testSingleWord() {
        assertEquals("Chase", "chase".toWordTitleCase())
        assertEquals("Chase", "Chase".toWordTitleCase())
    }

    @Test
    fun testMultipleWords() {
        assertEquals("Chase Bank", "chase bank".toWordTitleCase())
        assertEquals("Chase Bank And Trust", "chase bank and trust".toWordTitleCase())
    }

    @Test
    fun testTrailingAndMultipleSpaces() {
        assertEquals("Chase ", "chase ".toWordTitleCase())
        assertEquals("Chase  Bank ", "chase  bank ".toWordTitleCase())
    }

    @Test
    fun testWithNumbersAndSymbols() {
        assertEquals("1st Bank", "1st bank".toWordTitleCase())
        assertEquals("Costco - Gas", "costco - gas".toWordTitleCase())
    }
}
