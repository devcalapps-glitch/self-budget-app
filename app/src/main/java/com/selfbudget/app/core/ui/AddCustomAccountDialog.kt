package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import java.util.UUID

internal data class AccountTypeOption(
    val type: AccountType,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomAccountDialog(
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirm: (AccountEntity) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var initialBalanceText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.CHECKING) }
    var accountTypeExpanded by remember { mutableStateOf(false) }
    // New accounts just inherit the app's currency preference (Settings) - no separate picker
    // here. EditCustomAccountDialog still lets you change an individual account's currency later
    // for the rare case of a genuinely foreign-currency account.
    val selectedCurrency = Currencies.codeForSymbol(currencySymbol)
    var creditLimitText by remember { mutableStateOf("") }
    var aprText by remember { mutableStateOf("") }
    var minPaymentText by remember { mutableStateOf("") }

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
            decorFitsSystemWindows = true
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
                                text = "Add Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (accountName.isNotBlank()) {
                                    val balance = initialBalanceText.toDoubleOrNull() ?: 0.0
                                    val newAcc = AccountEntity(
                                        id = "acc_custom_${UUID.randomUUID()}",
                                        userId = "custom",
                                        name = accountName.trim(),
                                        type = selectedType,
                                        initialBalance = balance,
                                        colorHex = accColorHex,
                                        currencyCode = selectedCurrency,
                                        creditLimit = if (isDebtType) creditLimitText.toDoubleOrNull() else null,
                                        interestRateApr = if (isDebtType) aprText.toDoubleOrNull() else null,
                                        minimumPayment = if (isDebtType) minPaymentText.toDoubleOrNull() else null
                                    )
                                    onConfirm(newAcc)
                                }
                            },
                            enabled = accountName.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    // 1. Top Amount Entry Stepper (- $0.00 +) without card wrapping
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "STARTING BALANCE ($currencySymbol)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Minus Button
                            Surface(
                                onClick = {
                                    val current = initialBalanceText.toDoubleOrNull() ?: 0.0
                                    val next = maxOf(0.0, current - 50.0)
                                    initialBalanceText = if (next == 0.0) "" else "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Subtract Balance",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Centered Big Amount Field (44.sp ExtraBold)
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedTextField(
                                    value = initialBalanceText,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                            initialBalanceText = input
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            text = "0.00",
                                            style = TextStyle(
                                                fontSize = 44.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = accColor.copy(alpha = 0.35f),
                                                textAlign = TextAlign.Center
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    prefix = {
                                        Text(
                                            text = currencySymbol,
                                            style = TextStyle(
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accColor
                                            ),
                                            modifier = Modifier.padding(end = 2.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accColor,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Plus Button
                            Surface(
                                onClick = {
                                    val current = initialBalanceText.toDoubleOrNull() ?: 0.0
                                    val next = current + 50.0
                                    initialBalanceText = "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = accColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Balance",
                                        tint = accColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Preset Amount Chips ($100, $500, $1000, $5000)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(100, 500, 1000, 5000).forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable {
                                        val currentVal = initialBalanceText.toDoubleOrNull() ?: 0.0
                                        initialBalanceText = "%.2f".format(currentVal + preset)
                                    }
                                ) {
                                    Text(
                                        text = "+$currencySymbol$preset",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 2. Account Type Dropdown List
                    ExposedDropdownMenuBox(
                        expanded = accountTypeExpanded,
                        onExpandedChange = { accountTypeExpanded = !accountTypeExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedOption.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account Type") },
                            leadingIcon = {
                                Icon(
                                    imageVector = selectedOption.icon,
                                    contentDescription = null,
                                    tint = accColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountTypeExpanded)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = accountTypeExpanded,
                            onDismissRequest = { accountTypeExpanded = false }
                        ) {
                            accountTypes.forEach { option ->
                                val isSelected = selectedType == option.type
                                val optionColorHex = when (option.type) {
                                    AccountType.CREDIT_CARD -> "#E91E63"
                                    AccountType.CASH -> "#4CAF50"
                                    AccountType.SAVINGS -> "#9C27B0"
                                    AccountType.INVESTMENT -> "#FF9800"
                                    AccountType.LOAN -> "#795548"
                                    else -> "#2196F3"
                                }
                                val optionColor = try { Color(android.graphics.Color.parseColor(optionColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) optionColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            tint = optionColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        selectedType = option.type
                                        accountTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. Account Name Field
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

                    // 4. Optional Debt / Credit Details (Moved before Hero Card, side-by-side Credit Limit & APR)
                    if (isDebtType) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Debt / Credit Details (optional)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Side-by-Side Credit Limit & APR % fields
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = creditLimitText,
                                    onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) creditLimitText = input },
                                    label = { Text("Credit Limit ($currencySymbol)") },
                                    placeholder = { Text("0.00") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = aprText,
                                    onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) aprText = input },
                                    label = { Text("APR (%)") },
                                    placeholder = { Text("24.99") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = minPaymentText,
                                onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) minPaymentText = input },
                                label = { Text("Minimum Monthly Payment ($currencySymbol)") },
                                placeholder = { Text("0.00") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 5. Real-Time Account Card Preview (Hero Card)
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

                            val balanceVal = initialBalanceText.toDoubleOrNull() ?: 0.0
                            Text(
                                text = "$currencySymbol%.2f".format(balanceVal),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                if (accountName.isNotBlank()) {
                                    val balance = initialBalanceText.toDoubleOrNull() ?: 0.0
                                    val newAcc = AccountEntity(
                                        id = "acc_custom_${UUID.randomUUID()}",
                                        userId = "custom",
                                        name = accountName.trim(),
                                        type = selectedType,
                                        initialBalance = balance,
                                        colorHex = accColorHex,
                                        currencyCode = selectedCurrency,
                                        creditLimit = if (isDebtType) creditLimitText.toDoubleOrNull() else null,
                                        interestRateApr = if (isDebtType) aprText.toDoubleOrNull() else null,
                                        minimumPayment = if (isDebtType) minPaymentText.toDoubleOrNull() else null
                                    )
                                    onConfirm(newAcc)
                                }
                            },
                            enabled = accountName.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                        ) {
                            Text("Save Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
