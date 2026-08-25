package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.ui.theme.ExpenseRed

@Composable
fun EditCustomAccountDialog(
    account: AccountEntity,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirm: (AccountEntity) -> Unit,
    onDelete: ((AccountEntity) -> Unit)? = null
) {
    var accountName by remember { mutableStateOf(account.name) }
    var initialBalanceText by remember { mutableStateOf(if (account.initialBalance > 0.0) "%.2f".format(account.initialBalance) else "") }
    var selectedType by remember { mutableStateOf(account.type) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(account.currencyCode) }
    var creditLimitText by remember { mutableStateOf(account.creditLimit?.let { "%.2f".format(it) } ?: "") }
    var aprText by remember { mutableStateOf(account.interestRateApr?.let { "%.2f".format(it) } ?: "") }
    var minPaymentText by remember { mutableStateOf(account.minimumPayment?.let { "%.2f".format(it) } ?: "") }

    val accountTypes = listOf(
        AccountTypeOption(AccountType.CHECKING, "Checking", Icons.Default.AccountBalance),
        AccountTypeOption(AccountType.CREDIT_CARD, "Credit Card", Icons.Default.CreditCard),
        AccountTypeOption(AccountType.CASH, "Cash Wallet", Icons.Default.Payments),
        AccountTypeOption(AccountType.SAVINGS, "Savings", Icons.Default.Savings),
        AccountTypeOption(AccountType.INVESTMENT, "Investment", Icons.Default.Wallet),
        AccountTypeOption(AccountType.LOAN, "Loan / Debt", Icons.Default.AccountBalance)
    )
    val isDebtType = selectedType == AccountType.CREDIT_CARD || selectedType == AccountType.LOAN

    val selectedOption = accountTypes.firstOrNull { it.type == selectedType } ?: accountTypes.first()
    val accColorHex = when (selectedType) {
        AccountType.CREDIT_CARD -> "#E91E63"
        AccountType.CASH -> "#4CAF50"
        AccountType.SAVINGS -> "#9C27B0"
        AccountType.INVESTMENT -> "#FF9800"
        AccountType.LOAN -> "#795548"
        else -> "#2196F3"
    }
    val accColor = try { Color(android.graphics.Color.parseColor(accColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
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
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit Payment Account",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (onDelete != null) {
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Account",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Real-Time Account Card Preview
                    Text(
                        text = "Live Account Card Preview",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(accColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = selectedOption.icon,
                                        contentDescription = null,
                                        tint = accColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = accColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = selectedOption.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = accountName.ifBlank { "Account Name" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (accountName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val balanceVal = initialBalanceText.toDoubleOrNull() ?: account.initialBalance
                            Text(
                                text = "$currencySymbol%.2f".format(balanceVal),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Account Type Selector Horizontal Chips
                    Text(
                        text = "Account Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Non-scrolling 3-row, 2-column grid layout so all 6 Account Types are cleanly visible at once with no text cut-off
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accountTypes.chunked(2).forEach { rowOptions ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowOptions.forEach { option ->
                                    val isSelected = selectedType == option.type
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedType = option.type },
                                        label = {
                                            Text(
                                                text = option.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(imageVector = option.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Account Name Field
                    val accountNamePlaceholder = when (selectedType) {
                        AccountType.CREDIT_CARD -> "e.g. Credit Card"
                        AccountType.CASH -> "e.g. Cash Wallet"
                        AccountType.SAVINGS -> "e.g. Savings Account"
                        AccountType.INVESTMENT -> "e.g. Investment"
                        AccountType.LOAN -> "e.g. Car Loan"
                        else -> "e.g. Checking Account"
                    }

                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { input ->
                            accountName = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        },
                        label = { Text("Account Name") },
                        placeholder = { Text(accountNamePlaceholder) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Initial Balance Field
                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                initialBalanceText = input
                            }
                        },
                        label = { Text("Starting Balance") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isDebtType) {
                        Text(
                            text = "Debt Details (optional)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = creditLimitText,
                            onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) creditLimitText = input },
                            label = { Text("Credit Limit") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = aprText,
                            onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) aprText = input },
                            label = { Text("Interest Rate / APR (%)") },
                            placeholder = { Text("e.g. 24.99") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = minPaymentText,
                            onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) minPaymentText = input },
                            label = { Text("Minimum Monthly Payment") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        val estimate = remember(creditLimitText, aprText, minPaymentText, initialBalanceText) {
                            val balance = initialBalanceText.toDoubleOrNull()?.let { kotlin.math.abs(it) } ?: 0.0
                            val apr = aprText.toDoubleOrNull()
                            val payment = minPaymentText.toDoubleOrNull()
                            if (apr != null && payment != null && balance > 0.0) {
                                com.selfbudget.app.core.util.DebtPayoffCalculator.estimatePayoff(balance, apr, payment)
                            } else null
                        }
                        if (estimate != null && !estimate.isPaymentTooLow) {
                            Text(
                                text = "Estimated payoff: ${estimate.monthsToPayoff} months • ~$currencySymbol${"%.2f".format(estimate.totalInterestPaid)} total interest",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (estimate != null && estimate.isPaymentTooLow) {
                            Text(
                                text = "This monthly payment won't cover the interest — balance will keep growing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // In-Form Action Buttons (Cancel | Save Changes)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                if (accountName.isNotBlank()) {
                                    val balance = initialBalanceText.toDoubleOrNull() ?: account.initialBalance
                                    val updatedAcc = account.copy(
                                        name = accountName.trim(),
                                        type = selectedType,
                                        initialBalance = balance,
                                        colorHex = accColorHex,
                                        currencyCode = selectedCurrency,
                                        creditLimit = if (isDebtType) creditLimitText.toDoubleOrNull() else null,
                                        interestRateApr = if (isDebtType) aprText.toDoubleOrNull() else null,
                                        minimumPayment = if (isDebtType) minPaymentText.toDoubleOrNull() else null
                                    )
                                    onConfirm(updatedAcc)
                                }
                            },
                            enabled = accountName.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    if (onDelete != null) {
                        androidx.compose.material3.TextButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Delete Payment Account",
                                color = ExpenseRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Sticky Bottom Action Bar
                Surface(
                    shadowElevation = 12.dp,
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                if (accountName.isNotBlank()) {
                                    val balance = initialBalanceText.toDoubleOrNull() ?: account.initialBalance
                                    val updatedAcc = account.copy(
                                        name = accountName.trim(),
                                        type = selectedType,
                                        initialBalance = balance,
                                        colorHex = accColorHex,
                                        currencyCode = selectedCurrency,
                                        creditLimit = if (isDebtType) creditLimitText.toDoubleOrNull() else null,
                                        interestRateApr = if (isDebtType) aprText.toDoubleOrNull() else null,
                                        minimumPayment = if (isDebtType) minPaymentText.toDoubleOrNull() else null
                                    )
                                    onConfirm(updatedAcc)
                                }
                            },
                            enabled = accountName.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // Custom Delete Confirmation Modal
        if (showDeleteConfirmation && onDelete != null) {
            Dialog(onDismissRequest = { showDeleteConfirmation = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ExpenseRed.copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Delete Payment Account?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Are you sure you want to delete \"${account.name}\"? Associated transaction records will remain saved.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmation = false },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onDelete(account)
                                    showDeleteConfirmation = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text("Delete", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
