package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.DebtPayoffCalculator
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapePill
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
@OptIn(ExperimentalLayoutApi::class)
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
    var showAccountDropdown by remember { mutableStateOf(false) }
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
                            Text("Payoff calculator", style = com.selfbudget.app.ui.theme.SelfBudgetType.title, color = MaterialTheme.colorScheme.onSurface)
                        }
                        // Done lives in the footer only — never duplicated in the header (spec §14/§19).
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Debt Account Selection Grouped Surface
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "SELECT DEBT ACCOUNT",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAccountDropdown = true }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RampIconTile(
                                            icon = if (selectedAccount != null) getAccountIcon(selectedAccount!!.type) else Icons.Default.EditNote,
                                            ramp = if (selectedAccount != null) Ramp.Coral else Ramp.Teal,
                                            size = 38.dp,
                                            iconSize = 20.dp
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = if (selectedAccount != null) "Linked debt account" else "Calculation mode",
                                                style = SelfBudgetType.meta,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = selectedAccount?.name ?: "Manual Entry",
                                                style = SelfBudgetType.rowTitle,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (selectedAccount != null) {
                                                val owed = kotlin.math.abs(accountBalances[selectedAccount!!.id] ?: selectedAccount!!.initialBalance)
                                                Text(
                                                    text = "Owed: $currencySymbol%.2f".format(owed) + (selectedAccount!!.interestRateApr?.let { " • %.2f%% APR".format(it) } ?: ""),
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                Text(
                                                    text = "Custom balance, APR & payment",
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Account",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                DropdownMenu(
                                    expanded = showAccountDropdown,
                                    onDismissRequest = { showAccountDropdown = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    // Manual Entry Option
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.EditNote,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Manual Entry",
                                                        fontWeight = if (selectedAccount == null) FontWeight.Medium else FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Custom amounts",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (selectedAccount == null) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectAccount(null)
                                            showAccountDropdown = false
                                        }
                                    )

                                    if (debtAccounts.isNotEmpty()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )

                                        debtAccounts.forEach { acc ->
                                            val isSelected = selectedAccount?.id == acc.id
                                            val owed = kotlin.math.abs(accountBalances[acc.id] ?: acc.initialBalance)
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        RampIconTile(
                                                            icon = getAccountIcon(acc.type),
                                                            ramp = Ramp.Coral,
                                                            size = 34.dp,
                                                            iconSize = 18.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = acc.name,
                                                                style = SelfBudgetType.rowTitle,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Owed: $currencySymbol%.2f".format(owed) + (acc.interestRateApr?.let { " • %.2f%% APR".format(it) } ?: ""),
                                                                style = SelfBudgetType.meta,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Selected",
                                                                tint = getExpenseColor(),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    selectAccount(acc)
                                                    showAccountDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Hero Target Monthly Payment Card — shared component (spec §16):
                    // every amount-entry card in the app uses this one implementation.
                    com.selfbudget.app.core.ui.components.TransactionAmountHero(
                        type = com.selfbudget.app.core.ui.components.EntryType.Expense,
                        amountText = paymentText,
                        onAmountChange = { paymentText = it },
                        currencySymbol = currencySymbol,
                        badgeText = "MONTHLY PAYMENT",
                        ramp = com.selfbudget.app.ui.theme.Ramp.Coral,
                        stepAmount = 25.0
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    com.selfbudget.app.core.ui.components.QuickAmountChips(
                        presets = listOf(25, 50, 100, 200, 500),
                        currencySymbol = currencySymbol,
                        onPick = { preset ->
                            val currentVal = paymentText.toDoubleOrNull() ?: 0.0
                            paymentText = "%.2f".format(currentVal + preset)
                        }
                    )

                    // 3. Debt Balance & APR Details Grouped Surface
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "DEBT PARAMETERS",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = balanceText,
                                        onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) balanceText = input },
                                        label = { Text("Balance Owed ($currencySymbol)") },
                                        placeholder = { Text("0.00") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = getExpenseColor(),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = ShapeChip,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = aprText,
                                        onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) aprText = input },
                                        label = { Text("APR (%)") },
                                        placeholder = { Text("24.99") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Percent,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = ShapeChip,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Projected Payoff Grouped Surface
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "PROJECTED PAYOFF",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )

                        when {
                            !canCalculate -> {
                                Surface(
                                    shape = ShapeCard,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
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
                                Surface(
                                    shape = ShapeCard,
                                    color = getExpenseColor().copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, getExpenseColor().copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = getExpenseColor())
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "This payment won't cover the monthly interest - the balance will never shrink. Increase the payment amount.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = getExpenseColor(),
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
                                Surface(
                                    shape = ShapeCard,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RampIconTile(
                                                icon = Icons.Default.Calculate,
                                                ramp = Ramp.Teal,
                                                size = 38.dp,
                                                iconSize = 20.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Debt-free in ${result.monthsToPayoff} month" + if (result.monthsToPayoff == 1) "" else "s",
                                                    style = SelfBudgetType.heading,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = (if (years > 0) "$years yr${if (years != 1) "s" else ""} $remMonths mo • " else "$remMonths mo • ") + "Paid off by $payoffDateLabel",
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            // Total interest is information, not an alarm — neutral, not red (spec §19).
                                            Text("Total interest paid", style = SelfBudgetType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                 "$currencySymbol%.2f".format(result.totalInterestPaid),
                                                 style = SelfBudgetType.rowTitle,
                                                 color = MaterialTheme.colorScheme.onSurface
                                             )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Total Paid (Balance + Interest)", style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                 "$currencySymbol%.2f".format(balance + result.totalInterestPaid),
                                                 style = SelfBudgetType.heading,
                                                 color = MaterialTheme.colorScheme.onSurface
                                             )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Action Button
                    PrimaryPillButton(
                        text = "Done",
                        onClick = onDismiss,
                        ramp = Ramp.Teal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    )

                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}
