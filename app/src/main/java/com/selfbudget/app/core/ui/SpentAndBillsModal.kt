package com.selfbudget.app.core.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.sectionRamp
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpentAndBillsModal(
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    recurringList: List<RecurringTransactionEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    currencySymbol: String = "$",
    selectedMonthYear: String,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    // Budgeted category IDs
    val budgetedCategoryIds = remember(budgets) {
        budgets.filter { it.amountLimit > 0.0 }.map { it.categoryId }.toSet()
    }

    // Expense transactions belonging to budgeted categories
    val budgetedTransactions = remember(transactions, budgetedCategoryIds) {
        transactions.filter {
            it.type == TransactionType.EXPENSE && it.categoryId in budgetedCategoryIds
        }.sortedByDescending { it.timestamp }
    }

    val totalActualSpent = remember(budgetedTransactions) {
        Money.sum(budgetedTransactions.map { it.amount })
    }

    // Recurring expense bills belonging to budgeted categories
    val budgetedRecurringBills = remember(recurringList, budgetedCategoryIds) {
        recurringList.filter {
            it.type == TransactionType.EXPENSE && !it.isArchived && it.categoryId in budgetedCategoryIds
        }.sortedBy { it.nextDueDate }
    }

    val totalRecurringCommitted = remember(budgetedRecurringBills) {
        Money.sum(budgetedRecurringBills.map {
            RecurringFrequencyNormalizer.toMonthlyAmount(it.amount, it.frequency)
        })
    }

    val totalClaimed = remember(totalActualSpent, totalRecurringCommitted) {
        Money.add(totalActualSpent, totalRecurringCommitted)
    }

    // Group actual spending by category
    val spendingByCategory = remember(budgetedTransactions, categoryMap) {
        budgetedTransactions.groupBy { it.categoryId }.map { (catId, txs) ->
            val cat = categoryMap[catId] ?: CategoryEntity(
                id = catId,
                name = "Other",
                iconName = "MoreHoriz",
                colorHex = "#64748B",
                type = TransactionType.EXPENSE
            )
            cat to txs
        }.sortedByDescending { (_, txs) -> Money.sum(txs.map { it.amount }) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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
                TopAppBar(
                    title = {
                        Text(
                            text = "Spent & Bills Breakdown",
                            style = SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        NeutralBadge(
                            text = "$currencySymbol%,.0f total".format(totalClaimed)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Hero Summary Card
                    Surface(
                        shape = ShapeHero,
                        color = Ramp.Coral.tintFill(isDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Ramp.Coral.solidFill(isDark),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = Ramp.Coral.onSolidFill(isDark)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SPENT & COMMITTED BILLS".uppercase(),
                                    style = SelfBudgetType.eyebrow,
                                    color = if (isDark) Ramp.Coral.c200 else Ramp.Coral.secondaryText(isDark)
                                )
                                Text(
                                    text = "$currencySymbol%,.2f".format(totalClaimed),
                                    style = SelfBudgetType.display,
                                    color = if (isDark) Color.White else Ramp.Coral.titleText(isDark)
                                )
                                Text(
                                    text = "$currencySymbol%,.0f actual spent + $currencySymbol%,.0f recurring bills".format(totalActualSpent, totalRecurringCommitted),
                                    style = SelfBudgetType.meta,
                                    color = if (isDark) Ramp.Coral.c100 else Ramp.Coral.secondaryText(isDark)
                                )
                            }
                        }
                    }

                    // 2. Section: Recurring & Committed Bills
                    if (budgetedRecurringBills.isNotEmpty()) {
                        SectionHeaderBand(
                            title = "Recurring & Committed Bills",
                            ramp = Ramp.Purple,
                            icon = Icons.Default.Repeat,
                            countPill = "$currencySymbol%,.0f/mo".format(totalRecurringCommitted)
                        ) {
                            budgetedRecurringBills.forEachIndexed { index, bill ->
                                val cat = categoryMap[bill.categoryId]
                                val monthlyAmount = RecurringFrequencyNormalizer.toMonthlyAmount(bill.amount, bill.frequency)
                                val account = accountMap[bill.accountId]
                                val nextDueStr = dateFormatter.format(Date(bill.nextDueDate))

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
                                        RampIconTile(
                                            icon = getCategoryIcon(cat?.iconName ?: "Receipt"),
                                            ramp = sectionRamp(cat?.name ?: "Bills"),
                                            size = 36.dp,
                                            iconSize = 18.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = bill.title,
                                                style = SelfBudgetType.rowTitle,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${bill.frequency.name.lowercase().replaceFirstChar { it.uppercase() }} • Due $nextDueStr${if (account != null) " • ${account.name}" else ""}",
                                                style = SelfBudgetType.meta,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$currencySymbol%.2f".format(bill.amount),
                                            style = SelfBudgetType.body,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (bill.frequency != com.selfbudget.app.data.model.RecurringFrequency.MONTHLY) {
                                            Text(
                                                text = "($currencySymbol%.0f/mo)".format(monthlyAmount),
                                                style = SelfBudgetType.meta,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (index < budgetedRecurringBills.size - 1) {
                                    SectionRowDivider()
                                }
                            }
                        }
                    }

                    // 3. Section: Realized Expenses in Budget
                    if (spendingByCategory.isEmpty()) {
                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                RampIconTile(
                                    icon = Icons.Default.ShoppingCart,
                                    ramp = Ramp.Teal,
                                    size = 56.dp,
                                    iconSize = 28.dp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No expenses logged in budget",
                                    style = SelfBudgetType.heading,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Expenses logged under your budgeted categories will appear here.",
                                    style = SelfBudgetType.body,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        spendingByCategory.forEach { (cat, txs) ->
                            val catTotal = Money.sum(txs.map { it.amount })
                            val catRamp = sectionRamp(cat.name)
                            SectionHeaderBand(
                                title = cat.name,
                                ramp = catRamp,
                                icon = getCategoryIcon(cat.iconName),
                                countPill = "$currencySymbol%,.0f".format(catTotal)
                            ) {
                                txs.forEachIndexed { index, tx ->
                                    val account = accountMap[tx.accountId]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tx.title,
                                                style = SelfBudgetType.rowTitle,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${dateFormatter.format(Date(tx.timestamp))}${if (account != null) " • ${account.name}" else ""}",
                                                style = SelfBudgetType.meta,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "-$currencySymbol%.2f".format(tx.amount),
                                            style = SelfBudgetType.body,
                                            color = getExpenseColor()
                                        )
                                    }
                                    if (index < txs.size - 1) {
                                        SectionRowDivider()
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
