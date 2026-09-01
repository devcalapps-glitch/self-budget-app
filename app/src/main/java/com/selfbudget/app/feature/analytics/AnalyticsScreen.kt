package com.selfbudget.app.feature.analytics

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfbudget.app.core.ui.CategoryAnalyticsDetailModal
import com.selfbudget.app.core.ui.MonthYearHeader
import com.selfbudget.app.core.ui.NetWorthHistoryModal
import com.selfbudget.app.core.ui.SavingsGoalsAnalyticsModal
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onPreviousMonth != null && onNextMonth != null && onSelectMonthYear != null) {
            item {
                MonthYearHeader(
                    currentMonthYear = selectedMonthYear,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectMonthYear = onSelectMonthYear
                )
            }
        }

        // Segmented Timeframe Toggle Pill (Monthly vs Annual YTD)
        item {
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
                                if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                            .clickable { selectedTimeframe = AnalyticsTimeframe.MONTHLY },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Monthly ($currentMonthName)",
                            fontWeight = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedTimeframe == AnalyticsTimeframe.ANNUAL) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                            .clickable { selectedTimeframe = AnalyticsTimeframe.ANNUAL },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Annual ($selectedYearStr YTD)",
                            fontWeight = if (selectedTimeframe == AnalyticsTimeframe.ANNUAL) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTimeframe == AnalyticsTimeframe.ANNUAL) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 1. Net Worth Summary Card (Top Card)
        item {
            val totalAssets = remember(accounts, accountBalances) {
                accounts.filter {
                    it.type == AccountType.CHECKING ||
                    it.type == AccountType.SAVINGS ||
                    it.type == AccountType.CASH ||
                    it.type == AccountType.INVESTMENT ||
                    it.type == AccountType.RETIREMENT
                }.sumOf { accountBalances[it.id] ?: it.initialBalance }
            }
            val totalDebts = remember(accounts, accountBalances) {
                accounts.filter {
                    it.type == AccountType.CREDIT_CARD ||
                    it.type == AccountType.LOAN
                }.sumOf { kotlin.math.abs(accountBalances[it.id] ?: it.initialBalance) }
            }
            val liveNetWorth = totalAssets - totalDebts
            val latestSnapshot = netWorthHistory.lastOrNull()
            val earliestSnapshot = netWorthHistory.firstOrNull()
            val trendUp = (latestSnapshot?.netWorth ?: liveNetWorth) >= (earliestSnapshot?.netWorth ?: liveNetWorth)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNetWorthModal = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (trendUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (trendUp) getIncomeColor() else getExpenseColor(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Net Worth",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View Details",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$currencySymbol%.2f".format(liveNetWorth),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val sparklinePoints = remember(netWorthHistory, liveNetWorth) {
                        val points = netWorthHistory.map { it.netWorth }.toMutableList()
                        if (points.isEmpty()) points.add(liveNetWorth)
                        if (points.size == 1) points.add(0, points.first())
                        points
                    }
                    val sparklineStrokeColor = if (trendUp) getIncomeColor() else getExpenseColor()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
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

                            val strokeColor = sparklineStrokeColor

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(strokeColor.copy(alpha = 0.35f), strokeColor.copy(alpha = 0.05f))
                                )
                            )
                            drawPath(
                                path = path,
                                color = strokeColor,
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1b. Savings Goals Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGoalsModal = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Savings Goals",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View Details",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (totalGoalsCount == 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No savings goals yet. Set one from the Plan tab to track progress here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol%.2f of $currencySymbol%.2f".format(totalGoalsSaved, totalGoalsTarget),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { goalsOverallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = getIncomeColor()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Active Goals",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalGoalsCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Goals Met",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$goalsMetCount / $totalGoalsCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = getIncomeColor()
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Total Expense Hero Card (Clickable to view Expense Category details)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showExpenseDetailModal = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Total Spending ($currentMonthName)" else "Total Annual Spending ($selectedYearStr YTD)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View Details",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol%.2f".format(activeTotalExpense),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (selectedTimeframe == AnalyticsTimeframe.ANNUAL) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$currencySymbol%.2f / month average across $monthsLoggedCount months".format(monthlyAverageExpense),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "vs. Last Month",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val isSavedMoney = diffAmountExpense <= 0
                            val badgeColor = if (isSavedMoney) getIncomeColor() else getExpenseColor()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(badgeColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSavedMoney) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "%s%.1f%%".format(if (diffAmountExpense > 0) "+" else "", diffPercentExpense),
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "$previousMonthName: $currencySymbol%.2f".format(prevTotalExpense),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Tracking $monthsLoggedCount month(s) of expense history in $selectedYearStr. Tap to view category breakdowns.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. Total Income Hero Card (Clickable to view Income Category details)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showIncomeDetailModal = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Total Income ($currentMonthName)" else "Total Annual Income ($selectedYearStr YTD)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View Details",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol%.2f".format(activeTotalIncome),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = getIncomeColor()
                    )

                    if (selectedTimeframe == AnalyticsTimeframe.ANNUAL) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$currencySymbol%.2f / month average earned".format(monthlyAverageIncome),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = getIncomeColor(),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "vs. Last Month Income",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val isHigherIncome = diffAmountIncome >= 0
                            val badgeColor = if (isHigherIncome) getIncomeColor() else getExpenseColor()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(badgeColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isHigherIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "%s%.1f%%".format(if (diffAmountIncome > 0) "+" else "", diffPercentIncome),
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "$previousMonthName: $currencySymbol%.2f".format(prevTotalIncome),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Total YTD income earned across $monthsLoggedCount month(s) in $selectedYearStr. Tap to view category breakdowns.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 4. Total Debt Payoff Hero Card (Clickable to view Debt Payoff details)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDebtPayoffModal = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Debt Payoff ($currentMonthName)" else "Annual Debt Payoff ($selectedYearStr YTD)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View Details",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol%.2f".format(activeTotalDebtPayoff),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = getIncomeColor()
                    )

                    if (selectedTimeframe == AnalyticsTimeframe.ANNUAL) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$currencySymbol%.2f / month average paid down".format(monthlyAverageDebtPayoff),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = getIncomeColor(),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "vs. Last Month Payoff",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val isHigherPayoff = diffAmountDebtPayoff >= 0
                            val badgeColor = if (isHigherPayoff) getIncomeColor() else getExpenseColor()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(badgeColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isHigherPayoff) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "%s%.1f%%".format(if (diffAmountDebtPayoff > 0) "+" else "", diffPercentDebtPayoff),
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "$previousMonthName: $currencySymbol%.2f".format(prevTotalDebtPayoff),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Total debt eliminated YTD across $monthsLoggedCount month(s) in $selectedYearStr. Tap to view payoff breakdown by debt account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
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
