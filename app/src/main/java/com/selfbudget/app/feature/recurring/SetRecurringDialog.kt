package com.selfbudget.app.feature.recurring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.selfbudget.app.core.ui.AccountSelectionModal
import com.selfbudget.app.core.ui.AddCustomAccountDialog
import com.selfbudget.app.core.ui.AddCustomCategoryDialog
import com.selfbudget.app.core.ui.CategorySelectionModal
import com.selfbudget.app.core.ui.getCategoryIcon
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.selfbudget.app.core.util.toWordTitleCase
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.TransactionType
import androidx.compose.animation.animateContentSize
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.onSolidFill
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SetRecurringDialog(
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    accounts: List<AccountEntity> = emptyList(),
    accountBalances: Map<String, Double> = emptyMap(),
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, type: TransactionType, categoryId: String, frequency: RecurringFrequency, remainingOccurrences: Int?, nextDueDate: Long?, transferAccountId: String?) -> Unit,
    onAddCustomCategory: ((CategoryEntity) -> Unit)? = null,
    onAddCustomAccount: ((AccountEntity) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedFrequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var hasLimitedOccurrences by remember { mutableStateOf(false) }
    var occurrencesText by remember { mutableStateOf("") }
    var showNewAccountDialog by remember { mutableStateOf(false) }

    val availableCategories = remember(categories) {
        if (categories.isNotEmpty()) categories else AppDatabase.DEFAULT_CATEGORIES
    }
    val filteredCategories = remember(availableCategories, selectedType) {
        availableCategories.filter { it.type == selectedType }
    }
    var selectedCategory by remember {
        mutableStateOf<CategoryEntity?>(null)
    }

    var expandedCategory by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    var selectedTargetDebtAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showTargetDebtAccountModal by remember { mutableStateOf(false) }
    val availableTargetAccounts = remember(accounts) {
        accounts.filter { 
            com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(it.type) || 
            it.type == AccountType.INVESTMENT || 
            it.type == AccountType.RETIREMENT || 
            it.type == AccountType.SAVINGS 
        }
    }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
    
    val scrollState = rememberScrollState()
    val focusAnchor = remember { FocusRequester() }

    LaunchedEffect(expandedCategory, showTargetDebtAccountModal) {
        runCatching { focusAnchor.requestFocus() }
        keyboardController?.hide()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedType == TransactionType.INCOME) "New Recurring Income" else "New Recurring Expense",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    },
                    // Save lives in the footer only — never duplicated as a header action (spec §14/§19).
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                val currentAccentColor = if (selectedType == TransactionType.INCOME) getIncomeColor() else getExpenseColor()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Inert focus target used to steal focus away from real fields when the
                    // category modal opens/closes (see focusAnchor LaunchedEffect above).
                    Box(
                        modifier = Modifier
                            .size(0.dp)
                            .focusRequester(focusAnchor)
                            .focusable()
                    )

                    // 1. Amount Hero Card — shared component (spec §16): every amount-entry
                    // card in the app uses this one implementation, not a per-screen copy.
                    com.selfbudget.app.core.ui.components.TransactionAmountHero(
                        type = if (selectedType == TransactionType.INCOME) com.selfbudget.app.core.ui.components.EntryType.Income else com.selfbudget.app.core.ui.components.EntryType.Expense,
                        amountText = amountText,
                        onAmountChange = { amountText = it },
                        currencySymbol = currencySymbol,
                        badgeText = if (selectedType == TransactionType.INCOME) "RECURRING INCOME AMOUNT" else "RECURRING EXPENSE AMOUNT",
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    com.selfbudget.app.core.ui.components.QuickAmountChips(
                        presets = listOf(25, 50, 100, 500, 1000),
                        currencySymbol = currencySymbol,
                        onPick = { preset ->
                            val currentVal = amountText.toDoubleOrNull() ?: 0.0
                            amountText = "%.2f".format(currentVal + preset)
                        }
                    )

                    // 2. Details Grouped Surface
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "COMMITMENT DETAILS",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = com.selfbudget.app.ui.theme.ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Title row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    com.selfbudget.app.core.ui.components.GrayIconTile(
                                        icon = Icons.Default.CreditCard,
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it.toWordTitleCase() },
                                        placeholder = {
                                            Text(
                                                if (selectedType == TransactionType.INCOME) "Payer or Title (eg.Salary, Client)" else "Bill Title (e.g. Netflix, Rent)",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                        ),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 70.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                )

                                // Category row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            expandedCategory = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Category tile is the one detail-row exception that carries the category's own ramp (spec §14).
                                    com.selfbudget.app.core.ui.components.GrayIconTile(
                                        icon = getCategoryIcon(selectedCategory),
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Category",
                                            style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                            color = com.selfbudget.app.ui.theme.TextMuted
                                        )
                                        Text(
                                            text = selectedCategory?.name ?: "Select category",
                                            style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                            color = if (selectedCategory != null) MaterialTheme.colorScheme.onSurface else com.selfbudget.app.ui.theme.TextMuted
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 70.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                )

                                // Start / Next Due Date row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            showDatePickerModal = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    com.selfbudget.app.core.ui.components.GrayIconTile(
                                        icon = Icons.Default.CalendarToday,
                                        size = 40.dp,
                                        iconSize = 18.dp
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Start / next due date",
                                            style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                            color = com.selfbudget.app.ui.theme.TextMuted
                                        )
                                        Text(
                                            text = dateFormatter.format(Date(selectedTimestamp)),
                                            style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Repeat Frequency & Schedule Surface
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "FREQUENCY & SCHEDULE",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = com.selfbudget.app.ui.theme.ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Repeat frequency",
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                com.selfbudget.app.core.ui.components.FrequencySegmentedControl(
                                    selected = selectedFrequency,
                                    onSelect = { freq ->
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        selectedFrequency = freq
                                    }
                                )

                                val amountNum = amountText.toDoubleOrNull() ?: 0.0
                                val monthlyCalculated = RecurringFrequencyNormalizer.toMonthlyAmount(amountNum, selectedFrequency)
                                if (amountNum > 0.0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = com.selfbudget.app.ui.theme.ShapeChip,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val helperText = when (selectedFrequency) {
                                            RecurringFrequency.SEMI_MONTHLY -> "$currencySymbol%.2f × 2 = $currencySymbol%.2f/mo reserved in budget".format(amountNum, monthlyCalculated)
                                            RecurringFrequency.BI_WEEKLY -> "$currencySymbol%.2f every 2 wks = ~$currencySymbol%.2f/mo reserved in budget".format(amountNum, monthlyCalculated)
                                            RecurringFrequency.WEEKLY -> "$currencySymbol%.2f weekly = ~$currencySymbol%.2f/mo reserved in budget".format(amountNum, monthlyCalculated)
                                            RecurringFrequency.MONTHLY -> "$currencySymbol%.2f billed monthly".format(amountNum)
                                            RecurringFrequency.YEARLY -> "$currencySymbol%.2f annual = $currencySymbol%.2f/mo reserved in budget".format(amountNum, monthlyCalculated)
                                        }
                                        Text(
                                            text = helperText,
                                            style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Optional Target Account Field
                        val recurringCategoryName = selectedCategory?.name?.lowercase() ?: ""
                        val isTargetCategory = recurringCategoryName.contains("credit") ||
                            recurringCategoryName.contains("card") ||
                            recurringCategoryName.contains("loan") ||
                            recurringCategoryName.contains("debt") ||
                            recurringCategoryName.contains("mortgage") ||
                            recurringCategoryName.contains("rent") ||
                            recurringCategoryName.contains("invest") ||
                            recurringCategoryName.contains("saving") ||
                            recurringCategoryName.contains("stock") ||
                            recurringCategoryName.contains("crypto") ||
                            recurringCategoryName.contains("401k") ||
                            recurringCategoryName.contains("ira") ||
                            recurringCategoryName.contains("retire") ||
                            recurringCategoryName.contains("transfer")
                        val shouldShowTargetAccountField = selectedType == TransactionType.EXPENSE &&
                            availableTargetAccounts.isNotEmpty() &&
                            (isTargetCategory || selectedTargetDebtAccount != null)

                        if (shouldShowTargetAccountField) {
                            val isInvestmentOrSavings = selectedTargetDebtAccount?.type == AccountType.INVESTMENT ||
                                selectedTargetDebtAccount?.type == AccountType.RETIREMENT ||
                                selectedTargetDebtAccount?.type == AccountType.SAVINGS ||
                                recurringCategoryName.contains("invest") ||
                                recurringCategoryName.contains("saving") ||
                                recurringCategoryName.contains("retire") ||
                                recurringCategoryName.contains("stock") ||
                                recurringCategoryName.contains("401k") ||
                                recurringCategoryName.contains("ira")

                            val labelText = if (isInvestmentOrSavings) {
                                "Deposit / Contribute Toward Account"
                            } else {
                                "Apply Payment Toward Debt"
                            }

                            val iconVector = if (selectedTargetDebtAccount?.type == AccountType.INVESTMENT || selectedTargetDebtAccount?.type == AccountType.RETIREMENT || recurringCategoryName.contains("invest") || recurringCategoryName.contains("retire") || recurringCategoryName.contains("stock") || recurringCategoryName.contains("401k") || recurringCategoryName.contains("ira")) {
                                Icons.Default.TrendingUp
                            } else if (selectedTargetDebtAccount?.type == AccountType.SAVINGS || recurringCategoryName.contains("saving")) {
                                Icons.Default.Savings
                            } else {
                                Icons.Default.CreditCard
                            }

                            Surface(
                                shape = com.selfbudget.app.ui.theme.ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        showTargetDebtAccountModal = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    com.selfbudget.app.core.ui.components.GrayIconTile(icon = iconVector, size = 40.dp)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = labelText,
                                            style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                            color = com.selfbudget.app.ui.theme.TextMuted
                                        )
                                        val targetText = selectedTargetDebtAccount?.let { acc ->
                                            val rawBal = accountBalances[acc.id] ?: acc.initialBalance
                                            val accSym = com.selfbudget.app.core.util.Currencies.symbolFor(acc.currencyCode).ifBlank { currencySymbol }
                                            if (com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(acc.type)) {
                                                val dispBal = kotlin.math.abs(rawBal)
                                                "${acc.name} ($accSym%.2f owed)".format(dispBal)
                                            } else {
                                                "${acc.name} ($accSym%.2f balance)".format(rawBal)
                                            }
                                        } ?: "None (standard expense)"
                                        Text(
                                            text = targetText,
                                            style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                            color = if (selectedTargetDebtAccount != null) MaterialTheme.colorScheme.onSurface else com.selfbudget.app.ui.theme.TextMuted
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = com.selfbudget.app.ui.theme.TextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // 5. Finite Lifespan / End Date Card
                        Surface(
                            shape = com.selfbudget.app.ui.theme.ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        ) {
                            Column {
                                com.selfbudget.app.core.ui.components.ToggleRow(
                                    icon = Icons.Default.Autorenew,
                                    title = "Finite lifespan",
                                    description = "Stop reminding once installments are complete",
                                    checked = hasLimitedOccurrences,
                                    onCheckedChange = { hasLimitedOccurrences = it }
                                )

                                if (hasLimitedOccurrences) {
                                    com.selfbudget.app.core.ui.components.SectionRowDivider()

                                    OutlinedTextField(
                                        value = occurrencesText,
                                        onValueChange = { occurrencesText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Payments remaining") },
                                        placeholder = { Text("e.g. 12") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = com.selfbudget.app.ui.theme.ShapeChip,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full-width Primary Save CTA — Teal (form-save actions stay brand teal, not direction-colored; spec §16)
                    com.selfbudget.app.core.ui.components.PrimaryPillButton(
                        text = if (selectedType == TransactionType.INCOME) "Save recurring income" else "Save recurring expense",
                        ramp = com.selfbudget.app.ui.theme.Ramp.Teal,
                        enabled = title.isNotBlank() &&
                                  (amountText.toDoubleOrNull() ?: 0.0) > 0.0 &&
                                  selectedCategory != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            val categoryId = selectedCategory?.id ?: "cat_other"
                            if (title.isNotBlank() && amount > 0.0) {
                                val remainingOccurrences = if (hasLimitedOccurrences) occurrencesText.toIntOrNull() else null
                                onConfirm(title, amount, selectedType, categoryId, selectedFrequency, remainingOccurrences, selectedTimestamp, selectedTargetDebtAccount?.id)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (showNewCategoryDialog) {
        AddCustomCategoryDialog(
            initialType = selectedType,
            lockType = true,
            onDismiss = { showNewCategoryDialog = false },
            onConfirm = { newCat ->
                onAddCustomCategory?.invoke(newCat)
                selectedCategory = newCat
                showNewCategoryDialog = false
            }
        )
    }

    if (expandedCategory) {
        CategorySelectionModal(
            categories = categories,
            selectedCategory = selectedCategory,
            transactionType = selectedType,
            onDismiss = { expandedCategory = false },
            onSelectCategory = { cat ->
                selectedCategory = cat
                expandedCategory = false
            },
            onAddCustomCategory = {
                expandedCategory = false
                showNewCategoryDialog = true
            },
            onArchiveCategory = onAddCustomCategory?.let { { cat -> onAddCustomCategory.invoke(cat.copy(isArchived = true)) } }
        )
    }

    if (showTargetDebtAccountModal) {
        AccountSelectionModal(
            accounts = availableTargetAccounts,
            selectedAccount = selectedTargetDebtAccount,
            currencySymbol = currencySymbol,
            accountBalances = accountBalances,
            onDismiss = { showTargetDebtAccountModal = false },
            onSelectAccount = { acc ->
                selectedTargetDebtAccount = acc
                showTargetDebtAccountModal = false
            },
            onAddCustomAccount = {
                showTargetDebtAccountModal = false
                showNewAccountDialog = true
            }
        )
    }

    if (showNewAccountDialog) {
        AddCustomAccountDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showNewAccountDialog = false },
            onConfirm = { newAcc ->
                onAddCustomAccount?.invoke(newAcc)
                selectedTargetDebtAccount = newAcc
                showNewAccountDialog = false
            }
        )
    }

    if (showDatePickerModal) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = com.selfbudget.app.core.util.DateUtils.localDateToUtcMillis(selectedTimestamp)
        )

        DatePickerDialog(
            onDismissRequest = {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                showDatePickerModal = false
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedTimestamp = com.selfbudget.app.core.util.DateUtils.utcMillisToLocalDate(millis, selectedTimestamp)
                        }
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        showDatePickerModal = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        showDatePickerModal = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
