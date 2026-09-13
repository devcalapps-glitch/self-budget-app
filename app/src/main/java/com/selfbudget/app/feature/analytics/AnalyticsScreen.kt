package com.selfbudget.app.feature.analytics

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.selfbudget.app.core.ui.CategoryAnalyticsDetailModal
import com.selfbudget.app.core.ui.NetWorthHistoryModal
import com.selfbudget.app.core.ui.SavingsGoalsAnalyticsModal
import com.selfbudget.app.core.ui.components.DeltaBadge
import com.selfbudget.app.core.ui.components.DeltaMetric
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.getAccountIcon
import com.selfbudget.app.core.ui.getCategoryIcon
import com.selfbudget.app.core.ui.getExpenseCategoryGroup
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.sectionRamp
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.titleText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class AnalyticsTimeframe {
    MONTHLY, ANNUAL
}

@Composable
fun AnalyticsScreen(
    allTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    selectedMonthYear: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
    onPreviousMonth: (() -> Unit)? = null,
    onNextMonth: (() -> Unit)? = null,
    onSelectMonthYear: ((String) -> Unit)? = null,
    currencySymbol: String = "$",
    netWorthHistory: List<NetWorthSnapshotEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    accountBalances: Map<String, Double> = emptyMap(),
    goals: List<GoalEntity> = emptyList()
) {
    var selectedTimeframe by remember { mutableStateOf(AnalyticsTimeframe.MONTHLY) }
    var showNetWorthModal by remember { mutableStateOf(false) }
    var showIncomeDetailModal by remember { mutableStateOf(false) }
    var showExpenseDetailModal by remember { mutableStateOf(false) }
    var showDebtPayoffModal by remember { mutableStateOf(false) }
    var showGoalsModal by remember { mutableStateOf(false) }

    val sdfMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val sdfMonthName = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val sdfYear = remember { SimpleDateFormat("yyyy", Locale.getDefault()) }

    // Parse selected year string
    val selectedYearStr = remember(selectedMonthYear) {
        try {
            val date = sdfMonth.parse(selectedMonthYear)
            if (date != null) sdfYear.format(date) else sdfYear.format(Date())
        } catch (e: Exception) {
            sdfYear.format(Date())
        }
    }

    // Parse current month and previous month names
    val (currentMonthName, previousMonthName, previousMonthYearStr) = remember(selectedMonthYear) {
        val cal = Calendar.getInstance()
        var currentName = selectedMonthYear
        var prevName = "Previous Month"
        var prevYearStr = ""
        try {
            val date = sdfMonth.parse(selectedMonthYear)
            if (date != null) {
                cal.time = date
                currentName = sdfMonthName.format(cal.time)
                cal.add(Calendar.MONTH, -1)
                prevYearStr = sdfMonth.format(cal.time)
                prevName = sdfMonthName.format(cal.time)
            }
        } catch (e: Exception) {
            // fallback
        }
        Triple(currentName, prevName, prevYearStr)
    }

    // 1. Current Month & Previous Month Transactions (Expenses and Income)
    val currentMonthExpenseTxs = remember(allTransactions, categories, selectedMonthYear) {
        val catMap = categories.associateBy { it.id }
        allTransactions.filter { tx ->
            sdfMonth.format(Date(tx.timestamp)) == selectedMonthYear &&
            tx.type == TransactionType.EXPENSE &&
            (catMap[tx.categoryId]?.type == TransactionType.EXPENSE || catMap[tx.categoryId] == null)
        }
    }
    val currentMonthIncomeTxs = remember(allTransactions, categories, selectedMonthYear) {
        val catMap = categories.associateBy { it.id }
        allTransactions.filter { tx ->
            sdfMonth.format(Date(tx.timestamp)) == selectedMonthYear &&
            tx.type == TransactionType.INCOME &&
            (catMap[tx.categoryId]?.type == TransactionType.INCOME || catMap[tx.categoryId] == null)
        }
    }

    val previousMonthExpenseTxs = remember(allTransactions, categories, previousMonthYearStr) {
        val catMap = categories.associateBy { it.id }
        allTransactions.filter { tx ->
            sdfMonth.format(Date(tx.timestamp)) == previousMonthYearStr &&
            tx.type == TransactionType.EXPENSE &&
            (catMap[tx.categoryId]?.type == TransactionType.EXPENSE || catMap[tx.categoryId] == null)
        }
    }
    val previousMonthIncomeTxs = remember(allTransactions, categories, previousMonthYearStr) {
        val catMap = categories.associateBy { it.id }
        allTransactions.filter { tx ->
            sdfMonth.format(Date(tx.timestamp)) == previousMonthYearStr &&
            tx.type == TransactionType.INCOME &&
            (catMap[tx.categoryId]?.type == TransactionType.INCOME || catMap[tx.categoryId] == null)
        }
    }

    val totalMonthExpense = remember(currentMonthExpenseTxs) { currentMonthExpenseTxs.sumOf { it.amount } }
    val totalMonthIncome = remember(currentMonthIncomeTxs) { currentMonthIncomeTxs.sumOf { it.amount } }

    val prevTotalExpense = remember(previousMonthExpenseTxs) { previousMonthExpenseTxs.sumOf { it.amount } }
    val prevTotalIncome = remember(previousMonthIncomeTxs) { previousMonthIncomeTxs.sumOf { it.amount } }

    val diffAmountExpense = totalMonthExpense - prevTotalExpense
    val diffPercentExpense = if (prevTotalExpense > 0) ((diffAmountExpense / prevTotalExpense) * 100) else 0.0

    val diffAmountIncome = totalMonthIncome - prevTotalIncome
    val diffPercentIncome = if (prevTotalIncome > 0) ((diffAmountIncome / prevTotalIncome) * 100) else 0.0

    // 2. Annual (YTD) Transactions & Averages
    val annualExpenseTxs = remember(allTransactions, categories, selectedYearStr) {
        val catMap = categories.associateBy { it.id }
        allTransactions.filter { tx ->
            sdfYear.format(Date(tx.timestamp)) == selectedYearStr &&
            tx.type == TransactionType.EXPENSE &&
            (catMap[tx.categoryId]?.type == TransactionType.EXPENSE || catMap[tx.categoryId] == null)
        }
    }
    val annualIncomeTxs = remember(allTransactions, categories, selectedYearStr) {
        val catMap = categories.associateBy { it.id }
        allTransactions.filter { tx ->
            sdfYear.format(Date(tx.timestamp)) == selectedYearStr &&
            tx.type == TransactionType.INCOME &&
            (catMap[tx.categoryId]?.type == TransactionType.INCOME || catMap[tx.categoryId] == null)
        }
    }

    val totalAnnualExpense = remember(annualExpenseTxs) { Money.sum(annualExpenseTxs.map { it.amount }) }
    val totalAnnualIncome = remember(annualIncomeTxs) { Money.sum(annualIncomeTxs.map { it.amount }) }

    val monthsLoggedCount = remember(annualExpenseTxs, annualIncomeTxs) {
        (annualExpenseTxs + annualIncomeTxs).map { sdfMonth.format(Date(it.timestamp)) }.distinct().size.coerceAtLeast(1)
    }
    val monthlyAverageExpense = Money.round(totalAnnualExpense / monthsLoggedCount)
    val monthlyAverageIncome = Money.round(totalAnnualIncome / monthsLoggedCount)

    val activeTotalExpense = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) totalMonthExpense else totalAnnualExpense
    val activeTotalIncome = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) totalMonthIncome else totalAnnualIncome

    // 3. Debt Payoff Transactions & Totals
    val debtAccounts = remember(accounts) {
        accounts.filter { AccountBalanceCalculator.isLiability(it.type) }
    }
    val debtAccountIds = remember(debtAccounts) {
        debtAccounts.map { it.id }.toSet()
    }
    val currentMonthDebtPayoffTxs = remember(allTransactions, debtAccountIds, selectedMonthYear) {
        allTransactions.filter { tx ->
            sdfMonth.format(Date(tx.timestamp)) == selectedMonthYear &&
            ((tx.type == TransactionType.EXPENSE && tx.transferAccountId in debtAccountIds) ||
             (tx.type == TransactionType.TRANSFER && tx.transferAccountId in debtAccountIds))
        }
    }
    val annualDebtPayoffTxs = remember(allTransactions, debtAccountIds, selectedYearStr) {
        allTransactions.filter { tx ->
            sdfYear.format(Date(tx.timestamp)) == selectedYearStr &&
            ((tx.type == TransactionType.EXPENSE && tx.transferAccountId in debtAccountIds) ||
             (tx.type == TransactionType.TRANSFER && tx.transferAccountId in debtAccountIds))
        }
    }
    val totalMonthDebtPayoff = remember(currentMonthDebtPayoffTxs) { Money.sum(currentMonthDebtPayoffTxs.map { it.amount }) }
    val totalAnnualDebtPayoff = remember(annualDebtPayoffTxs) { Money.sum(annualDebtPayoffTxs.map { it.amount }) }

    val previousMonthDebtPayoffTxs = remember(allTransactions, debtAccountIds, previousMonthYearStr) {
        allTransactions.filter { tx ->
            sdfMonth.format(Date(tx.timestamp)) == previousMonthYearStr &&
            ((tx.type == TransactionType.EXPENSE && tx.transferAccountId in debtAccountIds) ||
             (tx.type == TransactionType.TRANSFER && tx.transferAccountId in debtAccountIds))
        }
    }
    val prevTotalDebtPayoff = remember(previousMonthDebtPayoffTxs) { Money.sum(previousMonthDebtPayoffTxs.map { it.amount }) }
    val diffAmountDebtPayoff = totalMonthDebtPayoff - prevTotalDebtPayoff
    val diffPercentDebtPayoff = if (prevTotalDebtPayoff > 0) ((diffAmountDebtPayoff / prevTotalDebtPayoff) * 100) else if (totalMonthDebtPayoff > 0) 100.0 else 0.0
    val monthlyAverageDebtPayoff = Money.round(totalAnnualDebtPayoff / monthsLoggedCount)

    val activeTotalDebtPayoff = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) totalMonthDebtPayoff else totalAnnualDebtPayoff

    // 5. Savings Goals Progress
    val goalCurrentAmounts = remember(goals, accounts, accountBalances) {
        goals.map { goal ->
            val linkedAccount = accounts.firstOrNull { it.id == goal.linkedAccountId }
            val accountAmount = linkedAccount?.let { accountBalances[it.id] ?: it.initialBalance } ?: 0.0
            accountAmount + goal.savedAmount
        }
    }
    val totalGoalsCount = goals.size
    val goalsMetCount = remember(goals, goalCurrentAmounts) {
        goals.indices.count { i -> goals[i].targetAmount > 0 && goalCurrentAmounts[i] >= goals[i].targetAmount }
    }
    val totalGoalsTarget = remember(goals) { goals.sumOf { it.targetAmount } }
    val totalGoalsSaved = remember(goalCurrentAmounts) { goalCurrentAmounts.sum() }
    val goalsOverallProgress = if (totalGoalsTarget > 0) (totalGoalsSaved / totalGoalsTarget).toFloat().coerceIn(0f, 1f) else 0f

    val activeExpenseTxs = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) currentMonthExpenseTxs else annualExpenseTxs
    val topExpenseCategories = remember(activeExpenseTxs, categories, activeTotalExpense) {
        val catMap = categories.associateBy { it.id }
        activeExpenseTxs
            .groupBy { it.categoryId }
            .map { (catId, txs) ->
                val sum = Money.sum(txs.map { it.amount })
                val cat = catMap[catId] ?: CategoryEntity(
                    id = catId,
                    name = "General / Other",
                    iconName = "MoreHoriz",
                    colorHex = "#64748B",
                    type = TransactionType.EXPENSE
                )
                val pct = if (activeTotalExpense > 0) (sum / activeTotalExpense).toFloat() else 0f
                Triple(cat, sum, pct)
            }
            .sortedByDescending { it.second }
            .take(4)
    }

    val activeIncomeTxs = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) currentMonthIncomeTxs else annualIncomeTxs
    val topIncomeCategories = remember(activeIncomeTxs, categories, activeTotalIncome) {
        val catMap = categories.associateBy { it.id }
        activeIncomeTxs
            .groupBy { it.categoryId }
            .map { (catId, txs) ->
                val sum = Money.sum(txs.map { it.amount })
                val cat = catMap[catId] ?: CategoryEntity(
                    id = catId,
                    name = "General Income",
                    iconName = "TrendingUp",
                    colorHex = "#10B981",
                    type = TransactionType.INCOME
                )
                val pct = if (activeTotalIncome > 0) (sum / activeTotalIncome).toFloat() else 0f
                Triple(cat, sum, pct)
            }
            .sortedByDescending { it.second }
            .take(4)
    }

    val activeDebtTxs = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) currentMonthDebtPayoffTxs else annualDebtPayoffTxs
    val topDebtPaidAccounts = remember(activeDebtTxs, debtAccounts, activeTotalDebtPayoff) {
        debtAccounts.map { acc ->
            val accTxs = activeDebtTxs.filter { it.transferAccountId == acc.id }
            val sum = Money.sum(accTxs.map { it.amount })
            val pct = if (activeTotalDebtPayoff > 0) (sum / activeTotalDebtPayoff).toFloat() else 0f
            Triple(acc, sum, pct)
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(4)
    }

    val isDarkScreen = isAppInDarkTheme()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Segmented Timeframe Toggle Pill (Monthly vs Annual YTD)
        item {
            Surface(
                shape = ShapePill,
                color = if (isDarkScreen) Ramp.Gray.c800 else Ramp.Gray.c50,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Monthly" to AnalyticsTimeframe.MONTHLY, "Annual" to AnalyticsTimeframe.ANNUAL).forEach { (label, timeframe) ->
                        val selected = selectedTimeframe == timeframe
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ShapePill)
                                .background(if (selected) Ramp.Teal.solidFill(isDarkScreen) else Color.Transparent)
                                .clickable { selectedTimeframe = timeframe },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = SelfBudgetType.rowTitle,
                                color = if (selected) Ramp.Teal.onSolidFill(isDarkScreen) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 1. Net Worth Summary Card (Top Card) — report band identity: Teal (spec §11)
        item {
            val totalAssets = remember(accounts, accountBalances) {
                accounts.filter { !AccountBalanceCalculator.isLiability(it.type) }
                    .sumOf { accountBalances[it.id] ?: it.initialBalance }
            }
            val totalDebts = remember(accounts, accountBalances) {
                accounts.filter { AccountBalanceCalculator.isLiability(it.type) }
                    .sumOf { kotlin.math.abs(accountBalances[it.id] ?: it.initialBalance) }
            }
            val liveNetWorth = totalAssets - totalDebts
            val latestSnapshot = netWorthHistory.lastOrNull()
            val earliestSnapshot = netWorthHistory.firstOrNull()
            val trendUp = (latestSnapshot?.netWorth ?: liveNetWorth) >= (earliestSnapshot?.netWorth ?: liveNetWorth)
            val trendRamp = if (trendUp) Ramp.Teal else Ramp.Red

            SectionHeaderBand(
                title = "Net worth",
                ramp = Ramp.Teal,
                icon = if (trendUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                trailingText = "View details ›",
                onTrailingClick = { showNetWorthModal = true },
                modifier = Modifier.clickable { showNetWorthModal = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$currencySymbol%.2f".format(liveNetWorth),
                        style = SelfBudgetType.display,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val sparklinePoints = remember(netWorthHistory, liveNetWorth) {
                        val points = netWorthHistory.map { it.netWorth }.toMutableList()
                        if (points.isEmpty()) points.add(liveNetWorth)
                        if (points.size == 1) points.add(0, points.first())
                        points
                    }
                    val sparklineStrokeColor = trendRamp.c400
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(ShapeChip)
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val minV = (sparklinePoints.minOrNull() ?: 0.0).coerceAtMost(0.0)
                            val maxV = (sparklinePoints.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                            val rangeV = (maxV - minV).coerceAtLeast(1.0)

                            val path = Path()
                            val areaPath = Path()
                            val stepX = w / (sparklinePoints.size - 1).coerceAtLeast(1)

                            sparklinePoints.forEachIndexed { i, valPt ->
                                val x = i * stepX
                                val normY = (valPt - minV) / rangeV
                                val y = h - (normY * (h - 6f) + 3f).toFloat()
                                if (i == 0) {
                                    path.moveTo(x, y)
                                    areaPath.moveTo(x, h)
                                    areaPath.lineTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                    areaPath.lineTo(x, y)
                                }
                            }
                            areaPath.lineTo(w, h)
                            areaPath.close()

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(sparklineStrokeColor.copy(alpha = 0.35f), sparklineStrokeColor.copy(alpha = 0.05f))
                                )
                            )
                            drawPath(
                                path = path,
                                color = sparklineStrokeColor,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (netWorthHistory.size > 1) {
                            "Tracking ${netWorthHistory.size} month(s) • Tap to view interactive progress chart & asset split"
                        } else {
                            "Tap to view interactive progress chart & asset split"
                        },
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1b. Savings Goals Summary Card — report band identity: Amber (spec §11)
        item {
            SectionHeaderBand(
                title = "Savings goals",
                ramp = Ramp.Amber,
                icon = Icons.Default.Savings,
                trailingText = "View details ›",
                onTrailingClick = { showGoalsModal = true },
                modifier = Modifier.clickable { showGoalsModal = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (totalGoalsCount == 0) {
                        Text(
                            text = "Set a savings goal from the Plan tab to track it here.",
                            style = SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "$currencySymbol%.2f of $currencySymbol%.2f".format(totalGoalsSaved, totalGoalsTarget),
                            style = SelfBudgetType.display,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { goalsOverallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(ShapeChip),
                            color = Ramp.Teal.c400,
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Active goals",
                                    style = SelfBudgetType.meta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalGoalsCount",
                                    style = SelfBudgetType.title,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Goals met",
                                    style = SelfBudgetType.meta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$goalsMetCount / $totalGoalsCount",
                                    style = SelfBudgetType.title,
                                    color = if (goalsMetCount > 0) Ramp.Teal.let { if (isDarkScreen) it.c100 else it.c600 } else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Total Expense Hero Card — report band identity: Coral (spec §11)
        item {
            SectionHeaderBand(
                title = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Monthly spending" else "Annual spending",
                ramp = Ramp.Coral,
                icon = Icons.Default.ArrowDownward,
                trailingText = "View details ›",
                onTrailingClick = { showExpenseDetailModal = true },
                modifier = Modifier.clickable { showExpenseDetailModal = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$currencySymbol%.2f".format(activeTotalExpense),
                        style = SelfBudgetType.display,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (selectedTimeframe == AnalyticsTimeframe.ANNUAL) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol%.2f / month average across $monthsLoggedCount months".format(monthlyAverageExpense),
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "vs last month: $currencySymbol%.2f".format(prevTotalExpense),
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DeltaBadge(percentChange = diffPercentExpense.toFloat(), metric = DeltaMetric.SPENDING)
                        }
                    } else {
                        Text(
                            text = "Tracking $monthsLoggedCount month(s) of expense history in $selectedYearStr.",
                            style = SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (topExpenseCategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "TOP EXPENSE CATEGORIES",
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            topExpenseCategories.forEach { (category, amount, pct) ->
                                val catRamp = sectionRamp(getExpenseCategoryGroup(category))
                                BreakdownRow(
                                    icon = getCategoryIcon(category),
                                    ramp = catRamp,
                                    name = category.name,
                                    amountText = "$currencySymbol%.2f".format(amount),
                                    pct = pct
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Total Income Hero Card — report band identity: Teal (spec §11)
        item {
            SectionHeaderBand(
                title = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Monthly income" else "Annual income",
                ramp = Ramp.Teal,
                icon = Icons.Default.ArrowUpward,
                trailingText = "View details ›",
                onTrailingClick = { showIncomeDetailModal = true },
                modifier = Modifier.clickable { showIncomeDetailModal = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$currencySymbol%.2f".format(activeTotalIncome),
                        style = SelfBudgetType.display,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (selectedTimeframe == AnalyticsTimeframe.ANNUAL) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol%.2f / month average earned".format(monthlyAverageIncome),
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "vs last month: $currencySymbol%.2f".format(prevTotalIncome),
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DeltaBadge(percentChange = diffPercentIncome.toFloat(), metric = DeltaMetric.INCOME)
                        }
                    } else {
                        Text(
                            text = "Total YTD income earned across $monthsLoggedCount month(s) in $selectedYearStr.",
                            style = SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (topIncomeCategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "TOP INCOME SOURCES",
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            topIncomeCategories.forEach { (category, amount, pct) ->
                                BreakdownRow(
                                    icon = getCategoryIcon(category),
                                    ramp = Ramp.Teal,
                                    name = category.name,
                                    amountText = "$currencySymbol%.2f".format(amount),
                                    pct = pct
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Total Debt Payoff Hero Card — report band identity: Purple (spec §11)
        item {
            SectionHeaderBand(
                title = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Monthly debt payoff" else "Annual debt payoff",
                ramp = Ramp.Purple,
                icon = Icons.Default.Payments,
                trailingText = "View details ›",
                onTrailingClick = { showDebtPayoffModal = true },
                modifier = Modifier.clickable { showDebtPayoffModal = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$currencySymbol%.2f".format(activeTotalDebtPayoff),
                        style = SelfBudgetType.display,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (selectedTimeframe == AnalyticsTimeframe.ANNUAL) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol%.2f / month average paid down".format(monthlyAverageDebtPayoff),
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "vs last month: $currencySymbol%.2f".format(prevTotalDebtPayoff),
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DeltaBadge(percentChange = diffPercentDebtPayoff.toFloat(), metric = DeltaMetric.DEBT_PAYOFF)
                        }
                    } else {
                        Text(
                            text = "Total debt eliminated YTD across $monthsLoggedCount month(s) in $selectedYearStr.",
                            style = SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (topDebtPaidAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "DEBT PAYOFF BREAKDOWN",
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            topDebtPaidAccounts.forEach { (account, amount, pct) ->
                                BreakdownRow(
                                    icon = getAccountIcon(account.type),
                                    ramp = Ramp.Purple,
                                    name = account.name,
                                    amountText = "$currencySymbol%.2f".format(amount),
                                    pct = pct
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showNetWorthModal) {
        NetWorthHistoryModal(
            history = netWorthHistory,
            accounts = accounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            onDismiss = { showNetWorthModal = false }
        )
    }

    if (showIncomeDetailModal) {
        CategoryAnalyticsDetailModal(
            title = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Income Analytics" else "Annual Income Analytics",
            subtitle = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) currentMonthName else "$selectedYearStr YTD",
            transactionType = TransactionType.INCOME,
            timeframe = selectedTimeframe,
            periodLabel = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) selectedMonthYear else selectedYearStr,
            allTransactions = allTransactions,
            categories = categories,
            currencySymbol = currencySymbol,
            onDismiss = { showIncomeDetailModal = false }
        )
    }

    if (showExpenseDetailModal) {
        CategoryAnalyticsDetailModal(
            title = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Expense Analytics" else "Annual Expense Analytics",
            subtitle = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) currentMonthName else "$selectedYearStr YTD",
            transactionType = TransactionType.EXPENSE,
            timeframe = selectedTimeframe,
            periodLabel = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) selectedMonthYear else selectedYearStr,
            allTransactions = allTransactions,
            categories = categories,
            currencySymbol = currencySymbol,
            onDismiss = { showExpenseDetailModal = false }
        )
    }

    if (showDebtPayoffModal) {
        com.selfbudget.app.core.ui.DebtPayoffAnalyticsModal(
            title = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Debt Payoff Analytics" else "Annual Debt Payoff Analytics",
            subtitle = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) currentMonthName else "$selectedYearStr YTD",
            timeframe = selectedTimeframe,
            periodLabel = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) selectedMonthYear else selectedYearStr,
            allTransactions = allTransactions,
            accounts = accounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            onDismiss = { showDebtPayoffModal = false }
        )
    }

    if (showGoalsModal) {
        SavingsGoalsAnalyticsModal(
            goals = goals,
            accounts = accounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            onDismiss = { showGoalsModal = false }
        )
    }
}

/**
 * A ranking/breakdown row (spec §15): icon tile + bar in the category's ramp,
 * but the % share badge is always NEUTRAL gray — per-category colored
 * percentages make the column unreadable. Sub-1% shares read as "<1%".
 */
@Composable
private fun BreakdownRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    ramp: Ramp,
    name: String,
    amountText: String,
    pct: Float
) {
    val isDark = isAppInDarkTheme()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                RampIconTile(icon = icon, ramp = ramp, size = 28.dp, iconSize = 15.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = name,
                    style = SelfBudgetType.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = amountText,
                    style = SelfBudgetType.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                NeutralBadge(text = formatSharePercent(pct))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { pct.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(ShapeChip),
            color = ramp.c400,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun formatSharePercent(pct: Float): String {
    if (pct in 0f..0.009999f && pct > 0f) return "<1%"
    return "${(pct * 100f).roundToInt()}%"
}
