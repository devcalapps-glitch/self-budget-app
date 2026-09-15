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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Work
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
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.getIncomeColor
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
fun RecurringIncomeModal(
    recurringList: List<RecurringTransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    currencySymbol: String = "$",
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val activeIncomeStreams = remember(recurringList) {
        recurringList.filter { it.type == TransactionType.INCOME && !it.isArchived }
            .sortedBy { it.nextDueDate }
    }

    val totalMonthlyIncome = remember(activeIncomeStreams) {
        Money.sum(activeIncomeStreams.map {
            RecurringFrequencyNormalizer.toMonthlyAmount(it.amount, it.frequency)
        })
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
                            text = "Recurring Paychecks",
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
                        NeutralBadge(text = "${activeIncomeStreams.size} active")
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
                        color = Ramp.Teal.tintFill(isDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Ramp.Teal.solidFill(isDark),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Ramp.Teal.onSolidFill(isDark)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "COMMITTED RECURRING INCOME".uppercase(),
                                    style = SelfBudgetType.eyebrow,
                                    color = Ramp.Teal.secondaryText(isDark)
                                )
                                Text(
                                    text = "$currencySymbol%,.2f/mo".format(totalMonthlyIncome),
                                    style = SelfBudgetType.display,
                                    color = Ramp.Teal.titleText(isDark)
                                )
                                Text(
                                    text = "${activeIncomeStreams.size} active paycheck / income stream${if (activeIncomeStreams.size == 1) "" else "s"}",
                                    style = SelfBudgetType.meta,
                                    color = Ramp.Teal.secondaryText(isDark)
                                )
                            }
                        }
                    }

                    // 2. Breakdown List (Read-Only)
                    if (activeIncomeStreams.isEmpty()) {
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
                                    icon = Icons.Default.Work,
                                    ramp = Ramp.Teal,
                                    size = 56.dp,
                                    iconSize = 28.dp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No recurring income streams",
                                    style = SelfBudgetType.heading,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add regular salary, client retainers, or passive income to automate your cash flow projections.",
                                    style = SelfBudgetType.body,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        SectionHeaderBand(
                            title = "Income Schedules",
                            ramp = Ramp.Teal,
                            icon = Icons.Default.ArrowDownward,
                            countPill = "$currencySymbol%,.0f/mo".format(totalMonthlyIncome)
                        ) {
                            activeIncomeStreams.forEachIndexed { index, item ->
                                val cat = categoryMap[item.categoryId]
                                val account = accountMap[item.accountId]
                                val monthly = RecurringFrequencyNormalizer.toMonthlyAmount(item.amount, item.frequency)
                                val nextDueStr = dateFormatter.format(Date(item.nextDueDate))
                                val catRamp = sectionRamp(cat?.name ?: "Income")

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RampIconTile(
                                            icon = getCategoryIcon(cat?.iconName ?: "AccountBalanceWallet"),
                                            ramp = catRamp,
                                            size = 38.dp,
                                            iconSize = 20.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = item.title,
                                                style = SelfBudgetType.rowTitle,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${item.frequency.name.lowercase().replaceFirstChar { it.uppercase() }} • Next: $nextDueStr${if (account != null) " • ${account.name}" else ""}",
                                                style = SelfBudgetType.meta,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "+$currencySymbol%.2f".format(item.amount),
                                            style = SelfBudgetType.body,
                                            color = getIncomeColor()
                                        )
                                        if (item.frequency != RecurringFrequency.MONTHLY) {
                                            Text(
                                                text = "($currencySymbol%.0f/mo)".format(monthly),
                                                style = SelfBudgetType.meta,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (index < activeIncomeStreams.size - 1) {
                                    SectionRowDivider()
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
