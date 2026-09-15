package com.selfbudget.app

import com.selfbudget.app.core.util.VoiceParser
import com.selfbudget.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceParserTest {

    @Test
    fun testUserReportedCases() {
        // "spent 15 dollars for lunch at the chinese resturant"
        val result1 = VoiceParser.parseSpokenText("spent 15 dollars for lunch at the chinese resturant")
        assertNotNull(result1)
        assertEquals(15.0, result1!!.amount, 0.001)
        assertEquals("Lunch At The Chinese Resturant", result1.title)
        assertEquals(TransactionType.EXPENSE, result1.type)

        // STT alternative with currency formatting: "spend $15 for lunch at the chinese resturant"
        val result2 = VoiceParser.parseSpokenText("spend $15 for lunch at the chinese resturant")
        assertNotNull(result2)
        assertEquals(15.0, result2!!.amount, 0.001)
        assertEquals("Lunch At The Chinese Resturant", result2.title)

        // "10 at starbucks"
        val result3 = VoiceParser.parseSpokenText("10 at starbucks")
        assertNotNull(result3)
        assertEquals(10.0, result3!!.amount, 0.001)
        assertEquals("Starbucks", result3.title)

        // STT alternative: "$10 in Star"
        val result4 = VoiceParser.parseSpokenText("$10 in Star")
        assertNotNull(result4)
        assertEquals(10.0, result4!!.amount, 0.001)
        assertEquals("Star", result4.title)

        // "$10 in Starbucks"
        val result5 = VoiceParser.parseSpokenText("$10 in Starbucks")
        assertNotNull(result5)
        assertEquals(10.0, result5!!.amount, 0.001)
        assertEquals("Starbucks", result5.title)
    }

    @Test
    fun testAmountFormats() {
        // Plain dollar number with $
        val r1 = VoiceParser.parseSpokenText("$25 lunch")
        assertNotNull(r1)
        assertEquals(25.0, r1!!.amount, 0.001)
        assertEquals("Lunch", r1.title)

        // Decimal amount
        val r2 = VoiceParser.parseSpokenText("paid $45.50 for electric bill")
        assertNotNull(r2)
        assertEquals(45.50, r2!!.amount, 0.001)
        assertEquals("Electric Bill", r2.title)

        // Spoken "bucks"
        val r3 = VoiceParser.parseSpokenText("target 30 bucks")
        assertNotNull(r3)
        assertEquals(30.0, r3!!.amount, 0.001)
        assertEquals("Target", r3.title)
    }

    @Test
    fun testPreservesSubstringsContainingStopWords() {
        // "California" contains "for", should NOT be damaged
        val r1 = VoiceParser.parseSpokenText("California Pizza Kitchen $35")
        assertNotNull(r1)
        assertEquals(35.0, r1!!.amount, 0.001)
        assertEquals("California Pizza Kitchen", r1.title)

        // "Forks" contains "for", should NOT be damaged
        val r2 = VoiceParser.parseSpokenText("Target forks 8 dollars")
        assertNotNull(r2)
        assertEquals(8.0, r2!!.amount, 0.001)
        assertEquals("Target Forks", r2.title)
    }

    @Test
    fun testIncomeDetection() {
        val r1 = VoiceParser.parseSpokenText("salary deposit 3000")
        assertNotNull(r1)
        assertEquals(3000.0, r1!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, r1.type)
        assertEquals("Salary Deposit", r1.title)

        val r2 = VoiceParser.parseSpokenText("salary 3000")
        assertNotNull(r2)
        assertEquals(3000.0, r2!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, r2.type)
        assertEquals("Salary", r2.title)

        val r3 = VoiceParser.parseSpokenText("received refund 50 dollars")
        assertNotNull(r3)
        assertEquals(50.0, r3!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, r3.type)
        assertEquals("Refund", r3.title)
    }

    @Test
    fun testOnlyAmountSpoken() {
        val r1 = VoiceParser.parseSpokenText("$50")
        assertNotNull(r1)
        assertEquals(50.0, r1!!.amount, 0.001)
        assertEquals("Voice Expense", r1.title)

        val r2 = VoiceParser.parseSpokenText("income $500")
        assertNotNull(r2)
        assertEquals(500.0, r2!!.amount, 0.001)
        assertEquals("Voice Income", r2.title)
    }

    @Test
    fun testEmptyOrInvalidInput() {
        assertNull(VoiceParser.parseSpokenText(""))
        assertNull(VoiceParser.parseSpokenText("   "))
        assertNull(VoiceParser.parseSpokenText("no amount mentioned here"))
        assertNull(VoiceParser.parseSpokenText("spent zero dollars"))
    }
}
