package com.selfbudget.app

import com.selfbudget.app.core.util.ExcelExporter
import com.selfbudget.app.core.util.ExportDataType
import com.selfbudget.app.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ExcelExporterTest {

    @Test
    fun testToColumnName() {
        assertEquals("A", ExcelExporter.toColumnName(0))
        assertEquals("B", ExcelExporter.toColumnName(1))
        assertEquals("Z", ExcelExporter.toColumnName(25))
        assertEquals("AA", ExcelExporter.toColumnName(26))
        assertEquals("AB", ExcelExporter.toColumnName(27))
    }

    @Test
    fun testGenerateWorkbookBytes() {
        val categories = listOf(
            CategoryEntity("cat_food", "Food & Dining", "Restaurant", "#4CAF50", TransactionType.EXPENSE)
        )
        val accounts = listOf(
            AccountEntity(id = "acc_main", userId = "u1", name = "Checking Account")
        )
        val transactions = listOf(
            TransactionEntity(
                id = "tx1",
                userId = "u1",
                title = "Trader Joe's",
                amount = 45.20,
                type = TransactionType.EXPENSE,
                categoryId = "cat_food",
                accountId = "acc_main"
            )
        )
        val recurring = listOf(
            RecurringTransactionEntity(
                id = "rec1",
                userId = "u1",
                title = "Gym Membership",
                amount = 29.99,
                type = TransactionType.EXPENSE,
                categoryId = "cat_food",
                accountId = "acc_main",
                frequency = RecurringFrequency.MONTHLY
            )
        )
        val budgets = listOf(
            BudgetEntity(
                id = "b1",
                userId = "u1",
                categoryId = "cat_food",
                amountLimit = 500.0,
                monthYear = "2026-08"
            )
        )
        val goals = listOf(
            GoalEntity(
                id = "g1",
                userId = "u1",
                name = "Vacation",
                targetAmount = 2000.0,
                savedAmount = 800.0
            )
        )

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

        assertEquals(5, sheets.size)
        assertEquals("Transactions", sheets[0].name)
        assertEquals("Recurring Transactions", sheets[1].name)
        assertEquals("Budget Plan", sheets[2].name)
        assertEquals("Savings Goals", sheets[3].name)
        assertEquals("Accounts & Wallets", sheets[4].name)

        val bytes = ExcelExporter.generateWorkbookBytes(sheets)
        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        // Verify ZIP entries inside the .xlsx file
        val entryNames = mutableListOf<String>()
        val zipIn = ZipInputStream(ByteArrayInputStream(bytes))
        var entry = zipIn.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            entry = zipIn.nextEntry
        }

        assertTrue(entryNames.contains("[Content_Types].xml"))
        assertTrue(entryNames.contains("_rels/.rels"))
        assertTrue(entryNames.contains("xl/workbook.xml"))
        assertTrue(entryNames.contains("xl/_rels/workbook.xml.rels"))
        assertTrue(entryNames.contains("xl/styles.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet1.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet2.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet3.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet4.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet5.xml"))
    }
}
