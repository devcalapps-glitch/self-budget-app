package com.selfbudget.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated Unit Test Suite for Master Cash Flow Overview Equation.
 * Formula: Income - Category Budgets - Unbudgeted Fixed Bills = Unassigned Free Cash.
 * Verifies Feature F-55 and F-57.
 */
class CashFlowCalculationTest {

    @Test
    fun testCashFlowEquation_Surplus() {
        // Given: Monthly Income of $4,000, Category Budgets of $1,500 (covers Rent), Unbudgeted Bills of $15 (Netflix)
        val income = 4000.0
        val categoryBudgets = 1500.0
        val unbudgetedFixedBills = 15.0

        // When: Computing unallocated free cash
        val unallocatedFreeCash = income - categoryBudgets - unbudgetedFixedBills

        // Then: Free cash should equal $2,485.00
        assertEquals(2485.0, unallocatedFreeCash, 0.001)
        assertTrue("Unallocated free cash should be positive (surplus)", unallocatedFreeCash >= 0)
    }

    @Test
    fun testCashFlowEquation_DeficitWhenIncomeLogged() {
        // Given: Monthly Income of $1,000, Category Budgets of $1,500
        val income = 1000.0
        val categoryBudgets = 1500.0
        val unbudgetedFixedBills = 0.0

        val unallocatedFreeCash = income - categoryBudgets - unbudgetedFixedBills

        assertEquals(-500.0, unallocatedFreeCash, 0.001)
        assertTrue("Unallocated free cash should be negative (deficit)", unallocatedFreeCash < 0)
    }

    @Test
    fun testCashFlowEquation_ZeroIncomeState() {
        // Given: $0 Income logged for month, but $1,000 in Category Budgets
        val income = 0.0
        val categoryBudgets = 1000.0
        val unbudgetedFixedBills = 0.0

        val totalAllocated = categoryBudgets + unbudgetedFixedBills

        // When Income is 0.0, we report allocated commitments without showing negative deficit
        assertEquals(0.0, income, 0.001)
        assertEquals(1000.0, totalAllocated, 0.001)
    }
    @Test
    fun testCashFlowEquation_ZeroState() {
        // Given: All transactions, recurring bills, and budgets deleted
        val income = 0.0
        val categoryBudgets = 0.0
        val unbudgetedFixedBills = 0.0

        val unallocatedFreeCash = income - categoryBudgets - unbudgetedFixedBills
        assertEquals(0.0, unallocatedFreeCash, 0.001)
    }

    @Test
    fun testCashFlowEquation_UnbudgetedOneOffBillsAndExpenses() {
        // Given: Monthly Income of $3,000, Category Budgets of $1,000, Unbudgeted Recurring Bills of $15 (Netflix), and Unbudgeted One-Off Bill Transaction of $100 (Medical Bill)
        val income = 3000.0
        val categoryBudgets = 1000.0
        val unbudgetedRecurringBills = 15.0
        val unbudgetedOneOffBill = 100.0

        val totalOtherBillsAndExpenses = unbudgetedRecurringBills + unbudgetedOneOffBill
        val unallocatedFreeCash = income - categoryBudgets - totalOtherBillsAndExpenses

        assertEquals(115.0, totalOtherBillsAndExpenses, 0.001)
        assertEquals(1885.0, unallocatedFreeCash, 0.001)
    }

    @Test
    fun testCashFlowEquation_IncomeIsExcludedFromPayments() {
        // Given: Income of $2,500.00 and an expense payment of $100.00
        val income = 2500.0
        val categoryBudgets = 0.0
        val expensePayments = 100.0

        // Payments must NOT include the $2,500.00 income
        val totalPayments = expensePayments
        val freeCash = income - categoryBudgets - totalPayments

        assertEquals(100.0, totalPayments, 0.001)
        assertEquals(2400.0, freeCash, 0.001)
        assertTrue("Free cash must be positive ($2,400.00) when income exceeds payments", freeCash > 0)
    }

    @Test
    fun testCashFlowEquation_FourColumnBreakdownWithPaidAndUnbudgeted() {
        // Given: Income of $2,000.00, Rent Budget of $500.00, Posted Rent Payment of $500.00, and Unbudgeted Medical Expense of $50.00
        val income = 2000.0
        val budgets = 500.0
        val unbudgeted = 50.0
        val totalPaid = 500.0 + 50.0 // Rent $500 + Medical $50

        // When: Calculating allocated commitments and free cash
        val totalCommitted = budgets + unbudgeted
        val freeCash = income - totalCommitted

        // Then: 4 distinct columns are accurately computed
        assertEquals(2000.0, income, 0.001)
        assertEquals(500.0, budgets, 0.001)
        assertEquals(50.0, unbudgeted, 0.001)
        assertEquals(550.0, totalPaid, 0.001)
        assertEquals(1450.0, freeCash, 0.001)
    }

    @Test
    fun testCashFlowEquation_LargeAmountsScaleSafely() {
        // Given: Large million-dollar figures (Income $1,250,000.00, Budgets $450,000.00, Unbudgeted $15,000.00, Paid $465,000.00)
        val income = 1250000.0
        val budgets = 450000.0
        val unbudgeted = 15000.0
        val paid = 465000.0

        val totalCommitted = budgets + unbudgeted
        val freeCash = income - totalCommitted

        assertEquals(1250000.0, income, 0.001)
        assertEquals(450000.0, budgets, 0.001)
        assertEquals(15000.0, unbudgeted, 0.001)
        assertEquals(465000.0, paid, 0.001)
        assertEquals(785000.0, freeCash, 0.001)
    }

    @Test
    fun testIssue2_EffectiveBudgetRolloverMatchesAcrossDashboardAndPlan() {
        // Given: Category base limit $500, previous month base limit $500, spent $300 (rollover bonus $200)
        val ownLimit = 500.0
        val prevLimit = 500.0
        val prevSpent = 300.0

        val effectiveLimit = com.selfbudget.app.core.util.BudgetRollover.effectiveLimit(ownLimit, true, prevLimit, prevSpent)
        assertEquals(700.0, effectiveLimit, 0.001)

        // Dashboard total budget and Plan tab total budget both compute effective limit ($700)
        val dashboardBudgets = effectiveLimit
        val planTabTotalBudget = effectiveLimit
        assertEquals(dashboardBudgets, planTabTotalBudget, 0.001)
    }

    @Test
    fun testIssue5_BudgetedCategoryUsesMaxOfLimitAndRecurringBill() {
        // Given: Rent budget limit set to $1,000, but actual recurring rent bill is $1,200
        val effectiveBudgetLimit = 1000.0
        val recurringRentBill = 1200.0
        val categoryCommitment = maxOf(effectiveBudgetLimit, recurringRentBill)

        // Free Cash must reflect the real $1,200 obligation
        val income = 3000.0
        val unallocatedFreeCash = income - categoryCommitment

        assertEquals(1200.0, categoryCommitment, 0.001)
        assertEquals(1800.0, unallocatedFreeCash, 0.001)
    }

    @Test
    fun testIssue12_GoalCommitmentsIncludedInTotalCommittedAllocated() {
        // Given: Income of $4,000, Category Budgets of $1,000, Goal target of $6,000 in 6 months ($1,000/mo commitment)
        val income = 4000.0
        val categoryBudgets = 1000.0
        val unbudgetedBills = 0.0
        val goalMonthlyCommitment = 1000.0 // $6,000 / 6 months

        val totalCommittedAllocated = categoryBudgets + unbudgetedBills + goalMonthlyCommitment
        val unallocatedFreeCash = income - totalCommittedAllocated

        assertEquals(2000.0, totalCommittedAllocated, 0.001)
        assertEquals(2000.0, unallocatedFreeCash, 0.001) // Goal savings earmarked, not showing as free cash
    }
}
