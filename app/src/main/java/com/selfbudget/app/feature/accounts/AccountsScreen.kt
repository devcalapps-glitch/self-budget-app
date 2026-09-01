package com.selfbudget.app.feature.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.selfbudget.app.core.ui.AddCustomAccountDialog
import com.selfbudget.app.core.ui.EditCustomAccountDialog
import com.selfbudget.app.core.ui.NetWorthHistoryModal
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.feature.transaction.TransferDialog
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.getIncomeColor

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

    // Calculate totals
    val totalAssets = remember(accounts, accountBalances) {
        accounts.filter { it.type != AccountType.CREDIT_CARD && it.type != AccountType.LOAN }
            .sumOf { acc ->
                val bal = accountBalances[acc.id] ?: acc.initialBalance
                if (bal > 0) bal else 0.0
            }
    }

    val totalLiabilities = remember(accounts, accountBalances) {
        accounts.sumOf { acc ->
            val bal = accountBalances[acc.id] ?: acc.initialBalance
            if (acc.type == AccountType.CREDIT_CARD || acc.type == AccountType.LOAN) {
                kotlin.math.abs(bal)
            } else if (bal < 0) {
                kotlin.math.abs(bal)
            } else {
                0.0
            }
        }
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
            text = "Accounts & Assets",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Net Worth Card Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
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
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (totalNetWorth < 0) "-$currencySymbol" else currencySymbol}%.2f".format(kotlin.math.abs(totalNetWorth)),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
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
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Trends",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
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
                        val strokeColor = if (isPos) posIncomeColor else ExpenseRed

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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$currencySymbol%.2f".format(totalAssets),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.selfbudget.app.ui.theme.getIncomeColor()
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Liabilities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "-$currencySymbol%.2f".format(totalLiabilities),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
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
            Button(
                onClick = { showAddAccountDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = { showTransferDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Transfer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search accounts...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))



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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAccounts, key = { it.id }) { acc ->
                    val accColor = try {
                        Color(android.graphics.Color.parseColor(acc.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    val icon = com.selfbudget.app.core.ui.getAccountIcon(acc.type)

                    val typeLabel = when (acc.type) {
                        AccountType.CHECKING -> "Checking"
                        AccountType.CREDIT_CARD -> "Credit Card"
                        AccountType.SAVINGS -> "Savings Account"
                        AccountType.CASH -> "Cash Wallet"
                        AccountType.INVESTMENT -> "Investment"
                        AccountType.LOAN -> "Loan / Mortgage"
                        AccountType.RETIREMENT -> "Retirement (Non-Liquid)"
                    }

                    val rawBalance = accountBalances[acc.id] ?: acc.initialBalance
                    val isLiability = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(acc.type)
                    val displayBalance = if (isLiability) kotlin.math.abs(rawBalance) else rawBalance
                    val sym = if (acc.currencyCode.isNotBlank()) Currencies.symbolFor(acc.currencyCode) else currencySymbol

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingAccount = acc }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(accColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = accColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$sym%.2f".format(displayBalance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLiability) MaterialTheme.colorScheme.onSurface else if (displayBalance >= 0) com.selfbudget.app.ui.theme.getIncomeColor() else ExpenseRed
                                )
                                if (isLiability) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { payoffCalculatorAccount = acc },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = "Payoff Calculator",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Edit Account",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(150.dp))
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
