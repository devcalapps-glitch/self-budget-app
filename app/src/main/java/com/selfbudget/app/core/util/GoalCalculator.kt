package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.GoalEntity

/**
 * Centralized calculations for financial savings goals.
 * Provides a single source of truth for goal progress, remaining target amounts,
 * percentage completion, and target completion dates.
 */
object GoalCalculator {

    data class GoalProgressSummary(
        val totalSaved: Double,
        val targetAmount: Double,
        val remainingAmount: Double,
        val progressFraction: Float,
        val progressPercentage: Int,
        val isCompleted: Boolean
    )

    /**
     * Computes progress for a [GoalEntity].
     *
     * If [goal] has a linked account ([linkedAccountId]), its saved progress is derived
     * from that account's live balance ([accountBalances]) plus any direct manual contributions ([savedAmount]).
     * If no linked account exists, progress is strictly the manual [savedAmount].
     */
    fun computeGoalProgress(
        goal: GoalEntity,
        accounts: List<AccountEntity>,
        accountBalances: Map<String, Double>
    ): GoalProgressSummary {
        val linkedAccount = goal.linkedAccountId?.let { id -> accounts.firstOrNull { it.id == id } }
        val accountBalance = if (linkedAccount != null) {
            (accountBalances[linkedAccount.id] ?: 0.0).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val totalSaved = Money.add(accountBalance, goal.savedAmount)
        val target = goal.targetAmount.coerceAtLeast(0.0)
        val remaining = Money.subtract(target, totalSaved).coerceAtLeast(0.0)

        val fraction = if (target > 0.0) {
            (totalSaved / target).toFloat().coerceIn(0f, 1f)
        } else {
            1.0f
        }

        val percentage = (fraction * 100).toInt()
        val isCompleted = target > 0.0 && totalSaved >= (target - 0.005)

        return GoalProgressSummary(
            totalSaved = Money.round(totalSaved),
            targetAmount = Money.round(target),
            remainingAmount = Money.round(remaining),
            progressFraction = fraction,
            progressPercentage = percentage,
            isCompleted = isCompleted
        )
    }
}
