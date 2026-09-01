package com.selfbudget.app

import com.selfbudget.app.core.util.IncomeCalculator
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class IncomeCalculatorTest {

    @Test
    fun testPerSourceEffectiveIncome_SalaryPlusFreelance() {
        // Given: Recurring primary salary $4,000/mo in cat_salary
        val recurringSalary = RecurringTransactionEntity(
            id = "rec1",
            userId = "u1",
            title = "Primary Salary",
            amount = 4000.0,
            type = TransactionType.INCOME,
            categoryId = "cat_salary",
            frequency = RecurringFrequency.MONTHLY
        )

        // Logged partial salary paycheck $2,000 in cat_salary + $500 freelance income in cat_freelance (ad-hoc)
        val loggedSalary = TransactionEntity(
            userId = "u1",
            title = "Paycheck #1",
            amount = 2000.0,
            type = TransactionType.INCOME,
            categoryId = "cat_salary"
        )
        val loggedFreelance = TransactionEntity(
            userId = "u1",
            title = "Web Design Gigs",
            amount = 500.0,
            type = TransactionType.INCOME,
            categoryId = "cat_freelance"
        )

        val realizedIncome = IncomeCalculator.computeEffectiveMonthlyIncome(
            loggedIncomeTransactions = listOf(loggedSalary, loggedFreelance),
            recurringIncomeList = listOf(recurringSalary)
        )

        val expectedIncome = IncomeCalculator.computeExpectedMonthlyIncome(
            recurringIncomeList = listOf(recurringSalary)
        )

        // Realized income is strictly posted transactions: 2000 + 500 = 2500. Expected recurring = 4000.
        assertEquals(2500.0, realizedIncome, 0.001)
        assertEquals(4000.0, expectedIncome, 0.001)
    }
}
