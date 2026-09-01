package com.selfbudget.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.feature.analytics.AnalyticsTimeframe
import com.selfbudget.app.ui.theme.getIncomeColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class DebtAccountDetailItem(
    val account: AccountEntity,
    val totalPaidOff: Double,
    val percentageOfTotal: Float,
    val paymentCount: Int,
    val currentRemainingBalance: Double
)

@Composable
fun DebtPayoffAnalyticsModal(
    title: String = "Debt Payoff Analytics",
    subtitle: String = "",
    timeframe: AnalyticsTimeframe,
    periodLabel: String,
    allTransactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double> = emptyMap(),
    currencySymbol: String = "$",
    onDismiss: () -> Unit
) {
    val sdfMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val sdfYear = remember { SimpleDateFormat("yyyy", Locale.getDefault()) }
    val sdfMonthShort = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    val debtAccounts = remember(accounts) {
        accounts.filter { AccountBalanceCalculator.isLiability(it.type) }
    }
    val debtAccountIds = remember(debtAccounts) {
        debtAccounts.map { it.id }.toSet()
    }

    // Filter debt payoff transactions (payments/transfers targeting debt accounts)
    val debtPayoffTxs = remember(allTransactions, debtAccountIds) {
        allTransactions.filter { tx ->
            (tx.type == TransactionType.EXPENSE && tx.transferAccountId in debtAccountIds) ||
            (tx.type == TransactionType.TRANSFER && tx.transferAccountId in debtAccountIds)
        }
    }

    // Filter by timeframe
    val activeTxs = remember(debtPayoffTxs, timeframe, periodLabel) {
        if (timeframe == AnalyticsTimeframe.MONTHLY) {
            debtPayoffTxs.filter { sdfMonth.format(Date(it.timestamp)) == periodLabel }
        } else {
            debtPayoffTxs.filter { sdfYear.format(Date(it.timestamp)) == periodLabel }
        }
    }

    val totalPaidOffInPeriod = remember(activeTxs) { Money.sum(activeTxs.map { it.amount }) }

    val totalRemainingDebt = remember(debtAccounts, accountBalances) {
        debtAccounts.sumOf { acc ->
            kotlin.math.abs(accountBalances[acc.id] ?: acc.initialBalance)
        }
    }

    val debtAccountDetails = remember(activeTxs, debtAccounts, accountBalances, totalPaidOffInPeriod) {
        debtAccounts.map { acc ->
            val accTxs = activeTxs.filter { it.transferAccountId == acc.id }
            val paidOff = Money.sum(accTxs.map { it.amount })
            val pct = if (totalPaidOffInPeriod > 0) (paidOff / totalPaidOffInPeriod).toFloat() else 0f
            val remBal = kotlin.math.abs(accountBalances[acc.id] ?: acc.initialBalance)
            DebtAccountDetailItem(acc, paidOff, pct, accTxs.size, remBal)
        }.sortedByDescending { it.totalPaidOff }
    }

    // Monthly breakdown data for annual view (Jan - Dec totals)
    val monthlyTotals = remember(debtPayoffTxs, timeframe, periodLabel) {
        val result = FloatArray(12) { 0f }
        if (timeframe == AnalyticsTimeframe.ANNUAL) {
            val cal = Calendar.getInstance()
            debtPayoffTxs.forEach { tx ->
                val d = Date(tx.timestamp)
                if (sdfYear.format(d) == periodLabel) {
                    cal.time = d
                    val monthIdx = cal.get(Calendar.MONTH)
                    if (monthIdx in 0..11) {
                        result[monthIdx] += tx.amount.toFloat()
                    }
                }
            }
        }
        result
    }
    val maxMonthlyTotal = remember(monthlyTotals) { monthlyTotals.maxOrNull()?.coerceAtLeast(1f) ?: 1f }

    val themeAccentColor = getIncomeColor()

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
                // Persistent Header
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Total Debt Payoff Hero Summary Banner Card
                    item {
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = themeAccentColor.copy(alpha = 0.15f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingDown,
                                                    contentDescription = null,
                                                    tint = themeAccentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Debt Eliminated",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = themeAccentColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (timeframe == AnalyticsTimeframe.MONTHLY) "Monthly Paydown" else "YTD Paydown",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = themeAccentColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "$currencySymbol%.2f".format(totalPaidOffInPeriod),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = themeAccentColor
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Remaining Debt Balance:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$currencySymbol%.2f".format(totalRemainingDebt),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Annual Monthly Breakdown Bar Chart (Only visible in Annual mode)
                    if (timeframe == AnalyticsTimeframe.ANNUAL) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "$periodLabel Monthly Payoff History",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        val cal = Calendar.getInstance()
                                        for (m in 0..11) {
                                            cal.set(Calendar.MONTH, m)
                                            val monthLabel = sdfMonthShort.format(cal.time)
                                            val mVal = monthlyTotals[m]
                                            val barFraction = if (maxMonthlyTotal > 0) (mVal / maxMonthlyTotal).coerceIn(0f, 1f) else 0f

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Bottom,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (mVal > 0) {
                                                    Text(
                                                        text = "$currencySymbol%.0f".format(mVal),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = themeAccentColor
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.55f)
                                                        .height((barFraction * 75).dp.coerceAtLeast(4.dp))
                                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                        .background(if (mVal > 0) themeAccentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = monthLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Debt Account Breakdown Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Debt Accounts (${debtAccountDetails.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 4. Debt Account Detail List Items
                    if (debtAccountDetails.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = themeAccentColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No Debt Accounts Logged",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add a Credit Card or Loan account to track payoff progress.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(debtAccountDetails) { detail ->
                            val acc = detail.account
                            val accColor = try { Color(android.graphics.Color.parseColor(acc.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                            val icon = when (acc.type) {
                                AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                else -> Icons.Default.AccountBalance
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                                            Surface(
                                                shape = CircleShape,
                                                color = accColor.copy(alpha = 0.2f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = accColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${detail.paymentCount} payment(s) • Remaining: $currencySymbol%.2f".format(detail.currentRemainingBalance),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$currencySymbol%.2f".format(detail.totalPaidOff),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = themeAccentColor
                                            )
                                            Text(
                                                text = "%.1f%% of total".format(detail.percentageOfTotal * 100),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { detail.percentageOfTotal.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = themeAccentColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
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
