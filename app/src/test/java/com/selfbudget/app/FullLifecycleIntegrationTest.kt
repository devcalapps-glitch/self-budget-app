package com.selfbudget.app

import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.BudgetRollover
import com.selfbudget.app.core.util.CurrencyConverter
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.ExchangeRateEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-End Full Lifecycle Integration Test Suite.
 * Simulates complete real-world monthly personal finance workflows:
 * Paycheck → allocate budgets → recurring bill posts → grocery expense → credit-card purchase → credit-card payment → refund → month rollover → next month's budget
 */
class FullLifecycleIntegrationTest {

    private val checking = AccountEntity(id = "acc_checking", userId = "u1", name = "Checking", type = AccountType.CHECKING, initialBalance = 1000.0)
    private val creditCard = AccountEntity(id = "acc_cc", userId = "u1", name = "Credit Card", type = AccountType.CREDIT_CARD, initialBalance = 0.0)

    @Test
    fun testFullMonthlyLifecycle_paycheckToRolloverToNextMonthBudget() {
        val decTimestamp = 1767182400000L // Dec 2025

        // Step 1: Paycheck ($3,000 Income) deposited into Checking ($1,000 initial balance)
        val paycheck = TransactionEntity(
            id = "tx_paycheck",
            userId = "u1",
            title = "Bi-weekly Salary",
            amount = 3000.0,
            type = TransactionType.INCOME,
            categoryId = "cat_salary",
            accountId = "acc_checking",
            timestamp = decTimestamp
        )

        // Step 2: Allocate Category Budgets for December
        val rentBudgetDec = BudgetEntity(id = "b_rent_dec", userId = "u1", categoryId = "cat_rent", amountLimit = 1200.0, monthYear = "2025-12", rolloverEnabled = true)
        val groceriesBudgetDec = BudgetEntity(id = "b_groc_dec", userId = "u1", categoryId = "cat_groceries", amountLimit = 400.0, monthYear = "2025-12", rolloverEnabled = true)

        // Step 3: Recurring Bill Posts (Rent $1,200 from Checking)
        val rentPosted = TransactionEntity(
            id = "tx_rent",
            userId = "u1",
            title = "Apartment Rent",
            amount = 1200.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_rent",
            accountId = "acc_checking",
            timestamp = decTimestamp
        )

        // Step 4: Grocery Expense ($250 logged in Groceries category)
        val groceryExpense = TransactionEntity(
            id = "tx_groc",
            userId = "u1",
            title = "Trader Joe's",
            amount = 250.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_groceries",
            accountId = "acc_checking",
            timestamp = decTimestamp
        )

        // Step 5: Credit Card Purchase ($100 logged on Credit Card account)
        val ccPurchase = TransactionEntity(
            id = "tx_cc_shop",
            userId = "u1",
            title = "Electronics",
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_shopping",
            accountId = "acc_cc",
            timestamp = decTimestamp
        )

        // Step 6: Credit Card Payment ($100 transferred from Checking to Credit Card)
        val ccPayment = TransactionEntity(
            id = "tx_cc_pay",
            userId = "u1",
            title = "Pay CC Bill",
            amount = 100.0,
            type = TransactionType.TRANSFER,
            categoryId = "cat_transfer",
            accountId = "acc_checking",
            transferAccountId = "acc_cc",
            timestamp = decTimestamp
        )

        // Step 7: Refund ($50 returned for Grocery item logged as Income in Groceries category)
        val groceryRefund = TransactionEntity(
            id = "tx_refund",
            userId = "u1",
            title = "Trader Joe's Refund",
            amount = 50.0,
            type = TransactionType.INCOME,
            categoryId = "cat_groceries",
            accountId = "acc_checking",
            timestamp = decTimestamp
        )

        val decTransactions = listOf(paycheck, rentPosted, groceryExpense, ccPurchase, ccPayment, groceryRefund)

        // --- Verification of December State ---
        // Checking Balance: Initial $1,000 + $3,000 Salary - $1,200 Rent - $250 Groceries - $100 CC Payment + $50 Refund = $2,500.00
        val endingCheckingBalance = AccountBalanceCalculator.computeBalance(checking, decTransactions)
        assertEquals(2500.0, endingCheckingBalance, 0.001)

        // Credit Card Balance: Initial $0 - $100 Purchase + $100 Payment = $0.00 debt
        val endingCcBalance = AccountBalanceCalculator.computeBalance(creditCard, decTransactions)
        assertEquals(0.0, endingCcBalance, 0.001)

        // December Budget Calculations:
        // Rent: $1,200 limit - $1,200 spent = $0 remaining (0 rollover)
        val rentSpentDec = decTransactions.filter { it.type == TransactionType.EXPENSE && it.categoryId == "cat_rent" }.sumOf { it.amount }
        assertEquals(1200.0, rentSpentDec, 0.001)

        // Groceries: $400 limit - ($250 spent - $50 refund = $200 net spent) = $200 underspend bonus
        val groceriesExpensesDec = decTransactions.filter { it.type == TransactionType.EXPENSE && it.categoryId == "cat_groceries" }.sumOf { it.amount }
        val groceriesRefundDec = decTransactions.filter { it.type == TransactionType.INCOME && it.categoryId == "cat_groceries" }.sumOf { it.amount }
        val groceriesNetSpentDec = groceriesExpensesDec - groceriesRefundDec
        assertEquals(200.0, groceriesNetSpentDec, 0.001)

        // Step 8: Month Rollover from December 2025 to January 2026
        val janBaseGroceriesLimit = 400.0
        val janEffectiveGroceriesLimit = BudgetRollover.effectiveLimit(
            currentLimit = janBaseGroceriesLimit,
            rolloverEnabled = groceriesBudgetDec.rolloverEnabled,
            previousLimit = groceriesBudgetDec.amountLimit,
            previousSpent = groceriesNetSpentDec
        )

        // Step 9: Verify Next Month's Budget receives $200 December bonus -> $600.00 total limit
        assertEquals(600.0, janEffectiveGroceriesLimit, 0.001)
    }

    @Test
    fun testDebtPayoffAndMultiAccountLifecycle() {
        val autoLoan = AccountEntity(id = "acc_loan", userId = "u1", name = "Auto Loan", type = AccountType.LOAN, initialBalance = -10000.0)

        // 1. Initial Checking deposit
        val deposit = TransactionEntity(id = "1", userId = "u1", title = "Bonus", amount = 5000.0, type = TransactionType.INCOME, categoryId = "c1", accountId = "acc_checking")
        // 2. Auto Loan Payment ($400 from Checking to Loan)
        val loanPayment = TransactionEntity(id = "2", userId = "u1", title = "Loan Payment", amount = 400.0, type = TransactionType.EXPENSE, categoryId = "c2", accountId = "acc_checking", transferAccountId = "acc_loan")
        // 3. Credit Card Purchase ($200)
        val ccPurchase = TransactionEntity(id = "3", userId = "u1", title = "Gas", amount = 200.0, type = TransactionType.EXPENSE, categoryId = "c3", accountId = "acc_cc")
        // 4. Credit Card Payoff ($200 from Checking to Credit Card)
        val ccPayment = TransactionEntity(id = "4", userId = "u1", title = "CC Payoff", amount = 200.0, type = TransactionType.TRANSFER, categoryId = "c4", accountId = "acc_checking", transferAccountId = "acc_cc")

        val allTxs = listOf(deposit, loanPayment, ccPurchase, ccPayment)

        val endingChecking = AccountBalanceCalculator.computeBalance(checking, allTxs)
        val endingCc = AccountBalanceCalculator.computeBalance(creditCard, allTxs)
        val endingLoan = AccountBalanceCalculator.computeBalance(autoLoan, allTxs)

        // Checking: $1000 + $5000 - $400 - $200 = $5,400.00
        assertEquals(5400.0, endingChecking, 0.001)
        // CC: $0 - $200 + $200 = $0.00
        assertEquals(0.0, endingCc, 0.001)
        // Auto Loan: -$10,000 + $400 = -$9,600.00
        assertEquals(-9600.0, endingLoan, 0.001)
    }

    @Test
    fun testMultiCurrencyAndNetWorthLifecycle() {
        val eurSavings = AccountEntity(id = "acc_eur", userId = "u1", name = "EUR Vault", type = AccountType.SAVINGS, initialBalance = 1000.0, currencyCode = "EUR")
        val ratesv1 = listOf(ExchangeRateEntity(id = "r1", userId = "u1", fromCurrency = "EUR", toCurrency = "USD", rate = 1.10))

        // Initial Net Worth in USD: Checking ($1,000 USD) + EUR Vault (1,000 EUR * 1.10 = $1,100 USD) = $2,100.00 USD
        val initialCheckingUsd = checking.initialBalance
        val initialEurUsd = CurrencyConverter.convert(eurSavings.initialBalance, "EUR", "USD", ratesv1)
        val initialNetWorth = initialCheckingUsd + initialEurUsd
        assertEquals(2100.0, initialNetWorth, 0.001)

        // Exchange Rate Updates to 1.15 USD/EUR
        val ratesv2 = listOf(ExchangeRateEntity(id = "r1", userId = "u1", fromCurrency = "EUR", toCurrency = "USD", rate = 1.15))
        val updatedEurUsd = CurrencyConverter.convert(eurSavings.initialBalance, "EUR", "USD", ratesv2)
        val updatedNetWorth = initialCheckingUsd + updatedEurUsd

        // Updated Net Worth: $1,000 USD + $1,150 USD = $2,150.00 USD
        assertEquals(2150.0, updatedNetWorth, 0.001)
    }
}
