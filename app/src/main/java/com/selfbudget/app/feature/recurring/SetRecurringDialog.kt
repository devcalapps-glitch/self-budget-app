package com.selfbudget.app.feature.recurring

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.focusable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material.icons.filled.CalendarToday
import com.selfbudget.app.core.util.VoiceParser
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.getIncomeColor
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Autorenew

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetRecurringDialog(
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    accounts: List<AccountEntity> = emptyList(),
    accountBalances: Map<String, Double> = emptyMap(),
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, type: TransactionType, categoryId: String, frequency: RecurringFrequency, remainingOccurrences: Int?, nextDueDate: Long?, transferAccountId: String?) -> Unit,
    onAddCustomCategory: ((CategoryEntity) -> Unit)? = null,
    onAddCustomAccount: ((AccountEntity) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
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
    val availableDebtAccounts = remember(accounts) {
        accounts.filter { it.type == AccountType.CREDIT_CARD || it.type == AccountType.LOAN }
    }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
    
    val scrollState = rememberScrollState()
    val focusAnchor = remember { FocusRequester() }

    // Move focus to an inert anchor (not just clear it) whenever the category modal opens or
    // closes. Dismissing the nested picker Dialog hands window focus back to this dialog's
    // window, and Android will restore focus onto the last real field (e.g. Amount) unless
    // something else already holds it — clearFocus() alone loses that race.
    LaunchedEffect(expandedCategory, showTargetDebtAccountModal) {
        runCatching { focusAnchor.requestFocus() }
        keyboardController?.hide()
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull()
            if (spokenText != null && spokenText.isNotBlank()) {
                val parsed = VoiceParser.parseSpokenText(spokenText)
                if (parsed != null) {
                    title = parsed.title
                    amountText = "%.2f".format(parsed.amount)
                    selectedType = parsed.type
                } else {
                    title = spokenText
                }

                val textLower = spokenText.lowercase()
                if (textLower.contains("biweekly") || textLower.contains("bi-weekly") || textLower.contains("every two weeks")) {
                    selectedFrequency = RecurringFrequency.BI_WEEKLY
                } else if (textLower.contains("weekly") || textLower.contains("every week")) {
                    selectedFrequency = RecurringFrequency.WEEKLY
                } else if (textLower.contains("yearly") || textLower.contains("annual") || textLower.contains("every year")) {
                    selectedFrequency = RecurringFrequency.YEARLY
                } else if (textLower.contains("monthly") || textLower.contains("every month")) {
                    selectedFrequency = RecurringFrequency.MONTHLY
                }
            }
        }
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
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Add Recurring Item",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                val categoryId = selectedCategory?.id ?: "cat_other"
                                if (title.isNotBlank() && amount > 0.0) {
                                    val remainingOccurrences = if (hasLimitedOccurrences) occurrencesText.toIntOrNull() else null
                                    onConfirm(title, amount, selectedType, categoryId, selectedFrequency, remainingOccurrences, selectedTimestamp, selectedTargetDebtAccount?.id)
                                }
                            },
                            enabled = title.isNotBlank() &&
                                      (amountText.toDoubleOrNull() ?: 0.0) > 0.0 &&
                                      selectedCategory != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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

                    val currentAccentColor = if (selectedType == TransactionType.INCOME) com.selfbudget.app.ui.theme.getIncomeColor() else ExpenseRed

                    // 1. Top Amount Entry Stepper (- $0.00 +) without card wrapping
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedType == TransactionType.INCOME) "RECURRING INCOME AMOUNT ($currencySymbol)" else "RECURRING EXPENSE AMOUNT ($currencySymbol)",
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
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    val next = maxOf(0.0, current - 1.0)
                                    amountText = if (next == 0.0) "" else "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Subtract Amount",
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
                                    value = amountText,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                            amountText = input
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            text = "0.00",
                                            style = TextStyle(
                                                fontSize = 44.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = currentAccentColor.copy(alpha = 0.35f),
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
                                                color = currentAccentColor
                                            ),
                                            modifier = Modifier.padding(end = 2.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = currentAccentColor,
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
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    val next = current + 1.0
                                    amountText = "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = currentAccentColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Amount",
                                        tint = currentAccentColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Preset Amount Chips ($50, $100, $250, $500, $1000)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(50, 100, 250, 500, 1000).forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable {
                                        val currentVal = amountText.toDoubleOrNull() ?: 0.0
                                        amountText = "%.2f".format(currentVal + preset)
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

                    // Segmented Toggle Switch Pill (TYPE: Expense vs Income)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "TYPE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selectedType == TransactionType.EXPENSE) ExpenseRed.copy(alpha = 0.25f) else Color.Transparent
                                        )
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            selectedType = TransactionType.EXPENSE
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Expense 🔴",
                                        fontWeight = if (selectedType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedType == TransactionType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selectedType == TransactionType.INCOME) getIncomeColor().copy(alpha = 0.25f) else Color.Transparent
                                        )
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            selectedType = TransactionType.INCOME
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Income 🟢",
                                        fontWeight = if (selectedType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedType == TransactionType.INCOME) getIncomeColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.replaceFirstChar { char -> char.uppercase() } },
                        label = { Text(if (selectedType == TransactionType.INCOME) "Income Title (e.g. Salary)" else "Bill Title (e.g. Netflix, Rent)") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Repeat Frequency Segmented Container
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Repeat Frequency",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val frequencies = listOf(
                                    RecurringFrequency.WEEKLY to "Weekly",
                                    RecurringFrequency.BI_WEEKLY to "Bi-Weekly",
                                    RecurringFrequency.MONTHLY to "Monthly",
                                    RecurringFrequency.YEARLY to "Yearly"
                                )

                                frequencies.forEach { (freq, label) ->
                                    val isSelected = selectedFrequency == freq
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                            )
                                            .clickable {
                                                focusManager.clearFocus(force = true)
                                                keyboardController?.hide()
                                                selectedFrequency = freq
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Category Field (Taps to open CategorySelectionModal)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            leadingIcon = { Icon(getCategoryIcon(selectedCategory), contentDescription = null) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    expandedCategory = true
                                }
                        )
                    }

                    // Optional Target Debt Account Field - lets this recurring bill represent an
                    // actual planned monthly debt payment: when posted, it reduces that Credit
                    // Card / Loan account's balance the same way a one-off transfer does (see
                    // AccountBalanceCalculator), instead of just being a generic expense.
                    val recurringCategoryName = selectedCategory?.name?.lowercase() ?: ""
                    val isDebtOrMortgageCategory = recurringCategoryName.contains("credit") ||
                        recurringCategoryName.contains("card") ||
                        recurringCategoryName.contains("loan") ||
                        recurringCategoryName.contains("debt") ||
                        recurringCategoryName.contains("mortgage") ||
                        recurringCategoryName.contains("rent")
                    val shouldShowDebtAccountField = selectedType == TransactionType.EXPENSE &&
                        availableDebtAccounts.isNotEmpty() &&
                        (isDebtOrMortgageCategory || selectedTargetDebtAccount != null)

                    if (shouldShowDebtAccountField) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val targetDebtText = selectedTargetDebtAccount?.let { acc ->
                                    val rawBal = accountBalances[acc.id] ?: acc.initialBalance
                                    val dispBal = kotlin.math.abs(rawBal)
                                    "${acc.name} ($currencySymbol%.2f owed)".format(dispBal)
                                } ?: "None (Standard Expense)"

                                OutlinedTextField(
                                    value = targetDebtText,
                                    onValueChange = {},
                                    enabled = false,
                                    label = { Text("Apply Payment Toward Debt (Optional)") },
                                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            showTargetDebtAccountModal = true
                                        }
                                )
                            }

                            selectedTargetDebtAccount?.let { debtAcc ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Posting this bill will reduce ${debtAcc.name} debt balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Start/Next Due Date
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dateFormatter.format(Date(selectedTimestamp)),
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Start / Next Due Date") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            shape = RoundedCornerShape(14.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    showDatePickerModal = true
                                }
                        )
                    }

                    // Finite Lifespan Toggle (e.g. "12 more loan payments and I'm done")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("This Has an End Date", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "e.g. a car loan or installment plan - stop reminding you once it's paid off.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                }
                            }
                            Switch(checked = hasLimitedOccurrences, onCheckedChange = { hasLimitedOccurrences = it })
                        }

                        if (hasLimitedOccurrences) {
                            OutlinedTextField(
                                value = occurrencesText,
                                onValueChange = { occurrencesText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Payments Remaining") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Big Round Mic Voice Button at the Bottom
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(72.dp)
                                .clickable {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                                        putExtra(
                                            RecognizerIntent.EXTRA_PROMPT,
                                            if (selectedType == TransactionType.INCOME) "e.g. 'Paycheck 2500 dollars biweekly'" else "e.g. 'Netflix 15 dollars monthly'"
                                        )
                                    }
                                    voiceLauncher.launch(intent)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Entry Mic",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Voice",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                val categoryId = selectedCategory?.id ?: "cat_other"
                                if (title.isNotBlank() && amount > 0.0) {
                                    val remainingOccurrences = if (hasLimitedOccurrences) occurrencesText.toIntOrNull() else null
                                    onConfirm(title, amount, selectedType, categoryId, selectedFrequency, remainingOccurrences, selectedTimestamp, selectedTargetDebtAccount?.id)
                                }
                            },
                            enabled = title.isNotBlank() &&
                                      (amountText.toDoubleOrNull() ?: 0.0) > 0.0 &&
                                      selectedCategory != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Save Recurring", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }
    }

    if (showNewCategoryDialog) {
        AddCustomCategoryDialog(
            initialType = selectedType,
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
            accounts = availableDebtAccounts,
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
            initialSelectedDateMillis = selectedTimestamp
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
                            selectedTimestamp = millis
                        }
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        showDatePickerModal = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
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
