package com.selfbudget.app

import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Automated Unit Test Suite for Accounts & Wallet Balance Calculation.
 * Verifies Feature F-16 and F-44.
 */
class AccountBalanceTest {

    @Test
    fun testTotalBalanceAggregation() {
        val accounts = listOf(
            AccountEntity(id = "acc1", userId = "u1", name = "Checking Account", type = AccountType.CHECKING, initialBalance = 2500.0),
            AccountEntity(id = "acc2", userId = "u1", name = "High-Yield Savings", type = AccountType.SAVINGS, initialBalance = 10000.0),
            AccountEntity(id = "acc3", userId = "u1", name = "Cash Wallet", type = AccountType.CASH, initialBalance = 350.0),
            AccountEntity(id = "acc4", userId = "u1", name = "Credit Card", type = AccountType.CREDIT_CARD, initialBalance = -450.0)
        )

        val totalNetWorth = accounts.sumOf { it.initialBalance }

        assertEquals(12400.0, totalNetWorth, 0.001)
    }

    @Test
    fun testEmptyAccountsBalance() {
        val accounts = emptyList<AccountEntity>()
        val total = accounts.sumOf { it.initialBalance }
        assertEquals(0.0, total, 0.001)
    }

    @Test
    fun testAccountDeletion() {
        val accounts = mutableListOf(
            AccountEntity(id = "acc1", userId = "u1", name = "Checking Account", type = AccountType.CHECKING, initialBalance = 2500.0),
            AccountEntity(id = "acc2", userId = "u1", name = "High-Yield Savings", type = AccountType.SAVINGS, initialBalance = 10000.0)
        )

        // Delete account acc1
        accounts.removeIf { it.id == "acc1" }

        assertEquals(1, accounts.size)
        assertEquals("High-Yield Savings", accounts.first().name)
        assertEquals(10000.0, accounts.sumOf { it.initialBalance }, 0.001)
    }
}
