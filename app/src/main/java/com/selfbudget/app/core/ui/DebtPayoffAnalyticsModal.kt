package com.selfbudget.app.core.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.TrendingDown
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
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.feature.analytics.AnalyticsTimeframe
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.getProgressBarColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.ui.theme.tintFill
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

    // Report identity: Debt payoff = Purple (spec §11).
    val ramp = Ramp.Purple
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = title,
                                style = SelfBudgetType.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    style = SelfBudgetType.meta,
                                    color = ramp.secondaryText(isDark),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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
                    // 1. Total Debt Payoff Hero Summary — neutral display number (spec §11).
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RampIconTile(icon = Icons.Default.TrendingDown, ramp = ramp, size = 36.dp, iconSize = 20.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Debt eliminated",
                                            style = SelfBudgetType.heading,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Surface(shape = ShapePill, color = ramp.tintFill(isDark)) {
                                        Text(
                                            text = if (timeframe == AnalyticsTimeframe.MONTHLY) "Monthly paydown" else "YTD paydown",
                                            style = SelfBudgetType.badge,
                                            color = ramp.titleText(isDark),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "$currencySymbol%.2f".format(totalPaidOffInPeriod),
                                    style = SelfBudgetType.display,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Remaining debt balance",
                                        style = SelfBudgetType.meta,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$currencySymbol%.2f".format(totalRemainingDebt),
                                        style = SelfBudgetType.rowTitle,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 2. Annual Monthly Breakdown Bar Chart (Only visible in Annual mode)
                    if (timeframe == AnalyticsTimeframe.ANNUAL) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "$periodLabel monthly payoff history",
                                        style = SelfBudgetType.heading,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
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
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.55f)
                                                        .height((barFraction * 90).dp.coerceAtLeast(4.dp))
                                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                        .background(if (mVal > 0) ramp.c400 else MaterialTheme.colorScheme.outlineVariant)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = monthLabel,
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

                    // 3. Debt Account Breakdown Section Header
                    item {
                        Text(
                            text = "Debt accounts (${debtAccountDetails.size})",
                            style = SelfBudgetType.heading,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 4. Debt Account Detail List Items
                    if (debtAccountDetails.isEmpty()) {
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
                                    RampIconTile(icon = Icons.Default.CheckCircle, ramp = Ramp.Teal, size = 56.dp, iconSize = 28.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No debt accounts logged",
                                        style = SelfBudgetType.heading,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add a credit card or loan account to track payoff progress.",
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
                                    debtAccountDetails.forEachIndexed { index, detail ->
                                        val acc = detail.account
                                        val icon = when (acc.type) {
                                            AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                            else -> Icons.Default.AccountBalance
                                        }

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
                                                    RampIconTile(icon = icon, ramp = ramp, size = 36.dp, iconSize = 18.dp)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = acc.name,
                                                            style = SelfBudgetType.rowTitle,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "${detail.paymentCount} payment(s) • Remaining: $currencySymbol%.2f".format(detail.currentRemainingBalance),
                                                            style = SelfBudgetType.meta,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "$currencySymbol%.2f".format(detail.totalPaidOff),
                                                        style = SelfBudgetType.rowTitle,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    NeutralBadge(text = "${(detail.percentageOfTotal * 100).toInt()}% of total")
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                             LinearProgressIndicator(
                                                 progress = { detail.percentageOfTotal.coerceIn(0f, 1f) },
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .height(6.dp)
                                                     .clip(ShapeChip),
                                                 color = getProgressBarColor(ramp.c400),
                                                 trackColor = MaterialTheme.colorScheme.surfaceVariant
                                             )
                                        }

                                        if (index < debtAccountDetails.size - 1) {
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
