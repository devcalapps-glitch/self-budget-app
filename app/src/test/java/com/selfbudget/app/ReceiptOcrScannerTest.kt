package com.selfbudget.app

import com.selfbudget.app.core.util.ReceiptOcrScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReceiptOcrScannerTest {

    @Test
    fun testChipotleCashReceipt() {
        val lines = listOf(
            "CHIPOTLE MEXICAN GRILL",
            "Store # 1234",
            "123 Main St, Anytown CA",
            "Host: Devar M.",
            "Order: 82",
            "09/14/2026 12:45 PM",
            "1 Burrito Bowl    9.95",
            "Subtotal          9.95",
            "Tax               0.55",
            "Total            10.50",
            "Cash Tender      20.00",
            "Change Due        9.50",
            "Thanks for visiting Chipotle!"
        )

        val result = ReceiptOcrScanner.parseReceiptText(lines)
        assertEquals("Chipotle Mexican Grill", result.merchantName)
        assertEquals(10.50, result.totalAmount ?: 0.0, 0.001)
        assertNotNull(result.timestamp)
    }

    @Test
    fun testChipotleWithDeliciousnessReceivedHeader() {
        val lines = listOf(
            "DELICIOUSNESS RECEIVED",
            "CHIPOTLE MEXICAN GRILL",
            "Store # 1234",
            "Host: Devar M.",
            "Order: 82",
            "Subtotal: $9.95",
            "Tax: $0.55",
            "Total: $10.50",
            "Cash Tendered: $20.00",
            "Change: $9.50"
        )

        val result = ReceiptOcrScanner.parseReceiptText(lines)
        assertEquals("Chipotle Mexican Grill", result.merchantName)
        assertEquals(10.50, result.totalAmount ?: 0.0, 0.001)
    }

    @Test
    fun testGrandTotalAndTenderExclusion() {
        val lines = listOf(
            "Target Store #0123",
            "Cashier: Jane",
            "Item 1 $15.00",
            "Item 2 $22.50",
            "Subtotal $37.50",
            "Tax $3.00",
            "Grand Total $40.50",
            "Paid with Cash $100.00",
            "Change $59.50"
        )

        val result = ReceiptOcrScanner.parseReceiptText(lines)
        assertEquals("Target", result.merchantName)
        assertEquals(40.50, result.totalAmount ?: 0.0, 0.001)
    }

    @Test
    fun testSubtotalAndTaxSumFallback() {
        val lines = listOf(
            "Coffee Shop",
            "Server: Alex",
            "Latte 5.50",
            "Croissant 4.00",
            "Subtotal: 9.50",
            "Tax: 0.80",
            "Amount: 10.30",
            "Cash: 50.00"
        )

        val result = ReceiptOcrScanner.parseReceiptText(lines)
        assertEquals("Coffee Shop", result.merchantName)
        assertEquals(10.30, result.totalAmount ?: 0.0, 0.001)
    }

    @Test
    fun testWelcomeToPrefixExtraction() {
        val lines = listOf(
            "Welcome to Panera Bread",
            "Order # 44",
            "Total Due $14.25"
        )

        val result = ReceiptOcrScanner.parseReceiptText(lines)
        assertEquals("Panera Bread", result.merchantName)
        assertEquals(14.25, result.totalAmount ?: 0.0, 0.001)
    }
}
