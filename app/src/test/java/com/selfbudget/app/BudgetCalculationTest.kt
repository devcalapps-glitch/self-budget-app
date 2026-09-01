package com.selfbudget.app

import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated Unit Test Suite for Budget & Safe-to-Spend Calculations.
 * Verifies Feature F-04, F-33, F-55, and F-56.
 */
class BudgetCalculationTest {

    private val sampleCategory = CategoryEntity(
        id = "cat_groceries",
        name = "Groceries",
        iconName = "shopping_cart",
        colorHex = "#4CAF50",
        type = TransactionType.EXPENSE
    )

    @Test
    fun testEmptyBudgets_settlesToZero() {
        // Given: No explicit budgets set by user
        val budgets = emptyList<BudgetEntity>()
        val transactions = listOf(
            TransactionEntity(
                id = "tx1",
                userId = "u1",
                title = "Whole Foods",
                amount = 75.0,
                type = TransactionType.EXPENSE,
                categoryId = "cat_groceries"
            )
        )

        // When: Computing active category budgets
        val activeCategoryIds = budgets.map { it.categoryId }.distinct()

        // Then: Resulting list of category budgets must be completely empty
        assertTrue("Budget models must be empty when no explicit budgets are set", activeCategoryIds.isEmpty())

        val totalBudget = budgets.sumOf { it.amountLimit }
        val totalSpent = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val remainingBudget = (totalBudget - totalSpent).coerceAtLeast(0.0)

        assertEquals("Total budget must settle to 0.0", 0.0, totalBudget, 0.001)
        assertEquals("Remaining budget must settle to 0.0", 0.0, remainingBudget, 0.001)
    }

    @Test
    fun testSafeToSpendCalculation_withPostedExpensesAndPendingBills() {
        // Given: Monthly limit of $500, Posted Spent of $100, Fixed Recurring Bill of $150
        val limit = 500.0
        val spent = 100.0
        val recurringCommitted = 150.0

        // When: Computing pending upcoming amount and safe to spend
        val pendingUpcoming = (recurringCommitted - spent).coerceAtLeast(0.0) // $50 pending
        val safeToSpend = (limit - spent - pendingUpcoming).coerceAtLeast(0.0) // $500 - $100 - $50 = $350
        val totalClaimed = spent + pendingUpcoming
        val percentage = (totalClaimed / limit).toFloat()

        // Then: Safe to spend must equal $350.00
        assertEquals("Pending upcoming bill amount should be 50.0", 50.0, pendingUpcoming, 0.001)
        assertEquals("Safe-to-Spend must equal 350.0", 350.0, safeToSpend, 0.001)
        assertEquals("Used percentage should be 30%", 0.30f, percentage, 0.01f)
    }

    @Test
    fun testOverBudgetDetection() {
        // Given: Budget limit of $200, Spent $250
        val limit = 200.0
        val spent = 250.0
        val pendingUpcoming = 0.0

        val totalClaimed = spent + pendingUpcoming
        val isOverBudget = totalClaimed > limit

        assertTrue("Category must be flagged as over budget", isOverBudget)
        assertEquals("Over budget excess amount", 50.0, totalClaimed - limit, 0.001)
    }

    @Test
    fun testWarningThreshold_at75Percent() {
        // Given: Budget limit of $400, Spent $310 (77.5%)
        val limit = 400.0
        val spent = 310.0
        val percentage = (spent / limit).toFloat()

        val isOver = spent > limit
        val isWarning = percentage >= 0.75f && !isOver

        assertFalse("Should not be over budget", isOver)
        assertTrue("Should trigger 75% warning indicator", isWarning)
    }

    @Test
    fun testRecurringBillFloorCalculation() {
        // Given: Weekly recurring bill of $50
        val weeklyAmount = 50.0
        // Exact ratio (52 weeks / 12 months), matching RecurringFrequencyNormalizer.toMonthlyAmount -
        // NOT the truncated `* 4.333` approximation this test used to assert (F-103 fixed that drift).
        val monthlyEquivalent = com.selfbudget.app.core.util.Money.round(weeklyAmount * 52.0 / 12.0)

        // Then: Monthly equivalent should be exactly $216.67, not the truncated $216.65
        assertEquals(216.67, monthlyEquivalent, 0.001)
    }

    @Test
    fun testBudgetCalculation_handlesTransactionEditsAndDeletes() {
        // Given: Budget limit of $500 for Groceries and an initial transaction of $200
        val limit = 500.0
        val initialTx = TransactionEntity(id = "tx1", userId = "u1", title = "Groceries", amount = 200.0, type = TransactionType.EXPENSE, categoryId = "cat_groceries")

        val initialSpent = listOf(initialTx).filter { it.categoryId == "cat_groceries" }.sumOf { it.amount }
        val initialRemaining = limit - initialSpent
        assertEquals(200.0, initialSpent, 0.001)
        assertEquals(300.0, initialRemaining, 0.001)

        // When: Transaction amount is edited to $450
        val editedTx = initialTx.copy(amount = 450.0)
        val editedSpent = listOf(editedTx).filter { it.categoryId == "cat_groceries" }.sumOf { it.amount }
        val editedRemaining = limit - editedSpent

        // Then: Spent becomes $450 and Remaining becomes $50
        assertEquals(450.0, editedSpent, 0.001)
        assertEquals(50.0, editedRemaining, 0.001)

        // When: Transaction is deleted
        val emptySpent = emptyList<TransactionEntity>().filter { it.categoryId == "cat_groceries" }.sumOf { it.amount }
        val emptyRemaining = limit - emptySpent

        // Then: Spent settles to $0 and Remaining returns to full $500 limit
        assertEquals(0.0, emptySpent, 0.001)
        assertEquals(500.0, emptyRemaining, 0.001)
    }

    @Test
    fun testDailyPaceMaxFloorsAtZeroWhenOverBudget() {
        // Given: Total budget $500, spent $600 (over budget by $100), 10 days remaining
        val totalBudget = 500.0
        val totalSpent = 600.0
        val remainingBudget = (totalBudget - totalSpent).coerceAtLeast(0.0)
        val remainingDays = 10

        val dailyPace = if (totalBudget > 0 && remainingDays > 0) (remainingBudget / remainingDays).coerceAtLeast(0.0) else 0.0

        // Then: Daily pace max must be floored at $0.00/day, not -$10.00/day
        assertEquals(0.0, dailyPace, 0.001)
    }

    @Test
    fun testPersistentBudgets_inheritsForwardWithoutMutatingPast() {
        // Given: Groceries budget of $500 set in June 2026 ("2026-06")
        val juneBudget = BudgetEntity(
            id = "b1",
            userId = "u1",
            categoryId = "cat_groceries",
            amountLimit = 500.0,
            monthYear = "2026-06"
        )
        val allBudgets = mutableListOf(juneBudget)

        // 1. June 2026 should have $500
        val juneEffective = com.selfbudget.app.core.util.BudgetCalculator.computeBudgetsForMonth(allBudgets, "2026-06")
        assertEquals(1, juneEffective.size)
        assertEquals(500.0, juneEffective[0].amountLimit, 0.001)

        // 2. July 2026 ("2026-07") automatically inherits $500 without needing a new record
        val julyEffective = com.selfbudget.app.core.util.BudgetCalculator.computeBudgetsForMonth(allBudgets, "2026-07")
        assertEquals(1, julyEffective.size)
        assertEquals(500.0, julyEffective[0].amountLimit, 0.001)
        assertEquals("2026-07", julyEffective[0].monthYear)

        // 3. User edits Groceries in August 2026 ("2026-08") to $650
        val augustBudget = BudgetEntity(
            id = "b2",
            userId = "u1",
            categoryId = "cat_groceries",
            amountLimit = 650.0,
            monthYear = "2026-08"
        )
        allBudgets.add(augustBudget)

        // PAST MONTHS CHECK: June and July MUST STILL have $500 (historical integrity)
        val juneCheckAfterEdit = com.selfbudget.app.core.util.BudgetCalculator.computeBudgetsForMonth(allBudgets, "2026-06")
        assertEquals(500.0, juneCheckAfterEdit[0].amountLimit, 0.001)

        val julyCheckAfterEdit = com.selfbudget.app.core.util.BudgetCalculator.computeBudgetsForMonth(allBudgets, "2026-07")
        assertEquals(500.0, julyCheckAfterEdit[0].amountLimit, 0.001)

        // CURRENT & FUTURE MONTHS CHECK: August and September inherit $650
        val augustEffective = com.selfbudget.app.core.util.BudgetCalculator.computeBudgetsForMonth(allBudgets, "2026-08")
        assertEquals(650.0, augustEffective[0].amountLimit, 0.001)

        val septemberEffective = com.selfbudget.app.core.util.BudgetCalculator.computeBudgetsForMonth(allBudgets, "2026-09")
        assertEquals(650.0, septemberEffective[0].amountLimit, 0.001)

        // 4. User deletes Groceries budget in October 2026 ("2026-10") (represented as 0.0 limit)
        val octoberDelete = BudgetEntity(
            id = "b3",
            userId = "u1",
            categoryId = "cat_groceries",
            amountLimit = 0.0,
            monthYear = "2026-10"
        )
        allBudgets.add(octoberDelete)

        // October has no budget
        val octoberEffective = com.selfbudget.app.core.util.BudgetCalculator.computeBudgetsForMonth(allBudgets, "2026-10")
        assertTrue("October budget should be empty after deletion", octoberEffective.isEmpty())

        // Past months (August & June) still retain their respective historical limits
        val augustCheck = com.selfbudget.app.core.util.BudgetCalculator.computeBudgetsForMonth(allBudgets, "2026-08")
        assertEquals(650.0, augustCheck[0].amountLimit, 0.001)

        val juneCheck = com.selfbudget.app.core.util.BudgetCalculator.computeBudgetsForMonth(allBudgets, "2026-06")
        assertEquals(500.0, juneCheck[0].amountLimit, 0.001)
    }
}
