package com.selfbudget.app.feature.budget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfbudget.app.core.ui.DebtPayoffCalculatorDialog
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType

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
        Text(text = "Payoff Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("See exactly how long payoff takes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pick a Credit Card or Loan from your wallet - or enter numbers by hand - then set any payment amount to see months to debt-free, total interest, and payoff date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showBlankCalculator = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Payoff Calculation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        if (debtAccounts.isNotEmpty()) {
            Text(
                text = "YOUR DEBT ACCOUNTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            debtAccounts.forEach { acc ->
                val accColor = try {
                    Color(android.graphics.Color.parseColor(acc.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }
                val remaining = kotlin.math.abs(accountBalances[acc.id] ?: acc.initialBalance)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { calculatorAccount = acc },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = accColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (acc.type == AccountType.CREDIT_CARD) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = accColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(acc.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Owed: $currencySymbol%.2f".format(remaining) + (acc.interestRateApr?.let { " • %.2f%% APR".format(it) } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculate Payoff",
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
