package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.core.util.CurrencyConverter
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.core.util.MonthlyReviewHelper
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.ExchangeRateEntity
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.pillFill
import com.selfbudget.app.ui.theme.pillText
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import java.util.Locale

/**
 * Account group classification for structured first-of-the-month review.
 */
private enum class ReviewGroupCategory(
    val title: String,
    val ramp: Ramp,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    CASH_CHECKING("Checking & Everyday Cash", Ramp.Teal, Icons.Default.AccountBalance),
    SAVINGS("Savings Accounts", Ramp.Teal, Icons.Default.Savings),
    INVESTMENTS("Investments & Retirement", Ramp.Purple, Icons.AutoMirrored.Filled.TrendingUp),
    PROPERTIES("Properties & Assets", Ramp.Blue, Icons.Default.Home),
    DEBTS("Debts & Credit Cards", Ramp.Coral, Icons.Default.CreditCard)
}

private fun categorizeAccountForReview(type: AccountType): ReviewGroupCategory {
    return when (type) {
        AccountType.CHECKING, AccountType.CASH -> ReviewGroupCategory.CASH_CHECKING
        AccountType.SAVINGS -> ReviewGroupCategory.SAVINGS
        AccountType.INVESTMENT, AccountType.RETIREMENT -> ReviewGroupCategory.INVESTMENTS
        AccountType.REAL_ESTATE, AccountType.VEHICLE -> ReviewGroupCategory.PROPERTIES
        AccountType.CREDIT_CARD, AccountType.LOAN, AccountType.MORTGAGE,
        AccountType.AUTO_LOAN, AccountType.STUDENT_LOAN -> ReviewGroupCategory.DEBTS
    }
}

/**
 * Full-screen modal review screen presented on the first day of each month.
 *
 * Allows users to manually audit and update current balances across all accounts
 * (checking, savings, investments, debts, and more). As numbers are entered,
 * total assets, liabilities, and net worth recalculate live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAccountReviewModal(
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double>,
    currencySymbol: String = "$",
    exchangeRates: List<ExchangeRateEntity> = emptyList(),
    onSaveBalances: (Map<String, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val baseCurrencyCode = Currencies.codeForSymbol(currencySymbol)

    // Store editable text input for each account balance
    val balanceInputMap = remember(accounts, accountBalances) {
        mutableStateMapOf<String, String>().apply {
            accounts.forEach { acc ->
                val cur = accountBalances[acc.id] ?: acc.initialBalance
                val displayBal = if (AccountBalanceCalculator.isLiability(acc.type)) kotlin.math.abs(cur) else cur
                put(acc.id, if (kotlin.math.abs(displayBal) > 0.0001) "%.2f".format(Locale.US, displayBal) else "0.00")
            }
        }
    }

    // Baseline net worth before edits
    val baselineNetWorth = remember(accounts, accountBalances, exchangeRates) {
        val perAccount = accounts.map { acc ->
            val bal = accountBalances[acc.id] ?: acc.initialBalance
            val converted = CurrencyConverter.convert(bal, acc.currencyCode, baseCurrencyCode, exchangeRates)
            if (AccountBalanceCalculator.isLiability(acc.type)) -kotlin.math.abs(converted) else converted
        }
        Money.sum(perAccount)
    }

    // Live updated net worth, assets, and liabilities as user edits inputs
    val currentValues = accounts.map { acc ->
        val enteredRaw = balanceInputMap[acc.id]?.toDoubleOrNull()
        val cur = accountBalances[acc.id] ?: acc.initialBalance
        val isDebt = AccountBalanceCalculator.isLiability(acc.type)
        val liveEntered = enteredRaw ?: if (isDebt) kotlin.math.abs(cur) else cur
        val signedLive = if (isDebt && liveEntered > 0.0) -liveEntered else liveEntered
        val converted = CurrencyConverter.convert(signedLive, acc.currencyCode, baseCurrencyCode, exchangeRates)
        Pair(acc, converted)
    }

    val liveAssets = currentValues.sumOf { (acc, converted) ->
        if (AccountBalanceCalculator.isLiability(acc.type)) 0.0 else if (converted > 0.0) converted else 0.0
    }
    val liveLiabilities = currentValues.sumOf { (acc, converted) ->
        if (AccountBalanceCalculator.isLiability(acc.type)) kotlin.math.abs(converted) else if (converted < 0.0) kotlin.math.abs(converted) else 0.0
    }
    val liveNetWorth = liveAssets - liveLiabilities
    val netWorthDelta = liveNetWorth - baselineNetWorth

    // Group accounts cleanly
    val groupedAccounts = remember(accounts) {
        accounts.sortedWith(
            compareBy(
                { categorizeAccountForReview(it.type).ordinal },
                { getAccountTypePriority(it.type) },
                { it.name.lowercase() }
            )
        ).groupBy { categorizeAccountForReview(it.type) }
    }

    val firstOfMonthLabel = remember { MonthlyReviewHelper.formatFirstOfMonthLabel() }

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
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar following spec §8.16-18
                TopAppBar(
                    title = {
                        Text(
                            text = "Monthly Balance Review",
                            style = SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        CircularBackButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. First-of-Month Review Hero Card
                        item(key = "hero_card") {
                            Surface(
                                shape = ShapeHero,
                                color = Ramp.Teal.tintFill(isDark),
                                border = BorderStroke(1.dp, Ramp.Teal.containerBorder(isDark)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$firstOfMonthLabel REVIEW".uppercase(),
                                            style = SelfBudgetType.eyebrow,
                                            color = Ramp.Teal.secondaryText(isDark)
                                        )

                                        // Net worth delta badge
                                        if (kotlin.math.abs(netWorthDelta) > 0.005) {
                                            val isPositiveDelta = netWorthDelta > 0.0
                                            val badgeRamp = if (isPositiveDelta) Ramp.Teal else Ramp.Coral
                                            Surface(
                                                shape = ShapeChip,
                                                color = badgeRamp.pillFill(isDark)
                                            ) {
                                                Text(
                                                    text = "${if (isPositiveDelta) "+$currencySymbol" else "-$currencySymbol"}%.2f vs recorded".format(
                                                        kotlin.math.abs(netWorthDelta)
                                                    ),
                                                    style = SelfBudgetType.badge,
                                                    color = badgeRamp.pillText(isDark),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Projected Net Worth",
                                        style = SelfBudgetType.heading,
                                        color = Ramp.Teal.titleText(isDark)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${if (liveNetWorth < 0.0) "-$currencySymbol" else currencySymbol}%.2f".format(
                                            kotlin.math.abs(liveNetWorth)
                                        ),
                                        style = SelfBudgetType.display,
                                        color = Ramp.Teal.titleText(isDark)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Total Assets",
                                                style = SelfBudgetType.meta,
                                                color = Ramp.Teal.secondaryText(isDark)
                                            )
                                            Text(
                                                text = "$currencySymbol%.2f".format(liveAssets),
                                                style = SelfBudgetType.rowTitle,
                                                color = Ramp.Teal.titleText(isDark)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Total Liabilities",
                                                style = SelfBudgetType.meta,
                                                color = Ramp.Teal.secondaryText(isDark)
                                            )
                                            Text(
                                                text = "${if (liveLiabilities > 0.0) "-$currencySymbol" else currencySymbol}%.2f".format(
                                                    liveLiabilities
                                                ),
                                                style = SelfBudgetType.rowTitle,
                                                color = if (liveLiabilities > 0.0) getExpenseColor() else Ramp.Teal.titleText(isDark)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Subtitle instruction
                        item(key = "instruction_text") {
                            Text(
                                text = "Update your real-world balances below to keep your cash flow, net worth, and safe-to-spend accurate for the month.",
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // 2. Account Group Sections
                        groupedAccounts.forEach { (category, accountsInCategory) ->
                            item(key = "section_${category.name}") {
                                SectionHeaderBand(
                                    title = category.title,
                                    ramp = category.ramp,
                                    icon = category.icon,
                                    countPill = "${accountsInCategory.size}"
                                ) {
                                    accountsInCategory.forEachIndexed { idx, account ->
                                        if (idx > 0) SectionRowDivider()

                                        val curBalance = accountBalances[account.id] ?: account.initialBalance
                                        val isDebt = AccountBalanceCalculator.isLiability(account.type)
                                        val displayCurBalance = if (isDebt) kotlin.math.abs(curBalance) else curBalance
                                        val enteredStr = balanceInputMap[account.id] ?: ""
                                        val enteredVal = enteredStr.toDoubleOrNull() ?: displayCurBalance
                                        val delta = enteredVal - displayCurBalance
                                        val accCurrencySym = Currencies.symbolFor(account.currencyCode)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RampIconTile(
                                                icon = getAccountIcon(account.type),
                                                ramp = accountTypeRamp(account.type)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = account.name,
                                                    style = SelfBudgetType.rowTitle,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Recorded: $accCurrencySym%.2f".format(displayCurBalance),
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                // Delta difference badge
                                                if (kotlin.math.abs(delta) > 0.005) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    val isGoodChange = if (isDebt) delta < 0.0 else delta > 0.0
                                                    val deltaColor = if (isGoodChange) getIncomeColor() else getExpenseColor()
                                                    val deltaPrefix = if (delta > 0.0) "+$accCurrencySym" else "-$accCurrencySym"
                                                    Text(
                                                        text = "$deltaPrefix%.2f${if (isDebt) " debt" else ""}".format(kotlin.math.abs(delta)),
                                                        style = SelfBudgetType.badge,
                                                        color = deltaColor
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Numerical Input Field for updating the balance
                                            OutlinedTextField(
                                                value = enteredStr,
                                                onValueChange = { input ->
                                                    // Allow valid decimal formatting up to 2 places
                                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                                        balanceInputMap[account.id] = input
                                                    }
                                                },
                                                prefix = {
                                                    Text(
                                                        text = accCurrencySym,
                                                        style = SelfBudgetType.body,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                singleLine = true,
                                                textStyle = SelfBudgetType.rowTitle.copy(textAlign = TextAlign.End),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Decimal,
                                                    imeAction = ImeAction.Next
                                                ),
                                                modifier = Modifier.width(130.dp),
                                                shape = ShapeChip,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    focusedBorderColor = Ramp.Teal.solidFill(isDark),
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sticky Footer Action Bar following spec §8.1 & §8.15
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            PrimaryPillButton(
                                text = "Confirm & Update Balances",
                                onClick = {
                                    val finalMap = mutableMapOf<String, Double>()
                                    accounts.forEach { acc ->
                                        val cur = accountBalances[acc.id] ?: acc.initialBalance
                                        val isDebt = AccountBalanceCalculator.isLiability(acc.type)
                                        val fallback = if (isDebt) kotlin.math.abs(cur) else cur
                                        val entered = balanceInputMap[acc.id]?.toDoubleOrNull() ?: fallback
                                        finalMap[acc.id] = entered
                                    }
                                    onSaveBalances(finalMap)
                                },
                                ramp = Ramp.Teal,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SecondaryPillButton(
                                text = "Remind Me Later",
                                onClick = onDismiss,
                                ramp = Ramp.Gray,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
