package com.selfbudget.app

import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.AnalyticsSummaryCalculator
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.ActivityAction
import com.selfbudget.app.data.model.ActivityEntityType
import com.selfbudget.app.data.model.ActivityLogEntity
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalyticsSummaryCalculatorTest {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun timestampFor(dateStr: String): Long {
        return sdf.parse(dateStr)?.time ?: 0L
    }

    private val grocCat = CategoryEntity(
        id = "cat_groceries",
        name = "Groceries",
        iconName = "ShoppingCart",
        colorHex = "#4CAF50",
        type = TransactionType.EXPENSE
    )
    private val rentCat = CategoryEntity(
        id = "cat_rent",
        name = "Housing",
        iconName = "Home",
        colorHex = "#2196F3",
        type = TransactionType.EXPENSE
    )
    private val salaryCat = CategoryEntity(
        id = "cat_salary",
        name = "Salary",
        iconName = "Payments",
        colorHex = "#10B981",
        type = TransactionType.INCOME
    )
    private val categories = listOf(grocCat, rentCat, salaryCat)

    @Test
    fun testMonthlySummary_underBudgetWithSavingsAndGoalContributions() {
        val month = "2026-09"
        val prevMonth = "2026-08"

        val transactions = listOf(
            TransactionEntity(
                id = "tx1",
                userId = "u1",
                title = "Whole Foods",
                amount = 450.0,
                type = TransactionType.EXPENSE,
                categoryId = grocCat.id,
                timestamp = timestampFor("2026-09-05")
            ),
            TransactionEntity(
                id = "tx2",
                userId = "u1",
                title = "Apartment Rent",
                amount = 1200.0,
                type = TransactionType.EXPENSE,
                categoryId = rentCat.id,
                timestamp = timestampFor("2026-09-01")
            ),
            TransactionEntity(
                id = "tx_prev",
                userId = "u1",
                title = "August Rent",
                amount = 1800.0,
                type = TransactionType.EXPENSE,
                categoryId = rentCat.id,
                timestamp = timestampFor("2026-08-01")
            ),
            TransactionEntity(
                id = "tx_inc",
                userId = "u1",
                title = "Tech Corp Paycheck",
                amount = 3500.0,
                type = TransactionType.INCOME,
                categoryId = salaryCat.id,
                timestamp = timestampFor("2026-09-15")
            )
        )

        val budgets = listOf(
            BudgetEntity(userId = "u1", categoryId = grocCat.id, amountLimit = 500.0, monthYear = month),
            BudgetEntity(userId = "u1", categoryId = rentCat.id, amountLimit = 1200.0, monthYear = month)
        )

        val goal1 = GoalEntity(
            id = "goal1",
            userId = "u1",
            name = "Emergency Fund",
            targetAmount = 5000.0,
            savedAmount = 1500.0
        )
        val goals = listOf(goal1)

        val activityLog = listOf(
            ActivityLogEntity(
                userId = "u1",
                entityType = ActivityEntityType.GOAL,
                action = ActivityAction.CONTRIBUTED,
                entityId = "goal1",
                title = "Emergency Fund",
                amount = 250.0,
                timestamp = timestampFor("2026-09-10")
            )
        )

        val summary = AnalyticsSummaryCalculator.computeSummary(
            isAnnual = false,
            selectedMonthYear = month,
            currencySymbol = "$",
            allTransactions = transactions,
            categories = categories,
            budgets = budgets,
            allBudgets = budgets,
            goals = goals,
            activityLog = activityLog
        )

        assertEquals("Total expense must be 450 + 1200 = 1650.0", 1650.0, summary.totalExpense, 0.001)
        assertEquals("Total budget must be 500 + 1200 = 1700.0", 1700.0, summary.totalBudget, 0.001)
        assertEquals("Total income must be 3500.0", 3500.0, summary.totalIncome, 0.001)
        assertEquals("Net savings must be 3500 - 1650 = 1850.0", 1850.0, summary.netSavings, 0.001)
        assertEquals("Previous month expense must be 1800.0", 1800.0, summary.prevExpense, 0.001)
        assertTrue("Diff expense should be negative (decrease)", summary.diffExpense < 0)

        // Category breakdown
        assertNotNull("Top spending category must be Housing", summary.topSpendingCategory)
        assertEquals("cat_rent", summary.topSpendingCategory?.category?.id)
        assertTrue("No categories over budget", summary.overBudgetCategories.isEmpty())
        assertEquals(2, summary.underBudgetCategories.size)

        // Goals
        assertEquals(1, summary.totalGoalsCount)
        assertEquals(250.0, summary.totalAmountAddedToGoals, 0.001)
        assertEquals(1, summary.goalContributions.size)
        assertEquals("Emergency Fund", summary.goalContributions.first().goalName)
        assertEquals(250.0, summary.goalContributions.first().amountAdded, 0.001)

        // Narrative in words must highlight all required points
        assertTrue("Headline must mention positive savings or staying within budget",
            summary.headlineText.contains("within your budget") || summary.headlineText.contains("positive savings"))
        assertTrue("High level narrative must synthesize all pillars cleanly",
            summary.highLevelNarrative.contains("$1,650.00") &&
            summary.highLevelNarrative.contains("$1,700.00") &&
            summary.highLevelNarrative.contains("decreased") &&
            summary.highLevelNarrative.contains("$1,850.00") &&
            summary.highLevelNarrative.contains("$250.00"))
        assertTrue("Spending narrative must contain spent and budget numbers",
            summary.spendingNarrative.contains("$1,650.00") && summary.spendingNarrative.contains("$1,700.00"))
        assertTrue("Spending narrative must mention top expense Housing",
            summary.spendingNarrative.contains("Housing"))
        assertTrue("Trend narrative must mention spending decreased",
            summary.trendNarrative.contains("decreased"))
        assertTrue("Savings narrative must mention net savings $1,850.00",
            summary.savingsNarrative.contains("$1,850.00"))
        assertTrue("Goals narrative must mention $250.00 contribution to Emergency Fund",
            summary.goalsNarrative.contains("$250.00") && summary.goalsNarrative.contains("Emergency Fund"))
    }

    @Test
    fun testMonthlySummary_overBudgetWithDeficit() {
        val month = "2026-10"

        val transactions = listOf(
            TransactionEntity(
                id = "tx1",
                userId = "u1",
                title = "Whole Foods",
                amount = 750.0,
                type = TransactionType.EXPENSE,
                categoryId = grocCat.id,
                timestamp = timestampFor("2026-10-05")
            ),
            TransactionEntity(
                id = "tx_inc",
                userId = "u1",
                title = "Part-time job",
                amount = 500.0,
                type = TransactionType.INCOME,
                categoryId = salaryCat.id,
                timestamp = timestampFor("2026-10-15")
            )
        )

        val budgets = listOf(
            BudgetEntity(userId = "u1", categoryId = grocCat.id, amountLimit = 500.0, monthYear = month)
        )

        val summary = AnalyticsSummaryCalculator.computeSummary(
            isAnnual = false,
            selectedMonthYear = month,
            currencySymbol = "$",
            allTransactions = transactions,
            categories = categories,
            budgets = budgets,
            allBudgets = budgets
        )

        assertEquals(750.0, summary.totalExpense, 0.001)
        assertEquals(500.0, summary.totalBudget, 0.001)
        assertEquals(500.0, summary.totalIncome, 0.001)
        assertEquals(-250.0, summary.netSavings, 0.001)
        assertEquals(1, summary.overBudgetCategories.size)
        assertEquals("cat_groceries", summary.overBudgetCategories.first().category.id)

        // Narrative checks
        assertTrue("Headline must mention budget exceeded",
            summary.headlineText.contains("exceeded your planned budget"))
        assertTrue("High level narrative must mention exceeding budget and net deficit",
            summary.highLevelNarrative.contains("exceeding") &&
            summary.highLevelNarrative.contains("deficit"))
        assertTrue("Spending narrative must mention $250.00 over",
            summary.spendingNarrative.contains("over your planned limit") || summary.spendingNarrative.contains("+$250.00"))
        assertTrue("Savings narrative must mention deficit",
            summary.savingsNarrative.contains("deficit"))
    }

    @Test
    fun testAnnualSummary_acrossMultipleMonths() {
        val year = "2026"
        val transactions = listOf(
            TransactionEntity(
                id = "tx1",
                userId = "u1",
                title = "Jan Rent",
                amount = 1000.0,
                type = TransactionType.EXPENSE,
                categoryId = rentCat.id,
                timestamp = timestampFor("2026-01-05")
            ),
            TransactionEntity(
                id = "tx2",
                userId = "u1",
                title = "Feb Rent",
                amount = 1000.0,
                type = TransactionType.EXPENSE,
                categoryId = rentCat.id,
                timestamp = timestampFor("2026-02-05")
            ),
            TransactionEntity(
                id = "tx3",
                userId = "u1",
                title = "Jan Salary",
                amount = 3000.0,
                type = TransactionType.INCOME,
                categoryId = salaryCat.id,
                timestamp = timestampFor("2026-01-15")
            ),
            TransactionEntity(
                id = "tx4",
                userId = "u1",
                title = "Feb Salary",
                amount = 3000.0,
                type = TransactionType.INCOME,
                categoryId = salaryCat.id,
                timestamp = timestampFor("2026-02-15")
            )
        )

        val allBudgets = listOf(
            BudgetEntity(userId = "u1", categoryId = rentCat.id, amountLimit = 1000.0, monthYear = "2026-01")
        )

        val summary = AnalyticsSummaryCalculator.computeSummary(
            isAnnual = true,
            selectedMonthYear = "2026-09",
            currencySymbol = "$",
            allTransactions = transactions,
            categories = categories,
            allBudgets = allBudgets
        )

        assertTrue(summary.isAnnual)
        assertEquals(2000.0, summary.totalExpense, 0.001)
        assertEquals(6000.0, summary.totalIncome, 0.001)
        assertEquals(4000.0, summary.netSavings, 0.001)
        assertEquals(2, summary.monthsLoggedCount)
        assertEquals(1000.0, summary.monthlyAverageExpense, 0.001)
        assertEquals(3000.0, summary.monthlyAverageIncome, 0.001)

        assertEquals("Annual total budget for 12 months at 1000/mo must be 12000.0", 12000.0, summary.totalBudget, 0.001)

        assertTrue("Annual headline must mention net savings or budget surplus",
            summary.headlineText.contains("net savings") || summary.headlineText.contains("budget surplus"))
        assertTrue("Annual high level narrative must mention annual spending and average pace",
            summary.highLevelNarrative.contains("$2,000.00") &&
            summary.highLevelNarrative.contains("$1,000.00/month"))
        assertTrue("Annual trend narrative must mention monthly average $1,000.00",
            summary.trendNarrative.contains("$1,000.00"))
    }

    @Test
    fun testMonthlySummary_withRolloverCarryoverSurplus() {
        val month = "2026-09"
        val budgets = listOf(
            BudgetEntity(
                userId = "u1",
                categoryId = grocCat.id,
                amountLimit = 500.0,
                rolloverEnabled = true,
                monthYear = month
            )
        )
        val prevBudgets = listOf(
            BudgetEntity(
                userId = "u1",
                categoryId = grocCat.id,
                amountLimit = 500.0,
                rolloverEnabled = true,
                monthYear = "2026-08"
            )
        )
        // Last month spent $350 against $500 limit -> $150 surplus carryover
        val prevSpent = mapOf(grocCat.id to 350.0)

        val summary = AnalyticsSummaryCalculator.computeSummary(
            isAnnual = false,
            selectedMonthYear = month,
            currencySymbol = "$",
            allTransactions = emptyList(),
            categories = categories,
            budgets = budgets,
            allBudgets = budgets,
            previousMonthBudgets = prevBudgets,
            previousMonthSpentByCategory = prevSpent
        )

        // Effective limit = 500 + (500 - 350) = 650.0
        assertEquals("Total budget must reflect $150 rollover carryover surplus", 650.0, summary.totalBudget, 0.001)
        val catItem = summary.categorySummaries.firstOrNull { it.category.id == grocCat.id }
        assertNotNull(catItem)
        assertEquals(650.0, catItem?.budgeted ?: 0.0, 0.001)
    }

    @Test
    fun testMonthlySummary_withRolloverCarryoverDeficit() {
        val month = "2026-09"
        val budgets = listOf(
            BudgetEntity(
                userId = "u1",
                categoryId = grocCat.id,
                amountLimit = 500.0,
                rolloverEnabled = true,
                monthYear = month
            )
        )
        val prevBudgets = listOf(
            BudgetEntity(
                userId = "u1",
                categoryId = grocCat.id,
                amountLimit = 500.0,
                rolloverEnabled = true,
                monthYear = "2026-08"
            )
        )
        // Last month spent $650 against $500 limit -> $150 deficit carryover
        val prevSpent = mapOf(grocCat.id to 650.0)

        val summary = AnalyticsSummaryCalculator.computeSummary(
            isAnnual = false,
            selectedMonthYear = month,
            currencySymbol = "$",
            allTransactions = emptyList(),
            categories = categories,
            budgets = budgets,
            allBudgets = budgets,
            previousMonthBudgets = prevBudgets,
            previousMonthSpentByCategory = prevSpent
        )

        // Effective limit = 500 - 150 = 350.0
        assertEquals("Total budget must reflect $150 rollover deficit", 350.0, summary.totalBudget, 0.001)
        val catItem = summary.categorySummaries.firstOrNull { it.category.id == grocCat.id }
        assertNotNull(catItem)
        assertEquals(350.0, catItem?.budgeted ?: 0.0, 0.001)
    }

    @Test
    fun testMonthlySummary_fallbackWhenBudgetConfiguredInLaterMonth() {
        // Budget was created in 2026-10, user views 2026-09
        val allBudgets = listOf(
            BudgetEntity(
                userId = "u1",
                categoryId = rentCat.id,
                amountLimit = 1500.0,
                monthYear = "2026-10"
            )
        )

        val summary = AnalyticsSummaryCalculator.computeSummary(
            isAnnual = false,
            selectedMonthYear = "2026-09",
            currencySymbol = "$",
            allTransactions = emptyList(),
            categories = categories,
            budgets = emptyList(),
            allBudgets = allBudgets
        )

        assertEquals("Should fall back to baseline budget from allBudgets", 1500.0, summary.totalBudget, 0.001)
    }

    @Test
    fun testEmptyState_gracefulFallback() {
        val summary = AnalyticsSummaryCalculator.computeSummary(
            isAnnual = false,
            selectedMonthYear = "2026-09",
            currencySymbol = "$",
            allTransactions = emptyList(),
            categories = emptyList()
        )

        assertEquals(0.0, summary.totalExpense, 0.001)
        assertEquals(0.0, summary.totalIncome, 0.001)
        assertEquals(0.0, summary.totalBudget, 0.001)
        assertEquals(0.0, summary.netSavings, 0.001)
        assertTrue(summary.headlineText.contains("No spending"))
        assertTrue(summary.spendingNarrative.contains("No spending activity"))
        assertTrue(summary.goalsNarrative.contains("No active savings goals found"))
    }
}
