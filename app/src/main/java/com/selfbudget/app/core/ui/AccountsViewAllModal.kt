package com.selfbudget.app.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.IconTile
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.GoalEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsViewAllModal(
    accounts: List<AccountEntity>,
    currencySymbol: String = "$",
    accountBalances: Map<String, Double> = emptyMap(),
    goals: List<GoalEntity> = emptyList(),
    onDismiss: () -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onAddAccount: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary

    val filteredAccounts = remember(accounts, searchQuery) {
        accounts.filter { acc ->
            searchQuery.isBlank() || acc.name.contains(searchQuery, ignoreCase = true)
        }.sortedWith(
            compareBy(
                { !it.isDefault },
                { getAccountTypePriority(it.type) },
                { it.name.lowercase() }
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .drawWithContent {
                    drawContent()
                    if (!isDark) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.035f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, 160f),
                                radius = size.width * 0.75f
                            )
                        )
                    }
                },
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Persistent Top App Bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Accounts & wallets",
                                style = SelfBudgetType.title,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            NeutralBadge(text = "${accounts.size}")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        PrimaryPillButton(
                            text = "New",
                            onClick = onAddAccount,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Search Bar
                    AppSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search accounts & wallets...",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val groupedAccounts = remember(filteredAccounts, accountBalances) {
                        filteredAccounts
                            .groupBy { it.type }
                            .toList()
                            .sortedBy { (type, _) -> getAccountTypePriority(type) }
                    }

                    if (filteredAccounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No accounts matching '$searchQuery'" else "No accounts found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val isDark = com.selfbudget.app.ui.theme.isAppInDarkTheme()
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            groupedAccounts.forEach { (type, accountsInType) ->
                                val typeLabel = getAccountTypeLabel(type)
                                val typeRamp = accountTypeRamp(type)
                                val typeIcon = getAccountIcon(type)
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

                                            val icon = getAccountIcon(acc.type)
                                            val rawBalance = accountBalances[acc.id] ?: acc.initialBalance
                                            val isLiability = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(acc.type)
                                            val currentBalance = if (isLiability) kotlin.math.abs(rawBalance) else rawBalance
                                            val sym = if (acc.currencyCode.isNotBlank()) Currencies.symbolFor(acc.currencyCode) else currencySymbol

                                            val linkedGoals = goals.filter { it.linkedAccountId == acc.id }
                                            val earmarked = linkedGoals.sumOf { if (it.savedAmount > 0) it.savedAmount else minOf(rawBalance, it.targetAmount) }
                                            val availableToSpend = (currentBalance - earmarked).coerceAtLeast(0.0)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                        onEditAccount(acc)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    val accRamp = if (isLiability) Ramp.Coral else Ramp.Teal
                                                    RampIconTile(
                                                        icon = icon,
                                                        ramp = accRamp,
                                                        size = 36.dp,
                                                        iconSize = 18.dp
                                                    )

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = acc.name,
                                                                style = SelfBudgetType.rowTitle,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1
                                                            )
                                                            if (acc.isDefault) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Surface(
                                                                    shape = ShapePill,
                                                                    color = Ramp.Teal.tintFill(isDark)
                                                                ) {
                                                                    Text(
                                                                        text = "Default",
                                                                        style = SelfBudgetType.badge,
                                                                        color = Ramp.Teal.titleText(isDark),
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        if (earmarked > 0 && !isLiability) {
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "Total: $sym%.2f".format(currentBalance),
                                                                style = SelfBudgetType.meta,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val displayBalance = if (earmarked > 0 && !isLiability) availableToSpend else currentBalance
                                                    val cleanDisplayBalance = if (kotlin.math.abs(displayBalance) < 0.005) 0.0 else displayBalance
                                                    val isNegBalance = (isLiability && cleanDisplayBalance > 0.0) || (!isLiability && cleanDisplayBalance < 0.0)
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = "${if (isNegBalance) "-$sym" else sym}%.2f".format(kotlin.math.abs(cleanDisplayBalance)),
                                                            style = SelfBudgetType.rowTitle,
                                                            color = if (isNegBalance) getExpenseColor() else if (cleanDisplayBalance > 0.0) getIncomeColor() else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        if (earmarked > 0 && !isLiability) {
                                                            Text(
                                                                text = "Available",
                                                                style = SelfBudgetType.meta,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
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
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PrimaryPillButton(
                        text = "Add new account or wallet",
                        onClick = onAddAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

