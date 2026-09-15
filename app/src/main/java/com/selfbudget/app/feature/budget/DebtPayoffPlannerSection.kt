package com.selfbudget.app.feature.budget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Icon
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
import com.selfbudget.app.core.ui.DebtPayoffCalculatorDialog
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.ui.getAccountIcon
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.ui.theme.CardSurfaceDark
import com.selfbudget.app.ui.theme.DividerDark
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.TextPrimaryDark
import com.selfbudget.app.ui.theme.TextSecondaryDark
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import androidx.compose.ui.unit.dp

/**
 * Plan tab's forward-looking "what if I paid $X/month" tool - distinct from the Analytics tab's
 * DebtPayoffAnalyticsModal, which only reports historical payoff progress from posted
 * transactions. This never touches the ledger; it's a pure projection scratchpad.
 */
@Composable
fun DebtPayoffPlannerSection(
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double>,
    currencySymbol: String
) {
    val debtAccounts = remember(accounts) {
        accounts.filter { AccountBalanceCalculator.isLiability(it.type) }
    }
    var calculatorAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showBlankCalculator by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Payoff calculator", style = SelfBudgetType.heading, color = MaterialTheme.colorScheme.onSurface)

        // Explainer card: icon tile + headline + body + one secondary CTA.
        val isDark = isAppInDarkTheme()
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeHero,
            color = if (isDark) CardSurfaceDark else Ramp.Coral.tintFill(isDark),
            border = if (isDark) BorderStroke(0.5.dp, DividerDark) else null
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RampIconTile(icon = Icons.Default.Calculate, ramp = Ramp.Coral, size = 36.dp, iconSize = 20.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "See exactly how long payoff takes",
                        style = SelfBudgetType.heading,
                        color = if (isDark) TextPrimaryDark else Ramp.Coral.titleText(isDark)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pick a Credit Card or Loan from your wallet - or enter numbers by hand - then set any payment amount to see months to debt-free, total interest, and payoff date.",
                    style = SelfBudgetType.body,
                    color = if (isDark) TextSecondaryDark else Ramp.Coral.secondaryText(isDark)
                )
                Spacer(modifier = Modifier.height(14.dp))
                SecondaryPillButton(
                    text = "New payoff calculation",
                    onClick = { showBlankCalculator = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }
        }

        if (debtAccounts.isNotEmpty()) {
            SectionHeaderBand(
                title = "Your debt accounts",
                ramp = Ramp.Coral,
                icon = Icons.Default.Calculate
            ) {
                debtAccounts.forEachIndexed { index, acc ->
                    if (index > 0) SectionRowDivider()
                    val remaining = kotlin.math.abs(accountBalances[acc.id] ?: acc.initialBalance)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { calculatorAccount = acc }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            RampIconTile(icon = getAccountIcon(acc.type), ramp = Ramp.Coral, size = 36.dp, iconSize = 18.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(acc.name, style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = "Owed: $currencySymbol%.2f".format(remaining) + (acc.interestRateApr?.let { " • %.2f%% APR".format(it) } ?: ""),
                                    style = SelfBudgetType.meta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculate payoff",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showBlankCalculator) {
        DebtPayoffCalculatorDialog(
            accounts = accounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            preselectedAccount = null,
            onDismiss = { showBlankCalculator = false }
        )
    }

    calculatorAccount?.let { acc ->
        DebtPayoffCalculatorDialog(
            accounts = accounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            preselectedAccount = acc,
            onDismiss = { calculatorAccount = null }
        )
    }
}
