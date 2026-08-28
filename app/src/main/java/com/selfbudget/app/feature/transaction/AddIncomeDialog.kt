package com.selfbudget.app.feature.transaction

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.AccountSelectionModal
import com.selfbudget.app.core.ui.AddCustomAccountDialog
import com.selfbudget.app.core.ui.AddCustomCategoryDialog
import com.selfbudget.app.core.ui.CategorySelectionModal
import com.selfbudget.app.core.ui.getCategoryIcon
import com.selfbudget.app.core.util.VoiceParser
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated income-entry form. Split from what used to be a single Income/Expense
 * AddTransactionDialog (see AddExpenseDialog) - income has no receipt to scan, no debt account
 * to pay down, and no budget ceiling to set, so this form only asks for what actually applies:
 * who paid you, how much, which account it landed in, and whether it repeats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    allTransactions: List<TransactionEntity> = emptyList(),
    recurringList: List<RecurringTransactionEntity> = emptyList(),
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        amount: Double,
        categoryId: String,
        accountId: String,
        note: String?,
        timestamp: Long,
        isRecurring: Boolean,
        recurringFrequency: RecurringFrequency
    ) -> Unit,
    onAddCustomCategory: ((CategoryEntity) -> Unit)? = null,
    onAddCustomAccount: ((AccountEntity) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    var isRecurring by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(RecurringFrequency.BI_WEEKLY) }

    val availableAccounts = remember(accounts) { accounts }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var expandedAccountDropdown by remember { mutableStateOf(false) }
    var showNewAccountDialog by remember { mutableStateOf(false) }

    var expandedCategoryDropdown by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var autoSuggestedCategoryName by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val scrollState = rememberScrollState()
    val focusAnchor = remember { FocusRequester() }

    // Move focus to an inert anchor (not just clear it) whenever selection modals open or close.
    // Dismissing the nested picker Dialog hands window focus back to this dialog's window, and
    // Android will restore focus onto the last real field (e.g. Amount) unless something else
    // already holds it — clearFocus() alone loses that race.
    LaunchedEffect(expandedAccountDropdown, expandedCategoryDropdown, showDatePickerModal) {
        focusAnchor.requestFocus()
        keyboardController?.hide()
    }

    val availableCategories = remember(categories) {
        val dbCategories = if (categories.isNotEmpty()) categories else AppDatabase.DEFAULT_CATEGORIES
        (dbCategories + AppDatabase.DEFAULT_CATEGORIES).distinctBy { it.id }
    }

    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    val existingRecurring = remember(title, selectedCategory, recurringList) {
        if (title.isNotBlank() && selectedCategory != null) {
            recurringList.firstOrNull {
                it.categoryId == selectedCategory?.id &&
                it.title.trim().equals(title.trim(), ignoreCase = true)
            }
        } else null
    }

    LaunchedEffect(existingRecurring) {
        if (existingRecurring != null && !existingRecurring.isArchived) {
            isRecurring = true
            selectedFrequency = existingRecurring.frequency
        }
    }

    LaunchedEffect(title) {
        if (title.isNotBlank() && allTransactions.isNotEmpty()) {
            val matchingPastTx = allTransactions.firstOrNull {
                it.title.trim().equals(title.trim(), ignoreCase = true)
            }
            val matchedCat = matchingPastTx?.let { tx ->
                availableCategories.firstOrNull { it.id == tx.categoryId && it.type == TransactionType.INCOME }
            }
            if (matchedCat != null) {
                selectedCategory = matchedCat
                autoSuggestedCategoryName = matchedCat.name
            } else {
                autoSuggestedCategoryName = null
            }
        } else {
            autoSuggestedCategoryName = null
        }
    }

    var note by remember { mutableStateOf("") }

    // Paychecks/stipends are the overwhelmingly common recurring-income case, so this only
    // watches for that pattern rather than the broader bill-keyword list the expense form uses.
    val recurringKeywordsBiWeekly = remember {
        listOf("paycheck", "payroll", "salary", "direct deposit", "stipend")
    }

    val smartRecurringSuggestion = remember(title, recurringList, isRecurring) {
        if (isRecurring || title.trim().length < 2) null
        else {
            val lowerTitle = title.trim().lowercase()

            val existingRecurringMatch = recurringList.firstOrNull { item ->
                if (item.isArchived) return@firstOrNull false
                val itemLower = item.title.trim().lowercase()
                itemLower == lowerTitle || lowerTitle.contains(itemLower) || itemLower.contains(lowerTitle)
            }
            if (existingRecurringMatch != null) {
                existingRecurringMatch.frequency
            } else if (recurringKeywordsBiWeekly.any { lowerTitle.contains(it) }) {
                RecurringFrequency.BI_WEEKLY
            } else {
                null
            }
        }
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
                } else {
                    title = spokenText
                }
            }
        }
    }

    fun submitForm() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val categoryId = selectedCategory?.id ?: "cat_income_others"
        val accId = selectedAccount?.id ?: "acc_checking"
        if (title.isNotBlank() && amount > 0.0) {
            onConfirm(
                title,
                amount,
                categoryId,
                accId,
                note.ifBlank { null },
                selectedTimestamp,
                isRecurring,
                selectedFrequency
            )
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
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "New Income Entry",
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
                            onClick = { submitForm() },
                            enabled = title.isNotBlank() &&
                                      (amountText.toDoubleOrNull() ?: 0.0) > 0.0 &&
                                      selectedAccount != null &&
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Inert focus target used to steal focus away from real fields when a
                    // selection modal opens/closes (see focusAnchor LaunchedEffect above).
                    Box(
                        modifier = Modifier
                            .size(0.dp)
                            .focusRequester(focusAnchor)
                            .focusable()
                    )

                    // 1. Top Amount Entry Stepper (- $0.00 +) without card wrapping
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "INCOME AMOUNT ($currencySymbol)",
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
                                                color = com.selfbudget.app.ui.theme.getIncomeColor().copy(alpha = 0.35f),
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
                                                color = com.selfbudget.app.ui.theme.getIncomeColor()
                                            ),
                                            modifier = Modifier.padding(end = 2.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = com.selfbudget.app.ui.theme.getIncomeColor(),
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
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
                                color = com.selfbudget.app.ui.theme.getIncomeColor().copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Amount",
                                        tint = com.selfbudget.app.ui.theme.getIncomeColor()
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

                    // 2. Title / Payer
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.replaceFirstChar { char -> char.uppercase() } },
                        label = { Text("Title / Payer") },
                        placeholder = { Text("e.g. Salary, Freelance, Bonus") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Smart Recurring Suggestion Banner
                    if (smartRecurringSuggestion != null) {
                        val freqText = when (smartRecurringSuggestion) {
                            RecurringFrequency.WEEKLY -> "weekly"
                            RecurringFrequency.BI_WEEKLY -> "bi-weekly"
                            RecurringFrequency.MONTHLY -> "monthly"
                            RecurringFrequency.YEARLY -> "annual"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Smart Suggestion",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "\"${title.trim()}\" looks like a $freqText commitment.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        isRecurring = true
                                        selectedFrequency = smartRecurringSuggestion
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Set Recurring", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 3. Date Picker Input Box (Full Width)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                showDatePickerModal = true
                            }
                    ) {
                        OutlinedTextField(
                            value = dateFormatter.format(Date(selectedTimestamp)),
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Date") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick Date",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false }
                        )
                    }

                    // Deposit Account Field (Taps to open AccountSelectionModal)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                expandedAccountDropdown = true
                            }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.name ?: "Select Deposit Account",
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Deposit Account") },
                            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false }
                        )
                    }

                    // Category Field (Taps to open CategorySelectionModal)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    expandedCategoryDropdown = true
                                }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory?.name ?: "Select Category",
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Category") },
                                leadingIcon = { Icon(getCategoryIcon(selectedCategory), contentDescription = null) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusProperties { canFocus = false }
                            )
                        }

                        autoSuggestedCategoryName?.let { catName ->
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
                                    text = "Auto-selected past category for \"$title\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Dynamic Recurring Income Options Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Recurring Income?",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = existingRecurring?.let { "Active Recurring Item: ${currencySymbol}${"%.2f".format(it.amount)} (${it.frequency.name.lowercase().replaceFirstChar { c -> c.uppercase() }})" }
                                                ?: "Schedule future paychecks",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (existingRecurring != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (existingRecurring != null) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }

                                Switch(
                                    checked = isRecurring,
                                    onCheckedChange = {
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        isRecurring = it
                                    }
                                )
                            }

                            if (isRecurring) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Repeat Frequency",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

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
                                                    .fillMaxSize()
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
                        }
                    }

                    // Note
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.replaceFirstChar { char -> char.uppercase() } },
                        label = { Text("Note (Optional)") },
                        placeholder = { Text("Add transaction notes...") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Big Round Mic Voice Button (centered - no receipt to scan for income)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                            )
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                            putExtra(
                                                RecognizerIntent.EXTRA_PROMPT,
                                                "e.g. 'Salary 2500 dollars'"
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
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            onClick = { submitForm() },
                            enabled = title.isNotBlank() &&
                                      (amountText.toDoubleOrNull() ?: 0.0) > 0.0 &&
                                      selectedAccount != null &&
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
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }

        // Material 3 Compose Date Picker Dialog
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
                    TextButton(
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
                    TextButton(
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

    // Upgraded Custom Account Creation Modal
    if (showNewAccountDialog) {
        AddCustomAccountDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showNewAccountDialog = false },
            onConfirm = { newAcc ->
                onAddCustomAccount?.invoke(newAcc)
                selectedAccount = newAcc
                showNewAccountDialog = false
            }
        )
    }

    // Upgraded Custom Category Creation Modal
    if (showNewCategoryDialog) {
        AddCustomCategoryDialog(
            initialType = TransactionType.INCOME,
            onDismiss = { showNewCategoryDialog = false },
            onConfirm = { newCat ->
                onAddCustomCategory?.invoke(newCat)
                selectedCategory = newCat
                showNewCategoryDialog = false
            }
        )
    }

    if (expandedCategoryDropdown) {
        CategorySelectionModal(
            categories = categories,
            selectedCategory = selectedCategory,
            transactionType = TransactionType.INCOME,
            onDismiss = { expandedCategoryDropdown = false },
            onSelectCategory = { cat ->
                selectedCategory = cat
                autoSuggestedCategoryName = null
                expandedCategoryDropdown = false
            },
            onAddCustomCategory = {
                expandedCategoryDropdown = false
                showNewCategoryDialog = true
            }
        )
    }

    if (expandedAccountDropdown) {
        AccountSelectionModal(
            accounts = availableAccounts,
            selectedAccount = selectedAccount,
            currencySymbol = currencySymbol,
            onDismiss = { expandedAccountDropdown = false },
            onSelectAccount = { acc ->
                selectedAccount = acc
                expandedAccountDropdown = false
            },
            onAddCustomAccount = {
                expandedAccountDropdown = false
                showNewAccountDialog = true
            }
        )
    }
}
