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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.ui.components.DestructivePillButton
import com.selfbudget.app.core.ui.components.EntryType
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.QuickAmountChips
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.TransactionAmountHero
import com.selfbudget.app.core.util.toWordTitleCase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePage
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.icon
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText

data class AccountEditSnapshot(
    val name: String,
    val type: AccountType,
    val balanceText: String,
    val currency: String,
    val creditLimitText: String,
    val aprText: String,
    val minPaymentText: String,
    val loanTermMonthsText: String
)

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and this.toArgb())

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var accountName by remember { mutableStateOf(account.name) }
    var selectedType by remember { mutableStateOf(account.type) }
    val isDebtType = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(selectedType)
    val isLoanType = selectedType in listOf(AccountType.MORTGAGE, AccountType.AUTO_LOAN, AccountType.STUDENT_LOAN, AccountType.LOAN)
    val isAsset = selectedType in listOf(AccountType.INVESTMENT, AccountType.RETIREMENT, AccountType.REAL_ESTATE, AccountType.VEHICLE)
    val liveBalance = currentBalance ?: account.initialBalance
    val liveDisplayBalance = if (isDebtType) kotlin.math.abs(liveBalance) else liveBalance
    var initialBalanceText by remember {
        mutableStateOf(
            if (kotlin.math.abs(liveDisplayBalance) > 0.0001) "%.2f".format(liveDisplayBalance) else "0.00"
        )
    }
    val txDelta = remember(account, currentBalance) {
        val cur = currentBalance ?: account.initialBalance
        cur - account.initialBalance
    }
    var accountTypeExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(account.currencyCode) }
    var creditLimitText by remember { mutableStateOf(account.creditLimit?.let { "%.2f".format(it) } ?: "") }
    var aprText by remember { mutableStateOf(account.interestRateApr?.let { "%.2f".format(it) } ?: "") }
    var minPaymentText by remember { mutableStateOf(account.minimumPayment?.let { "%.2f".format(it) } ?: "") }
    var loanTermMonthsText by remember { mutableStateOf(account.loanTermMonths?.toString() ?: "") }
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

    val selectedOption = accountTypes.firstOrNull { it.type == selectedType } ?: accountTypes.first()
    // Account color follows its type's design-system section identity, rather
    // than a bespoke per-type hex map (spec §2 "one-ramp rule").
    val accRamp = accountTypeRamp(selectedType)
    val accColorHex = accRamp.c400.toHex()
    val isDark = isAppInDarkTheme()

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
        minPaymentText = minPaymentText,
        loanTermMonthsText = loanTermMonthsText
    )

    val isDirty = editBaseline != null && editBaseline != captureEditSnapshot()

    fun enterEditMode() {
        editBaseline = captureEditSnapshot()
        isEditMode = true
    }

    fun buildUpdatedAccount(): AccountEntity {
        val rawEntered = initialBalanceText.toDoubleOrNull() ?: liveDisplayBalance
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
            minimumPayment = if (isDebtType) minPaymentText.toDoubleOrNull() else null,
            loanTermMonths = if (isDebtType) loanTermMonthsText.toIntOrNull() else null
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
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // No header Edit/Save action (spec §14/§16): the header holds only close +
                // title. Edit is triggered from the view-mode footer; Save lives in the
                // edit-mode footer below.
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEditMode) {
                                if (isAsset) "Edit asset" else "Edit account"
                            } else {
                                if (isAsset) "Asset details" else "Account details"
                            },
                            style = SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    if (!isEditMode) {
                        AccountViewModeSummary(
                            accountName = accountName,
                            accountType = selectedType,
                            typeLabel = selectedOption.label,
                            typeIcon = selectedOption.icon,
                            accRamp = accRamp,
                            currencySymbol = currencySymbol,
                            balance = liveDisplayBalance,
                            initialBalance = if (isDebtType) kotlin.math.abs(account.initialBalance) else account.initialBalance,
                            isDebtType = isDebtType,
                            creditLimitText = creditLimitText,
                            aprText = aprText,
                            minPaymentText = minPaymentText,
                            loanTermMonthsText = loanTermMonthsText,
                            onEditClick = { enterEditMode() },
                            onDeleteClick = { showDeleteConfirmation = true },
                            onClose = onDismiss,
                            canDelete = onDelete != null
                        )
                    } else {

                    // 1. Balance Amount Hero — shared component (spec §16): every amount-entry
                    // card in the app uses this one implementation, not a per-screen copy.
                    TransactionAmountHero(
                        type = EntryType.Income,
                        amountText = initialBalanceText,
                        onAmountChange = { initialBalanceText = it },
                        currencySymbol = currencySymbol,
                        badgeText = "CURRENT ACCOUNT BALANCE",
                        ramp = accRamp,
                        stepAmount = 50.0
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    QuickAmountChips(
                        presets = listOf(100, 500, 1000, 2000),
                        currencySymbol = currencySymbol,
                        onPick = { preset ->
                            val currentVal = initialBalanceText.toDoubleOrNull() ?: account.initialBalance
                            initialBalanceText = "%.2f".format(currentVal + preset)
                        }
                    )

                    Surface(shape = ShapeChip, color = Ramp.Gray.tintFill(isDark)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = buildAnnotatedString {
                                    append("Initial opening balance: ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)) {
                                        append("$currencySymbol%.2f".format(account.initialBalance))
                                    }
                                },
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. Account Details in Grouped Surface
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ACCOUNT DETAILS",
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
                                                Text(text = "ACCOUNT TYPE", style = SelfBudgetType.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(text = selectedOption.label, style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    AccountType.INVESTMENT -> "Investment"
                                    AccountType.LOAN -> "Car Loan"
                                    AccountType.RETIREMENT -> "401(k), IRA"
                                    else -> "Checking Account"
                                }

                                OutlinedTextField(
                                    value = accountName,
                                    onValueChange = { input ->
                                        accountName = input.toWordTitleCase()
                                    },
                                    label = { Text("Account name") },
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
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

                    } // end isEditMode form fields

                    // Linked Savings Goals Section & Cash Availability Summary
                    val linkedGoals = remember(goals, account.id) { goals.filter { it.linkedAccountId == account.id } }
                    val totalEarmarked = remember(linkedGoals, liveDisplayBalance) {
                        linkedGoals.sumOf { if (it.savedAmount > 0) it.savedAmount else minOf(liveDisplayBalance, it.targetAmount) }
                    }
                    val availableToSpend = remember(liveDisplayBalance, totalEarmarked) {
                        (liveDisplayBalance - totalEarmarked).coerceAtLeast(0.0)
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "LINKED SAVINGS GOALS & CASH AVAILABILITY",
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )

                        if (linkedGoals.isNotEmpty()) {
                            Surface(
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.5.dp, Ramp.Teal.containerBorder(isDark)),
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
                                                RampIconTile(icon = Icons.Default.Savings, ramp = Ramp.Teal, size = 32.dp, iconSize = 17.dp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(text = goal.name, style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            Text(
                                                text = "$currencySymbol%.2f goal target".format(goal.targetAmount),
                                                style = SelfBudgetType.rowTitle,
                                                color = getIncomeColor()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Available to spend", style = SelfBudgetType.body, color = getIncomeColor())
                                        Text("$currencySymbol%.2f".format(availableToSpend), style = SelfBudgetType.body, color = getIncomeColor())
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Earmarked for goals", style = SelfBudgetType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("-$currencySymbol%.2f".format(totalEarmarked), style = SelfBudgetType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total account balance", style = SelfBudgetType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$currencySymbol%.2f".format(liveDisplayBalance), style = SelfBudgetType.body, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = ShapeCard,
                                color = Ramp.Gray.tintFill(isDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No savings goals linked to this account yet.",
                                    style = SelfBudgetType.body,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    if (isEditMode) {

                    // 5. Real-Time Account Card Preview (Hero Card) — neutral amount (spec §11/§14)
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

                                val balanceVal = initialBalanceText.toDoubleOrNull() ?: liveDisplayBalance
                                Text(
                                    text = "$currencySymbol%.2f".format(balanceVal),
                                    style = SelfBudgetType.display,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (estimate != null && estimate.isPaymentTooLow) {
                            Text(
                                text = "This monthly payment won't cover the interest — balance will keep growing.",
                                style = SelfBudgetType.body,
                                color = Ramp.Red.secondaryText(isDark)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons: [Cancel] [Save changes], then an isolated destructive Delete below (spec §14).
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryPillButton(
                                text = "Cancel",
                                onClick = onDismiss,
                                ramp = Ramp.Gray,
                                modifier = Modifier.weight(1f).height(54.dp)
                            )

                            PrimaryPillButton(
                                text = "Save changes",
                                onClick = { trySave() },
                                enabled = isDirty && accountName.isNotBlank(),
                                ramp = accRamp,
                                modifier = Modifier.weight(1.3f).height(54.dp)
                            )
                        }

                        if (onDelete != null) {
                            DestructivePillButton(
                                text = "Delete account",
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            )
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
                    shape = ShapePage,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RampIconTile(icon = Icons.Default.Delete, ramp = Ramp.Red, size = 56.dp, iconSize = 28.dp)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Delete payment account?", style = SelfBudgetType.title, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Are you sure you want to delete \"${account.name}\"? Associated transaction records will remain saved.",
                            style = SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryPillButton(
                                text = "Cancel",
                                onClick = { showDeleteConfirmation = false },
                                ramp = Ramp.Gray,
                                modifier = Modifier.weight(1f).height(48.dp)
                            )

                            PrimaryPillButton(
                                text = "Delete",
                                onClick = {
                                    onDelete(account)
                                    showDeleteConfirmation = false
                                },
                                ramp = Ramp.Red,
                                modifier = Modifier.weight(1f).height(48.dp)
                            )
                        }
                    }
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

@Composable
private fun AccountViewModeSummary(
    accountName: String,
    accountType: AccountType,
    typeLabel: String,
    typeIcon: ImageVector,
    accRamp: Ramp,
    currencySymbol: String,
    balance: Double,
    initialBalance: Double,
    isDebtType: Boolean,
    creditLimitText: String,
    aprText: String,
    minPaymentText: String,
    loanTermMonthsText: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClose: () -> Unit,
    canDelete: Boolean
) {
    val isDark = isAppInDarkTheme()
    val isLoanType = accountType in listOf(AccountType.MORTGAGE, AccountType.AUTO_LOAN, AccountType.STUDENT_LOAN, AccountType.LOAN)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Account Balance Display — neutral amount (spec §11/§14)
        Surface(
            shape = ShapeHero,
            color = accRamp.tintFill(isDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CURRENT ACCOUNT BALANCE ($currencySymbol)",
                    style = SelfBudgetType.eyebrow,
                    color = accRamp.secondaryText(isDark)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$currencySymbol%.2f".format(balance),
                    style = SelfBudgetType.display,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = ShapePill, color = accRamp.tintFill(isDark)) {
                    Text(
                        text = typeLabel,
                        style = SelfBudgetType.badge,
                        color = accRamp.titleText(isDark),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Single Grouped Details Card — field-type icon tiles are gray, not
        // rainbow (spec §14/§17); only the Account Type row keeps its ramp,
        // matching the "category row" exception.
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ACCOUNT DETAILS",
                style = SelfBudgetType.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AccountViewModeInfoItem(icon = Icons.Default.AccountBalance, ramp = Ramp.Gray, label = "Account name", value = accountName)

                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    AccountViewModeInfoItem(icon = typeIcon, ramp = accRamp, label = "Account type", value = typeLabel)

                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    AccountViewModeInfoItem(
                        icon = Icons.Default.Info,
                        ramp = Ramp.Gray,
                        label = "Initial opening balance",
                        value = "$currencySymbol%.2f".format(initialBalance)
                    )

                    if (isDebtType) {
                        val creditLimitVal = creditLimitText.toDoubleOrNull()
                        val aprVal = aprText.toDoubleOrNull()
                        val termMonths = loanTermMonthsText.toIntOrNull()
                        val currentOwed = kotlin.math.abs(balance)

                        if (isLoanType) {
                            if (creditLimitVal != null && creditLimitVal > 0.0) {
                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(
                                    icon = Icons.Default.Info,
                                    ramp = Ramp.Gray,
                                    label = "Original loan amount",
                                    value = "$currencySymbol%.2f".format(creditLimitVal)
                                )
                            }

                            if (termMonths != null && termMonths > 0) {
                                val termYearsStr = when {
                                    termMonths % 12 == 0 -> " (${termMonths / 12} years)"
                                    termMonths >= 12 -> " (${"%.1f".format(termMonths / 12.0)} years)"
                                    else -> ""
                                }
                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(
                                    icon = Icons.Default.Schedule,
                                    ramp = Ramp.Gray,
                                    label = "Loan term",
                                    value = "$termMonths months$termYearsStr"
                                )
                            }

                            if (aprVal != null && aprVal > 0.0) {
                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(
                                    icon = Icons.Default.Percent,
                                    ramp = Ramp.Gray,
                                    label = "APR",
                                    value = "$aprText%"
                                )

                                if (termMonths != null && termMonths > 0) {
                                    val amortizedMonthlyPayment = com.selfbudget.app.core.util.DebtPayoffCalculator.calculateAmortizedMonthlyPayment(
                                        balance = currentOwed,
                                        aprPercent = aprVal,
                                        termMonths = termMonths
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                    AccountViewModeInfoItem(
                                        icon = Icons.Default.Payments,
                                        ramp = Ramp.Coral,
                                        label = "Estimated monthly amortized payment",
                                        value = "$currencySymbol%.2f".format(amortizedMonthlyPayment)
                                    )
                                }
                            } else if (aprText.isNotBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(icon = Icons.Default.Percent, ramp = Ramp.Gray, label = "APR", value = "$aprText%")
                            }

                            if (minPaymentText.isNotBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(icon = Icons.Default.Payments, ramp = Ramp.Gray, label = "Scheduled monthly payment", value = "$currencySymbol$minPaymentText")
                            }
                        } else {
                            // Credit Card or other revolving debt
                            if (creditLimitVal != null && creditLimitVal > 0.0) {
                                val availableCredit = (creditLimitVal - currentOwed).coerceAtLeast(0.0)

                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(
                                    icon = Icons.Default.CreditCard,
                                    ramp = Ramp.Gray,
                                    label = "Credit limit",
                                    value = "$currencySymbol%.2f".format(creditLimitVal)
                                )

                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(
                                    icon = Icons.Default.Savings,
                                    ramp = Ramp.Teal,
                                    label = "Available balance / credit",
                                    value = "$currencySymbol%.2f".format(availableCredit)
                                )
                            } else if (creditLimitText.isNotBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(icon = Icons.Default.CreditCard, ramp = Ramp.Gray, label = "Credit limit", value = "$currencySymbol$creditLimitText")
                            }

                            if (aprVal != null && aprVal > 0.0) {
                                // Standard US bank minimum payment formula: max($25 or 1% principal + accrued monthly interest, or 2% of balance)
                                val monthlyInterest = currentOwed * ((aprVal / 100.0) / 12.0)
                                val formulaPrincipalPlusInterest = (currentOwed * 0.01) + monthlyInterest
                                val estimatedMinPayment = when {
                                    currentOwed <= 0.0 -> 0.0
                                    currentOwed < 25.0 -> currentOwed
                                    else -> maxOf(25.0, formulaPrincipalPlusInterest, currentOwed * 0.02)
                                }

                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(
                                    icon = Icons.Default.Percent,
                                    ramp = Ramp.Gray,
                                    label = "APR",
                                    value = "$aprText%"
                                )

                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(
                                    icon = Icons.Default.Payments,
                                    ramp = Ramp.Coral,
                                    label = "Estimated monthly minimum payment",
                                    value = "$currencySymbol%.2f".format(estimatedMinPayment)
                                )
                            } else if (aprText.isNotBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(icon = Icons.Default.Percent, ramp = Ramp.Gray, label = "APR", value = "$aprText%")
                            }

                            if (minPaymentText.isNotBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                AccountViewModeInfoItem(icon = Icons.Default.Payments, ramp = Ramp.Gray, label = "Custom minimum monthly payment", value = "$currencySymbol$minPaymentText")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons: [Close] [Edit account], then an isolated destructive Delete below (spec §14).
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryPillButton(
                    text = "Close",
                    onClick = onClose,
                    ramp = Ramp.Gray,
                    modifier = Modifier.weight(1f).height(54.dp)
                )

                PrimaryPillButton(
                    text = "Edit account",
                    onClick = onEditClick,
                    ramp = Ramp.Teal,
                    modifier = Modifier.weight(1.3f).height(54.dp)
                )
            }

            if (canDelete) {
                DestructivePillButton(
                    text = "Delete account",
                    onClick = onDeleteClick,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountViewModeInfoItem(
    icon: ImageVector,
    ramp: Ramp,
    label: String,
    value: String
) {
    val isDark = isAppInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RampIconTile(icon = icon, ramp = ramp, size = 34.dp, iconSize = 18.dp)

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = label.uppercase(), style = SelfBudgetType.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
