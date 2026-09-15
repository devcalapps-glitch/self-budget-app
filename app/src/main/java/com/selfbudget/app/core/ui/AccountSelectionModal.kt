package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.selfbudget.app.core.ui.components.CircularBackButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.icon
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.ui.theme.tintFill

private val LIQUID_ACCOUNT_TYPES = setOf(AccountType.CHECKING, AccountType.SAVINGS, AccountType.CASH)

@Composable
fun AccountSelectionModal(
    accounts: List<AccountEntity>,
    selectedAccount: AccountEntity?,
    currencySymbol: String = "$",
    accountBalances: Map<String, Double> = emptyMap(),
    onDismiss: () -> Unit,
    onSelectAccount: (AccountEntity) -> Unit,
    onAddCustomAccount: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredAccounts = remember(accounts, searchQuery) {
        accounts.filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
            .sortedWith(
                compareBy(
                    { !it.isDefault },
                    { getAccountTypePriority(it.type) },
                    { it.name.lowercase() }
                )
            )
    }

    val liquidAccounts = remember(filteredAccounts) {
        filteredAccounts.filter { !AccountBalanceCalculator.isLiability(it.type) && it.type in LIQUID_ACCOUNT_TYPES }
    }
    val debtAccounts = remember(filteredAccounts) {
        filteredAccounts.filter { AccountBalanceCalculator.isLiability(it.type) }
    }
    val assetAccounts = remember(filteredAccounts) {
        filteredAccounts.filter { !AccountBalanceCalculator.isLiability(it.type) && it.type !in LIQUID_ACCOUNT_TYPES }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
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
                // Persistent Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularBackButton(onClick = onDismiss)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select account",
                                style = SelfBudgetType.title,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        PrimaryPillButton(
                            text = "New",
                            onClick = {
                                onDismiss()
                                onAddCustomAccount()
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Search Box
                    AppSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search payment accounts...",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredAccounts.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No payment accounts found",
                                    style = SelfBudgetType.heading,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap + New above to create your checking, credit card, or savings account.",
                                    style = SelfBudgetType.body,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (searchQuery.isBlank() && (liquidAccounts.isNotEmpty() || debtAccounts.isNotEmpty() || assetAccounts.isNotEmpty())) {
                        if (liquidAccounts.isNotEmpty()) {
                            AccountSectionLabel(text = "Liquid")
                            AccountGroup(
                                accounts = liquidAccounts,
                                selectedAccount = selectedAccount,
                                accountBalances = accountBalances,
                                isDebtSection = false,
                                onSelectAccount = { acc ->
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    onSelectAccount(acc)
                                    onDismiss()
                                }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        if (debtAccounts.isNotEmpty()) {
                            AccountSectionLabel(text = "Debt")
                            AccountGroup(
                                accounts = debtAccounts,
                                selectedAccount = selectedAccount,
                                accountBalances = accountBalances,
                                isDebtSection = true,
                                onSelectAccount = { acc ->
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    onSelectAccount(acc)
                                    onDismiss()
                                }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        if (assetAccounts.isNotEmpty()) {
                            AccountSectionLabel(text = "Assets")
                            AccountGroup(
                                accounts = assetAccounts,
                                selectedAccount = selectedAccount,
                                accountBalances = accountBalances,
                                isDebtSection = false,
                                onSelectAccount = { acc ->
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    onSelectAccount(acc)
                                    onDismiss()
                                }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    } else {
                        AccountGroup(
                            accounts = filteredAccounts,
                            selectedAccount = selectedAccount,
                            accountBalances = accountBalances,
                            isDebtSection = false,
                            onSelectAccount = { acc ->
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                onSelectAccount(acc)
                                onDismiss()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }
    }
}

@Composable
private fun AccountSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = SelfBudgetType.eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
    )
}

@Composable
private fun AccountGroup(
    accounts: List<AccountEntity>,
    selectedAccount: AccountEntity?,
    accountBalances: Map<String, Double>,
    isDebtSection: Boolean,
    onSelectAccount: (AccountEntity) -> Unit
) {
    val isDark = isAppInDarkTheme()
    val borderColor = if (isDebtSection) {
        Ramp.Coral.containerBorder(isDark)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeCard)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(0.5.dp, borderColor), ShapeCard)
    ) {
        accounts.forEachIndexed { index, acc ->
            if (index > 0) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
            AccountRow(
                account = acc,
                isSelected = selectedAccount?.id == acc.id,
                accountBalances = accountBalances,
                isDebtSection = isDebtSection,
                onClick = { onSelectAccount(acc) }
            )
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountEntity,
    isSelected: Boolean,
    accountBalances: Map<String, Double>,
    isDebtSection: Boolean,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val accColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val iconTint = when {
        isDebtSection -> Ramp.Coral.icon(isDark)
        isSelected -> Ramp.Teal.titleText(isDark)
        else -> accColor
    }

    val accSym = Currencies.symbolFor(account.currencyCode)
    val rawBal = accountBalances[account.id] ?: account.initialBalance
    val isLiability = AccountBalanceCalculator.isLiability(account.type)
    val displayBal = if (isLiability) kotlin.math.abs(rawBal) else rawBal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Ramp.Teal.tintFill(isDark) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getAccountIcon(account.type),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.height(20.dp).width(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = SelfBudgetType.body,
                color = if (isSelected) Ramp.Teal.titleText(isDark) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$accSym%.2f".format(displayBal),
                style = SelfBudgetType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Ramp.Teal.titleText(isDark),
                modifier = Modifier.height(18.dp).width(18.dp)
            )
        }
    }
}
