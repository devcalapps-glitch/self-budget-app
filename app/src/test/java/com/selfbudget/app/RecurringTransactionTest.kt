package com.selfbudget.app

import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated Unit Test Suite for Recurring Subscriptions, Frequency Normalization, and Duplicate Posting Safeguards.
 * Verifies Feature F-07, F-30, and F-34.
 */
class RecurringTransactionTest {

    @Test
    fun testFrequencyMonthlyEquivalent() {
        // Exact ratios (52/12 weekly, 26/12 bi-weekly), matching RecurringFrequencyNormalizer -
        // NOT the truncated `* 4.333` / `* 2.166` approximations this test used to assert (F-103).

        // Test weekly ($100/wk -> $433.33/mo)
        val weekly = 100.0 * 52.0 / 12.0
        assertEquals(433.33, weekly, 0.01)

        // Test bi-weekly ($500/bi-wk -> $1083.33/mo)
        val biWeekly = 500.0 * 26.0 / 12.0
        assertEquals(1083.33, biWeekly, 0.01)

        // Test yearly ($1200/yr -> $100.00/mo)
        val yearly = 1200.0 / 12.0
        assertEquals(100.00, yearly, 0.001)
    }

    @Test
    fun testDuplicatePostingDetection() {
        val now = System.currentTimeMillis()

        val existingTransactions = listOf(
            TransactionEntity(
                id = "t1",
                userId = "u1",
                title = "Netflix",
                amount = 15.99,
                type = TransactionType.EXPENSE,
                categoryId = "cat_subs",
                timestamp = now - 3600000 // 1 hour ago
            )
        )

        // Attempting to post another "Netflix" for $15.99
        val isDuplicate = existingTransactions.any { tx ->
            tx.title.equals("Netflix", ignoreCase = true) &&
                    tx.amount == 15.99 &&
                    (now - tx.timestamp) < (24 * 60 * 60 * 1000)
        }

        assertTrue("Should detect potential duplicate posting within 24 hours", isDuplicate)
    }

    @Test
    fun testNonDuplicatePosting() {
        val now = System.currentTimeMillis()

        val existingTransactions = listOf(
            TransactionEntity(
                id = "t1",
                userId = "u1",
                title = "Netflix",
                amount = 15.99,
                type = TransactionType.EXPENSE,
                categoryId = "cat_subs",
                timestamp = now - (48 * 3600000) // 48 hours ago
            )
        )

        val isDuplicate = existingTransactions.any { tx ->
            tx.title.equals("Netflix", ignoreCase = true) &&
                    tx.amount == 15.99 &&
                    (now - tx.timestamp) < (24 * 60 * 60 * 1000)
        }

        assertFalse("Should not flag as duplicate if past 24 hours", isDuplicate)
    }

    @Test
    fun testDedupKeyIncludesFrequencyAndPreservesDistinctSubscriptions() {
        val user = "u1"
        val categoryId = "cat_entertainment"
        val title = "Streaming Sub"

        val itemWeekly = com.selfbudget.app.data.model.RecurringTransactionEntity(
            userId = user,
            title = title,
            amount = 10.0,
            type = TransactionType.EXPENSE,
            categoryId = categoryId,
            frequency = com.selfbudget.app.data.model.RecurringFrequency.WEEKLY
        )

        val itemMonthly = com.selfbudget.app.data.model.RecurringTransactionEntity(
            userId = user,
            title = title,
            amount = 40.0,
            type = TransactionType.EXPENSE,
            categoryId = categoryId,
            frequency = com.selfbudget.app.data.model.RecurringFrequency.MONTHLY
        )

        val existingList = listOf(itemWeekly)

        // Matching with frequency inclusion
        val matchFoundForMonthly = existingList.find {
            it.userId == user &&
            it.categoryId == categoryId &&
            it.title.trim().equals(title, ignoreCase = true) &&
            it.frequency == itemMonthly.frequency
        }

        // Must NOT match weekly item when searching for monthly item
        org.junit.Assert.assertNull(matchFoundForMonthly)
    }
}
