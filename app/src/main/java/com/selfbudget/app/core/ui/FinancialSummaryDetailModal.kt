package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.ui.components.DeltaBadge
import com.selfbudget.app.core.ui.components.DeltaMetric
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.ui.components.StatusBadge
import com.selfbudget.app.core.util.PeriodFinancialSummary
import com.selfbudget.app.ui.theme.BudgetStatus
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.sectionRamp
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import kotlin.math.abs

/**
 * Dedicated full-screen subpage modal displaying the clean, high-level financial breakdown summary
 * in words and key category/trend highlights for either Monthly or Annual timeframes.
 */
@Composable
fun FinancialSummaryDetailModal(
    summary: PeriodFinancialSummary,
    currencySymbol: String = "$",
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    val statusRamp = when {
        summary.netSavings >= 0.0 && (summary.totalBudget == 0.0 || summary.totalExpense <= summary.totalBudget) -> Ramp.Teal
        summary.totalBudget > 0.0 && summary.totalExpense > summary.totalBudget -> Ramp.Coral
        summary.netSavings < 0.0 -> Ramp.Coral
        else -> Ramp.Blue
    }

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
                // Persistent Flat Header with CircularBackButton (Design System §8.16–18)
                Surface(color = MaterialTheme.colorScheme.background) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularBackButton(onClick = onDismiss)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (summary.isAnnual) "Annual summary breakdown" else "Monthly summary breakdown",
                                style = SelfBudgetType.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = summary.periodLabel,
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. High-Level Overview in Words (Story Card)
                    item {
                        Surface(
                            shape = ShapeCard,
                            color = statusRamp.tintFill(isDark),
                            border = BorderStroke(1.dp, statusRamp.containerBorder(isDark)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RampIconTile(
                                        icon = when (statusRamp) {
                                            Ramp.Teal -> Icons.Default.CheckCircle
                                            Ramp.Coral -> Icons.Default.Warning
                                            else -> Icons.Default.Summarize
                                        },
                                        ramp = statusRamp,
                                        size = 30.dp,
                                        iconSize = 16.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "HIGH-LEVEL SUMMARY",
                                            style = SelfBudgetType.eyebrow,
                                            color = statusRamp.secondaryText(isDark)
                                        )
                                        Text(
                                            text = summary.headlineText,
                                            style = SelfBudgetType.rowTitle,
                                            color = statusRamp.titleText(isDark)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = statusRamp.containerBorder(isDark),
                                    thickness = 0.5.dp
                                )

                                Text(
                                    text = summary.highLevelNarrative,
                                    style = SelfBudgetType.body,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 2. High-Level Breakdown Pillars (4 Core Pillars)
                    item {
                        SectionHeaderBand(
                            title = "High-level breakdown",
                            ramp = Ramp.Blue,
                            icon = Icons.Default.Summarize
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Pillar 1: Spending & Budget
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RampIconTile(
                                        icon = Icons.Default.ArrowDownward,
                                        ramp = Ramp.Coral,
                                        size = 32.dp,
                                        iconSize = 18.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Spending & budget", style = SelfBudgetType.rowTitle)
                                        val spendSubtitle = if (summary.totalBudget > 0.0) {
                                            val pct = (summary.totalExpense / summary.totalBudget) * 100.0
                                            "$currencySymbol%,.2f of $currencySymbol%,.2f (%.0f%%)".format(
                                                summary.totalExpense,
                                                summary.totalBudget,
                                                pct
                                            )
                                        } else {
                                            "$currencySymbol%,.2f total spent".format(summary.totalExpense)
                                        }
                                        Text(
                                            spendSubtitle,
                                            style = SelfBudgetType.meta,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (summary.totalBudget > 0.0) {
                                            val diffText = if (summary.totalExpense <= summary.totalBudget) {
                                                "$currencySymbol%,.2f remaining under budget".format(summary.totalBudget - summary.totalExpense)
                                            } else {
                                                "$currencySymbol%,.2f over budget".format(summary.totalExpense - summary.totalBudget)
                                            }
                                            Text(
                                                text = diffText,
                                                style = SelfBudgetType.eyebrow,
                                                color = if (summary.totalExpense <= summary.totalBudget) Ramp.Teal.titleText(isDark) else Ramp.Red.titleText(isDark)
                                            )
                                        }
                                    }
                                    if (summary.totalBudget > 0.0) {
                                        StatusBadge(
                                            text = if (summary.totalExpense <= summary.totalBudget) "On track" else "Over",
                                            status = if (summary.totalExpense <= summary.totalBudget) BudgetStatus.Safe else BudgetStatus.Over
                                        )
                                    }
                                }

                                SectionRowDivider()

                                // Pillar 2: Spending Trend
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val trendDown = summary.diffExpense <= 0
                                    RampIconTile(
                                        icon = if (trendDown) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                        ramp = Ramp.Purple,
                                        size = 32.dp,
                                        iconSize = 18.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Spending trend", style = SelfBudgetType.rowTitle)
                                        val trendSubtitle = if (!summary.isAnnual) {
                                            if (summary.prevExpense > 0.0) {
                                                val dir = if (trendDown) "Down" else "Up"
                                                val sign = if (trendDown) "less" else "more"
                                                "$dir %.1f%% ($currencySymbol%,.2f $sign) vs prior month".format(
                                                    abs(summary.diffExpensePercent),
                                                    abs(summary.diffExpense)
                                                )
                                            } else {
                                                "Initial month of spending ($currencySymbol%,.2f)".format(summary.totalExpense)
                                            }
                                        } else {
                                            "$currencySymbol%,.2f / month pace across ${summary.monthsLoggedCount} month(s)".format(
                                                summary.monthlyAverageExpense
                                            )
                                        }
                                        Text(
                                            trendSubtitle,
                                            style = SelfBudgetType.meta,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!summary.isAnnual && summary.prevExpense > 0.0) {
                                        DeltaBadge(
                                            percentChange = summary.diffExpensePercent.toFloat(),
                                            metric = DeltaMetric.SPENDING
                                        )
                                    } else if (summary.isAnnual) {
                                        NeutralBadge("$currencySymbol%,.0f/mo".format(summary.monthlyAverageExpense))
                                    }
                                }

                                SectionRowDivider()

                                // Pillar 3: Savings & Cash Flow
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RampIconTile(
                                        icon = Icons.Default.Savings,
                                        ramp = Ramp.Teal,
                                        size = 32.dp,
                                        iconSize = 18.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Savings & cash flow", style = SelfBudgetType.rowTitle)
                                        val cashFlowSubtitle = "$currencySymbol%,.2f earned minus $currencySymbol%,.2f spent".format(
                                            summary.totalIncome,
                                            summary.totalExpense
                                        )
                                        Text(
                                            cashFlowSubtitle,
                                            style = SelfBudgetType.meta,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val netSign = if (summary.netSavings >= 0) "+$currencySymbol" else "-$currencySymbol"
                                    NeutralBadge(
                                        text = "$netSign%,.2f (%.1f%%)".format(
                                            abs(summary.netSavings),
                                            summary.savingsRatePercent
                                        )
                                    )
                                }

                                SectionRowDivider()

                                // Pillar 4: Savings Goals
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RampIconTile(
                                        icon = Icons.Default.CheckCircle,
                                        ramp = Ramp.Amber,
                                        size = 32.dp,
                                        iconSize = 18.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Savings goals", style = SelfBudgetType.rowTitle)
                                        val goalSubtitle = if (summary.totalAmountAddedToGoals > 0.0) {
                                            "+$currencySymbol%,.2f added • ${summary.goalsMetCount}/${summary.totalGoalsCount} completed".format(
                                                summary.totalAmountAddedToGoals
                                            )
                                        } else {
                                            "No contributions added • ${summary.totalGoalsCount} active goal(s)"
                                        }
                                        Text(
                                            goalSubtitle,
                                            style = SelfBudgetType.meta,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    NeutralBadge(
                                        text = "${(summary.goalsProgressFraction * 100).toInt()}% funded"
                                    )
                                }
                            }
                        }
                    }

                    // 3. Top Categories High-Level Breakdown
                    item {
                        val activeCategories = summary.categorySummaries.filter { it.spent > 0.0 }
                        if (activeCategories.isNotEmpty()) {
                            val topCategories = activeCategories.take(4)
                            val otherCategories = activeCategories.drop(4)
                            val otherSpent = otherCategories.sumOf { it.spent }
                            val otherShare = if (summary.totalExpense > 0.0) (otherSpent / summary.totalExpense).toFloat() else 0f

                            SectionHeaderBand(
                                title = "Category breakdown",
                                ramp = Ramp.Coral,
                                icon = Icons.Default.PieChart,
                                countPill = "${activeCategories.size} active"
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    topCategories.forEachIndexed { index, item ->
                                        val catRamp = sectionRamp(getExpenseCategoryGroup(item.category))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            RampIconTile(
                                                icon = getCategoryIcon(item.category),
                                                ramp = catRamp,
                                                size = 30.dp,
                                                iconSize = 16.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.category.name,
                                                    style = SelfBudgetType.rowTitle,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val budgetInfo = if (item.budgeted > 0.0) {
                                                    "$currencySymbol%,.2f of $currencySymbol%,.2f budget".format(item.spent, item.budgeted)
                                                } else {
                                                    "Unbudgeted"
                                                }
                                                Text(
                                                    budgetInfo,
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "$currencySymbol%,.2f".format(item.spent),
                                                    style = SelfBudgetType.rowTitle
                                                )
                                                Text(
                                                    text = "%.1f%% of spend".format(item.sharePercent * 100f),
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (item.budgeted > 0.0) {
                                                StatusBadge(
                                                    text = if (item.isOverBudget) "Over" else "Safe",
                                                    status = if (item.isOverBudget) BudgetStatus.Over else BudgetStatus.Safe
                                                )
                                            }
                                        }
                                        if (index < topCategories.size - 1 || otherCategories.isNotEmpty()) {
                                            SectionRowDivider()
                                        }
                                    }

                                    if (otherCategories.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            RampIconTile(
                                                icon = Icons.Default.MoreHoriz,
                                                ramp = Ramp.Gray,
                                                size = 30.dp,
                                                iconSize = 16.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Other categories (${otherCategories.size})",
                                                    style = SelfBudgetType.rowTitle
                                                )
                                                Text(
                                                    text = "Remaining active expenses",
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "$currencySymbol%,.2f".format(otherSpent),
                                                    style = SelfBudgetType.rowTitle
                                                )
                                                Text(
                                                    text = "%.1f%% of spend".format(otherShare * 100f),
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 150dp bottom scroll clearance (Design System §8.10)
                    item {
                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }
            }
        }
    }
}
