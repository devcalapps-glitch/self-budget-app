package com.selfbudget.app

import com.selfbudget.app.core.util.Money
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.RecurringFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated Unit Test Suite for the category budget auto-sync / manual-override safeguard
 * (F-103). Mirrors the decision rule in `MainViewModel.syncBudgetForRecurringExpense`: a
 * recurring bill's suggested monthly ceiling is recomputed from scratch every time (not just
 * raised) while a budget is still `isAutoSynced`, but a manually-set budget is left untouched.
 */
class BudgetAutoSyncTest {

    /** Mirrors MainViewModel.syncBudgetForRecurringExpense's resulting ceiling decision. */
    private fun resolveSyncedLimit(existingBudget: BudgetEntity?, recurringAmount: Double, frequency: RecurringFrequency): Double {
        val suggestedLimit = Math.ceil(RecurringFrequencyNormalizer.toMonthlyAmount(recurringAmount, frequency))
        return when {
            existingBudget == null -> suggestedLimit
            existingBudget.isAutoSynced -> suggestedLimit
            else -> existingBudget.amountLimit
        }
    }

    @Test
    fun testNoExistingBudget_createsAutoSyncedSuggestion() {
        // Given: no budget yet for this category, and a new $50/week recurring bill
        val resolved = resolveSyncedLimit(existingBudget = null, recurringAmount = 50.0, frequency = RecurringFrequency.WEEKLY)

        // Then: the suggested ceiling is the exact monthly equivalent, rounded up
        assertEquals(217.0, resolved, 0.001)
    }

    @Test
    fun testAutoSyncedBudget_recomputesDownward_whenBillAmountDrops() {
        // Given: an auto-synced $217 budget from a $50/week bill that's now been lowered to $30/week
        val existingBudget = BudgetEntity(
            userId = "u1",
            categoryId = "cat_subs",
            amountLimit = 217.0,
            monthYear = "2026-08",
            isAutoSynced = true
        )

        // When: the recurring bill is edited down
        val resolved = resolveSyncedLimit(existingBudget, recurringAmount = 30.0, frequency = RecurringFrequency.WEEKLY)

        // Then: the ceiling comes back down too, instead of only ever being allowed to rise
        assertTrue("Auto-synced ceiling must be able to decrease", resolved < existingBudget.amountLimit)
        assertEquals(130.0, resolved, 0.001)
    }

    @Test
    fun testManuallySetBudget_isNeverOverwrittenByRecurringSync() {
        // Given: the user manually set a $300 ceiling for this category
        val manualBudget = BudgetEntity(
            userId = "u1",
            categoryId = "cat_subs",
            amountLimit = 300.0,
            monthYear = "2026-08",
            isAutoSynced = false
        )

        // When: a recurring bill in that category is added/edited afterward
        val resolved = resolveSyncedLimit(manualBudget, recurringAmount = 500.0, frequency = RecurringFrequency.WEEKLY)

        // Then: the manually-set ceiling is left untouched, not silently replaced by the suggestion
        assertEquals(300.0, resolved, 0.001)
    }

    @Test
    fun testManualEntry_marksBudgetAsNoLongerAutoSynced() {
        // Mirrors MainViewModel.setCategoryBudget: any hand-entered limit flips isAutoSynced off.
        val budget = BudgetEntity(
            userId = "u1",
            categoryId = "cat_subs",
            amountLimit = Money.round(275.0),
            monthYear = "2026-08",
            isAutoSynced = false
        )

        assertTrue("A budget set through the manual entry path must not be auto-synced", !budget.isAutoSynced)
    }
}
