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
import com.selfbudget.app.core.ui.AccountSelectionModal
import com.selfbudget.app.core.ui.CategorySelectionModal
import com.selfbudget.app.core.ui.getCategoryIcon
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.RecurringFrequency
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
import com.selfbudget.app.core.ui.components.DestructivePillButton
import com.selfbudget.app.core.ui.components.GrayIconTile
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.QuickAmountChips
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.ui.components.TransactionAmountHero
import com.selfbudget.app.core.util.ReceiptOcrScanner
import com.selfbudget.app.core.util.VoiceParser
import com.selfbudget.app.core.util.toWordTitleCase
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.selfbudget.app.data.model.RecurringTransactionEntity

private data class TransactionEditSnapshot(
    val title: String,
    val amountText: String,
    val type: TransactionType,
    val timestamp: Long,
    val accountId: String?,
    val categoryId: String?,
    val targetDebtAccountId: String?,
    val receiptUri: Uri?,
    val isRecurring: Boolean,
    val frequency: RecurringFrequency,
    val setAsBudget: Boolean,
    val budgetLimitText: String
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    accountBalances: Map<String, Double> = emptyMap(),
    budgets: List<BudgetEntity> = emptyList(),
    recurringList: List<RecurringTransactionEntity> = emptyList(),
    allTransactions: List<TransactionEntity> = emptyList(),
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirmUpdate: (updated: TransactionEntity) -> Unit,
    onDelete: ((toDelete: TransactionEntity) -> Unit)? = null,
    onAddRecurring: ((title: String, amount: Double, type: TransactionType, categoryId: String, frequency: RecurringFrequency) -> Unit)? = null,
    onDeleteRecurring: ((RecurringTransactionEntity) -> Unit)? = null,
    onSetCategoryBudget: ((categoryId: String, limit: Double) -> Unit)? = null,
    onDeleteCategoryBudget: ((categoryId: String) -> Unit)? = null,
    onAddCustomCategory: ((CategoryEntity) -> Unit)? = null,
    onAddCustomAccount: ((AccountEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var title by remember(transaction) { mutableStateOf(transaction.title) }
    var amountText by remember(transaction) { mutableStateOf("%.2f".format(transaction.amount)) }
    var selectedType by remember(transaction) { mutableStateOf(transaction.type) }
    var selectedTimestamp by remember(transaction) { mutableLongStateOf(transaction.timestamp) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    var isRecurring by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }

    var setAsBudget by remember { mutableStateOf(false) }
    var budgetLimitText by remember { mutableStateOf("") }

    val availableAccounts = remember(accounts) { accounts }
    var selectedAccount by remember(availableAccounts, transaction) {
        mutableStateOf(availableAccounts.firstOrNull { it.id == transaction.accountId } ?: availableAccounts.firstOrNull())
    }
    var selectedTargetDebtAccount by remember(availableAccounts, transaction) {
        mutableStateOf(availableAccounts.firstOrNull { it.id == transaction.transferAccountId })
    }
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

    val initialReceiptUri = remember(transaction) {
        transaction.receiptImageUri?.let { Uri.parse(it) }
    }
    var receiptImageUri by remember { mutableStateOf<Uri?>(initialReceiptUri) }
    var isScanningOcr by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val availableCategories = remember(categories) {
        val dbCategories = if (categories.isNotEmpty()) categories else AppDatabase.DEFAULT_CATEGORIES
        (dbCategories + AppDatabase.DEFAULT_CATEGORIES).distinctBy { it.id }
    }
    val filteredCategories = remember(availableCategories, selectedType) {
        availableCategories.filter { it.type == selectedType }
    }

    var selectedCategory by remember(filteredCategories, transaction) {
        mutableStateOf(filteredCategories.firstOrNull { it.id == transaction.categoryId } ?: filteredCategories.firstOrNull())
    }

    LaunchedEffect(selectedType, filteredCategories) {
        if (selectedCategory == null || selectedCategory?.type != selectedType) {
            selectedCategory = filteredCategories.firstOrNull()
        }
    }

    // Dialog opens read-only; tapping the header "Edit" button is the deliberate action that
    // unlocks the form. editBaseline is captured at that moment (after recurring/budget defaults
    // have already resolved from the loaded transaction) so isDirty reflects real user changes.
    var isEditMode by remember { mutableStateOf(false) }
    var editBaseline by remember { mutableStateOf<TransactionEditSnapshot?>(null) }

    fun captureEditSnapshot() = TransactionEditSnapshot(
        title = title,
        amountText = amountText,
        type = selectedType,
        timestamp = selectedTimestamp,
        accountId = selectedAccount?.id,
        categoryId = selectedCategory?.id,
        targetDebtAccountId = selectedTargetDebtAccount?.id,
        receiptUri = receiptImageUri,
        isRecurring = isRecurring,
        frequency = selectedFrequency,
        setAsBudget = setAsBudget,
        budgetLimitText = budgetLimitText
    )

    val isDirty = editBaseline != null && editBaseline != captureEditSnapshot()

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

    var note by remember(transaction) { mutableStateOf(transaction.note ?: "") }

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
            val updated = transaction.copy(
                title = title.trim(),
                amount = amount,
                type = selectedType,
                categoryId = categoryId,
                accountId = accId,
                timestamp = selectedTimestamp,
                note = null,
                receiptImageUri = receiptImageUri?.toString(),
                transferAccountId = selectedTargetDebtAccount?.id
            )
            onConfirmUpdate(updated)
            if (isRecurring) {
                onAddRecurring?.invoke(title.trim(), amount, selectedType, categoryId, selectedFrequency)
            } else if (!isRecurring && existingRecurring != null) {
                onDeleteRecurring?.invoke(existingRecurring)
            }
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
                            text = if (isEditMode) "Edit Transaction" else "Transaction Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    // No header Edit/Save action (spec §14/§16): the header holds only close +
                    // title. Edit is triggered from the view-mode footer; Save lives in the
                    // edit-mode footer below.
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val themeColor = if (selectedType == TransactionType.INCOME) getIncomeColor() else getExpenseColor()

                    // Inert focus target used to steal focus away from real fields when a
                    // selection modal opens/closes (see focusAnchor LaunchedEffect above).
                    Box(
                        modifier = Modifier
                            .size(0.dp)
                            .focusRequester(focusAnchor)
                            .focusable()
                    )

                    if (!isEditMode) {
                        TransactionViewModeSummary(
                            transaction = transaction,
                            themeColor = themeColor,
                            currencySymbol = currencySymbol,
                            dateFormatter = dateFormatter,
                            accountName = selectedAccount?.name,
                            categoryName = selectedCategory?.name,
                            debtAccountName = selectedTargetDebtAccount?.name,
                            isRecurringActive = isRecurring && existingRecurring != null,
                            recurringFrequency = selectedFrequency,
                            isBudgetActive = existingCategoryBudget != null && existingCategoryBudget.amountLimit > 0.0,
                            budgetLimitText = budgetLimitText,
                            onEditClick = {
                                editBaseline = captureEditSnapshot()
                                isEditMode = true
                            },
                            onDeleteClick = { showDeleteConfirmation = true },
                            onClose = onDismiss,
                            canDelete = onDelete != null
                        )
                    } else {

                    // 1. Hero Amount Card
                    TransactionAmountHero(
                        type = if (selectedType == TransactionType.INCOME) EntryType.Income else EntryType.Expense,
                        amountText = amountText,
                        onAmountChange = { amountText = it },
                        currencySymbol = currencySymbol,
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    QuickAmountChips(
                        presets = listOf(10, 25, 50, 100, 250),
                        currencySymbol = currencySymbol,
                        onPick = { preset ->
                            val currentVal = amountText.toDoubleOrNull() ?: 0.0
                            amountText = "%.2f".format(currentVal + preset)
                        }
                    )

                    // 2. Grouped Transaction Details Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TRANSACTION DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )

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
                        val shouldShowTargetAccountField = selectedType == TransactionType.EXPENSE &&
                                                         availableTargetAccounts.isNotEmpty() &&
                                                         (isTargetCategory || selectedTargetDebtAccount != null)

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Title Field
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = themeColor.copy(alpha = 0.12f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = themeColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it.toWordTitleCase() },
                                        placeholder = { Text(if (selectedType == TransactionType.INCOME) "e.g. Salary, Client Pay" else "e.g. Grocery Store, Netflix") },
                                        label = { Text(if (selectedType == TransactionType.INCOME) "Title / Payer" else "Title / Merchant") },
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words,
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

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 70.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )

                                // Date Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            showDatePickerModal = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "Date",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = dateFormatter.format(Date(selectedTimestamp)),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Pick Date",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 70.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )

                                // Account Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            expandedAccountDropdown = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalance,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = if (selectedType == TransactionType.INCOME) "Deposit Account" else "Payment Account",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = selectedAccount?.name ?: "Select Account",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            selectedAccount?.let { acc ->
                                                val rawBal = accountBalances[acc.id] ?: acc.initialBalance
                                                val isLiab = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(acc.type)
                                                val dispBal = if (isLiab) kotlin.math.abs(rawBal) else rawBal
                                                val accSym = com.selfbudget.app.core.util.Currencies.symbolFor(acc.currencyCode).ifBlank { currencySymbol }
                                                Text(
                                                    text = "${if (isLiab) "Owed: " else "Balance: "}$accSym%.2f".format(dispBal),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 70.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )

                                // Category Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            expandedCategoryDropdown = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = getCategoryIcon(selectedCategory),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "Category",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = selectedCategory?.name ?: "Select Category",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Category",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (shouldShowTargetAccountField) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 70.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    )

                                    val isInvestmentOrSavings = selectedTargetDebtAccount?.type == AccountType.INVESTMENT ||
                                        selectedTargetDebtAccount?.type == AccountType.RETIREMENT ||
                                        selectedTargetDebtAccount?.type == AccountType.SAVINGS ||
                                        catName.contains("invest") ||
                                        catName.contains("saving") ||
                                        catName.contains("retire") ||
                                        catName.contains("stock") ||
                                        catName.contains("401k") ||
                                        catName.contains("ira")

                                    val labelText = if (isInvestmentOrSavings) "Contribute Toward Account" else "Apply Payment Toward Debt"
                                    val iconVector = if (isInvestmentOrSavings) Icons.Default.TrendingUp else Icons.Default.CreditCard

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                focusManager.clearFocus(force = true)
                                                keyboardController?.hide()
                                                showTargetDebtAccountModal = true
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = iconVector,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = labelText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = selectedTargetDebtAccount?.name ?: "None (Standard Expense)",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Target",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Grouped Planning & Automation Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PLANNING & AUTOMATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth().animateContentSize()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            modifier = Modifier.size(38.dp)
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
                                                text = if (selectedType == TransactionType.INCOME) "Recurring Income?" else "Recurring Expense?",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = existingRecurring?.let { "Active: ${currencySymbol}${"%.2f".format(it.amount)} (${it.frequency.name.lowercase().replaceFirstChar { c -> c.uppercase() }})" }
                                                    ?: if (selectedType == TransactionType.INCOME) "Schedule future paychecks" else "Schedule future bills",
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
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Repeat Frequency",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    com.selfbudget.app.core.ui.components.FrequencySegmentedControl(
                                        selected = selectedFrequency,
                                        onSelect = { freq ->
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            selectedFrequency = freq
                                        }
                                    )
                                }

                                if (selectedType == TransactionType.EXPENSE) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                                modifier = Modifier.size(38.dp)
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
                                                    text = existingCategoryBudget?.let { "Active: ${currencySymbol}${"%.2f".format(it.amountLimit)}/mo" }
                                                        ?: selectedCategory?.let { "Limit for ${it.name}" }
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
                                            shape = ShapeChip,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Grouped Note & Receipt Tools Section
                    if (selectedType == TransactionType.EXPENSE || receiptImageUri != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "TOOLS & ATTACHMENTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Surface(
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            imagePickerLauncher.launch("image/*")
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (receiptImageUri != null) getIncomeColor().copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isScanningOcr) Icons.Default.AutoAwesome else (if (receiptImageUri != null) Icons.Default.Check else Icons.Default.CameraAlt),
                                                    contentDescription = null,
                                                    tint = if (receiptImageUri != null) getIncomeColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = if (receiptImageUri != null) "Receipt Scanned" else "Scan Receipt (OCR)",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (receiptImageUri != null) "Receipt photo attached" else "Auto-fill merchant, amount & date",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons Layout (spec §14/§16: one primary + one secondary, destructive isolated below)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SecondaryPillButton(
                                text = "Cancel",
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            )

                            PrimaryPillButton(
                                text = "Save changes",
                                onClick = { submitForm() },
                                enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                                ramp = Ramp.Teal,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(54.dp)
                            )
                        }

                        if (onDelete != null) {
                            DestructivePillButton(
                                text = "Delete transaction",
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    showDeleteConfirmation = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            )
                        }
                    }

                    } // end isEditMode form content

                    Spacer(modifier = Modifier.height(150.dp))
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
            initialType = selectedType,
            onDismiss = { showNewCategoryDialog = false },
            onConfirm = { newCat ->
                onAddCustomCategory?.invoke(newCat)
                selectedCategory = newCat
                selectedType = newCat.type
                showNewCategoryDialog = false
            }
        )
    }

    // Delete Transaction Confirmation Modal
    if (showDeleteConfirmation && onDelete != null) {
        val isIncome = transaction.type == TransactionType.INCOME

        Dialog(
            onDismissRequest = { showDeleteConfirmation = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RampIconTile(icon = Icons.Default.Delete, ramp = Ramp.Red, size = 64.dp, iconSize = 32.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Delete transaction?",
                        style = SelfBudgetType.title,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Are you sure you want to delete this transaction record? This cannot be undone.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Highlighted Transaction Card
                    Surface(
                        shape = ShapeCard,
                        color = Ramp.Gray.tintFill(isAppInDarkTheme()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = transaction.title,
                                style = SelfBudgetType.rowTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${if (isIncome) "+" else "-"}$currencySymbol%.2f".format(transaction.amount),
                                style = SelfBudgetType.rowTitle,
                                color = if (isIncome) getIncomeColor() else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons Row — the confirm step's destructive button may be solid red (spec §14)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = { showDeleteConfirmation = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        PrimaryPillButton(
                            text = "Delete",
                            onClick = {
                                onDelete(transaction)
                                showDeleteConfirmation = false
                            },
                            ramp = Ramp.Red,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
                    }
                }
            }
        }
    }

    if (expandedCategoryDropdown) {
        CategorySelectionModal(
            categories = categories,
            selectedCategory = selectedCategory,
            transactionType = selectedType,
            onDismiss = { expandedCategoryDropdown = false },
            onSelectCategory = { cat ->
                selectedCategory = cat
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

@Composable
private fun TransactionViewModeSummary(
    transaction: TransactionEntity,
    themeColor: Color,
    currencySymbol: String,
    dateFormatter: SimpleDateFormat,
    accountName: String?,
    categoryName: String?,
    debtAccountName: String?,
    isRecurringActive: Boolean,
    recurringFrequency: RecurringFrequency,
    isBudgetActive: Boolean,
    budgetLimitText: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClose: () -> Unit,
    canDelete: Boolean
) {
    val isIncome = transaction.type == TransactionType.INCOME

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Amount Card — neutral display number, the type badge is the only colored element (spec §14)
        val heroRamp = if (isIncome) Ramp.Teal else Ramp.Red
        val isDarkHero = isAppInDarkTheme()
        Surface(
            shape = ShapeHero,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(color = heroRamp.tintFill(isDarkHero), shape = ShapePill) {
                    Text(
                        text = if (isIncome) "INCOME" else "EXPENSE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = SelfBudgetType.eyebrow,
                        color = heroRamp.titleText(isDarkHero)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${if (isIncome) "+" else "-"}$currencySymbol${"%.2f".format(transaction.amount)}",
                    style = SelfBudgetType.display,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Grouped Details Card
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Transaction details",
                style = SelfBudgetType.section,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ViewModeInfoItem(
                        icon = Icons.Default.Info,
                        label = if (isIncome) "Title / Payer" else "Title / Merchant",
                        value = transaction.title
                    )
                    SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                    ViewModeInfoItem(
                        icon = Icons.Default.CalendarToday,
                        label = "Date",
                        value = dateFormatter.format(Date(transaction.timestamp))
                    )
                    SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                    ViewModeInfoItem(
                        icon = Icons.Default.AccountBalance,
                        label = if (isIncome) "Deposit Account" else "Payment Account",
                        value = accountName ?: "—"
                    )
                    SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                    ViewModeInfoItem(
                        icon = Icons.Default.Category,
                        label = "Category",
                        value = categoryName ?: "—"
                    )

                    if (debtAccountName != null) {
                        SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                        ViewModeInfoItem(
                            icon = Icons.Default.CreditCard,
                            label = "Applied Toward Debt",
                            value = debtAccountName
                        )
                    }

                    if (isRecurringActive) {
                        SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                        ViewModeInfoItem(
                            icon = Icons.Default.Repeat,
                            label = "Recurring Schedule",
                            value = recurringFrequency.name.lowercase().replaceFirstChar { it.uppercase() }
                        )
                    }

                    if (isBudgetActive) {
                        SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                        ViewModeInfoItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = "Monthly Category Budget",
                            value = "$currencySymbol${budgetLimitText.ifBlank { "0.00" }}/mo"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons (spec §14: Close + Edit pair, destructive isolated below)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryPillButton(
                    text = "Close",
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                )

                PrimaryPillButton(
                    text = "Edit transaction",
                    onClick = onEditClick,
                    ramp = Ramp.Teal,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(54.dp)
                )
            }

            if (canDelete) {
                DestructivePillButton(
                    text = "Delete transaction",
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )
            }
        }
    }
}

/** Detail-row icon tiles are Gray (spec §14): they label a field TYPE, not a category. */
@Composable
private fun ViewModeInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            GrayIconTile(icon = icon, size = 36.dp, iconSize = 18.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = SelfBudgetType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = SelfBudgetType.rowTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}
