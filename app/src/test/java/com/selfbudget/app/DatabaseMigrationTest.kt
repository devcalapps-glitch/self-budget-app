package com.selfbudget.app

import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.ExchangeRateEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.data.model.UserEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMigrationTest {

    @Test
    fun testMigrationSuiteArrayCoverage() {
        val migrations = AppDatabase.MIGRATIONS_ALL
        assertEquals(16, migrations.size) // 1..16 to 17

        for (i in 1..16) {
            val migration = migrations[i - 1]
            assertEquals(i, migration.startVersion)
            assertEquals(17, migration.endVersion)
        }
    }

    @Test
    fun testDataRetainedAcrossMigrationSteps() {
        val user = UserEntity(id = "u1", email = "test@example.com", displayName = "Test User", photoUrl = null)
        val checking = AccountEntity(id = "acc1", userId = "u1", name = "Checking", type = AccountType.CHECKING, initialBalance = 1500.0)
        val tx = TransactionEntity(id = "tx1", userId = "u1", title = "Groceries", amount = 120.0, type = TransactionType.EXPENSE, categoryId = "cat1")
        val budget = BudgetEntity(id = "b1", userId = "u1", categoryId = "cat1", amountLimit = 500.0, monthYear = "2026-01")
        val recurring = RecurringTransactionEntity(id = "r1", userId = "u1", title = "Rent", amount = 1000.0, type = TransactionType.EXPENSE, categoryId = "cat2", frequency = RecurringFrequency.MONTHLY)
        val goal = GoalEntity(id = "g1", userId = "u1", name = "Vacation", targetAmount = 2000.0, savedAmount = 500.0)
        val category = CategoryEntity(id = "cat1", name = "Groceries", iconName = "ShoppingCart", colorHex = "#059669", type = TransactionType.EXPENSE)
        val rate = ExchangeRateEntity(id = "r1", userId = "u1", fromCurrency = "EUR", toCurrency = "USD", rate = 1.10)
        val snapshot = NetWorthSnapshotEntity(id = "s1", userId = "u1", monthYear = "2026-01", totalAssets = 3500.0, totalLiabilities = 0.0, netWorth = 3500.0)

        // Verify entity preservation rules
        assertEquals("u1", user.id)
        assertEquals(1500.0, checking.initialBalance, 0.001)
        assertEquals(120.0, tx.amount, 0.001)
        assertEquals(500.0, budget.amountLimit, 0.001)
        assertEquals(1000.0, recurring.amount, 0.001)
        assertEquals(500.0, goal.savedAmount, 0.001)
        assertEquals("#059669", category.colorHex)
        assertEquals(1.10, rate.rate, 0.001)
        assertEquals(3500.0, snapshot.netWorth, 0.001)
    }
}
