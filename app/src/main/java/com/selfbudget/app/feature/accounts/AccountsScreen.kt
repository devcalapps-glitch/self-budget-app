package com.selfbudget.app.feature.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.selfbudget.app.core.ui.AddCustomAccountDialog
import com.selfbudget.app.core.ui.EditCustomAccountDialog
import com.selfbudget.app.core.ui.NetWorthHistoryModal
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.core.ui.accountTypeRamp
import com.selfbudget.app.core.ui.components.IconTile
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.feature.transaction.TransferDialog
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapeTile
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    currencySymbol: String = "$",
    accountBalances: Map<String, Double> = emptyMap(),
    netWorthHistory: List<NetWorthSnapshotEntity> = emptyList(),
    onAddAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onAddTransfer: (fromId: String, toId: String, amount: Double, note: String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showNetWorthModal by remember { mutableStateOf(false) }
    var payoffCalculatorAccount by remember { mutableStateOf<AccountEntity?>(null) }

    // Calculate totals - via the same canonical classification (AccountBalanceCalculator.isLiability)
    // used everywhere else net worth is computed, not an ad-hoc type/sign check that can silently
    // disagree with Analytics and the Net Worth details page.
    val totalAssets = remember(accounts, accountBalances) {
        accounts.filterNot { AccountBalanceCalculator.isLiability(it.type) }
            .sumOf { acc -> accountBalances[acc.id] ?: acc.initialBalance }
    }

    val totalLiabilities = remember(accounts, accountBalances) {
        accounts.filter { AccountBalanceCalculator.isLiability(it.type) }
            .sumOf { acc -> kotlin.math.abs(accountBalances[acc.id] ?: acc.initialBalance) }
    }

    val totalNetWorth = totalAssets - totalLiabilities

    val filteredAccounts = remember(accounts, searchQuery) {
        accounts.filter { acc ->
            searchQuery.isBlank() || acc.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Accounts & assets",
            style = SelfBudgetType.heading,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Net Worth hero card (spec §4/§11: Net worth report identity = Teal)
        val isDarkNw = isAppInDarkTheme()
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeHero,
            color = Ramp.Teal.tintFill(isDarkNw)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL NET WORTH",
                            style = SelfBudgetType.eyebrow,
                            color = Ramp.Teal.secondaryText(isDarkNw)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val cleanNetWorth = if (kotlin.math.abs(totalNetWorth) < 0.005) 0.0 else totalNetWorth
                        val netWorthPrefix = if (cleanNetWorth < 0) "-$currencySymbol" else currencySymbol
                        Text(
                            text = "$netWorthPrefix%.2f".format(kotlin.math.abs(cleanNetWorth)),
                            style = SelfBudgetType.display,
                            color = Ramp.Teal.titleText(isDarkNw)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Ramp.Teal.solidFill(isDarkNw),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showNetWorthModal = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = "History",
                                tint = Ramp.Teal.onSolidFill(isDarkNw),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Trends",
                                style = SelfBudgetType.badge,
                                color = Ramp.Teal.onSolidFill(isDarkNw)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mini Net Worth Progress Sparkline Preview
                val sparklinePoints = remember(netWorthHistory, totalNetWorth) {
                    val points = netWorthHistory.map { it.netWorth }.toMutableList()
                    if (points.isEmpty()) points.add(totalNetWorth)
                    if (points.size == 1) points.add(0, points.first())
                    points
                }
                val posIncomeColor = com.selfbudget.app.ui.theme.getIncomeColor()
                val negExpenseColor = getExpenseColor()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(ShapeTile)
                        .clickable { showNetWorthModal = true }
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
                            val y = h - (normY * (h - 8f) + 4f).toFloat()
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

                        val isPos = (sparklinePoints.lastOrNull() ?: 0.0) >= 0
                        val strokeColor = if (isPos) posIncomeColor else negExpenseColor

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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Assets",
                            style = SelfBudgetType.meta,
                            color = Ramp.Teal.secondaryText(isDarkNw)
                        )
                        Text(
                            text = "$currencySymbol%.2f".format(totalAssets),
                            style = SelfBudgetType.heading,
                            color = Ramp.Teal.titleText(isDarkNw)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Liabilities",
                            style = SelfBudgetType.meta,
                            color = Ramp.Teal.secondaryText(isDarkNw)
                        )
                        val cleanLiabilities = if (kotlin.math.abs(totalLiabilities) < 0.005) 0.0 else totalLiabilities
                        Text(
                            text = "${if (cleanLiabilities > 0.0) "-$currencySymbol" else currencySymbol}%.2f".format(cleanLiabilities),
                            style = SelfBudgetType.heading,
                            color = if (cleanLiabilities > 0.0) getExpenseColor() else Ramp.Teal.titleText(isDarkNw)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryPillButton(
                text = "Add account",
                onClick = { showAddAccountDialog = true },
                modifier = Modifier.weight(1f)
            )

            SecondaryPillButton(
                text = "Transfer",
                onClick = { showTransferDialog = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        com.selfbudget.app.core.ui.AppSearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search accounts...",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))



        val groupedAccounts = remember(filteredAccounts, accountBalances) {
            filteredAccounts
                .groupBy { it.type }
                .toList()
                .sortedBy { (type, _) -> com.selfbudget.app.core.ui.getAccountTypePriority(type) }
        }

        // Accounts List
        if (filteredAccounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No accounts matching '$searchQuery'" else "No accounts created yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedAccounts.forEach { (type, accountsInType) ->
                    val typeLabel = com.selfbudget.app.core.ui.getAccountTypeLabel(type)
                    val typeRamp = accountTypeRamp(type)
                    val typeIcon = com.selfbudget.app.core.ui.getAccountIcon(type)
                    val isLiabilityGroup = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(type)
                    val groupTotal = accountsInType.sumOf { acc ->
                        val bal = accountBalances[acc.id] ?: acc.initialBalance
                        if (isLiabilityGroup) kotlin.math.abs(bal) else bal
                    }
                    val cleanGroupTotal = if (kotlin.math.abs(groupTotal) < 0.005) 0.0 else groupTotal
                    val isNegGroup = (isLiabilityGroup && cleanGroupTotal > 0.0) || (!isLiabilityGroup && cleanGroupTotal < 0.0)

                    item(key = "section_${type.name}") {
                        SectionHeaderBand(
                            title = typeLabel,
                            ramp = typeRamp,
                            icon = typeIcon,
                            countPill = "${accountsInType.size}",
                            trailingText = "${if (isNegGroup) "-$currencySymbol" else currencySymbol}%.2f".format(kotlin.math.abs(cleanGroupTotal))
                        ) {
                            accountsInType.forEachIndexed { index, acc ->
                                if (index > 0) SectionRowDivider()

                                val accColor = try {
                                    Color(android.graphics.Color.parseColor(acc.colorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }

                                val icon = com.selfbudget.app.core.ui.getAccountIcon(acc.type)
                                val rawBalance = accountBalances[acc.id] ?: acc.initialBalance
                                val isLiability = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(acc.type)
                                val displayBalance = if (isLiability) kotlin.math.abs(rawBalance) else rawBalance
                                val cleanDisplayBalance = if (kotlin.math.abs(displayBalance) < 0.005) 0.0 else displayBalance
                                val isNegBalance = (isLiability && cleanDisplayBalance > 0.0) || (!isLiability && cleanDisplayBalance < 0.0)
                                val sym = if (acc.currencyCode.isNotBlank()) Currencies.symbolFor(acc.currencyCode) else currencySymbol

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingAccount = acc }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        IconTile(
                                            icon = icon,
                                            tint = accColor,
                                            background = accColor.copy(alpha = 0.15f),
                                            shape = CircleShape,
                                            size = 36.dp,
                                            iconSize = 18.dp
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = acc.name,
                                                style = SelfBudgetType.rowTitle,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            if (acc.isDefault) {
                                                Text(
                                                    text = "Default account",
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${if (isNegBalance) "-$sym" else sym}%.2f".format(kotlin.math.abs(cleanDisplayBalance)),
                                            style = SelfBudgetType.rowTitle,
                                            color = if (isNegBalance) getExpenseColor() else if (cleanDisplayBalance > 0.0) getIncomeColor() else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isLiability) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { payoffCalculatorAccount = acc },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Calculate,
                                                    contentDescription = "Payoff calculator",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Edit account",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
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

    // Dialogs
    if (showAddAccountDialog) {
        AddCustomAccountDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { newAcc ->
                onAddAccount(newAcc)
                showAddAccountDialog = false
            }
        )
    }

    editingAccount?.let { acc ->
        EditCustomAccountDialog(
            account = acc,
            currentBalance = accountBalances[acc.id],
            currencySymbol = currencySymbol,
            onDismiss = { editingAccount = null },
            onConfirm = { updatedAcc ->
                onUpdateAccount(updatedAcc)
                editingAccount = null
            },
            onDelete = { deletedAcc ->
                onDeleteAccount(deletedAcc)
                editingAccount = null
            }
        )
    }

    payoffCalculatorAccount?.let { acc ->
        com.selfbudget.app.core.ui.DebtPayoffCalculatorDialog(
            accounts = accounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            preselectedAccount = acc,
            onDismiss = { payoffCalculatorAccount = null }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            accounts = accounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromId, toId, amount, note ->
                onAddTransfer(fromId, toId, amount, note)
                showTransferDialog = false
            },
            onAddCustomAccount = onAddAccount
        )
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
