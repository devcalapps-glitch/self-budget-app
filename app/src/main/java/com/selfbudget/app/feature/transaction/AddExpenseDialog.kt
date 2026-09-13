package com.selfbudget.app.feature.transaction

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import com.selfbudget.app.core.ui.AccountSelectionModal
import com.selfbudget.app.core.ui.CategorySelectionModal
import com.selfbudget.app.core.ui.getCategoryIcon
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import com.selfbudget.app.data.model.BudgetEntity
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
import androidx.compose.ui.platform.LocalContext
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
import com.selfbudget.app.core.ui.AddCustomAccountDialog
import com.selfbudget.app.core.ui.AddCustomCategoryDialog
import com.selfbudget.app.core.ui.components.EntryType
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.QuickAmountChips
import com.selfbudget.app.core.ui.components.TransactionAmountHero
import com.selfbudget.app.core.util.ReceiptOcrScanner
import com.selfbudget.app.core.util.VoiceParser
import com.selfbudget.app.core.util.toWordTitleCase
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Dedicated expense-entry form. Split from what used to be a single Income/Expense
 * AddTransactionDialog (see AddIncomeDialog) so each form only shows fields relevant to that
 * type - debt-account paydown, receipt scanning, and the budget-ceiling shortcut all only make
 * sense for an expense, not income.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    accountBalances: Map<String, Double> = emptyMap(),
    budgets: List<BudgetEntity> = emptyList(),
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
        recurringFrequency: RecurringFrequency,
        debtAccountId: String?
    ) -> Unit,
    onSetCategoryBudget: ((categoryId: String, limit: Double) -> Unit)? = null,
    onAddCustomCategory: ((CategoryEntity) -> Unit)? = null,
    onAddCustomAccount: ((AccountEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val selectedType = TransactionType.EXPENSE
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    var isRecurring by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }

    var setAsBudget by remember { mutableStateOf(false) }
    var budgetLimitText by remember { mutableStateOf("") }

    val availableAccounts = remember(accounts) { accounts }
    var selectedAccount by remember {
        mutableStateOf<AccountEntity?>(null)
    }
    var selectedTargetDebtAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showTargetDebtAccountModal by remember { mutableStateOf(false) }

    val availableTargetAccounts = remember(availableAccounts, selectedAccount) {
        availableAccounts.filter { 
            (com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(it.type) || 
             it.type == AccountType.INVESTMENT || 
             it.type == AccountType.RETIREMENT || 
             it.type == AccountType.SAVINGS) && 
            it.id != selectedAccount?.id 
        }
    }
    var expandedAccountDropdown by remember { mutableStateOf(false) }
    var showNewAccountDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }

    var expandedCategoryDropdown by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var receiptImageUri by remember { mutableStateOf<Uri?>(null) }
    var isScanningOcr by remember { mutableStateOf(false) }
    var autoSuggestedCategoryName by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val scrollState = rememberScrollState()
    val focusAnchor = remember { FocusRequester() }

    // Move focus to an inert anchor (not just clear it) whenever selection modals open or close.
    // Dismissing the nested picker Dialog hands window focus back to this dialog's window, and
    // Android will restore focus onto the last real field (e.g. Amount) unless something else
    // already holds it — clearFocus() alone loses that race.
    LaunchedEffect(expandedAccountDropdown, expandedCategoryDropdown, showDatePickerModal, showTargetDebtAccountModal) {
        runCatching { focusAnchor.requestFocus() }
        keyboardController?.hide()
    }

    val availableCategories = remember(categories) {
        val dbCategories = if (categories.isNotEmpty()) categories else AppDatabase.DEFAULT_CATEGORIES
        (dbCategories + AppDatabase.DEFAULT_CATEGORIES).distinctBy { it.id }
    }
    var selectedCategory by remember {
        mutableStateOf<CategoryEntity?>(null)
    }

    val existingCategoryBudget = remember(selectedCategory, budgets) {
        selectedCategory?.let { cat -> budgets.firstOrNull { it.categoryId == cat.id && it.amountLimit > 0.0 } }
    }

    LaunchedEffect(existingCategoryBudget) {
        if (existingCategoryBudget != null) {
            budgetLimitText = "%.2f".format(existingCategoryBudget.amountLimit)
        } else {
            budgetLimitText = ""
        }
    }

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
            if (matchingPastTx != null) {
                val matchedCat = availableCategories.firstOrNull { it.id == matchingPastTx.categoryId && it.type == TransactionType.EXPENSE }
                if (matchedCat != null) {
                    selectedCategory = matchedCat
                    autoSuggestedCategoryName = matchedCat.name
                }
            } else {
                autoSuggestedCategoryName = null
            }
        } else {
            autoSuggestedCategoryName = null
        }
    }

    var note by remember { mutableStateOf("") }

    val recurringKeywordsMonthly = remember {
        listOf(
            "netflix", "hulu", "spotify", "disney", "apple", "amazon prime", "hbo", "youtube",
            "gym", "fitness", "membership", "chatgpt", "cloud", "icloud", "rent", "mortgage",
            "electric", "gas bill", "water bill", "internet", "wifi", "comcast", "att", "t-mobile",
            "verizon", "car insurance", "health insurance", "geico", "state farm", "storage", "utility", "subscription"
        )
    }
    val recurringKeywordsBiWeekly = remember {
        listOf("paycheck", "payroll", "salary", "direct deposit", "stipend")
    }

    val smartRecurringSuggestion = remember(title, allTransactions, recurringList, isRecurring) {
        if (isRecurring || title.trim().length < 2) null
        else {
            val lowerTitle = title.trim().lowercase()

            // 1. Check if title matches an existing recurring commitment
            val existingRecurringMatch = recurringList.firstOrNull { item ->
                if (item.isArchived) return@firstOrNull false
                val itemLower = item.title.trim().lowercase()
                itemLower == lowerTitle || lowerTitle.contains(itemLower) || itemLower.contains(lowerTitle)
            }
            if (existingRecurringMatch != null) {
                return@remember existingRecurringMatch.frequency
            }

            // 2. Keyword detection
            val matchesBiWeekly = recurringKeywordsBiWeekly.any { lowerTitle.contains(it) }
            val matchesMonthly = recurringKeywordsMonthly.any { lowerTitle.contains(it) || lowerTitle.contains("bill") || lowerTitle.contains("sub") }

            // 3. Past history count
            val pastCount = allTransactions.count { item ->
                val itemLower = item.title.trim().lowercase()
                itemLower == lowerTitle || lowerTitle.contains(itemLower)
            }

            if (matchesBiWeekly) {
                RecurringFrequency.BI_WEEKLY
            } else if (matchesMonthly || pastCount >= 1) {
                RecurringFrequency.MONTHLY
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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptImageUri = uri
            isScanningOcr = true
            ReceiptOcrScanner.scanReceipt(
                context,
                uri,
                onSuccess = { scanResult ->
                    isScanningOcr = false
                    scanResult.merchantName?.let { title = it }
                    scanResult.totalAmount?.let { amountText = "%.2f".format(it) }
                    scanResult.timestamp?.let { selectedTimestamp = it }
                },
                onError = {
                    isScanningOcr = false
                }
            )
        }
    }

    fun submitForm() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val categoryId = selectedCategory?.id ?: "cat_other"
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
                selectedFrequency,
                selectedTargetDebtAccount?.id
            )
            if (setAsBudget && selectedCategory != null) {
                val budgetLimit = budgetLimitText.toDoubleOrNull() ?: 0.0
                if (budgetLimit > 0.0) {
                    onSetCategoryBudget?.invoke(selectedCategory!!.id, budgetLimit)
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
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "New Expense",
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
                    // No header Save action (spec §14/§16): the header holds only close + title;
                    // the single primary action lives in the full-width footer button below.
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Inert focus target used to steal focus away from real fields when a
                    // selection modal opens/closes (see focusAnchor LaunchedEffect above).
                    Box(
                        modifier = Modifier
                            .size(0.dp)
                            .focusRequester(focusAnchor)
                            .focusable()
                    )

                    // 1. Amount Hero Card
                    TransactionAmountHero(
                        type = EntryType.Expense,
                        amountText = amountText,
                        onAmountChange = { amountText = it },
                        currencySymbol = currencySymbol,
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    QuickAmountChips(
                        presets = listOf(25, 50, 100, 250, 500),
                        currencySymbol = currencySymbol,
                        onPick = { preset ->
                            val currentVal = amountText.toDoubleOrNull() ?: 0.0
                            amountText = "%.2f".format(currentVal + preset)
                        }
                    )

                    // Smart Recurring Suggestion Banner
                    if (smartRecurringSuggestion != null) {
                        val freqText = when (smartRecurringSuggestion) {
                            RecurringFrequency.WEEKLY -> "weekly"
                            RecurringFrequency.BI_WEEKLY -> "bi-weekly"
                            RecurringFrequency.SEMI_MONTHLY -> "semi-monthly"
                            RecurringFrequency.MONTHLY -> "monthly"
                            RecurringFrequency.YEARLY -> "annual"
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
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
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Smart Suggestion",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
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
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Set Recurring", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // 2. Transaction Details Grouped Surface
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TRANSACTION DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Title / Merchant row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it.toWordTitleCase() },
                                        placeholder = {
                                            Text(
                                                "Merchant or Title (e.g. Starbucks)",
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

                                // Category Selector Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            expandedCategoryDropdown = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedCategory != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = getCategoryIcon(selectedCategory),
                                                contentDescription = null,
                                                tint = if (selectedCategory != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Category",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = selectedCategory?.name ?: "Select Category",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (selectedCategory != null) FontWeight.Medium else FontWeight.Normal,
                                            color = if (selectedCategory != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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

                                // Account Selector Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            expandedAccountDropdown = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedAccount != null) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = if (selectedAccount != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Payment Account",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                        if (selectedAccount != null) {
                                            val rawBal = accountBalances[selectedAccount!!.id] ?: selectedAccount!!.initialBalance
                                            val isLiab = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(selectedAccount!!.type)
                                            val dispBal = if (isLiab) kotlin.math.abs(rawBal) else rawBal
                                            val accSym = com.selfbudget.app.core.util.Currencies.symbolFor(selectedAccount!!.currencyCode).ifBlank { currencySymbol }
                                            Text(
                                                text = selectedAccount!!.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Balance: $accSym%.2f".format(dispBal),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Text(
                                                text = "Select Payment Account",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
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

                                // Date Selector Row
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
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = dateFormatter.format(Date(selectedTimestamp)),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
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

                        autoSuggestedCategoryName?.let { catName ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
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
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Optional Target Account Field (Shown for Credit Card / Loan Debt, Investments, Savings, Retirement, etc.)
                    val catName = selectedCategory?.name?.lowercase() ?: ""
                    val isTargetCategory = catName.contains("credit") || 
                                           catName.contains("card") || 
                                           catName.contains("loan") || 
                                           catName.contains("debt") || 
                                           catName.contains("mortgage") || 
                                           catName.contains("rent") ||
                                           catName.contains("invest") ||
                                           catName.contains("saving") ||
                                           catName.contains("stock") ||
                                           catName.contains("crypto") ||
                                           catName.contains("401k") ||
                                           catName.contains("ira") ||
                                           catName.contains("retire") ||
                                           catName.contains("transfer")
                    val shouldShowTargetAccountField = availableTargetAccounts.isNotEmpty() &&
                                                     (isTargetCategory || selectedTargetDebtAccount != null)

                    if (shouldShowTargetAccountField) {
                        val isInvestmentOrSavings = selectedTargetDebtAccount?.type == AccountType.INVESTMENT ||
                            selectedTargetDebtAccount?.type == AccountType.RETIREMENT ||
                            selectedTargetDebtAccount?.type == AccountType.SAVINGS ||
                            catName.contains("invest") ||
                            catName.contains("saving") ||
                            catName.contains("retire") ||
                            catName.contains("stock") ||
                            catName.contains("401k") ||
                            catName.contains("ira")

                        val labelText = if (isInvestmentOrSavings) {
                            "Deposit / Contribute Toward Account"
                        } else {
                            "Apply Payment Toward Debt"
                        }

                        val iconVector = if (selectedTargetDebtAccount?.type == AccountType.INVESTMENT || selectedTargetDebtAccount?.type == AccountType.RETIREMENT || catName.contains("invest") || catName.contains("retire") || catName.contains("stock") || catName.contains("401k") || catName.contains("ira")) {
                            Icons.Default.TrendingUp
                        } else if (selectedTargetDebtAccount?.type == AccountType.SAVINGS || catName.contains("saving")) {
                            Icons.Default.Savings
                        } else {
                            Icons.Default.CreditCard
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "LINKED ACCOUNT (OPTIONAL)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.1.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
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
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = iconVector,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = labelText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
                                        } ?: "None (Standard Expense)"
                                        Text(
                                            text = targetText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (selectedTargetDebtAccount != null) FontWeight.Medium else FontWeight.Normal,
                                            color = if (selectedTargetDebtAccount != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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

                            selectedTargetDebtAccount?.let { targetAcc ->
                                val isLiability = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(targetAcc.type)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isLiability) "💡 Will reduce ${targetAcc.name} debt balance" else "💡 Will increase ${targetAcc.name} balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // 3. Automation & Planning Options
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "PLANNING & AUTOMATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        // Dynamic Recurring Transaction Options Card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Repeat,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "Recurring Expense?",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = existingRecurring?.let { "Active Recurring: ${currencySymbol}${"%.2f".format(it.amount)} (${it.frequency.name.lowercase().replaceFirstChar { c -> c.uppercase() }})" }
                                                    ?: "Schedule upcoming regular bills",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (existingRecurring != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (existingRecurring != null) FontWeight.Medium else FontWeight.Normal
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isRecurring,
                                        onCheckedChange = {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            isRecurring = it
                                        },
                                        colors = androidx.compose.material3.SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = com.selfbudget.app.ui.theme.getAccentColor(),
                                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                if (isRecurring) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Repeat Frequency",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
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
                                }
                            }
                        }

                        // Dynamic Budget Option Card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalanceWallet,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "Set Category Budget?",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = existingCategoryBudget?.let { "Active Budget: ${currencySymbol}${"%.2f".format(it.amountLimit)}/mo" }
                                                    ?: selectedCategory?.let { "Set monthly limit for ${it.name}" }
                                                    ?: "Show on Budget page with monthly limit",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (existingCategoryBudget != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (existingCategoryBudget != null) FontWeight.Medium else FontWeight.Normal
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = setAsBudget,
                                        onCheckedChange = { isChecked ->
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            setAsBudget = isChecked
                                            if (isChecked && budgetLimitText.isBlank()) {
                                                val existingLimit = existingCategoryBudget?.amountLimit
                                                if (existingLimit != null && existingLimit > 0.0) {
                                                    budgetLimitText = "%.2f".format(existingLimit)
                                                }
                                            }
                                        },
                                        colors = androidx.compose.material3.SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = com.selfbudget.app.ui.theme.getAccentColor(),
                                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                if (setAsBudget) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Category Budget Scope Information Banner
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Note: This monthly limit applies to ALL bills and expenses in this category, not just this single item.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = budgetLimitText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                                budgetLimitText = input
                                            }
                                        },
                                        label = { Text("Monthly Limit Amount ($currencySymbol)") },
                                        placeholder = { Text("0.00") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // 4. Note Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "NOTE & RECEIPT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it.toWordTitleCase() },
                                    placeholder = {
                                        Text(
                                            "Add transaction notes or memo...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    singleLine = false,
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Quick Tools",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Scan Receipt Button
                                        Surface(
                                            onClick = {
                                                focusManager.clearFocus(force = true)
                                                keyboardController?.hide()
                                                imagePickerLauncher.launch("image/*")
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (receiptImageUri != null) com.selfbudget.app.ui.theme.getIncomeColor().copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            border = BorderStroke(1.dp, if (receiptImageUri != null) com.selfbudget.app.ui.theme.getIncomeColor().copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isScanningOcr) Icons.Default.AutoAwesome else (if (receiptImageUri != null) Icons.Default.Check else Icons.Default.CameraAlt),
                                                    contentDescription = "Scan Receipt",
                                                    tint = if (receiptImageUri != null) com.selfbudget.app.ui.theme.getIncomeColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isScanningOcr) "Scanning..." else (if (receiptImageUri != null) "Receipt Attached" else "Scan Receipt"),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (receiptImageUri != null) com.selfbudget.app.ui.theme.getIncomeColor() else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Voice Entry Button
                                        Surface(
                                            onClick = {
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
                                                        "e.g. 'Spent 25 dollars Lunch'"
                                                    )
                                                }
                                                voiceLauncher.launch(intent)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Voice Entry",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Voice",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full-width Primary Save CTA Button (spec §16: Teal 800/Teal 200 full-width pill)
                    PrimaryPillButton(
                        text = "Save expense",
                        onClick = { submitForm() },
                        enabled = title.isNotBlank() &&
                                  (amountText.toDoubleOrNull() ?: 0.0) > 0.0 &&
                                  selectedAccount != null &&
                                  selectedCategory != null,
                        ramp = Ramp.Teal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // Material 3 Compose Date Picker Dialog
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
                    TextButton(
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
            initialType = TransactionType.EXPENSE,
            lockType = true,
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
            transactionType = selectedType,
            onDismiss = { expandedCategoryDropdown = false },
            onSelectCategory = { cat ->
                selectedCategory = cat
                autoSuggestedCategoryName = null
                expandedCategoryDropdown = false
            },
            onAddCustomCategory = {
                expandedCategoryDropdown = false
                showNewCategoryDialog = true
            },
            onArchiveCategory = onAddCustomCategory?.let { { cat -> onAddCustomCategory.invoke(cat.copy(isArchived = true)) } }
        )
    }

    if (expandedAccountDropdown) {
        AccountSelectionModal(
            accounts = availableAccounts,
            selectedAccount = selectedAccount,
            currencySymbol = currencySymbol,
            accountBalances = accountBalances,
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
}
