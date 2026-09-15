package com.selfbudget.app

import com.selfbudget.app.core.util.GoalCalculator
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalCalculatorTest {

    private val savingsAccount = AccountEntity(
        id = "acc_savings",
        userId = "u1",
        name = "High Yield Savings",
        type = AccountType.SAVINGS,
        initialBalance = 1000.0
    )

    @Test
    fun testGoalWithNoLinkedAccountUsesSavedAmount() {
        val goal = GoalEntity(
            id = "g1",
            userId = "u1",
            name = "Emergency Fund",
            targetAmount = 5000.0,
            savedAmount = 1500.0,
            linkedAccountId = null
        )

        val summary = GoalCalculator.computeGoalProgress(
            goal = goal,
            accounts = emptyList(),
            accountBalances = emptyMap()
        )

        assertEquals(1500.0, summary.totalSaved, 0.001)
        assertEquals(3500.0, summary.remainingAmount, 0.001)
        assertEquals(0.3f, summary.progressFraction, 0.001f)
        assertEquals(30, summary.progressPercentage)
        assertFalse(summary.isCompleted)
    }

    @Test
    fun testGoalWithLinkedAccountCombinesAccountBalanceAndSavedAmount() {
        val goal = GoalEntity(
            id = "g2",
            userId = "u1",
            name = "Vacation Savings",
            targetAmount = 2000.0,
            savedAmount = 200.0,
            linkedAccountId = "acc_savings"
        )

        val accounts = listOf(savingsAccount)
        val balances = mapOf("acc_savings" to 1200.0)

        val summary = GoalCalculator.computeGoalProgress(
            goal = goal,
            accounts = accounts,
            accountBalances = balances
        )

        // Total saved = 1200 (linked account balance) + 200 (direct envelope additions) = 1400
        assertEquals(1400.0, summary.totalSaved, 0.001)
        assertEquals(600.0, summary.remainingAmount, 0.001)
        assertEquals(0.7f, summary.progressFraction, 0.001f)
        assertEquals(70, summary.progressPercentage)
        assertFalse(summary.isCompleted)
    }

    @Test
    fun testGoalCompletedWhenTotalSavedMeetsOrExceedsTarget() {
        val goal = GoalEntity(
            id = "g3",
            userId = "u1",
            name = "New Laptop",
            targetAmount = 1500.0,
            savedAmount = 1500.0,
            linkedAccountId = null
        )

        val summary = GoalCalculator.computeGoalProgress(
            goal = goal,
            accounts = emptyList(),
            accountBalances = emptyMap()
        )

        assertEquals(1500.0, summary.totalSaved, 0.001)
        assertEquals(0.0, summary.remainingAmount, 0.001)
        assertEquals(1.0f, summary.progressFraction, 0.001f)
        assertEquals(100, summary.progressPercentage)
        assertTrue(summary.isCompleted)
    }

    @Test
    fun testGoalExceedingTargetClampsFractionToOneAndRemainingToZero() {
        val goal = GoalEntity(
            id = "g4",
            userId = "u1",
            name = "Car Down Payment",
            targetAmount = 3000.0,
            savedAmount = 3500.0,
            linkedAccountId = null
        )

        val summary = GoalCalculator.computeGoalProgress(
            goal = goal,
            accounts = emptyList(),
            accountBalances = emptyMap()
        )

        assertEquals(3500.0, summary.totalSaved, 0.001)
        assertEquals(0.0, summary.remainingAmount, 0.001)
        assertEquals(1.0f, summary.progressFraction, 0.001f)
        assertEquals(100, summary.progressPercentage)
        assertTrue(summary.isCompleted)
    }
}
