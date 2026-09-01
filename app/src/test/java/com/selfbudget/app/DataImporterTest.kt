package com.selfbudget.app

import com.selfbudget.app.core.util.*
import com.selfbudget.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

class DataImporterTest {

    private val testUserId = "user_test_123"

    @Test
    fun testParseTransactionsCsv() {
        val csv = """
            Transaction ID,Date & Time,Title,Type,Amount ($),Category,Account / Wallet,Transfer Destination,Payment Method,Note
            "tx-101","2026-08-30 15:30:00","Whole Foods","EXPENSE","78.50","Groceries","Main Checking","","Debit Card","Weekly grocery run"
            "tx-102","2026-08-31 09:00:00","Salary Direct Deposit","INCOME","3200.00","Salary","Main Checking","","Direct Deposit","Paycheck"
        """.trimIndent()

        val result = DataImporter.parseCsvBytes(csv.toByteArray(Charsets.UTF_8), "transactions.csv", testUserId)
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()

        assertEquals("CSV (Transactions)", data.format)
        assertEquals(2, data.transactions.size)
        assertEquals("Whole Foods", data.transactions[0].title)
        assertEquals(78.50, data.transactions[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, data.transactions[0].type)
        assertEquals("Weekly grocery run", data.transactions[0].note)

        assertEquals("Salary Direct Deposit", data.transactions[1].title)
        assertEquals(3200.00, data.transactions[1].amount, 0.001)
        assertEquals(TransactionType.INCOME, data.transactions[1].type)
    }

    @Test
    fun testParseRecurringCsv() {
        val csv = """
            Recurring ID,Title,Type,Amount ($),Category,Account / Wallet,Frequency,Next Due Date,Remaining Occurrences,Status,Payment Method,Note
            "rec-1","Electric Bill","EXPENSE","120.00","Utilities","Main Checking","MONTHLY","2026-09-15","Indefinite","Active","Auto-Pay","Power"
        """.trimIndent()

        val result = DataImporter.parseCsvBytes(csv.toByteArray(Charsets.UTF_8), "recurring.csv", testUserId)
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()

        assertEquals("CSV (Recurring Bills)", data.format)
        assertEquals(1, data.recurring.size)
        assertEquals("Electric Bill", data.recurring[0].title)
        assertEquals(120.00, data.recurring[0].amount, 0.001)
        assertEquals(RecurringFrequency.MONTHLY, data.recurring[0].frequency)
    }

    @Test
    fun testParseBudgetCsv() {
        val csv = """
            Budget ID,Category,Month / Period,Budget Limit ($),Rollover Enabled,Auto Synced
            "b-1","Dining","2026-08","450.00","Yes","No"
        """.trimIndent()

        val result = DataImporter.parseCsvBytes(csv.toByteArray(Charsets.UTF_8), "budgets.csv", testUserId)
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()

        assertEquals("CSV (Budget Plan)", data.format)
        assertEquals(1, data.budgets.size)
        assertEquals("Dining", data.budgets[0].categoryId)
        assertEquals(450.00, data.budgets[0].amountLimit, 0.001)
        assertTrue(data.budgets[0].rolloverEnabled)
        assertFalse(data.budgets[0].isAutoSynced)
    }

    @Test
    fun testParseGoalsCsv() {
        val csv = """
            Goal ID,Goal Name,Target Amount ($),Current Saved Amount ($),Linked Account / Wallet,Target Date,Created Date
            "g-1","Emergency Fund","10000.00","4500.00","Main Savings","2026-12-31","2026-01-01"
        """.trimIndent()

        val result = DataImporter.parseCsvBytes(csv.toByteArray(Charsets.UTF_8), "goals.csv", testUserId)
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()

        assertEquals("CSV (Savings Goals)", data.format)
        assertEquals(1, data.goals.size)
        assertEquals("Emergency Fund", data.goals[0].name)
        assertEquals(10000.00, data.goals[0].targetAmount, 0.001)
        assertEquals(4500.00, data.goals[0].savedAmount, 0.001)
    }

    @Test
    fun testParseAccountsCsv() {
        val csv = """
            Account ID,Account Name,Account Type,Live Balance ($),Initial Balance ($),Currency,Credit Limit ($),Interest Rate APR (%),Minimum Payment ($),Is Default
            "acc-1","Main Checking","CHECKING","2450.00","2450.00","USD","N/A","N/A","N/A","Yes"
            "acc-2","401(k) Retirement","RETIREMENT","45000.00","45000.00","USD","N/A","N/A","N/A","No"
        """.trimIndent()

        val result = DataImporter.parseCsvBytes(csv.toByteArray(Charsets.UTF_8), "accounts.csv", testUserId)
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()

        assertEquals("CSV (Accounts & Wallets)", data.format)
        assertEquals(2, data.accounts.size)
        assertEquals("Main Checking", data.accounts[0].name)
        assertEquals(AccountType.CHECKING, data.accounts[0].type)
        assertTrue(data.accounts[0].isDefault)

        assertEquals("401(k) Retirement", data.accounts[1].name)
        assertEquals(AccountType.RETIREMENT, data.accounts[1].type)
    }

    @Test
    fun testRoundtripExcelExportAndImport() {
        val categories = listOf(CategoryEntity("cat_groceries", "Groceries", "ShoppingBag", "#4CAF50", TransactionType.EXPENSE))
        val accounts = listOf(AccountEntity("acc_1", testUserId, "Checking Account", AccountType.CHECKING, initialBalance = 1500.0))
        val transactions = listOf(TransactionEntity("tx_1", testUserId, "Supermarket", 65.40, TransactionType.EXPENSE, "cat_groceries", "acc_1"))
        val recurring = listOf(RecurringTransactionEntity("rec_1", testUserId, "Gym", 35.0, TransactionType.EXPENSE, "cat_groceries", "acc_1", RecurringFrequency.MONTHLY))
        val budgets = listOf(BudgetEntity("b_1", testUserId, "cat_groceries", 400.0, "2026-08"))
        val goals = listOf(GoalEntity("g_1", testUserId, "New Laptop", 1200.0, savedAmount = 600.0))

        val sheets = ExcelExporter.buildSheets(
            selectedTypes = setOf(
                ExportDataType.TRANSACTIONS,
                ExportDataType.RECURRING,
                ExportDataType.BUDGET,
                ExportDataType.GOALS,
                ExportDataType.ACCOUNTS
            ),
            transactions = transactions,
            categories = categories,
            accounts = accounts,
            recurring = recurring,
            budgets = budgets,
            goals = goals
        )

        val xlsxBytes = ExcelExporter.generateWorkbookBytes(sheets)
        assertNotNull(xlsxBytes)
        assertTrue(xlsxBytes.isNotEmpty())

        val importResult = DataImporter.parseXlsxBytes(xlsxBytes, "ExportBundle.xlsx", testUserId)
        assertTrue(importResult.isSuccess)
        val data = importResult.getOrThrow()

        assertEquals(1, data.transactions.size)
        assertEquals("Supermarket", data.transactions[0].title)
        assertEquals(65.40, data.transactions[0].amount, 0.001)

        assertEquals(1, data.recurring.size)
        assertEquals("Gym", data.recurring[0].title)

        assertEquals(1, data.budgets.size)
        assertEquals(400.0, data.budgets[0].amountLimit, 0.001)

        assertEquals(1, data.goals.size)
        assertEquals("New Laptop", data.goals[0].name)

        assertEquals(1, data.accounts.size)
        assertEquals("Checking Account", data.accounts[0].name)
    }

    @Test
    fun testParseJsonBackup() {
        val payload = SyncDataPayload(
            schemaVersion = 7,
            exportTimestamp = System.currentTimeMillis(),
            appVersion = "1.0",
            transactions = listOf(TransactionEntity("tx_json", testUserId, "Coffee", 4.50, TransactionType.EXPENSE, "cat_food", "acc_main")),
            categories = emptyList(),
            accounts = listOf(AccountEntity("acc_main", testUserId, "Primary Checking")),
            budgets = emptyList(),
            recurringTransactions = emptyList(),
            goals = emptyList()
        )

        val jsonString = com.google.gson.Gson().toJson(payload)
        val result = DataImporter.parseJsonBytes(jsonString.toByteArray(Charsets.UTF_8), "backup.json", testUserId)
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()

        assertEquals(1, data.transactions.size)
        assertEquals("Coffee", data.transactions[0].title)
        assertEquals(1, data.accounts.size)
    }
}
