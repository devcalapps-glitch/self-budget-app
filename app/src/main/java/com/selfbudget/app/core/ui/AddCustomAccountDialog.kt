package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Wallet
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.ui.components.EntryType
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.QuickAmountChips
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.TransactionAmountHero
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.core.util.toWordTitleCase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import java.util.UUID

internal data class AccountTypeOption(
    val type: AccountType,
    val label: String,
    val icon: ImageVector
)

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and this.toArgb())

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCustomAccountDialog(
    currencySymbol: String = "$",
    initialType: AccountType = AccountType.CHECKING,
    onDismiss: () -> Unit,
    onConfirm: (AccountEntity) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var initialBalanceText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var accountTypeExpanded by remember { mutableStateOf(false) }
    // New accounts just inherit the app's currency preference (Settings) - no separate picker
    // here. EditCustomAccountDialog still lets you change an individual account's currency later
    // for the rare case of a genuinely foreign-currency account.
    val selectedCurrency = Currencies.codeForSymbol(currencySymbol)
    var creditLimitText by remember { mutableStateOf("") }
    var aprText by remember { mutableStateOf("") }
    var minPaymentText by remember { mutableStateOf("") }
    var loanTermMonthsText by remember { mutableStateOf("") }

    val accountTypes = listOf(
        AccountTypeOption(AccountType.CHECKING, "Checking", Icons.Default.AccountBalance),
        AccountTypeOption(AccountType.CREDIT_CARD, "Credit Card", Icons.Default.CreditCard),
        AccountTypeOption(AccountType.CASH, "Cash Wallet", Icons.Default.Payments),
        AccountTypeOption(AccountType.SAVINGS, "Savings", Icons.Default.Savings),
        AccountTypeOption(AccountType.REAL_ESTATE, "Real Estate / Property", Icons.Default.Home),
        AccountTypeOption(AccountType.MORTGAGE, "Mortgage (Home Loan)", Icons.Default.Home),
        AccountTypeOption(AccountType.VEHICLE, "Vehicle / Auto", Icons.Default.DirectionsCar),
        AccountTypeOption(AccountType.AUTO_LOAN, "Auto Loan", Icons.Default.DirectionsCar),
        AccountTypeOption(AccountType.STUDENT_LOAN, "Student Loan", Icons.Default.School),
        AccountTypeOption(AccountType.INVESTMENT, "Investment", Icons.Default.Wallet),
        AccountTypeOption(AccountType.RETIREMENT, "Retirement (Non-Liquid)", RetirementAccountIcon),
        AccountTypeOption(AccountType.LOAN, "Personal / Other Loan", Icons.Default.AccountBalance)
    )
    val isDebtType = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(selectedType)
    val isLoanType = selectedType in listOf(AccountType.MORTGAGE, AccountType.AUTO_LOAN, AccountType.STUDENT_LOAN, AccountType.LOAN)
    val isAsset = selectedType in listOf(AccountType.INVESTMENT, AccountType.RETIREMENT, AccountType.REAL_ESTATE, AccountType.VEHICLE)
    val isDark = isAppInDarkTheme()

    val selectedOption = accountTypes.firstOrNull { it.type == selectedType } ?: accountTypes.first()
    // Account color follows its type's design-system section identity (spec's
    // section-identity ramps), rather than a bespoke per-type hex map.
    val accRamp = accountTypeRamp(selectedType)
    val accColor = accRamp.c400
    val accColorHex = accColor.toHex()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // No header Save action (spec §14/§16): the header holds only close +
                // title. Save is triggered from the footer button below.
                TopAppBar(
                    title = {
                        Text(
                            text = if (isAsset) "Add asset" else "Add account",
                            style = SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        CircularBackButton(onClick = onDismiss, modifier = Modifier.padding(start = 12.dp, end = 8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Hero Balance Card — shared component (spec §16): every amount-entry
                    // card in the app uses this one implementation, not a per-screen copy.
                    TransactionAmountHero(
                        type = EntryType.Income,
                        amountText = initialBalanceText,
                        onAmountChange = { initialBalanceText = it },
                        currencySymbol = currencySymbol,
                        badgeText = if (isAsset) "ESTIMATED VALUE" else "STARTING BALANCE",
                        ramp = accRamp,
                        stepAmount = 50.0
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    QuickAmountChips(
                        presets = listOf(100, 500, 1000, 2000),
                        currencySymbol = currencySymbol,
                        onPick = { preset ->
                            val currentVal = initialBalanceText.toDoubleOrNull() ?: 0.0
                            initialBalanceText = "%.2f".format(currentVal + preset)
                        }
                    )

                    // 2. Account Details in Grouped Surface
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isAsset) "ASSET DETAILS" else "ACCOUNT DETAILS",
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, accRamp.containerBorder(isDark)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Account Type Selector Tile
                                Surface(
                                    shape = ShapeChip,
                                    color = Ramp.Gray.tintFill(isDark),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { accountTypeExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            RampIconTile(icon = selectedOption.icon, ramp = accRamp, size = 34.dp, iconSize = 18.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isAsset) "ASSET TYPE" else "ACCOUNT TYPE",
                                                    style = SelfBudgetType.eyebrow,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = selectedOption.label,
                                                    style = SelfBudgetType.rowTitle,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(14.dp))

                                // Account Name Field
                                val accountNamePlaceholder = when (selectedType) {
                                    AccountType.CREDIT_CARD -> "Credit Card"
                                    AccountType.CASH -> "Cash Wallet"
                                    AccountType.SAVINGS -> "Savings Account"
                                    AccountType.INVESTMENT -> "Brokerage / Stocks / Crypto"
                                    AccountType.RETIREMENT -> "401(k) / IRA"
                                    AccountType.REAL_ESTATE -> "Home / Property"
                                    AccountType.VEHICLE -> "Car / Vehicle"
                                    AccountType.MORTGAGE -> "Home Mortgage"
                                    AccountType.AUTO_LOAN -> "Auto Loan"
                                    AccountType.STUDENT_LOAN -> "Student Loan"
                                    AccountType.LOAN -> "Personal Loan"
                                    else -> "Checking Account"
                                }

                                OutlinedTextField(
                                    value = accountName,
                                    onValueChange = { input ->
                                        accountName = input.toWordTitleCase()
                                    },
                                    label = { Text(if (isAsset) "Asset name" else "Account name") },
                                    placeholder = { Text(accountNamePlaceholder) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    shape = ShapeChip,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 3. Optional Debt / Credit Details
                    if (isDebtType) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isLoanType) "LOAN & PAYMENT DETAILS (OPTIONAL)" else "DEBT & CREDIT DETAILS (OPTIONAL)",
                                style = SelfBudgetType.eyebrow,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )

                            Surface(
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.5.dp, accRamp.containerBorder(isDark)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (isLoanType) {
                                        // Side-by-Side APR % & Loan Term (Months)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = aprText,
                                                onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) aprText = input },
                                                label = { Text("APR (%)") },
                                                placeholder = { Text(if (selectedType == AccountType.MORTGAGE) "6.5" else "7.99") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                shape = ShapeChip,
                                                modifier = Modifier.weight(1f)
                                            )

                                            OutlinedTextField(
                                                value = loanTermMonthsText,
                                                onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d{0,4}$"""))) loanTermMonthsText = input },
                                                label = { Text("Loan term (months)") },
                                                placeholder = { Text(if (selectedType == AccountType.MORTGAGE) "360" else "60") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                shape = ShapeChip,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // Quick Term Presets
                                        val termPresets = if (selectedType == AccountType.MORTGAGE) {
                                            listOf(180 to "15 yrs", 240 to "20 yrs", 360 to "30 yrs")
                                        } else {
                                            listOf(36 to "3 yrs", 48 to "4 yrs", 60 to "5 yrs", 72 to "6 yrs", 84 to "7 yrs")
                                        }

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            termPresets.forEach { (months, label) ->
                                                val isSelected = loanTermMonthsText == months.toString()
                                                Surface(
                                                    shape = ShapePill,
                                                    color = if (isSelected) accRamp.tintFill(isDark) else Ramp.Gray.tintFill(isDark),
                                                    border = BorderStroke(
                                                        0.5.dp,
                                                        if (isSelected) accRamp.c400 else MaterialTheme.colorScheme.outlineVariant
                                                    ),
                                                    modifier = Modifier.clickable {
                                                        loanTermMonthsText = if (isSelected) "" else months.toString()
                                                    }
                                                ) {
                                                    Text(
                                                        text = "$label ($months mo)",
                                                        style = SelfBudgetType.meta,
                                                        color = if (isSelected) accRamp.titleText(isDark) else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = minPaymentText,
                                            onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) minPaymentText = input },
                                            label = { Text("Monthly payment ($currencySymbol)") },
                                            placeholder = { Text("Calculated automatically or custom") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = ShapeChip,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = creditLimitText,
                                            onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) creditLimitText = input },
                                            label = { Text("Original loan amount ($currencySymbol)") },
                                            placeholder = { Text("0.00") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = ShapeChip,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        // Side-by-Side Credit Limit & APR % fields
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = creditLimitText,
                                                onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) creditLimitText = input },
                                                label = { Text("Credit limit ($currencySymbol)") },
                                                placeholder = { Text("0.00") },
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
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                shape = ShapeChip,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = minPaymentText,
                                            onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) minPaymentText = input },
                                            label = { Text("Minimum monthly payment ($currencySymbol)") },
                                            placeholder = { Text("0.00") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = ShapeChip,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Live Account Card Preview
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "LIVE ACCOUNT PREVIEW",
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, accRamp.containerBorder(isDark)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RampIconTile(icon = selectedOption.icon, ramp = accRamp, size = 40.dp, iconSize = 22.dp)

                                    Surface(shape = ShapePill, color = accRamp.tintFill(isDark)) {
                                        Text(
                                            text = selectedOption.label,
                                            style = SelfBudgetType.badge,
                                            color = accRamp.titleText(isDark),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = accountName.ifBlank { "Account name" },
                                    style = SelfBudgetType.heading,
                                    color = if (accountName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val balanceVal = initialBalanceText.toDoubleOrNull() ?: 0.0
                                Text(
                                    text = "$currencySymbol%.2f".format(balanceVal),
                                    style = SelfBudgetType.display,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            ramp = Ramp.Gray,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )

                        PrimaryPillButton(
                            text = if (isAsset) "Save asset" else "Save account",
                            onClick = {
                                if (accountName.isNotBlank()) {
                                    val rawBalance = initialBalanceText.toDoubleOrNull() ?: 0.0
                                    val balance = if (isDebtType && rawBalance > 0.0) -rawBalance else rawBalance
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
                                        minimumPayment = if (isDebtType) minPaymentText.toDoubleOrNull() else null,
                                        loanTermMonths = if (isDebtType) loanTermMonthsText.toIntOrNull() else null
                                    )
                                    onConfirm(newAcc)
                                }
                            },
                            enabled = accountName.isNotBlank(),
                            ramp = accRamp,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }

        if (accountTypeExpanded) {
            AccountTypeSelectionModal(
                selectedType = selectedType,
                onDismiss = { accountTypeExpanded = false },
                onSelectType = { type ->
                    selectedType = type
                    accountTypeExpanded = false
                }
            )
        }
    }
}
