package com.selfbudget.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.DoneChip
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.titleText

internal data class GoalDetailItem(
    val goal: GoalEntity,
    val linkedAccount: AccountEntity?,
    val currentAmount: Double,
    val progress: Float
)

@Composable
fun SavingsGoalsAnalyticsModal(
    goals: List<GoalEntity>,
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double> = emptyMap(),
    currencySymbol: String = "$",
    onDismiss: () -> Unit
) {
    val goalDetails = remember(goals, accounts, accountBalances) {
        goals.map { goal ->
            val linkedAccount = accounts.firstOrNull { it.id == goal.linkedAccountId }
            val accountAmount = linkedAccount?.let { accountBalances[it.id] ?: it.initialBalance } ?: 0.0
            val currentAmount = accountAmount + goal.savedAmount
            val progress = if (goal.targetAmount > 0) (currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
            GoalDetailItem(goal, linkedAccount, currentAmount, progress)
        }.sortedByDescending { it.progress }
    }

    val totalTarget = remember(goals) { goals.sumOf { it.targetAmount } }
    val totalSaved = remember(goalDetails) { goalDetails.sumOf { it.currentAmount } }
    val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f
    val goalsMetCount = remember(goalDetails) {
        goalDetails.count { it.goal.targetAmount > 0 && it.currentAmount >= it.goal.targetAmount }
    }

    // Report identity: Savings goals = Amber (spec §11). "Goal met" always reads Teal (spec §22).
    val ramp = Ramp.Amber
    val isDark = isAppInDarkTheme()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Header — ✕ and title only (spec §14).
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Savings goals analytics",
                            style = SelfBudgetType.heading,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Overall Progress Hero Summary Card — neutral display number (spec §11).
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RampIconTile(icon = Icons.Default.Savings, ramp = ramp, size = 36.dp, iconSize = 20.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Total saved",
                                        style = SelfBudgetType.heading,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "$currencySymbol%.2f".format(totalSaved),
                                    style = SelfBudgetType.display,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "of $currencySymbol%.2f target".format(totalTarget),
                                    style = SelfBudgetType.meta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { overallProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(ShapeChip),
                                    color = ramp.c400,
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Goals met",
                                        style = SelfBudgetType.meta,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    NeutralBadge(text = "$goalsMetCount / ${goalDetails.size}")
                                }
                            }
                        }
                    }

                    // 2. Goals Breakdown Section Header
                    item {
                        Text(
                            text = "Goals (${goalDetails.size})",
                            style = SelfBudgetType.heading,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 3. Goal Detail List Items
                    if (goalDetails.isEmpty()) {
                        item {
                            Surface(
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    RampIconTile(icon = Icons.Default.Savings, ramp = ramp, size = 56.dp, iconSize = 28.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No savings goals set",
                                        style = SelfBudgetType.heading,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Set a goal from the Plan tab to track it here.",
                                        style = SelfBudgetType.body,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Column {
                                    goalDetails.forEachIndexed { index, detail ->
                                        val isMet = detail.goal.targetAmount > 0 && detail.currentAmount >= detail.goal.targetAmount
                                        val rowRamp = if (isMet) Ramp.Teal else ramp

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    RampIconTile(
                                                        icon = if (isMet) Icons.Default.CheckCircle else Icons.Default.Savings,
                                                        ramp = rowRamp,
                                                        size = 36.dp,
                                                        iconSize = 18.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = detail.goal.name,
                                                            style = SelfBudgetType.rowTitle,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = detail.linkedAccount?.let { "Linked to ${it.name}" } ?: "Manual goal",
                                                            style = SelfBudgetType.meta,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                if (isMet) {
                                                    Text(
                                                        text = "$currencySymbol%.2f saved".format(detail.currentAmount),
                                                        style = SelfBudgetType.meta,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                } else {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = "$currencySymbol%.2f".format(detail.currentAmount),
                                                            style = SelfBudgetType.rowTitle,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "of $currencySymbol%.2f".format(detail.goal.targetAmount),
                                                            style = SelfBudgetType.meta,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Goal met replaces the bar+percentage with a done chip (spec §22) instead
                                            // of a progress bar/number that could read over 100%.
                                            if (isMet) {
                                                DoneChip(text = "Goal met", ramp = Ramp.Teal)
                                            } else {
                                                LinearProgressIndicator(
                                                    progress = { detail.progress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(ShapeChip),
                                                    color = rowRamp.c400,
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            }
                                        }

                                        if (index < goalDetails.size - 1) {
                                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }
            }
        }
    }
}
