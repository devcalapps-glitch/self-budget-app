package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.DebtPayoffCalculator
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.getIncomeColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * "What if I paid $X/month?" projection tool. Lets the user pick any Credit Card / Loan account
 * from the wallet (pre-filling its stored balance/APR/minimum payment) - or skip straight to
 * manual entry - then override any field to see months-to-payoff, total interest, and a projected
 * payoff date via DebtPayoffCalculator.estimatePayoff. Purely a scratchpad: nothing here is saved
 * to the account or the ledger, unlike DebtPayoffAnalyticsModal which tracks real historical
 * payoff progress from posted transactions.
 */
@Composable
fun DebtPayoffCalculatorDialog(
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double>,
    currencySymbol: String = "$",
    preselectedAccount: AccountEntity? = null,
    onDismiss: () -> Unit
) {
    val debtAccounts = remember(accounts) {
        accounts.filter { AccountBalanceCalculator.isLiability(it.type) }
    }

    fun prefillFor(acc: AccountEntity?): Triple<String, String, String> {
        if (acc == null) return Triple("", "", "")
        val owed = kotlin.math.abs(accountBalances[acc.id] ?: acc.initialBalance)
        return Triple(
            if (owed > 0) "%.2f".format(owed) else "",
            acc.interestRateApr?.let { "%.2f".format(it) } ?: "",
            acc.minimumPayment?.let { "%.2f".format(it) } ?: ""
        )
    }

    var selectedAccount by remember { mutableStateOf(preselectedAccount) }
    val initialFields = remember { prefillFor(preselectedAccount) }
    var balanceText by remember { mutableStateOf(initialFields.first) }
    var aprText by remember { mutableStateOf(initialFields.second) }
    var paymentText by remember { mutableStateOf(initialFields.third) }

    fun selectAccount(acc: AccountEntity?) {
        selectedAccount = acc
        val (b, a, p) = prefillFor(acc)
        balanceText = b
        aprText = a
        paymentText = p
    }

    val balance = balanceText.toDoubleOrNull() ?: 0.0
    val apr = aprText.toDoubleOrNull() ?: 0.0
    val payment = paymentText.toDoubleOrNull() ?: 0.0
    val canCalculate = balance > 0.0 && payment > 0.0

    val result = remember(balance, apr, payment, canCalculate) {
        if (canCalculate) DebtPayoffCalculator.estimatePayoff(balance, apr, payment) else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Payoff Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (debtAccounts.isNotEmpty()) {
                        Column {
                            Text(
                                text = "DEBT ACCOUNT (OPTIONAL)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                FilterChip(
                                    selected = selectedAccount == null,
                                    onClick = { selectAccount(null) },
                                    label = { Text("Manual Entry", fontWeight = FontWeight.Bold) }
                                )
                                debtAccounts.forEach { acc ->
                                    FilterChip(
                                        selected = selectedAccount?.id == acc.id,
                                        onClick = { selectAccount(acc) },
                                        label = { Text(acc.name, fontWeight = FontWeight.Bold) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (acc.type == AccountType.CREDIT_CARD) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) balanceText = input },
                        label = { Text("Current Balance Owed ($currencySymbol)") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = aprText,
                            onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) aprText = input },
                            label = { Text("Interest Rate APR (%)") },
                            placeholder = { Text("24.99") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paymentText,
                            onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) paymentText = input },
                            label = { Text("Monthly Payment ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Projected Payoff",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when {
                        !canCalculate -> {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Enter a balance and a monthly payment to see how long payoff will take.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                        result != null && result.isPaymentTooLow -> {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "This payment won't cover the monthly interest - the balance will never shrink. Increase the payment amount.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        result != null -> {
                            val years = result.monthsToPayoff / 12
                            val remMonths = result.monthsToPayoff % 12
                            val payoffDateLabel = remember(result.monthsToPayoff) {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.MONTH, result.monthsToPayoff)
                                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                            }
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = getIncomeColor().copy(alpha = 0.15f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Calculate, contentDescription = null, tint = getIncomeColor(), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Debt-free in ${result.monthsToPayoff} month" + if (result.monthsToPayoff == 1) "" else "s",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = (if (years > 0) "$years yr${if (years != 1) "s" else ""} $remMonths mo • " else "$remMonths mo • ") + "Paid off by $payoffDateLabel",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Interest Paid", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "$currencySymbol%.2f".format(result.totalInterestPaid),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Paid (Balance + Interest)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "$currencySymbol%.2f".format(balance + result.totalInterestPaid),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}
