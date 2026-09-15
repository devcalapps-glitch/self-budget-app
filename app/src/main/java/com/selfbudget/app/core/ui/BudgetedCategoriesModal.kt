package com.selfbudget.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.util.BudgetRollover
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.BudgetStatus
import com.selfbudget.app.ui.theme.ProgressTrackDark
import com.selfbudget.app.ui.theme.ProgressTrackLight
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.budgetStatus
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.getProgressBarColor
import com.selfbudget.app.ui.theme.getWarningColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.sectionRamp
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText

data class BudgetedCategoryDetailItem(
    val category: CategoryEntity,
    val limit: Double,
    val spent: Double,
    val rolloverEnabled: Boolean,
    val percent: Float,
    val status: BudgetStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetedCategoriesModal(
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    previousMonthBudgets: List<BudgetEntity> = emptyList(),
    previousMonthSpentByCategory: Map<String, Double> = emptyMap(),
    currencySymbol: String = "$",
    selectedMonthYear: String,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val previousBudgetMap = remember(previousMonthBudgets) { previousMonthBudgets.associateBy { it.categoryId } }

    val expenseTransactions = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }
    }
    val spentByCategory = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.categoryId }
            .mapValues { (_, txs) -> Money.sum(txs.map { it.amount }) }
    }

    val budgetedItems = remember(budgets, categoryMap, spentByCategory, previousBudgetMap, previousMonthSpentByCategory) {
        budgets.filter { it.amountLimit > 0.0 }.mapNotNull { b ->
            val cat = categoryMap[b.categoryId] ?: return@mapNotNull null
            val effectiveLimit = BudgetRollover.effectiveLimit(
                currentLimit = b.amountLimit,
                rolloverEnabled = b.rolloverEnabled,
                previousLimit = previousBudgetMap[b.categoryId]?.amountLimit ?: 0.0,
                previousSpent = previousMonthSpentByCategory[b.categoryId] ?: 0.0
            )
            val spent = spentByCategory[b.categoryId] ?: 0.0
            val pct = if (effectiveLimit > 0.0) (spent / effectiveLimit).toFloat() else 0f
            BudgetedCategoryDetailItem(
                category = cat,
                limit = effectiveLimit,
                spent = spent,
                rolloverEnabled = b.rolloverEnabled,
                percent = pct,
                status = budgetStatus(pct)
            )
        }.sortedByDescending { it.limit }
    }

    val totalBudgeted = remember(budgetedItems) { Money.sum(budgetedItems.map { it.limit }) }
    val totalSpentInBudget = remember(budgetedItems) { Money.sum(budgetedItems.map { it.spent }) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Budgeted Categories",
                            style = SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        CircularBackButton(onClick = onDismiss, modifier = Modifier.padding(start = 12.dp, end = 8.dp))
                    },
                    actions = {
                        NeutralBadge(text = "${budgetedItems.size} active")
                        Spacer(modifier = Modifier.width(16.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Hero Summary Card
                    Surface(
                        shape = ShapeHero,
                        color = Ramp.Teal.tintFill(isDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Ramp.Teal.solidFill(isDark),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = Ramp.Teal.onSolidFill(isDark)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PLANNED MONTHLY BUDGET".uppercase(),
                                    style = SelfBudgetType.eyebrow,
                                    color = Ramp.Teal.secondaryText(isDark)
                                )
                                Text(
                                    text = "$currencySymbol%,.2f".format(totalBudgeted),
                                    style = SelfBudgetType.display,
                                    color = Ramp.Teal.titleText(isDark)
                                )
                                Text(
                                    text = "$currencySymbol%,.0f spent of $currencySymbol%,.0f total planned limit".format(totalSpentInBudget, totalBudgeted),
                                    style = SelfBudgetType.meta,
                                    color = Ramp.Teal.secondaryText(isDark)
                                )
                            }
                        }
                    }

                    // 2. Budgeted Category Breakdown (Read-Only)
                    if (budgetedItems.isEmpty()) {
                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                RampIconTile(
                                    icon = Icons.Default.PieChart,
                                    ramp = Ramp.Teal,
                                    size = 56.dp,
                                    iconSize = 28.dp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No category budgets configured",
                                    style = SelfBudgetType.heading,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Set monthly spending limits for your categories to plan your budget.",
                                    style = SelfBudgetType.body,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        SectionHeaderBand(
                            title = "Active Category Limits",
                            ramp = Ramp.Teal,
                            icon = Icons.Default.PieChart,
                            countPill = "${budgetedItems.size} categories"
                        ) {
                            budgetedItems.forEachIndexed { index, item ->
                                val statusColor = when (item.status) {
                                    BudgetStatus.Over -> getExpenseColor()
                                    BudgetStatus.Watch -> getWarningColor()
                                    BudgetStatus.Safe -> getIncomeColor()
                                }
                                val catRamp = sectionRamp(item.category.name)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RampIconTile(
                                        icon = getCategoryIcon(item.category.iconName),
                                        ramp = catRamp,
                                        size = 38.dp,
                                        iconSize = 20.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = item.category.name,
                                                    style = SelfBudgetType.rowTitle,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (item.rolloverEnabled) {
                                                    NeutralBadge(text = "Rollover")
                                                }
                                            }
                                            Text(
                                                text = "$currencySymbol%.0f / $currencySymbol%.0f".format(item.spent, item.limit),
                                                style = SelfBudgetType.body,
                                                color = statusColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { item.percent.coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(ShapePill),
                                            color = getProgressBarColor(statusColor, isOverLimit = item.percent > 1.0f || item.spent > item.limit),
                                            trackColor = if (isDark) ProgressTrackDark else ProgressTrackLight
                                        )
                                    }
                                }
                                if (index < budgetedItems.size - 1) {
                                    SectionRowDivider()
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
