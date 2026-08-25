package com.selfbudget.app.feature.budget

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfbudget.app.core.ui.getCategoryIcon
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
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.IncomeGreen
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
    val isWarning: Boolean,
    val isFixedCommitmentCategory: Boolean
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
    onSetBudget: (categoryId: String, limit: Double, rolloverEnabled: Boolean) -> Unit,
    onDeleteBudget: (categoryId: String) -> Unit = {},
    goals: List<GoalEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    accountBalances: Map<String, Double> = emptyMap(),
    onAddGoal: (name: String, targetAmount: Double, linkedAccountId: String?) -> Unit = { _, _, _ -> },
    onDeleteGoal: (GoalEntity) -> Unit = {},
    onContributeToGoal: (GoalEntity, Double) -> Unit = { _, _ -> },
    onUpdateGoal: (GoalEntity) -> Unit = {},
    onAddCustomCategory: ((CategoryEntity) -> Unit)? = null,
    // Lets the single global "+" (owned by HomeScreen) open this screen's "new budget" dialog
    // from anywhere in the app, instead of this screen needing its own floating add button.
    requestNewBudget: Boolean = false,
    onNewBudgetRequestHandled: () -> Unit = {}
) {
    var showSetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<String?>(null) }
    var selectedLimitForEdit by remember { mutableStateOf<Double?>(null) }
    var selectedRolloverForEdit by remember { mutableStateOf(false) }
    var selectedTabFilter by remember { mutableStateOf(0) } // 0: All, 1: Fixed Bills, 2: Variable Discretionary
    var viewMode by remember { mutableStateOf(0) } // 0: Monthly Budget, 1: Savings Goals

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

    val budgetModels = remember(budgets, expenseCategories, expenseTransactions, recurringList, previousMonthBudgets, previousMonthSpentByCategory) {
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
        val activeCategoryIds = budgets.map { it.categoryId }.distinct()

        activeCategoryIds.mapNotNull { catId ->
            val cat = categoryMap[catId] ?: return@mapNotNull null
            val budget = budgetMap[catId] ?: return@mapNotNull null
            val committed = recurringMap[catId] ?: 0.0
            val ownLimit = budget.amountLimit
            val limit = BudgetRollover.effectiveLimit(
                currentLimit = ownLimit,
                rolloverEnabled = budget.rolloverEnabled,
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
            val isOver = totalClaimed > limit

            CategoryBudgetUiModel(
                category = cat,
                budgetLimit = limit,
                ownLimit = ownLimit,
                rolloverEnabled = budget.rolloverEnabled,
                spentAmount = spent,
                recurringCommittedAmount = committed,
                pendingUpcomingAmount = pendingUpcoming,
                safeToSpendAmount = safeToSpend,
                percentage = pct,
                isOverBudget = isOver,
                isWarning = (pct >= 0.75f || ownLimit < committed) && !isOver,
                isFixedCommitmentCategory = committed > 0.0
            )
        }.sortedByDescending { it.percentage }
    }

    val totalBudget = remember(budgetModels) { Money.sum(budgetModels.map { it.budgetLimit }) }
    val totalSpentInBudgets = remember(budgetModels) { Money.sum(budgetModels.map { it.spentAmount }) }
    val totalRecurringCommitted = remember(budgetModels) { Money.sum(budgetModels.map { it.recurringCommittedAmount }) }
    val remainingBudget = Money.subtract(totalBudget, totalSpentInBudgets).coerceAtLeast(0.0)

    val filteredModels = remember(budgetModels, selectedTabFilter) {
        when (selectedTabFilter) {
            1 -> budgetModels.filter { it.isFixedCommitmentCategory }
            2 -> budgetModels.filter { !it.isFixedCommitmentCategory }
            else -> budgetModels
        }
    }

    val calendarInfo = remember {
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
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
            // Segmented View Toggle Pill (Monthly Budget vs Savings Goals)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (viewMode == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                            .clickable { viewMode = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Monthly Budget",
                            fontWeight = if (viewMode == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (viewMode == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                            .clickable { viewMode = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Savings Goals",
                            fontWeight = if (viewMode == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
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
                    onUpdateGoal = onUpdateGoal
                )
            } else {
                // Total Budget Overview Hero Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Total Monthly Budget",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$currencySymbol%.2f".format(totalSpentInBudgets),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "of $currencySymbol%.2f limit".format(totalBudget),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val overallPct = if (totalBudget > 0) (totalSpentInBudgets / totalBudget).toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { overallPct.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (totalSpentInBudgets > totalBudget) ExpenseRed else IncomeGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Daily Pace Safeguard Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (totalBudget > 0) "$currencySymbol%.2f / day max pace".format(dailyPace) else "Set a category budget to see daily pace",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$currencySymbol%.2f remaining across $remainingDays days left".format(remainingBudget),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Limits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (budgetModels.isNotEmpty()) {
                        Text(
                            text = "${budgetModels.size} Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (budgetModels.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (budgetModels.isEmpty()) "No Category Budgets Set" else "No Categories in this Section",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Set monthly spending limits for categories like Groceries, Dining, and Rent to unlock daily spending pace safeguards.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showSetDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IncomeGreen,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Set Category Budget", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filteredModels.forEach { model ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategoryForEdit = model.category.id
                                        selectedLimitForEdit = model.ownLimit
                                        selectedRolloverForEdit = model.rolloverEnabled
                                        showSetDialog = true
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (model.isOverBudget) ExpenseRed.copy(alpha = 0.4f)
                                    else if (model.isWarning) Color(0xFFFF9800).copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (model.isOverBudget) ExpenseRed.copy(alpha = 0.15f)
                                                        else if (model.isWarning) Color(0xFFFF9800).copy(alpha = 0.15f)
                                                        else MaterialTheme.colorScheme.primaryContainer
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIcon(model.category),
                                                    contentDescription = null,
                                                    tint = if (model.isOverBudget) ExpenseRed
                                                            else if (model.isWarning) Color(0xFFFF9800)
                                                            else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = model.category.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                Spacer(modifier = Modifier.height(3.dp))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (model.recurringCommittedAmount > 0) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                                        ) {
                                                            Text(
                                                                text = "🔁 $currencySymbol%.2f/mo bill".format(model.recurringCommittedAmount),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    if (model.rolloverEnabled) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.surfaceVariant
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Autorenew,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(3.dp))
                                                                Text(
                                                                    text = "Rollover",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Spent vs Limit Display
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$currencySymbol%.2f".format(model.spentAmount),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (model.isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "of $currencySymbol%.2f limit".format(model.budgetLimit),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Row 2: Smooth Progress Bar + Percentage Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { model.percentage.coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp)),
                                            color = if (model.isOverBudget) ExpenseRed
                                            else if (model.isWarning) Color(0xFFFF9800)
                                            else IncomeGreen,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (model.isOverBudget) ExpenseRed.copy(alpha = 0.15f)
                                            else if (model.isWarning) Color(0xFFFF9800).copy(alpha = 0.15f)
                                            else IncomeGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "%.0f%%".format(model.percentage * 100f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (model.isOverBudget) ExpenseRed
                                                        else if (model.isWarning) Color(0xFFFF9800)
                                                        else IncomeGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Row 3: Bottom Metrics Container
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (model.pendingUpcomingAmount > 0)
                                                    "Spent: $currencySymbol%.2f  •  Pending: $currencySymbol%.2f".format(model.spentAmount, model.pendingUpcomingAmount)
                                                else
                                                    "Spent: $currencySymbol%.2f".format(model.spentAmount),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Text(
                                                text = if (model.isOverBudget)
                                                    "⚠️ Over by $currencySymbol%.2f".format((model.spentAmount + model.pendingUpcomingAmount) - model.budgetLimit)
                                                else
                                                    "$currencySymbol%.2f safe to spend".format(model.safeToSpendAmount),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (model.isOverBudget) ExpenseRed else IncomeGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }

        if (viewMode == 0) {
            if (showSetDialog) {
                SetBudgetDialog(
                    categories = expenseCategories,
                    initialCategoryId = selectedCategoryForEdit,
                    initialLimit = selectedLimitForEdit,
                    initialRolloverEnabled = selectedRolloverForEdit,
                    recurringList = recurringList,
                    onDismiss = { showSetDialog = false },
                    onConfirm = { categoryId, limit, rollover ->
                        onSetBudget(categoryId, limit, rollover)
                        showSetDialog = false
                    },
                    onDeleteBudget = { categoryId ->
                        onDeleteBudget(categoryId)
                        showSetDialog = false
                    },
                    onAddCustomCategory = onAddCustomCategory
                )
            }
        }
    }
}
