package com.selfbudget.app

import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.core.util.CurrencyConverter
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.core.util.MonthlyReviewHelper
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MonthlyAccountReviewTest {

    @Test
    fun testIsFirstDayOfMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        assertTrue("October 1st should be identified as the first day of the month", MonthlyReviewHelper.isFirstDayOfMonth(cal))

        cal.set(Calendar.DAY_OF_MONTH, 2)
        assertFalse("October 2nd is not the first day of the month", MonthlyReviewHelper.isFirstDayOfMonth(cal))

        cal.set(Calendar.DAY_OF_MONTH, 31)
        assertFalse("October 31st is not the first day of the month", MonthlyReviewHelper.isFirstDayOfMonth(cal))
    }

    @Test
    fun testGetMonthYearKeyAndFirstOfMonthLabel() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val key = MonthlyReviewHelper.getMonthYearKey(cal)
        assertEquals("2026-10", key)

        val label = MonthlyReviewHelper.formatFirstOfMonthLabel(cal)
        assertEquals("October 1", label)
    }

    @Test
    fun testCheckingAccountBalanceUpdate() {
        val account = AccountEntity(
            id = "acc_chk",
            userId = "u1",
            name = "Chase Checking",
            type = AccountType.CHECKING,
            initialBalance = 1000.0
        )
        val txs = listOf(
            TransactionEntity(userId = "u1", title = "Paycheck", amount = 500.0, type = TransactionType.INCOME, categoryId = "c1", accountId = "acc_chk"),
            TransactionEntity(userId = "u1", title = "Groceries", amount = 150.0, type = TransactionType.EXPENSE, categoryId = "c2", accountId = "acc_chk")
        )
        // Current balance: 1000 + 500 - 150 = 1350
        val currentBalance = AccountBalanceCalculator.computeBalance(account, txs)
        assertEquals(1350.0, currentBalance, 0.001)

        // User reviews on the 1st of the month and sets new balance to 1500.0
        val updatedAccount = AccountBalanceCalculator.calculateAccountWithUpdatedBalance(
            account = account,
            currentLiveBalance = currentBalance,
            targetLiveBalance = 1500.0
        )

        // Initial balance should be adjusted to 1150.0 (1500 - 350 delta)
        assertEquals(1150.0, updatedAccount.initialBalance, 0.001)

        // Computed balance with existing transactions should now exactly equal 1500.0
        val newComputed = AccountBalanceCalculator.computeBalance(updatedAccount, txs)
        assertEquals(1500.0, newComputed, 0.001)
    }

    @Test
    fun testSavingsAccountBalanceUpdate() {
        val savings = AccountEntity(
            id = "acc_sav",
            userId = "u1",
            name = "High Yield Savings",
            type = AccountType.SAVINGS,
            initialBalance = 10000.0
        )
        val txs = emptyList<TransactionEntity>()
        val currentBalance = AccountBalanceCalculator.computeBalance(savings, txs)
        assertEquals(10000.0, currentBalance, 0.001)

        // User updates balance to 10250.0 on 1st of month (reflecting interest earned)
        val updatedSavings = AccountBalanceCalculator.calculateAccountWithUpdatedBalance(
            account = savings,
            currentLiveBalance = currentBalance,
            targetLiveBalance = 10250.0
        )

        val newComputed = AccountBalanceCalculator.computeBalance(updatedSavings, txs)
        assertEquals(10250.0, newComputed, 0.001)
    }

    @Test
    fun testInvestmentAndRetirementAccountBalanceUpdate() {
        val retirement = AccountEntity(
            id = "acc_401k",
            userId = "u1",
            name = "Fidelity 401(k)",
            type = AccountType.RETIREMENT,
            initialBalance = 50000.0
        )
        val txs = emptyList<TransactionEntity>()

        // User updates investment balance reflecting market growth on 1st of month
        val updated401k = AccountBalanceCalculator.calculateAccountWithUpdatedBalance(
            account = retirement,
            currentLiveBalance = 50000.0,
            targetLiveBalance = 52400.0
        )

        val newComputed = AccountBalanceCalculator.computeBalance(updated401k, txs)
        assertEquals(52400.0, newComputed, 0.001)
    }

    @Test
    fun testCreditCardLiabilityAccountBalanceUpdate() {
        val creditCard = AccountEntity(
            id = "acc_cc",
            userId = "u1",
            name = "Amex Blue Cash",
            type = AccountType.CREDIT_CARD,
            initialBalance = -200.0
        )
        val txs = listOf(
            TransactionEntity(userId = "u1", title = "Dinner", amount = 100.0, type = TransactionType.EXPENSE, categoryId = "c1", accountId = "acc_cc")
        )
        // Current balance: -200 - 100 = -300 (debt of 300)
        val currentBalance = AccountBalanceCalculator.computeBalance(creditCard, txs)
        assertEquals(-300.0, currentBalance, 0.001)

        // User reviews on the 1st of the month, sees statement, enters new balance of 450 (meaning $450 debt)
        val updatedCard = AccountBalanceCalculator.calculateAccountWithUpdatedBalance(
            account = creditCard,
            currentLiveBalance = currentBalance,
            targetLiveBalance = 450.0
        )

        // Internal balance should now be -450.0
        val newComputed = AccountBalanceCalculator.computeBalance(updatedCard, txs)
        assertEquals(-450.0, newComputed, 0.001)

        // If user paid off the card to 0 debt:
        val zeroDebtCard = AccountBalanceCalculator.calculateAccountWithUpdatedBalance(
            account = updatedCard,
            currentLiveBalance = newComputed,
            targetLiveBalance = 0.0
        )
        val zeroComputed = AccountBalanceCalculator.computeBalance(zeroDebtCard, txs)
        assertEquals(0.0, zeroComputed, 0.001)
    }

    @Test
    fun testSimultaneousMonthlyReviewAcrossMultipleAccountTypes() {
        val checking = AccountEntity(id = "a1", userId = "u1", name = "Checking", type = AccountType.CHECKING, initialBalance = 1000.0)
        val savings = AccountEntity(id = "a2", userId = "u1", name = "Savings", type = AccountType.SAVINGS, initialBalance = 5000.0)
        val investment = AccountEntity(id = "a3", userId = "u1", name = "Brokerage", type = AccountType.INVESTMENT, initialBalance = 20000.0)
        val creditCard = AccountEntity(id = "a4", userId = "u1", name = "Credit Card", type = AccountType.CREDIT_CARD, initialBalance = -500.0)

        val allAccounts = listOf(checking, savings, investment, creditCard)
        val allTxs = emptyList<TransactionEntity>()

        // User enters new balances on 1st of month:
        // Checking: 1200, Savings: 5500, Investment: 21000, Credit Card debt: 300 (which is -300)
        val targets = mapOf(
            "a1" to 1200.0,
            "a2" to 5500.0,
            "a3" to 21000.0,
            "a4" to 300.0
        )

        val updatedAccounts = allAccounts.map { acc ->
            val cur = AccountBalanceCalculator.computeBalance(acc, allTxs)
            AccountBalanceCalculator.calculateAccountWithUpdatedBalance(
                account = acc,
                currentLiveBalance = cur,
                targetLiveBalance = targets[acc.id] ?: cur
            )
        }

        // Verify each individual account
        assertEquals(1200.0, AccountBalanceCalculator.computeBalance(updatedAccounts[0], allTxs), 0.001)
        assertEquals(5500.0, AccountBalanceCalculator.computeBalance(updatedAccounts[1], allTxs), 0.001)
        assertEquals(21000.0, AccountBalanceCalculator.computeBalance(updatedAccounts[2], allTxs), 0.001)
        assertEquals(-300.0, AccountBalanceCalculator.computeBalance(updatedAccounts[3], allTxs), 0.001)

        // Verify total Net Worth:
        // Assets: 1200 + 5500 + 21000 = 27700
        // Liabilities: 300
        // Net Worth: 27700 - 300 = 27400
        val netWorth = AccountBalanceCalculator.computeTotalInBaseCurrency(
            accounts = updatedAccounts,
            allTransactions = allTxs,
            baseCurrency = "USD",
            rates = emptyList()
        )
        assertEquals(27400.0, netWorth, 0.001)
    }
}
