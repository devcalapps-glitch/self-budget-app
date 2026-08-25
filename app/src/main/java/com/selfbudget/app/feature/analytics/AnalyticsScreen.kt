package com.selfbudget.app.feature.analytics

import com.selfbudget.app.core.util.Money

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfbudget.app.core.ui.NetWorthHistoryModal
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AnalyticsTimeframe {
    MONTHLY, ANNUAL
}

data class CategorySpending(
    val category: CategoryEntity,
    val totalAmount: Double,
    val percentage: Float
)

@Composable
fun AnalyticsScreen(
    allTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    selectedMonthYear: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
    currencySymbol: String = "$",
    netWorthHistory: List<NetWorthSnapshotEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    accountBalances: Map<String, Double> = emptyMap()
) {
    var selectedTimeframe by remember { mutableStateOf(AnalyticsTimeframe.MONTHLY) }
    var showNetWorthModal by remember { mutableStateOf(false) }

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

    // 1. Current Month & Previous Month Transactions
    val currentMonthTxs = remember(allTransactions, selectedMonthYear) {
        allTransactions.filter { tx ->
            sdfMonth.format(Date(tx.timestamp)) == selectedMonthYear && tx.type == TransactionType.EXPENSE
        }
    }
    val previousMonthTxs = remember(allTransactions, previousMonthYearStr) {
        allTransactions.filter { tx ->
            sdfMonth.format(Date(tx.timestamp)) == previousMonthYearStr && tx.type == TransactionType.EXPENSE
        }
    }

    val totalMonthExpense = remember(currentMonthTxs) { currentMonthTxs.sumOf { it.amount } }
    val prevTotalExpense = remember(previousMonthTxs) { previousMonthTxs.sumOf { it.amount } }

    val diffAmount = totalMonthExpense - prevTotalExpense
    val diffPercent = if (prevTotalExpense > 0) ((diffAmount / prevTotalExpense) * 100) else 0.0

    // 2. Annual (YTD) Transactions & Averages
    val annualTxs = remember(allTransactions, selectedYearStr) {
        allTransactions.filter { tx ->
            sdfYear.format(Date(tx.timestamp)) == selectedYearStr && tx.type == TransactionType.EXPENSE
        }
    }
    val totalAnnualExpense = remember(annualTxs) { Money.sum(annualTxs.map { it.amount }) }
    val monthsLoggedCount = remember(annualTxs) {
        annualTxs.map { sdfMonth.format(Date(it.timestamp)) }.distinct().size.coerceAtLeast(1)
    }
    val monthlyAverageExpense = Money.round(totalAnnualExpense / monthsLoggedCount)

    // Issue 10: Days-elapsed based annual spending pace extrapolation
    val annualSpendingPace = remember(totalAnnualExpense) {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR).coerceAtLeast(1)
        val daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
        Money.round((totalAnnualExpense / dayOfYear) * daysInYear)
    }

    // Active timeframe dataset
    val activeTxs = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) currentMonthTxs else annualTxs
    val activeTotalExpense = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) totalMonthExpense else totalAnnualExpense

    // Category Spending Breakdown for active timeframe
    val categorySpendings = remember(activeTxs, categories, activeTotalExpense) {
        val categoryMap = categories.associateBy { it.id }
        activeTxs
            .groupBy { it.categoryId }
            .map { (catId, txs) ->
                val sum = txs.sumOf { it.amount }
                val cat = categoryMap[catId] ?: CategoryEntity(
                    catId, "Unknown", "MoreHoriz", "#607D8B", TransactionType.EXPENSE
                )
                val pct = if (activeTotalExpense > 0) (sum / activeTotalExpense).toFloat() else 0f
                CategorySpending(cat, sum, pct)
            }
            .sortedByDescending { it.totalAmount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        // Total Expense Overview Card - hero total, plus timeframe-specific context folded into
        // the same card (previously a separate "Month-over-Month"/"Annual Pace" card duplicated
        // the total/average numbers already shown here).
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Total Spending ($currentMonthName)" else "Total Annual Spending ($selectedYearStr YTD)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol%.2f".format(activeTotalExpense),
                        style = MaterialTheme.typography.headlineMedium,
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
                                    imageVector = Icons.Default.CompareArrows,
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

                            // Percentage Change Pill / Badge
                            val isSavedMoney = diffAmount <= 0
                            val badgeColor = if (isSavedMoney) IncomeGreen else ExpenseRed
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(badgeColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSavedMoney) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "%s%.1f%%".format(if (diffAmount > 0) "+" else "", diffPercent),
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

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (diffAmount <= 0) {
                                "🎉 Great job! You spent $currencySymbol%.2f less than last month.".format(Math.abs(diffAmount))
                            } else {
                                "⚠️ Notice: You spent $currencySymbol%.2f more than $previousMonthName.".format(diffAmount)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (diffAmount <= 0) IncomeGreen else ExpenseRed,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Tracking $monthsLoggedCount month(s) of expense history in $selectedYearStr.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Net Worth Summary Card — Tapping opens NetWorthHistoryModal
        item {
            val totalAssets = remember(accounts, accountBalances) {
                accounts.filter {
                    it.type == AccountType.CHECKING ||
                    it.type == AccountType.SAVINGS ||
                    it.type == AccountType.CASH ||
                    it.type == AccountType.INVESTMENT
                }.sumOf { accountBalances[it.id] ?: it.initialBalance }
            }
            val totalDebts = remember(accounts, accountBalances) {
                accounts.filter {
                    it.type == AccountType.CREDIT_CARD ||
                    it.type == AccountType.LOAN
                }.sumOf { accountBalances[it.id] ?: it.initialBalance }
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
                                imageVector = if (trendUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (trendUp) IncomeGreen else ExpenseRed,
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (netWorthHistory.size > 1) {
                            "Tracking ${netWorthHistory.size} month(s) • Tap to view monthly breakdown & asset split"
                        } else {
                            "Tap to view detailed asset & debt breakdown"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Category Breakdown ($currentMonthName)" else "Category Breakdown ($selectedYearStr YTD)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (categorySpendings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "No Spending Analytics Available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (selectedTimeframe == AnalyticsTimeframe.MONTHLY) "Log expense entries for $currentMonthName to generate category pie charts and month-over-month trends." else "Log expense entries in $selectedYearStr to generate annual YTD category breakdowns.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        } else {
            items(categorySpendings) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val catColor = try {
                                    Color(android.graphics.Color.parseColor(item.category.colorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.category.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }

                            Text(
                                text = "$currencySymbol%.2f (%.1f%%)".format(item.totalAmount, item.percentage * 100),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val catColor = try {
                            Color(android.graphics.Color.parseColor(item.category.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        LinearProgressIndicator(
                            progress = { item.percentage.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = catColor,
                            trackColor = catColor.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(150.dp))
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
}
