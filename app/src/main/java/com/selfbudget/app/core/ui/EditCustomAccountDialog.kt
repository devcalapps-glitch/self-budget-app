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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.getIncomeColor

private data class AccountEditSnapshot(
    val name: String,
    val type: AccountType,
    val balanceText: String,
    val currency: String,
    val creditLimitText: String,
    val aprText: String,
    val minPaymentText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomAccountDialog(
    account: AccountEntity,
    currentBalance: Double? = null,
    currencySymbol: String = "$",
    goals: List<GoalEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (AccountEntity) -> Unit,
    onDelete: ((AccountEntity) -> Unit)? = null
) {
    val displayStartingBalance = currentBalance ?: account.initialBalance
    val txDelta = (currentBalance ?: account.initialBalance) - account.initialBalance

    var accountName by remember { mutableStateOf(account.name) }
    var selectedType by remember { mutableStateOf(account.type) }
    val isDebtType = selectedType == AccountType.CREDIT_CARD || selectedType == AccountType.LOAN

    var initialBalanceText by remember {
        mutableStateOf(
            if (displayStartingBalance != 0.0) "%.2f".format(if (isDebtType && displayStartingBalance < 0.0) kotlin.math.abs(displayStartingBalance) else displayStartingBalance) else ""
        )
    }
    var accountTypeExpanded by remember { mutableStateOf(false) }
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
        AccountTypeOption(AccountType.LOAN, "Loan / Debt", Icons.Default.AccountBalance),
        AccountTypeOption(AccountType.RETIREMENT, "Retirement (Non-Liquid)", RetirementAccountIcon)
    )

    val selectedOption = accountTypes.firstOrNull { it.type == selectedType } ?: accountTypes.first()
    val accColorHex = when (selectedType) {
        AccountType.CREDIT_CARD -> "#E91E63"
        AccountType.CASH -> "#4CAF50"
        AccountType.SAVINGS -> "#9C27B0"
        AccountType.INVESTMENT -> "#FF9800"
        AccountType.LOAN -> "#795548"
        AccountType.RETIREMENT -> "#607D8B"
        else -> "#2196F3"
    }
    val accColor = try { Color(android.graphics.Color.parseColor(accColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

    // Dialog opens read-only; tapping "Edit" is the deliberate action that unlocks the form.
    var isEditMode by remember { mutableStateOf(false) }
    var editBaseline by remember { mutableStateOf<AccountEditSnapshot?>(null) }

    fun captureEditSnapshot() = AccountEditSnapshot(
        name = accountName,
        type = selectedType,
        balanceText = initialBalanceText,
        currency = selectedCurrency,
        creditLimitText = creditLimitText,
        aprText = aprText,
        minPaymentText = minPaymentText
    )

    val isDirty = editBaseline != null && editBaseline != captureEditSnapshot()

    fun enterEditMode() {
        editBaseline = captureEditSnapshot()
        isEditMode = true
    }

    fun buildUpdatedAccount(): AccountEntity {
        val rawEntered = initialBalanceText.toDoubleOrNull() ?: kotlin.math.abs(displayStartingBalance)
        val signedEntered = if (isDebtType && rawEntered > 0.0) -rawEntered else rawEntered
        val targetInitialBalance = signedEntered - txDelta
        return account.copy(
            name = accountName.trim(),
            type = selectedType,
            initialBalance = targetInitialBalance,
            colorHex = accColorHex,
            currencyCode = selectedCurrency,
            creditLimit = if (isDebtType) creditLimitText.toDoubleOrNull() else null,
            interestRateApr = if (isDebtType) aprText.toDoubleOrNull() else null,
            minimumPayment = if (isDebtType) minPaymentText.toDoubleOrNull() else null
        )
    }

    fun trySave() {
        if (accountName.isNotBlank()) {
            onConfirm(buildUpdatedAccount())
        }
    }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEditMode) "Edit Payment Account" else "Account Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                if (isEditMode) {
                                    trySave()
                                } else {
                                    enterEditMode()
                                }
                            },
                            enabled = !isEditMode || (isDirty && accountName.isNotBlank()),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isEditMode) "Save" else "Edit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    if (!isEditMode) {
                        AccountViewModeSummary(
                            accountName = accountName,
                            typeLabel = selectedOption.label,
                            typeIcon = selectedOption.icon,
                            accColor = accColor,
                            currencySymbol = currencySymbol,
                            balance = displayStartingBalance,
                            initialBalance = account.initialBalance,
                            isDebtType = isDebtType,
                            creditLimitText = creditLimitText,
                            aprText = aprText,
                            minPaymentText = minPaymentText,
                            onEditClick = { enterEditMode() },
                            onDeleteClick = { showDeleteConfirmation = true },
                            onClose = onDismiss,
                            canDelete = onDelete != null
                        )
                    } else {

                    // 1. Top Amount Entry Stepper (- $0.00 +) without card wrapping
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CURRENT ACCOUNT BALANCE ($currencySymbol)",
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
                                    val current = initialBalanceText.toDoubleOrNull() ?: account.initialBalance
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
                                            text = "%.2f".format(account.initialBalance),
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
                                    val current = initialBalanceText.toDoubleOrNull() ?: account.initialBalance
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
                                        val currentVal = initialBalanceText.toDoubleOrNull() ?: account.initialBalance
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

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        append("Initial Opening Balance: ")
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                                            append("$currencySymbol%.2f".format(account.initialBalance))
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                                    AccountType.RETIREMENT -> "#607D8B"
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
                        AccountType.RETIREMENT -> "e.g. 401(k), IRA"
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

                    } // end isEditMode form fields

                    // Linked Savings Goals Section & Cash Availability Summary
                    val linkedGoals = remember(goals, account.id) { goals.filter { it.linkedAccountId == account.id } }
                    val totalEarmarked = remember(linkedGoals, displayStartingBalance) {
                        linkedGoals.sumOf { if (it.savedAmount > 0) it.savedAmount else minOf(displayStartingBalance, it.targetAmount) }
                    }
                    val availableToSpend = remember(displayStartingBalance, totalEarmarked) {
                        (displayStartingBalance - totalEarmarked).coerceAtLeast(0.0)
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "LINKED SAVINGS GOALS & CASH AVAILABILITY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (linkedGoals.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    linkedGoals.forEach { goal ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Savings, contentDescription = null, tint = getIncomeColor(), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = goal.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Text(
                                                text = "$currencySymbol%.2f Goal Target".format(goal.targetAmount),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = getIncomeColor()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Available to Spend", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = getIncomeColor())
                                        Text("$currencySymbol%.2f".format(availableToSpend), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = getIncomeColor())
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Earmarked for Goals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("-$currencySymbol%.2f".format(totalEarmarked), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Account Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$currencySymbol%.2f".format(displayStartingBalance), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No savings goals linked to this account yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    if (isEditMode) {

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

                            val balanceVal = initialBalanceText.toDoubleOrNull() ?: account.initialBalance
                            Text(
                                text = "$currencySymbol%.2f".format(balanceVal),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isDebtType) {
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

                    // Action Buttons Layout:
                    // Row 1: [ Cancel ] | [ Save Changes ]
                    // Row 2: [ 🗑️ Delete Account ] (if onDelete != null)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                onClick = { trySave() },
                                enabled = isDirty && accountName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                            ) {
                                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        if (onDelete != null) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, ExpenseRed.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                            ) {
                                Text("Delete Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    } // end isEditMode preview + inline actions

                    Spacer(modifier = Modifier.height(150.dp))
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

@Composable
private fun AccountViewModeSummary(
    accountName: String,
    typeLabel: String,
    typeIcon: ImageVector,
    accColor: Color,
    currencySymbol: String,
    balance: Double,
    initialBalance: Double,
    isDebtType: Boolean,
    creditLimitText: String,
    aprText: String,
    minPaymentText: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClose: () -> Unit,
    canDelete: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CURRENT ACCOUNT BALANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currencySymbol%.2f".format(balance),
                style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = accColor)
            )
        }

        AccountInfoRow(icon = Icons.Default.Info, label = "Initial Opening Balance", value = "$currencySymbol%.2f".format(initialBalance))
        AccountInfoRow(icon = typeIcon, label = "Account Type", value = typeLabel, valueColor = accColor)
        AccountInfoRow(icon = Icons.Default.AccountBalance, label = "Account Name", value = accountName)

        if (isDebtType) {
            if (creditLimitText.isNotBlank()) {
                AccountInfoRow(icon = Icons.Default.CreditCard, label = "Credit Limit", value = "$currencySymbol$creditLimitText")
            }
            if (aprText.isNotBlank()) {
                AccountInfoRow(icon = Icons.Default.Info, label = "APR", value = "$aprText%")
            }
            if (minPaymentText.isNotBlank()) {
                AccountInfoRow(icon = Icons.Default.Payments, label = "Minimum Monthly Payment", value = "$currencySymbol$minPaymentText")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Layout:
        // Row 1: [ Close ] | [ Edit Account ]
        // Row 2: [ 🗑️ Delete Account ] (if canDelete)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Edit Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            if (canDelete) {
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, ExpenseRed.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                ) {
                    Text("Delete Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun AccountInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor
                )
            }
        }
    }
}
