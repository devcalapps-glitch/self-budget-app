package com.selfbudget.app.feature.budget

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.selfbudget.app.core.ui.BudgetedCategoriesModal
import com.selfbudget.app.core.ui.SpentAndBillsModal
import com.selfbudget.app.core.ui.UnbudgetedTransactionsModal
import com.selfbudget.app.core.ui.getCategoryIcon
import com.selfbudget.app.core.ui.MonthYearHeader
import com.selfbudget.app.core.ui.components.IconTile
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.ui.components.StatusBadge
import com.selfbudget.app.core.ui.components.StatusProgressBar
import com.selfbudget.app.core.ui.components.formatPercentBadge
import com.selfbudget.app.core.util.BudgetRollover
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.feature.dashboard.GoalsSection
import com.selfbudget.app.ui.theme.BudgetStatus
import com.selfbudget.app.ui.theme.CardSurfaceDark
import com.selfbudget.app.ui.theme.DividerDark
import com.selfbudget.app.ui.theme.getProgressBarColor
import com.selfbudget.app.ui.theme.PageBackgroundDark
import com.selfbudget.app.ui.theme.ProgressTrackDark
import com.selfbudget.app.ui.theme.ProgressTrackLight
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.TextPrimaryDark
import com.selfbudget.app.ui.theme.TextSecondaryDark
import com.selfbudget.app.ui.theme.budgetStatus
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.sectionRamp
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import java.util.Calendar

data class CategoryBudgetUiModel(
    val category: CategoryEntity,
    val budgetLimit: Double,
    val ownLimit: Double,
    val rolloverEnabled: Boolean,
    val spentAmount: Double,
    val recurringCommittedAmount: Double,
    val pendingUpcomingAmount: Double,
    val safeToSpendAmount: Double,
    val percentage: Float,
    val isOverBudget: Boolean,
    val isAtLimit: Boolean,
    val isWarning: Boolean,
    val isFixedCommitmentCategory: Boolean
)

private data class LocalBudgetOverride(
    val limit: Double,
    val rolloverEnabled: Boolean
)

@Composable
fun BudgetScreen(
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    recurringList: List<RecurringTransactionEntity> = emptyList(),
    currencySymbol: String = "$",
    previousMonthBudgets: List<BudgetEntity> = emptyList(),
    previousMonthSpentByCategory: Map<String, Double> = emptyMap(),
    selectedMonthYear: String = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date()),
    onPreviousMonth: (() -> Unit)? = null,
    onNextMonth: (() -> Unit)? = null,
    onSelectMonthYear: ((String) -> Unit)? = null,
    onSetBudget: (categoryId: String, limit: Double, rolloverEnabled: Boolean) -> Unit,
    onDeleteBudget: (categoryId: String) -> Unit = {},
    goals: List<GoalEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    accountBalances: Map<String, Double> = emptyMap(),
    onAddGoal: (
        name: String,
        targetAmount: Double,
        linkedAccountId: String?,
        targetDate: Long?,
        monthlyTargetAmount: Double?,
        recurringFromAccountId: String?,
        recurringFrequency: RecurringFrequency?,
        recurringAmount: Double?
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteGoal: (GoalEntity) -> Unit = {},
    onContributeToGoal: (GoalEntity, Double) -> Unit = { _, _ -> },
    onUpdateGoal: (GoalEntity) -> Unit = {},
    onAddCustomCategory: ((CategoryEntity) -> Unit)? = null,
    onAddCustomAccount: (AccountEntity) -> Unit = {},
    // Lets the single global "+" (owned by HomeScreen) open this screen's "new budget" dialog
    // from anywhere in the app, instead of this screen needing its own floating add button.
    requestNewBudget: Boolean = false,
    onNewBudgetRequestHandled: () -> Unit = {},
    isSelected: Boolean = false
) {
    var showSetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<String?>(null) }
    var selectedLimitForEdit by remember { mutableStateOf<Double?>(null) }
    var selectedRolloverForEdit by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(0) } // 0: Spending (Monthly Budget), 1: Savings Goals, 2: Payoff
    var budgetFilter by remember { mutableStateOf("All") }
    var showUnbudgetedModal by remember { mutableStateOf(false) }
    var showBudgetedModal by remember { mutableStateOf(false) }
    var showSpentAndBillsModal by remember { mutableStateOf(false) }
    var localBudgetOverrides by remember(selectedMonthYear) {
        mutableStateOf<Map<String, LocalBudgetOverride>>(emptyMap())
    }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            viewMode = 0
        }
    }

    LaunchedEffect(budgets) {
        if (localBudgetOverrides.isNotEmpty()) {
            val confirmedOverrides = localBudgetOverrides.filter { (categoryId, override) ->
                budgets.any {
                    it.categoryId == categoryId &&
                        kotlin.math.abs(it.amountLimit - override.limit) < 0.005 &&
                        it.rolloverEnabled == override.rolloverEnabled
                }
            }.keys
            if (confirmedOverrides.isNotEmpty()) {
                localBudgetOverrides = localBudgetOverrides - confirmedOverrides
            }
        }
    }

    LaunchedEffect(requestNewBudget) {
        if (requestNewBudget) {
            selectedCategoryForEdit = null
            selectedLimitForEdit = null
            selectedRolloverForEdit = false
            viewMode = 0
            showSetDialog = true
            onNewBudgetRequestHandled()
        }
    }

    val expenseCategories = remember(categories) {
        categories.filter { it.type == TransactionType.EXPENSE }
    }
    val categoryMap = remember(categories) {
        categories.associateBy { it.id }
    }
    val expenseTransactions = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }
    }
    
    val budgetModels = remember(budgets, expenseCategories, expenseTransactions, recurringList, previousMonthBudgets, previousMonthSpentByCategory, localBudgetOverrides) {
        val spentMap = expenseTransactions.groupBy { it.categoryId }
            .mapValues { entry -> Money.sum(entry.value.map { it.amount }) }

        val recurringExpenseList = recurringList.filter { it.type == TransactionType.EXPENSE && !it.isArchived }
        val recurringMap = recurringExpenseList.groupBy { it.categoryId }
            .mapValues { entry ->
                Money.sum(entry.value.map { rec ->
                    com.selfbudget.app.core.util.RecurringFrequencyNormalizer.toMonthlyAmount(rec.amount, rec.frequency)
                })
            }

        val budgetMap = budgets.associateBy { it.categoryId }
        val previousBudgetMap = previousMonthBudgets.associateBy { it.categoryId }
        val activeCategoryIds = (budgets.map { it.categoryId } + localBudgetOverrides.keys).distinct()

        activeCategoryIds.mapNotNull { catId ->
            val cat = categoryMap[catId] ?: return@mapNotNull null
            val budget = budgetMap[catId]
            val localOverride = localBudgetOverrides[catId]
            val committed = recurringMap[catId] ?: 0.0
            val ownLimit = localOverride?.limit ?: budget?.amountLimit ?: return@mapNotNull null
            if (ownLimit <= 0.0) return@mapNotNull null
            val rolloverEnabled = localOverride?.rolloverEnabled ?: budget?.rolloverEnabled ?: false
            val limit = BudgetRollover.effectiveLimit(
                currentLimit = ownLimit,
                rolloverEnabled = rolloverEnabled,
                previousLimit = previousBudgetMap[catId]?.amountLimit ?: 0.0,
                previousSpent = previousMonthSpentByCategory[catId] ?: 0.0
            )
            val spent = spentMap[catId] ?: 0.0
            val pendingUpcoming = (committed - spent).coerceAtLeast(0.0)

            // NOTE (UX Behavior / Issue 13): When actual spent is less than recurring committed bill amount (Spent < Committed),
            // Safe to Spend = Limit - Spent - (Committed - Spent) = Limit - Committed. This remains constant regardless
            // of discretionary spending until the recurring bill posts.
            val safeToSpend = (limit - spent - pendingUpcoming).coerceAtLeast(0.0)

            val totalClaimed = spent + pendingUpcoming
            val pct = if (limit > 0) (totalClaimed / limit).toFloat() else 0f
            val isOver = limit > 0.0 && totalClaimed > (limit + 0.005)
            val isAtLimit = limit > 0.0 && !isOver && totalClaimed >= (limit - 0.005)

            CategoryBudgetUiModel(
                category = cat,
                budgetLimit = limit,
                ownLimit = ownLimit,
                rolloverEnabled = rolloverEnabled,
                spentAmount = spent,
                recurringCommittedAmount = committed,
                pendingUpcomingAmount = pendingUpcoming,
                safeToSpendAmount = safeToSpend,
                percentage = pct,
                isOverBudget = isOver,
                isAtLimit = isAtLimit,
                isWarning = (pct >= 0.8f && pct < 1.0f || ownLimit < committed) && !isOver && !isAtLimit,
                isFixedCommitmentCategory = committed > 0.0
            )
        }.sortedByDescending { it.percentage }
    }

    val totalBudget = remember(budgetModels) { Money.sum(budgetModels.map { it.budgetLimit }) }
    val totalSpentInBudgets = remember(budgetModels) { Money.sum(budgetModels.map { it.spentAmount }) }
    val totalPendingInBudgets = remember(budgetModels) { Money.sum(budgetModels.map { it.pendingUpcomingAmount }) }
    val totalClaimedInBudgets = remember(totalSpentInBudgets, totalPendingInBudgets) {
        Money.add(totalSpentInBudgets, totalPendingInBudgets)
    }
    val totalRecurringCommitted = remember(budgetModels) { Money.sum(budgetModels.map { it.recurringCommittedAmount }) }
    val remainingBudget = Money.subtract(totalBudget, totalClaimedInBudgets).coerceAtLeast(0.0)

    val unbudgetedSpent = remember(budgetModels, expenseTransactions) {
        val budgetedCategoryIds = budgetModels.map { it.category.id }.toSet()
        val unbudgetedTxs = expenseTransactions.filter { it.categoryId !in budgetedCategoryIds }
        Money.sum(unbudgetedTxs.map { it.amount })
    }
    val unbudgetedCategoryCount = remember(budgetModels, expenseTransactions) {
        val budgetedCategoryIds = budgetModels.map { it.category.id }.toSet()
        expenseTransactions.filter { it.categoryId !in budgetedCategoryIds }
            .map { it.categoryId }
            .distinct()
            .size
    }
    val spentAndBillsCategoryCount = remember(budgetModels) {
        budgetModels.count { it.spentAmount > 0.0 || it.pendingUpcomingAmount > 0.0 }
    }

    val attentionModels = remember(budgetModels) {
        budgetModels.filter { it.isOverBudget || it.isAtLimit || it.isWarning || it.pendingUpcomingAmount > 0.0 }
    }
    val filteredModels = remember(budgetModels, budgetFilter) {
        when (budgetFilter) {
            "Over" -> budgetModels.filter { it.isOverBudget || it.isAtLimit }
            "Watch" -> budgetModels.filter { it.isWarning }
            "Safe" -> budgetModels.filter { !it.isOverBudget && !it.isAtLimit && !it.isWarning }
            "Bills" -> budgetModels.filter { it.recurringCommittedAmount > 0.0 || it.pendingUpcomingAmount > 0.0 }
            else -> budgetModels
        }
    }
    val groupedModels = remember(filteredModels) {
        val groupOrder = listOf(
            "Housing & Essentials",
            "Food & Daily Living",
            "Lifestyle & Entertainment",
            "Debt & Financial",
            "Custom Categories",
            "Other"
        )
        val map = filteredModels.groupBy { com.selfbudget.app.core.ui.getExpenseCategoryGroup(it.category) }
        groupOrder.mapNotNull { groupName ->
            map[groupName]?.let { items -> groupName to items }
        } + (map.keys - groupOrder.toSet()).map { key -> key to map.getValue(key) }
    }

    val calendarInfo = remember(selectedMonthYear) {
        val cal = Calendar.getInstance()
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            cal.time = sdf.parse(selectedMonthYear) ?: cal.time
        } catch (_: Exception) {
            // Fall back to the current month if the selected month cannot be parsed.
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
        val currentDay = if (selectedMonthYear == currentMonth) {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        } else {
            1
        }
        val remainingDays = (daysInMonth - currentDay + 1).coerceAtLeast(1)
        Pair(remainingDays, daysInMonth)
    }

    val remainingDays = calendarInfo.first
    val dailyPace = if (totalBudget > 0 && remainingDays > 0) (remainingBudget / remainingDays).coerceAtLeast(0.0) else 0.0

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Segmented View Toggle Pill (Monthly Budget vs Savings Goals vs Payoff)
            val isDarkShell = isAppInDarkTheme()
            Surface(
                shape = ShapePill,
                color = if (isDarkShell) Ramp.Gray.c800 else Ramp.Gray.c50,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Spending" to 0, "Goals" to 1, "Payoff" to 2).forEach { (label, mode) ->
                        val selected = viewMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ShapePill)
                                .background(if (selected) Ramp.Teal.solidFill(isDarkShell) else Color.Transparent)
                                .clickable { viewMode = mode },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = SelfBudgetType.rowTitle,
                                color = if (selected) Ramp.Teal.onSolidFill(isDarkShell) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewMode == 1) {
                GoalsSection(
                    goals = goals,
                    accounts = accounts,
                    accountBalances = accountBalances,
                    currencySymbol = currencySymbol,
                    onAddGoal = onAddGoal,
                    onDeleteGoal = onDeleteGoal,
                    onContributeToGoal = onContributeToGoal,
                    onUpdateGoal = onUpdateGoal,
                    onAddCustomAccount = onAddCustomAccount
                )
            } else if (viewMode == 2) {
                DebtPayoffPlannerSection(
                    accounts = accounts,
                    accountBalances = accountBalances,
                    currencySymbol = currencySymbol
                )
            } else {
                val overBudgetCount = budgetModels.count { it.isOverBudget }
                val warningCount = budgetModels.count { it.isWarning }
                val onTrackCount = budgetModels.count { !it.isOverBudget && !it.isAtLimit && !it.isWarning }
                val attentionCount = attentionModels.size

                val isOverTotal = totalBudget > 0.0 && totalClaimedInBudgets > totalBudget
                val heroRamp = when {
                    budgetModels.isEmpty() -> Ramp.Teal
                    overBudgetCount > 0 || isOverTotal -> Ramp.Red
                    warningCount > 0 -> Ramp.Amber
                    else -> Ramp.Teal
                }
                val heroTitle = when {
                    budgetModels.isEmpty() -> "Set up your spending plan"
                    overBudgetCount > 0 -> "$overBudgetCount categor${if (overBudgetCount == 1) "y" else "ies"} over budget"
                    warningCount > 0 -> "$warningCount categor${if (warningCount == 1) "y" else "ies"} near limit"
                    else -> "All $onTrackCount categories on track"
                }
                val overallProgress = if (totalBudget > 0.0) (totalClaimedInBudgets / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
                val overallPercent = if (totalBudget > 0.0) ((totalClaimedInBudgets / totalBudget) * 100).toInt() else 0
                val cleanRemainingBudget = totalBudget - totalClaimedInBudgets

                // Spending Plan hero card
                val isDarkHero = isAppInDarkTheme()
                val heroFill = if (isDarkHero) CardSurfaceDark else heroRamp.tintFill(isDark = false, large = heroRamp == Ramp.Red)
                Surface(
                    shape = ShapeHero,
                    color = heroFill,
                    border = if (isDarkHero) BorderStroke(0.5.dp, DividerDark) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Row: Badge Icon + Eyebrow / Status + Spent Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RampIconTile(
                                    icon = when {
                                        overBudgetCount > 0 -> Icons.Default.Warning
                                        warningCount > 0 -> Icons.Default.Info
                                        budgetModels.isEmpty() -> Icons.Default.AutoAwesome
                                        else -> Icons.Default.Check
                                    },
                                    ramp = heroRamp,
                                    size = 40.dp,
                                    iconSize = 22.dp
                                )

                                Column {
                                    Text(
                                        text = "SPENDING PLAN",
                                        style = SelfBudgetType.eyebrow,
                                        color = if (isDarkHero) heroRamp.c400 else heroRamp.secondaryText(isDarkHero)
                                    )
                                    Text(
                                        text = heroTitle,
                                        style = SelfBudgetType.title,
                                        color = if (isDarkHero) TextPrimaryDark else heroRamp.titleText(isDarkHero)
                                    )
                                }
                            }

                            if (totalBudget > 0.0) {
                                StatusBadge(
                                    text = "$overallPercent% Spent",
                                    status = when (heroRamp) {
                                        Ramp.Red -> BudgetStatus.Over
                                        Ramp.Amber -> BudgetStatus.Watch
                                        else -> BudgetStatus.Safe
                                    }
                                )
                            }
                        }

                        // Main Hero Metric: Remaining Unallocated Budget (neutral — spec §11)
                        Column {
                            val displayRemaining = kotlin.math.abs(cleanRemainingBudget)
                            Text(
                                text = "$currencySymbol%,.2f".format(displayRemaining),
                                style = SelfBudgetType.display,
                                color = if (cleanRemainingBudget < -0.005) {
                                    if (isDarkHero) Ramp.Red.c200 else Ramp.Red.c600
                                } else {
                                    if (isDarkHero) TextPrimaryDark else heroRamp.titleText(isDarkHero)
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (cleanRemainingBudget >= 0.0) "remaining safe-to-spend this month" else "over total budgeted limit",
                                style = SelfBudgetType.meta,
                                color = if (isDarkHero) TextSecondaryDark else heroRamp.secondaryText(isDarkHero)
                            )
                        }

                        // Progress Bar & Allocation Breakdown
                        if (budgetModels.isNotEmpty() && totalBudget > 0.0) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LinearProgressIndicator(
                                    progress = { overallProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(ShapeChip),
                                    color = getProgressBarColor(heroRamp.c400, isOverLimit = heroRamp == Ramp.Red || isOverTotal),
                                    trackColor = if (isDarkHero) DividerDark else ProgressTrackLight
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$currencySymbol%,.0f spent & committed".format(totalClaimedInBudgets),
                                        style = SelfBudgetType.meta,
                                        color = if (isDarkHero) TextSecondaryDark else heroRamp.secondaryText(isDarkHero)
                                    )
                                    Text(
                                        text = "$currencySymbol%,.0f total budget".format(totalBudget),
                                        style = SelfBudgetType.meta,
                                        color = if (isDarkHero) TextPrimaryDark else heroRamp.titleText(isDarkHero)
                                    )
                                }
                            }
                        }

                        // 3 metric snapshot tiles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroMetricTile(
                                modifier = Modifier.weight(1f),
                                label = "Budgeted",
                                value = "$currencySymbol%,.0f".format(totalBudget),
                                caption = "${budgetModels.size} categories",
                                ramp = Ramp.Gray,
                                isDark = isDarkHero,
                                onClick = { showBudgetedModal = true }
                            )
                            HeroMetricTile(
                                modifier = Modifier.weight(1f),
                                label = "Spent & Bills",
                                value = "$currencySymbol%,.0f".format(totalClaimedInBudgets),
                                caption = if (spentAndBillsCategoryCount > 0) "$spentAndBillsCategoryCount categor${if (spentAndBillsCategoryCount == 1) "y" else "ies"}" else "0 categories",
                                ramp = if (isOverTotal) Ramp.Red else Ramp.Gray,
                                isDark = isDarkHero,
                                onClick = { showSpentAndBillsModal = true }
                            )
                            HeroMetricTile(
                                modifier = Modifier.weight(1f),
                                label = "Unbudgeted",
                                value = "$currencySymbol%,.0f".format(unbudgetedSpent),
                                caption = if (unbudgetedCategoryCount > 0) "$unbudgetedCategoryCount categor${if (unbudgetedCategoryCount == 1) "y" else "ies"}" else "no extra spend",
                                ramp = if (unbudgetedSpent > 0.0) Ramp.Amber else Ramp.Gray,
                                isDark = isDarkHero,
                                onClick = { showUnbudgetedModal = true }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isDarkFilters = isAppInDarkTheme()
                    listOf("All", "Over", "Watch", "Safe", "Bills").forEach { filter ->
                        val selected = budgetFilter == filter
                        Surface(
                            shape = ShapePill,
                            color = if (selected) Ramp.Teal.solidFill(isDarkFilters) else Ramp.Gray.tintFill(isDarkFilters),
                            border = if (selected) null else BorderStroke(0.5.dp, Ramp.Gray.containerBorder(isDarkFilters)),
                            modifier = Modifier
                                .height(42.dp)
                                .clickable { budgetFilter = filter }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
                                Text(
                                    text = filter,
                                    style = SelfBudgetType.rowTitle,
                                    color = if (selected) Ramp.Teal.onSolidFill(isDarkFilters) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Header and Active count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spending plan",
                        style = SelfBudgetType.heading,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (budgetModels.isNotEmpty()) {
                        NeutralBadge(
                            text = if (budgetFilter == "All") "${budgetModels.size} active" else "${filteredModels.size} ${budgetFilter.lowercase()}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (budgetModels.isEmpty()) {
                    // Empty state lives in the section's normal container — no warning tint (spec §12).
                    Surface(
                        shape = ShapeCard,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            RampIconTile(
                                icon = Icons.Default.PieChart,
                                ramp = Ramp.Teal,
                                size = 72.dp,
                                iconSize = 36.dp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No category budgets set",
                                style = SelfBudgetType.heading,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Set monthly spending limits for categories like Groceries, Dining, and Rent to unlock daily spending pace safeguards.",
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            PrimaryPillButton(
                                text = "Set category budget",
                                onClick = { showSetDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            )
                        }
                    }
                } else if (filteredModels.isEmpty()) {
                    Surface(
                        shape = ShapeCard,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "No $budgetFilter categories",
                                style = SelfBudgetType.heading,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try another filter to review the rest of your spending plan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        val isDarkGroups = isAppInDarkTheme()
                        groupedModels.forEach { (groupName, modelsInGroup) ->
                            val groupRamp = sectionRamp(groupName)
                            val groupIcon = com.selfbudget.app.core.ui.getExpenseCategoryGroupIcon(groupName)
                            val groupTotalLimit = Money.sum(modelsInGroup.map { it.budgetLimit })
                            val groupTotalSpent = Money.sum(modelsInGroup.map { it.spentAmount })
                            val groupTotalPending = Money.sum(modelsInGroup.map { it.pendingUpcomingAmount })
                            val groupTotalClaimed = Money.add(groupTotalSpent, groupTotalPending)

                            SectionHeaderBand(
                                title = groupName,
                                ramp = groupRamp,
                                icon = groupIcon,
                                countPill = "${modelsInGroup.size}",
                                trailingText = "$currencySymbol%.2f of $currencySymbol%.2f".format(groupTotalClaimed, groupTotalLimit)
                            ) {
                                modelsInGroup.forEachIndexed { index, model ->
                                    if (index > 0) SectionRowDivider()
                                    val status = budgetStatus(model.percentage)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCategoryForEdit = model.category.id
                                                selectedLimitForEdit = model.ownLimit
                                                selectedRolloverForEdit = model.rolloverEnabled
                                                showSetDialog = true
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        // Top Row: Category Icon + Name & Safe / Remaining Status
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
                                                    icon = getCategoryIcon(model.category),
                                                    ramp = groupRamp,
                                                    size = 38.dp,
                                                    iconSize = 20.dp
                                                )

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column {
                                                    Text(
                                                        text = model.category.name,
                                                        style = SelfBudgetType.rowTitle,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )

                                                    val subtitleText = buildString {
                                                        append("$currencySymbol%.2f spent of $currencySymbol%.2f".format(model.spentAmount, model.budgetLimit))
                                                        if (model.pendingUpcomingAmount > 0.0) {
                                                            append(" • $currencySymbol%.2f due".format(model.pendingUpcomingAmount))
                                                        }
                                                    }
                                                    Text(
                                                        text = subtitleText,
                                                        style = SelfBudgetType.meta,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Safe to Spend / Over Amount — red is reserved for over-limit (spec §10/§13);
                                            // an on-track "safe to spend" amount reads as neutral text, not a status color.
                                            Column(horizontalAlignment = Alignment.End) {
                                                val overAmount = ((model.spentAmount + model.pendingUpcomingAmount) - model.budgetLimit).coerceAtLeast(0.0)
                                                if (model.isOverBudget && overAmount > 0.005) {
                                                    Text(
                                                        text = "-$currencySymbol%.2f".format(overAmount),
                                                        style = SelfBudgetType.rowTitle,
                                                        color = Ramp.Red.secondaryText(isDarkGroups)
                                                    )
                                                    Text(
                                                        text = "over limit",
                                                        style = SelfBudgetType.meta,
                                                        color = Ramp.Red.secondaryText(isDarkGroups)
                                                    )
                                                } else {
                                                    Text(
                                                        text = "$currencySymbol%.2f".format(model.safeToSpendAmount),
                                                        style = SelfBudgetType.rowTitle,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "safe to spend",
                                                        style = SelfBudgetType.meta,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Middle Row: Status Progress Bar + Badge + Details Affordance
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            StatusProgressBar(
                                                progress = model.percentage,
                                                status = status,
                                                modifier = Modifier.weight(1f)
                                            )

                                            StatusBadge(text = formatPercentBadge(model.percentage), status = status)

                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "View details",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(100.dp))
        }

        if (viewMode == 0) {
            if (showSetDialog) {
                val selectedModel = remember(selectedCategoryForEdit, budgetModels) {
                    budgetModels.firstOrNull { it.category.id == selectedCategoryForEdit }
                }

                SetBudgetDialog(
                    categories = expenseCategories,
                    initialCategoryId = selectedCategoryForEdit,
                    initialLimit = selectedLimitForEdit,
                    initialRolloverEnabled = selectedRolloverForEdit,
                    recurringList = recurringList,
                    transactions = expenseTransactions,
                    budgetUiModel = selectedModel,
                    onDismiss = { showSetDialog = false },
                    onConfirm = { categoryId, limit, rollover ->
                        localBudgetOverrides = localBudgetOverrides + (
                            categoryId to LocalBudgetOverride(
                                limit = Money.round(limit),
                                rolloverEnabled = rollover
                            )
                        )
                        onSetBudget(categoryId, limit, rollover)
                        budgetFilter = "All"
                        showSetDialog = false
                    },
                    onDeleteBudget = { categoryId ->
                        localBudgetOverrides = localBudgetOverrides + (
                            categoryId to LocalBudgetOverride(
                                limit = 0.0,
                                rolloverEnabled = false
                            )
                        )
                        onDeleteBudget(categoryId)
                        budgetFilter = "All"
                        showSetDialog = false
                    },
                    onAddCustomCategory = onAddCustomCategory
                )
            }
        }

        if (showUnbudgetedModal) {
            UnbudgetedTransactionsModal(
                budgets = budgets,
                categories = categories,
                transactions = expenseTransactions,
                accounts = accounts,
                currencySymbol = currencySymbol,
                selectedMonthYear = selectedMonthYear,
                onSetBudget = onSetBudget,
                onDismiss = { showUnbudgetedModal = false }
            )
        }

        if (showBudgetedModal) {
            BudgetedCategoriesModal(
                budgets = budgets,
                categories = categories,
                transactions = expenseTransactions,
                previousMonthBudgets = previousMonthBudgets,
                previousMonthSpentByCategory = previousMonthSpentByCategory,
                currencySymbol = currencySymbol,
                selectedMonthYear = selectedMonthYear,
                onDismiss = { showBudgetedModal = false }
            )
        }

        if (showSpentAndBillsModal) {
            SpentAndBillsModal(
                budgets = budgets,
                categories = categories,
                transactions = expenseTransactions,
                recurringList = recurringList,
                accounts = accounts,
                currencySymbol = currencySymbol,
                selectedMonthYear = selectedMonthYear,
                onDismiss = { showSpentAndBillsModal = false }
            )
        }
    }
}

/** One of the hero card's 3 metric snapshot tiles (spec §11 breakdown row style). */
@Composable
private fun HeroMetricTile(
    label: String,
    value: String,
    caption: String,
    ramp: Ramp,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = ShapeChip,
        color = if (isDark) PageBackgroundDark else ramp.tintFill(isDark),
        border = if (isDark) BorderStroke(0.5.dp, DividerDark) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = SelfBudgetType.meta,
                color = if (isDark) TextSecondaryDark else ramp.secondaryText(isDark)
            )
            Text(
                text = value,
                style = SelfBudgetType.heading,
                color = if (isDark) {
                    if (ramp == Ramp.Red) Ramp.Red.c200
                    else if (ramp == Ramp.Amber) Ramp.Amber.c200
                    else TextPrimaryDark
                } else ramp.titleText(isDark)
            )
            Text(
                text = caption,
                style = SelfBudgetType.meta,
                color = if (isDark) TextSecondaryDark else ramp.secondaryText(isDark)
            )
        }
    }
}
