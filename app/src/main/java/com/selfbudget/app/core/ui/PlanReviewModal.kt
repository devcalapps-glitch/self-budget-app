package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.ui.components.EntryType
import com.selfbudget.app.core.ui.components.IconTile
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.QuickAmountChips
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.ui.components.ToggleRow
import com.selfbudget.app.core.ui.components.TransactionAmountHero
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.BudgetStatus
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.budgetStatus
import com.selfbudget.app.ui.theme.getBrandColor
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
import java.util.Calendar

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

data class PlanCategoryItem(
    val category: CategoryEntity,
    val spent: Double,
    val limit: Double,
    val rolloverEnabled: Boolean,
    val percent: Double,
    val status: BudgetStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanReviewModal(
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    recurringList: List<RecurringTransactionEntity> = emptyList(),
    currencySymbol: String = "$",
    onSetBudget: (categoryId: String, limit: Double, rolloverEnabled: Boolean) -> Unit,
    onNavigateToFullBudget: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val expenseTransactions = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }
    }
    val spentByCategory = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.categoryId }
            .mapValues { (_, txs) -> Money.sum(txs.map { it.amount }) }
    }

    val planItems = remember(budgets, categories, spentByCategory) {
        val budgetMap = budgets.associateBy { it.categoryId }

        budgets.filter { it.amountLimit > 0.0 }.mapNotNull { b ->
            val cat = categoryMap[b.categoryId] ?: return@mapNotNull null
            val limit = b.amountLimit
            val spent = spentByCategory[b.categoryId] ?: 0.0
            val pct = if (limit > 0.0) spent / limit else 0.0
            val status = budgetStatus(pct.toFloat())
            PlanCategoryItem(
                category = cat,
                spent = spent,
                limit = limit,
                rolloverEnabled = b.rolloverEnabled,
                percent = pct,
                status = status
            )
        }.sortedWith(
            compareByDescending<PlanCategoryItem> { it.status == BudgetStatus.Over }
                .thenByDescending { it.status == BudgetStatus.Watch }
                .thenByDescending { it.percent }
                .thenBy { it.category.name }
        )
    }

    val totalBudgetLimit = remember(planItems) { Money.sum(planItems.map { it.limit }) }
    val totalSpent = remember(planItems) { Money.sum(planItems.map { it.spent }) }
    val overBudgetCount = remember(planItems) { planItems.count { it.status == BudgetStatus.Over } }
    val watchCount = remember(planItems) { planItems.count { it.status == BudgetStatus.Watch } }

    // Edit budget state
    var editingCategory by remember { mutableStateOf<PlanCategoryItem?>(null) }
    var editAmountText by remember { mutableStateOf("") }
    var editRollover by remember { mutableStateOf(false) }

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
            val isEditing = editingCategory != null
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (isEditing) it.blur(20.dp) else it }
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Plan Review",
                            style = SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        CircularBackButton(onClick = onDismiss, modifier = Modifier.padding(start = 12.dp, end = 8.dp))
                    },
                    actions = {
                        PrimaryPillButton(
                            text = "Full Budget",
                            onClick = {
                                onDismiss()
                                onNavigateToFullBudget()
                            },
                            ramp = Ramp.Teal
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                    // 1. Overall Health Hero Banner
                    val heroRamp = when {
                        planItems.isEmpty() -> Ramp.Gray
                        overBudgetCount > 0 -> Ramp.Red
                        watchCount > 0 -> Ramp.Amber
                        else -> Ramp.Teal
                    }
                    Surface(
                        shape = ShapeHero,
                        color = heroRamp.tintFill(isDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = heroRamp.solidFill(isDark),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (overBudgetCount > 0) Icons.Default.Warning else Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = heroRamp.onSolidFill(isDark)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SPENDING PLAN HEALTH".uppercase(),
                                    style = SelfBudgetType.eyebrow,
                                    color = heroRamp.secondaryText(isDark)
                                )
                                Text(
                                    text = when {
                                        planItems.isEmpty() -> "No Active Category Budgets"
                                        overBudgetCount > 0 -> "$overBudgetCount Categories Over Limit"
                                        watchCount > 0 -> "$watchCount Categories Approaching Limit"
                                        else -> "All Budget Categories On Track"
                                    },
                                    style = SelfBudgetType.heading,
                                    color = heroRamp.titleText(isDark)
                                )
                                Text(
                                    text = if (planItems.isEmpty()) {
                                        "Set monthly limits on your categories to track spending pace."
                                    } else {
                                        "$currencySymbol%.0f spent of $currencySymbol%.0f planned".format(totalSpent, totalBudgetLimit)
                                    },
                                    style = SelfBudgetType.meta,
                                    color = heroRamp.secondaryText(isDark)
                                )
                            }
                        }
                    }

                    // 2. Section: All Categories & Quick Adjustments
                    if (planItems.isNotEmpty()) {
                        SectionHeaderBand(
                            title = "Categories and spending limits",
                            ramp = Ramp.Teal,
                            icon = Icons.Default.PieChart,
                            countPill = planItems.size.toString()
                        ) {
                            planItems.forEachIndexed { index, item ->
                                PlanCategoryRow(
                                    item = item,
                                    currencySymbol = currencySymbol,
                                    onEdit = {
                                        editingCategory = item
                                        editAmountText = if (item.limit > 0.0) "%.2f".format(item.limit) else ""
                                        editRollover = item.rolloverEnabled
                                    }
                                )
                                if (index < planItems.size - 1) {
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

    // Quick edit budget dialog
    editingCategory?.let { item ->
        val catRamp = sectionRamp(item.category.name)
        val isValid = (editAmountText.toDoubleOrNull() ?: 0.0) >= 0.0

        Dialog(
            onDismissRequest = { editingCategory = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { editingCategory = null }
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = ShapeCard,
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 72.dp, start = 20.dp, end = 20.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Consume clicks inside the card so it doesn't dismiss
                        )
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Bar (Header)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularBackButton(onClick = { editingCategory = null })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adjust Budget Limit",
                            style = SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 1. Transaction Amount Hero
                    TransactionAmountHero(
                        type = EntryType.Income,
                        amountText = editAmountText,
                        onAmountChange = { editAmountText = it },
                        currencySymbol = currencySymbol,
                        badgeText = "MONTHLY BUDGET LIMIT",
                        ramp = Ramp.Teal,
                        stepAmount = 25.0
                    )

                    // Quick Add Chips
                    QuickAmountChips(
                        presets = listOf(25, 50, 100, 250),
                        currencySymbol = currencySymbol,
                        onPick = { preset ->
                            val currentVal = editAmountText.toDoubleOrNull() ?: 0.0
                            editAmountText = "%.2f".format(currentVal + preset)
                        }
                    )

                    // 2. Category Identity & Rollover Settings Card
                    Surface(
                        shape = ShapeCard,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Category Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RampIconTile(
                                    icon = getCategoryIcon(item.category.iconName),
                                    ramp = catRamp,
                                    size = 36.dp,
                                    iconSize = 18.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Category",
                                        style = SelfBudgetType.meta,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.category.name,
                                        style = SelfBudgetType.rowTitle,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            SectionRowDivider(modifier = Modifier.padding(start = 62.dp))

                            // Rollover Toggle Row
                            ToggleRow(
                                icon = Icons.Default.Autorenew,
                                title = "Enable rollover balance",
                                checked = editRollover,
                                onCheckedChange = { editRollover = it },
                                description = "Unspent budget rolls over to next month"
                            )
                        }
                    }

                    // 3. Action Buttons (One Secondary Cancel + One Primary Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = { editingCategory = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )
                        PrimaryPillButton(
                            text = "Save limit",
                            onClick = {
                                val amount = editAmountText.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                                onSetBudget(item.category.id, amount, editRollover)
                                editingCategory = null
                            },
                            enabled = isValid,
                            ramp = Ramp.Teal,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun PlanCategoryRow(
    item: PlanCategoryItem,
    currencySymbol: String,
    onEdit: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val statusColor = when (item.status) {
        BudgetStatus.Over -> getExpenseColor()
        BudgetStatus.Watch -> getWarningColor()
        BudgetStatus.Safe -> getIncomeColor()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.selfbudget.app.core.ui.components.RampIconTile(
            icon = getCategoryIcon(item.category.iconName),
            ramp = sectionRamp(item.category.name),
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
                Text(
                    text = item.category.name,
                    style = SelfBudgetType.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (item.limit > 0.0) {
                        "$currencySymbol%.0f / $currencySymbol%.0f".format(item.spent, item.limit)
                    } else {
                        "$currencySymbol%.0f (No Limit)".format(item.spent)
                    },
                    style = SelfBudgetType.body,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (item.limit > 0.0) {
                LinearProgressIndicator(
                    progress = { item.percent.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(ShapePill),
                    color = getProgressBarColor(statusColor, isOverLimit = item.percent > 1.0 || (item.limit > 0.0 && item.spent > item.limit)),
                    trackColor = if (isDark) Ramp.Gray.c800 else Ramp.Gray.c100
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit limit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
