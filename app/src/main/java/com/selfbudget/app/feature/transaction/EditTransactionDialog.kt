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
import androidx.compose.ui.text.TextStyle
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
import com.selfbudget.app.core.util.ReceiptOcrScanner
import com.selfbudget.app.core.util.VoiceParser
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.selfbudget.app.data.model.RecurringTransactionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
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

    val availableDebtAccounts = remember(availableAccounts, selectedAccount) {
        availableAccounts.filter { (it.type == AccountType.CREDIT_CARD || it.type == AccountType.LOAN) && it.id != selectedAccount?.id }
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

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val scrollState = rememberScrollState()
    val focusAnchor = remember { FocusRequester() }

    // Move focus to an inert anchor (not just clear it) whenever selection modals open or close.
    // Dismissing the nested picker Dialog hands window focus back to this dialog's window, and
    // Android will restore focus onto the last real field (e.g. Amount) unless something else
    // already holds it — clearFocus() alone loses that race.
    LaunchedEffect(expandedAccountDropdown, expandedCategoryDropdown, showDatePickerModal, showTargetDebtAccountModal) {
        focusAnchor.requestFocus()
        keyboardController?.hide()
    }

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

    var note by remember(transaction) { mutableStateOf(transaction.note ?: "") }

    val existingCategoryBudget = remember(selectedCategory, budgets) {
        selectedCategory?.let { cat -> budgets.firstOrNull { it.categoryId == cat.id } }
    }

    LaunchedEffect(existingCategoryBudget) {
        if (existingCategoryBudget != null) {
            setAsBudget = true
            budgetLimitText = "%.2f".format(existingCategoryBudget.amountLimit)
        } else {
            setAsBudget = false
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
                val budgetLimit = budgetLimitText.toDoubleOrNull() ?: amount
                if (budgetLimit > 0.0) {
                    onSetCategoryBudget?.invoke(selectedCategory!!.id, budgetLimit)
                }
            } else if (!setAsBudget && selectedCategory != null && existingCategoryBudget != null) {
                onDeleteCategoryBudget?.invoke(selectedCategory!!.id)
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
                            text = "Edit Transaction",
                            style = MaterialTheme.typography.titleMedium,
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
                            enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
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

                    val themeColor = if (selectedType == TransactionType.INCOME) IncomeGreen else ExpenseRed

                    // Top Amount Entry Stepper (- $0.00 +) in 44.sp ExtraBold font
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AMOUNT ($currencySymbol)",
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
                                    val next = maxOf(0.0, current - 5.0)
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
                                                color = themeColor.copy(alpha = 0.35f),
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
                                                color = themeColor
                                            ),
                                            modifier = Modifier.padding(end = 2.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = themeColor,
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
                                    val next = current + 5.0
                                    amountText = "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = themeColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Amount",
                                        tint = themeColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Preset Amount Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val presets = if (selectedType == TransactionType.EXPENSE) listOf(5, 10, 25, 50, 100) else listOf(50, 100, 250, 500, 1000)
                            presets.forEach { preset ->
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

                    // Transaction Type Badge
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = themeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedType == TransactionType.EXPENSE) "Expense 🔴" else "Income 🟢",
                                fontWeight = FontWeight.Bold,
                                color = themeColor,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.replaceFirstChar { char -> char.uppercase() } },
                        label = { Text(if (selectedType == TransactionType.INCOME) "Title / Payer" else "Title / Merchant") },
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

                    // Date Picker Input Box (Full-width non-focusable surface)
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

                    // Account Field (Taps to open AccountSelectionModal)
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
                            value = selectedAccount?.name ?: if (selectedType == TransactionType.INCOME) "Select Deposit Account" else "Select Payment Account",
                            onValueChange = {},
                            enabled = false,
                            label = { Text(if (selectedType == TransactionType.INCOME) "Deposit Account" else "Payment Account") },
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

                    // Optional Target Debt Account Field (Only shown for Credit Card / Loan Payment or Rent / Mortgage categories)
                    val catName = selectedCategory?.name?.lowercase() ?: ""
                    val isDebtOrMortgageCategory = catName.contains("credit") || 
                                                   catName.contains("card") || 
                                                   catName.contains("loan") || 
                                                   catName.contains("debt") || 
                                                   catName.contains("mortgage") || 
                                                   catName.contains("rent")
                    val shouldShowDebtAccountField = selectedType == TransactionType.EXPENSE &&
                                                     availableDebtAccounts.isNotEmpty() &&
                                                     (isDebtOrMortgageCategory || selectedTargetDebtAccount != null)

                    if (shouldShowDebtAccountField) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedTargetDebtAccount?.name ?: "None (Standard Expense)",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Apply Payment Toward Debt (Optional)") },
                                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                                    shape = RoundedCornerShape(14.dp),
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
                                        text = "💡 Will reduce ${debtAcc.name} debt balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic Recurring Transaction Options Card
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
                                            text = if (selectedType == TransactionType.INCOME) "Recurring Income?" else "Recurring Expense?",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = existingRecurring?.let { "Active Recurring Item: ${currencySymbol}${"%.2f".format(it.amount)} (${it.frequency.name.lowercase().replaceFirstChar { c -> c.uppercase() }})" }
                                                ?: if (selectedType == TransactionType.INCOME) "Schedule future paychecks" else "Schedule future bills",
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
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val options = listOf(
                                            RecurringFrequency.MONTHLY to "Monthly",
                                            RecurringFrequency.WEEKLY to "Weekly",
                                            RecurringFrequency.BI_WEEKLY to "Bi-weekly",
                                            RecurringFrequency.YEARLY to "Yearly"
                                        )
                                        options.forEach { (freq, label) ->
                                            val isSelected = selectedFrequency == freq
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(3.dp)
                                                    .clip(RoundedCornerShape(9.dp))
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

                    // Dynamic Budget Option Card (Only for Expense transactions)
                    if (selectedType == TransactionType.EXPENSE) {
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
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Set Category Budget?",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = existingCategoryBudget?.let { "Active Category Budget: ${currencySymbol}${"%.2f".format(it.amountLimit)}/mo" }
                                                    ?: selectedCategory?.let { "Set monthly limit for ${it.name}" }
                                                    ?: "Show on Budget page with monthly limit",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (existingCategoryBudget != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (existingCategoryBudget != null) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = setAsBudget,
                                        onCheckedChange = {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            setAsBudget = it
                                            if (it && budgetLimitText.isBlank()) {
                                                budgetLimitText = amountText.ifBlank { "" }
                                            }
                                        }
                                    )
                                }

                                if (setAsBudget) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Category Budget Scope Information Banner
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
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

                                    Text(
                                        text = "Monthly Budget Limit",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons Layout:
                    // Row 1: [ Cancel ] | [ Save Changes ]
                    // Row 2: [ 🗑️ Delete Transaction ] (if onDelete != null)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    onDismiss()
                                },
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
                                enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        if (onDelete != null) {
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    showDeleteConfirmation = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, ExpenseRed.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                            ) {
                                Text("Delete Transaction", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
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
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing Red Trash Badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Delete Transaction?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Are you sure you want to delete this transaction record? This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Highlighted Transaction Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (isIncome) "+" else "-"}$currencySymbol%.2f".format(transaction.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onDelete(transaction)
                                showDeleteConfirmation = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
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

    if (showTargetDebtAccountModal) {
        AccountSelectionModal(
            accounts = availableDebtAccounts,
            selectedAccount = selectedTargetDebtAccount,
            currencySymbol = currencySymbol,
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
