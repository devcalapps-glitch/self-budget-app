package com.selfbudget.app.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.GrayIconTile
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Dedicated full-screen modal showing a comprehensive breakdown of Net Worth:
 * 1. Live Net Worth Hero Summary
 * 2. Assets vs. Liabilities breakdown cards & account lists
 * 3. Month-by-Month historical accumulation table with monthly change indicators
 */
@Composable
fun NetWorthHistoryModal(
    history: List<NetWorthSnapshotEntity>,
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit
) {
    val sdfMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val sdfLabel = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }

    // Group accounts into Assets and Liabilities - via the same canonical classification
    // (AccountBalanceCalculator.isLiability) used to compute uiState.netWorth and the persisted
    // monthly snapshots, not a separate hardcoded account-type list. That list used to omit
    // Mortgage/Auto Loan/Student Loan/Real Estate/Vehicle entirely (neither asset nor debt),
    // which is exactly what made this screen's total disagree with Analytics and Accounts.
    val assetAccounts = remember(accounts) {
        accounts.filterNot { AccountBalanceCalculator.isLiability(it.type) }
    }
    val debtAccounts = remember(accounts) {
        accounts.filter { AccountBalanceCalculator.isLiability(it.type) }
    }

    val totalAssets = remember(assetAccounts, accountBalances) {
        assetAccounts.sumOf { acc -> accountBalances[acc.id] ?: acc.initialBalance }
    }
    val totalDebts = remember(debtAccounts, accountBalances) {
        debtAccounts.sumOf { acc -> kotlin.math.abs(accountBalances[acc.id] ?: acc.initialBalance) }
    }
    val currentNetWorth = totalAssets - totalDebts

    val isDark = isAppInDarkTheme()

    var showAssetBreakdown by remember { mutableStateOf(false) }
    var showDebtBreakdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Top App Bar — ✕ and title only (spec §14).
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Net worth details & history",
                            style = SelfBudgetType.heading,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 48.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Financial Progress & Breakdown Graph — the one place this screen shows the
                    // net worth figure (spec §14: never duplicate the same headline number in two
                    // cards). Its own header already carries the number, the vs-last-month delta,
                    // and the sparkline, so a separate static hero card above it was pure repeat.
                    NetWorthProgressChart(
                        history = history,
                        currentNetWorth = currentNetWorth,
                        totalAssets = totalAssets,
                        totalDebts = totalDebts,
                        currencySymbol = currencySymbol
                    )

                    // 2b. Assets vs. Liabilities Split Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AssetDebtTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.AccountBalance,
                            label = "Total assets",
                            amountText = "$currencySymbol%.2f".format(totalAssets),
                            caption = "${assetAccounts.size} account(s)",
                            ramp = Ramp.Teal,
                            isDark = isDark,
                            onClick = { showAssetBreakdown = true }
                        )
                        AssetDebtTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CreditCard,
                            label = "Total debt",
                            amountText = "$currencySymbol%.2f".format(totalDebts),
                            caption = "${debtAccounts.size} account(s)",
                            ramp = Ramp.Red,
                            isDark = isDark,
                            onClick = { showDebtBreakdown = true }
                        )
                    }

                    // 3. Month-by-Month Accumulation Table
                    Text(
                        text = "Monthly growth history",
                        style = SelfBudgetType.heading,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (history.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Text(
                                text = "Monthly snapshot history will accumulate automatically as you use the app over time.",
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                history.reversed().forEachIndexed { index, snapshot ->
                                    val formattedLabel = try {
                                        val date = sdfMonth.parse(snapshot.monthYear)
                                        if (date != null) sdfLabel.format(date) else snapshot.monthYear
                                    } catch (e: Exception) {
                                        snapshot.monthYear
                                    }

                                    // Compare with the previous chronological snapshot
                                    val prevSnapshot = history.getOrNull(history.size - 1 - index - 1)
                                    val monthlyDelta = prevSnapshot?.let { snapshot.netWorth - it.netWorth }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = formattedLabel,
                                                style = SelfBudgetType.rowTitle,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (monthlyDelta != null) {
                                                val isGain = monthlyDelta >= 0
                                                val deltaRamp = if (isGain) Ramp.Teal else Ramp.Red
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (isGain) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                        contentDescription = null,
                                                        tint = deltaRamp.secondaryText(isDark),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "%s$currencySymbol%.2f vs prev month".format(
                                                            if (isGain) "+" else "",
                                                            monthlyDelta
                                                        ),
                                                        style = SelfBudgetType.meta,
                                                        color = deltaRamp.secondaryText(isDark)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "$currencySymbol%.2f".format(snapshot.netWorth),
                                            style = SelfBudgetType.rowTitle,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (index < history.size - 1) {
                                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Generous bottom spacer for comfortable scrolling
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }

    if (showAssetBreakdown) {
        AccountBreakdownModal(
            title = "Total assets",
            ramp = Ramp.Teal,
            icon = Icons.Default.AccountBalance,
            accounts = assetAccounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            onDismiss = { showAssetBreakdown = false }
        )
    }

    if (showDebtBreakdown) {
        AccountBreakdownModal(
            title = "Total debt",
            ramp = Ramp.Red,
            icon = Icons.Default.CreditCard,
            accounts = debtAccounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            onDismiss = { showDebtBreakdown = false }
        )
    }
}

/**
 * Drill-down opened by tapping a [AssetDebtTile] (spec §14): every account that
 * makes up that total assets/total debt figure, so the number is never a dead end.
 */
@Composable
fun AccountBreakdownModal(
    title: String,
    ramp: Ramp,
    icon: ImageVector,
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double>,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val total = remember(accounts, accountBalances) {
        accounts.sumOf { acc -> kotlin.math.abs(accountBalances[acc.id] ?: acc.initialBalance) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Top App Bar — ✕ and title only (spec §14).
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = SelfBudgetType.heading,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Total Card — neutral display number, the icon pill carries the color (spec §14).
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ShapeCard,
                        color = ramp.tintFill(isDark)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, tint = ramp.secondaryText(isDark), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${accounts.size} account${if (accounts.size != 1) "s" else ""}",
                                    style = SelfBudgetType.section,
                                    color = ramp.secondaryText(isDark)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$currencySymbol%.2f".format(total),
                                style = SelfBudgetType.display,
                                color = ramp.titleText(isDark)
                            )
                        }
                    }

                    if (accounts.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "No accounts in this group yet.",
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                accounts.forEachIndexed { index, acc ->
                                    if (index > 0) SectionRowDivider(modifier = Modifier.padding(start = 62.dp))

                                    val rawBal = accountBalances[acc.id] ?: acc.initialBalance
                                    val isLiability = AccountBalanceCalculator.isLiability(acc.type)
                                    val displayBal = if (isLiability) kotlin.math.abs(rawBal) else rawBal
                                    val sym = if (acc.currencyCode.isNotBlank()) Currencies.symbolFor(acc.currencyCode) else currencySymbol

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            RampIconTile(icon = getAccountIcon(acc.type), ramp = ramp, size = 36.dp, iconSize = 18.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    style = SelfBudgetType.rowTitle,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = getAccountTypeLabel(acc.type),
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = "$sym%.2f".format(displayBal),
                                            style = SelfBudgetType.rowTitle,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
private fun AssetDebtTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    amountText: String,
    caption: String,
    ramp: Ramp,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = ShapeCard,
        color = ramp.tintFill(isDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = ramp.secondaryText(isDark), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, style = SelfBudgetType.meta, color = ramp.secondaryText(isDark))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = amountText, style = SelfBudgetType.heading, color = ramp.titleText(isDark))
            Text(text = caption, style = SelfBudgetType.meta, color = ramp.secondaryText(isDark))
        }
    }
}
