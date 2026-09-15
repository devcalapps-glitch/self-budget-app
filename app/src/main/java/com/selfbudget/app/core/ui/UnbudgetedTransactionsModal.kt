package com.selfbudget.app.core.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.getBrandColor
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

private data class UnbudgetedCategoryGroup(
    val category: CategoryEntity,
    val totalSpent: Double,
    val transactions: List<TransactionEntity>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnbudgetedTransactionsModal(
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity> = emptyList(),
    currencySymbol: String = "$",
    selectedMonthYear: String,
    onSetBudget: (categoryId: String, limit: Double, rolloverEnabled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    // Budgeted category IDs (only those with active limit > 0)
    val budgetedCategoryIds = remember(budgets) {
        budgets.filter { it.amountLimit > 0.0 }.map { it.categoryId }.toSet()
    }

    // Unbudgeted expense transactions
    val unbudgetedTransactions = remember(transactions, budgetedCategoryIds) {
        transactions.filter {
            it.type == TransactionType.EXPENSE && it.categoryId !in budgetedCategoryIds
        }.sortedByDescending { it.timestamp }
    }

    val totalUnbudgeted = remember(unbudgetedTransactions) {
        Money.sum(unbudgetedTransactions.map { it.amount })
    }

    val unbudgetedGroups = remember(unbudgetedTransactions, categoryMap) {
        unbudgetedTransactions.groupBy { it.categoryId }.map { (catId, txs) ->
            val cat = categoryMap[catId] ?: CategoryEntity(
                id = catId,
                name = "Other",
                iconName = "MoreHoriz",
                colorHex = "#64748B",
                type = TransactionType.EXPENSE
            )
            UnbudgetedCategoryGroup(
                category = cat,
                totalSpent = Money.sum(txs.map { it.amount }),
                transactions = txs
            )
        }.sortedByDescending { it.totalSpent }
    }

    // Quick set budget dialog state
    var categoryForBudget by remember { mutableStateOf<CategoryEntity?>(null) }
    var budgetLimitInput by remember { mutableStateOf("") }

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
                            text = "Unbudgeted Spending",
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
                            text = "${unbudgetedTransactions.size} transactions"
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
                    // 1. Hero Summary Banner
                    Surface(
                        shape = ShapeHero,
                        color = Ramp.Amber.tintFill(isDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Ramp.Amber.solidFill(isDark),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = Ramp.Amber.onSolidFill(isDark)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "UNBUDGETED EXPENSES".uppercase(),
                                    style = SelfBudgetType.eyebrow,
                                    color = if (isDark) Ramp.Amber.c200 else Ramp.Amber.secondaryText(isDark)
                                )
                                Text(
                                    text = "$currencySymbol%,.2f".format(totalUnbudgeted),
                                    style = SelfBudgetType.display,
                                    color = if (isDark) Color.White else Ramp.Amber.titleText(isDark)
                                )
                                Text(
                                    text = if (unbudgetedGroups.isEmpty()) {
                                        "All your expenses this month belong to budgeted categories."
                                    } else {
                                        "Across ${unbudgetedGroups.size} categor${if (unbudgetedGroups.size == 1) "y" else "ies"} with no monthly limits"
                                    },
                                    style = SelfBudgetType.meta,
                                    color = if (isDark) Ramp.Amber.c100 else Ramp.Amber.secondaryText(isDark)
                                )
                            }
                        }
                    }

                    // 2. Empty State or Grouped Category Sections
                    if (unbudgetedGroups.isEmpty()) {
                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                RampIconTile(
                                    icon = Icons.Default.PieChart,
                                    ramp = Ramp.Teal,
                                    size = 56.dp,
                                    iconSize = 28.dp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No unbudgeted spending",
                                    style = SelfBudgetType.heading,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Every expense logged this month is tracked under an active spending plan limit.",
                                    style = SelfBudgetType.body,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        unbudgetedGroups.forEach { group ->
                            val catRamp = sectionRamp(group.category.name)
                            SectionHeaderBand(
                                title = group.category.name,
                                ramp = catRamp,
                                icon = getCategoryIcon(group.category.iconName),
                                countPill = "$currencySymbol%,.0f".format(group.totalSpent),
                                trailingText = "+ Set Budget",
                                onTrailingClick = {
                                    categoryForBudget = group.category
                                    budgetLimitInput = "%.0f".format(group.totalSpent)
                                }
                            ) {
                                group.transactions.forEachIndexed { index, tx ->
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
                                    if (index < group.transactions.size - 1) {
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

    // Set budget dialog for an unbudgeted category
    categoryForBudget?.let { cat ->
        Dialog(onDismissRequest = { categoryForBudget = null }) {
            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Set Category Budget",
                        style = SelfBudgetType.title,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cat.name,
                        style = SelfBudgetType.heading,
                        color = getBrandColor()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = budgetLimitInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                budgetLimitInput = input
                            }
                        },
                        label = { Text("Monthly Limit ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = ShapePill,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = getBrandColor(),
                            cursorColor = getBrandColor()
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrimaryPillButton(
                            text = "Cancel",
                            onClick = { categoryForBudget = null },
                            ramp = Ramp.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        PrimaryPillButton(
                            text = "Save Limit",
                            onClick = {
                                val limit = budgetLimitInput.toDoubleOrNull() ?: 0.0
                                if (limit > 0.0) {
                                    onSetBudget(cat.id, limit, false)
                                }
                                categoryForBudget = null
                            },
                            ramp = Ramp.Teal
                        )
                    }
                }
            }
        }
    }
}
