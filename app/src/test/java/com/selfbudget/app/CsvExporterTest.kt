package com.selfbudget.app

import com.selfbudget.app.core.util.CsvExporter
import com.selfbudget.app.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun testGenerateTransactionsCsv() {
        val categories = listOf(
            CategoryEntity("cat_groceries", "Groceries", "ShoppingBag", "#4CAF50", TransactionType.EXPENSE)
        )
        val accounts = listOf(
            AccountEntity(id = "acc_checking", userId = "user1", name = "Checking Account")
        )
        val transactions = listOf(
            TransactionEntity(
                id = "tx1",
                userId = "user1",
                title = "Whole Foods, Market",
                amount = 85.50,
                type = TransactionType.EXPENSE,
                categoryId = "cat_groceries",
                accountId = "acc_checking",
                timestamp = 1756598400000L,
                note = "Organic \"apples\" & milk",
                paymentMethod = "Debit Card"
            )
        )

        val csv = CsvExporter.generateTransactionsCsv(transactions, categories, accounts)
        assertTrue(csv.contains("Transaction ID,Date,Title,Type,Amount ($),Category,Account / Wallet"))
        assertTrue(csv.contains("\"Whole Foods, Market\""))
        assertTrue(csv.contains("\"Organic \"\"apples\"\" & milk\""))
        assertTrue(csv.contains("85.50"))
        assertTrue(csv.contains("Checking Account"))
    }

    @Test
    fun testGenerateRecurringCsv() {
        val categories = listOf(
            CategoryEntity("cat_subs", "Subscriptions", "Subscriptions", "#9C27B0", TransactionType.EXPENSE)
        )
        val recurring = listOf(
            RecurringTransactionEntity(
                id = "rec1",
                userId = "user1",
                title = "Netflix",
                amount = 15.99,
                type = TransactionType.EXPENSE,
                categoryId = "cat_subs",
                accountId = "acc_checking",
                frequency = RecurringFrequency.MONTHLY,
                remainingOccurrences = 12,
                isArchived = false
            )
        )

        val csv = CsvExporter.generateRecurringCsv(recurring, categories)
        assertTrue(csv.contains("Recurring ID,Title,Type,Amount ($),Category,Account / Wallet,Frequency"))
        assertTrue(csv.contains("Netflix"))
        assertTrue(csv.contains("MONTHLY"))
        assertTrue(csv.contains("15.99"))
        assertTrue(csv.contains("12"))
        assertTrue(csv.contains("Active"))
    }

    @Test
    fun testGenerateBudgetCsv() {
        val categories = listOf(
            CategoryEntity("cat_dining", "Dining Out", "Restaurant", "#FF9800", TransactionType.EXPENSE)
        )
        val budgets = listOf(
            BudgetEntity(
                id = "b1",
                userId = "user1",
                categoryId = "cat_dining",
                amountLimit = 400.0,
                monthYear = "2026-08",
                rolloverEnabled = true,
                isAutoSynced = false
            )
        )

        val csv = CsvExporter.generateBudgetCsv(budgets, categories)
        assertTrue(csv.contains("Budget ID,Category,Month / Period,Budget Limit ($),Rollover Enabled,Auto Synced"))
        assertTrue(csv.contains("Dining Out"))
        assertTrue(csv.contains("2026-08"))
        assertTrue(csv.contains("400.00"))
        assertTrue(csv.contains("Yes"))
        assertTrue(csv.contains("No"))
    }

    @Test
    fun testGenerateGoalsCsv() {
        val accounts = listOf(
            AccountEntity(id = "acc_savings", userId = "user1", name = "High Yield Savings")
        )
        val goals = listOf(
            GoalEntity(
                id = "g1",
                userId = "user1",
                name = "Emergency Fund",
                targetAmount = 10000.0,
                savedAmount = 5500.0,
                linkedAccountId = "acc_savings"
            )
        )

        val csv = CsvExporter.generateGoalsCsv(goals, accounts)
        assertTrue(csv.contains("Goal ID,Goal Name,Target Amount ($),Saved Amount ($),Linked Account / Wallet"))
        assertTrue(csv.contains("Emergency Fund"))
        assertTrue(csv.contains("10000.00"))
        assertTrue(csv.contains("5500.00"))
        assertTrue(csv.contains("High Yield Savings"))
    }

    @Test
    fun testGenerateAccountsCsv() {
        val accounts = listOf(
            AccountEntity(
                id = "acc_retire",
                userId = "user1",
                name = "401(k) Vanguard",
                type = AccountType.RETIREMENT,
                initialBalance = 50000.0,
                currencyCode = "USD"
            )
        )
        val balances = mapOf("acc_retire" to 54200.50)

        val csv = CsvExporter.generateAccountsCsv(accounts, balances)
        assertTrue(csv.contains("Account ID,Account Name,Account Type,Live Balance ($),Initial Balance ($)"))
        assertTrue(csv.contains("401(k) Vanguard"))
        assertTrue(csv.contains("RETIREMENT"))
        assertTrue(csv.contains("54200.50"))
        assertTrue(csv.contains("50000.00"))
    }

    @Test
    fun testEscapeCsvField() {
        assertEquals("normal", CsvExporter.escapeCsvField("normal"))
        assertEquals("\"with, comma\"", CsvExporter.escapeCsvField("with, comma"))
        assertEquals("\"with \"\"quotes\"\"\"", CsvExporter.escapeCsvField("with \"quotes\""))
        assertEquals("\"with\nnewline\"", CsvExporter.escapeCsvField("with\nnewline"))
    }
}
