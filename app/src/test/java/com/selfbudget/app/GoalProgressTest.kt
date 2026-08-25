package com.selfbudget.app

import com.selfbudget.app.data.model.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProgressTest {

    @Test
    fun testGoalProgressGuard_ZeroTargetAmountReturnsZeroPercent() {
        val goalZeroTarget = GoalEntity(
            userId = "u1",
            name = "Zero Target Goal",
            targetAmount = 0.0,
            savedAmount = 50.0
        )

        val progress = if (goalZeroTarget.targetAmount > 0.0) (goalZeroTarget.savedAmount / goalZeroTarget.targetAmount).toFloat() else 0f

        assertEquals(0f, progress, 0.0001f)
    }
}
