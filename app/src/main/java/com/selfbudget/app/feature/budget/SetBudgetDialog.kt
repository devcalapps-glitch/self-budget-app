package com.selfbudget.app.feature.budget

import android.app.Activity
import java.util.Locale
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.selfbudget.app.core.ui.AddCustomCategoryDialog
import com.selfbudget.app.core.ui.CategorySelectionModal
import com.selfbudget.app.core.ui.getCategoryIcon
import androidx.compose.material.icons.filled.Category
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.util.VoiceParser
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Switch

private data class BudgetEditSnapshot(
    val categoryId: String?,
    val limitText: String,
    val rolloverEnabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetDialog(
    categories: List<CategoryEntity>,
    initialCategoryId: String? = null,
    initialLimit: Double? = null,
    initialRolloverEnabled: Boolean = false,
    currencySymbol: String = "$",
    recurringList: List<RecurringTransactionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String, limit: Double, rolloverEnabled: Boolean) -> Unit,
    onDeleteBudget: ((categoryId: String) -> Unit)? = null,
    onAddCustomCategory: ((CategoryEntity) -> Unit)? = null
) {
    var rolloverEnabled by remember(initialRolloverEnabled) { mutableStateOf(initialRolloverEnabled) }
    val isEditing = remember(initialLimit, initialCategoryId) { initialLimit != null || initialCategoryId != null }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val expenseCategories = remember(categories) {
        categories.filter { it.type == TransactionType.EXPENSE }
    }

    var selectedCategory by remember(expenseCategories, initialCategoryId) {
        mutableStateOf(
            expenseCategories.firstOrNull { it.id == initialCategoryId }
        )
    }
    var limitText by remember(initialLimit) {
        mutableStateOf(initialLimit?.let { "%.2f".format(it) } ?: "")
    }
    var showDeleteConfirmationModal by remember { mutableStateOf(false) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Existing budgets open read-only; tapping "Edit" is the deliberate action that unlocks the
    // form. A brand-new budget (isEditing == false) has nothing to view yet, so it always opens
    // straight into the form.
    var isEditMode by remember(isEditing) { mutableStateOf(!isEditing) }
    var editBaseline by remember { mutableStateOf<BudgetEditSnapshot?>(null) }

    fun captureEditSnapshot() = BudgetEditSnapshot(
        categoryId = selectedCategory?.id,
        limitText = limitText,
        rolloverEnabled = rolloverEnabled
    )

    val isDirty = editBaseline != null && editBaseline != captureEditSnapshot()
    val isValidBudget = selectedCategory != null && (limitText.toDoubleOrNull() ?: 0.0) > 0.0
    val canSave = if (isEditing) isDirty && isValidBudget else isValidBudget

    fun enterEditMode() {
        editBaseline = captureEditSnapshot()
        isEditMode = true
    }

    fun saveBudget() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        val cat = selectedCategory
        val limit = limitText.toDoubleOrNull()
        if (cat != null && limit != null && limit > 0) {
            onConfirm(cat.id, limit, rolloverEnabled)
        }
    }

    val categoryRecurringMonthly = remember(selectedCategory, recurringList) {
        val catId = selectedCategory?.id ?: return@remember 0.0
        recurringList
            .filter { it.type == TransactionType.EXPENSE && !it.isArchived && it.categoryId == catId }
            .sumOf { rec -> RecurringFrequencyNormalizer.toMonthlyAmount(rec.amount, rec.frequency) }
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
                    limitText = "%.2f".format(parsed.amount)
                    val matchedCat = expenseCategories.firstOrNull {
                        it.name.contains(parsed.title, ignoreCase = true) || parsed.title.contains(it.name, ignoreCase = true)
                    }
                    if (matchedCat != null) {
                        selectedCategory = matchedCat
                    }
                } else {
                    val numberMatch = Regex("""\d+(\.\d+)?""").find(spokenText)?.value
                    if (numberMatch != null) {
                        limitText = numberMatch
                    }
                    val matchedCat = expenseCategories.firstOrNull {
                        spokenText.contains(it.name, ignoreCase = true)
                    }
                    if (matchedCat != null) {
                        selectedCategory = matchedCat
                    }
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
                            text = if (!isEditing) "Set Category Budget" else if (isEditMode) "Edit Category Budget" else "Category Budget Details",
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
                            onClick = {
                                if (isEditing && !isEditMode) {
                                    enterEditMode()
                                } else {
                                    saveBudget()
                                }
                            },
                            enabled = if (isEditing && !isEditMode) true else canSave,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(if (isEditing && !isEditMode) "Edit" else "Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    if (isEditing && !isEditMode) {
                        BudgetViewModeSummary(
                            currencySymbol = currencySymbol,
                            limitText = limitText,
                            categoryName = selectedCategory?.name,
                            rolloverEnabled = rolloverEnabled,
                            categoryRecurringMonthly = categoryRecurringMonthly
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
                            text = "MONTHLY BUDGET LIMIT ($currencySymbol)",
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
                                    val current = limitText.toDoubleOrNull() ?: 0.0
                                    val next = maxOf(0.0, current - 25.0)
                                    limitText = if (next == 0.0) "" else "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Subtract Limit",
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
                                    value = limitText,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                            limitText = input
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            text = "0.00",
                                            style = TextStyle(
                                                fontSize = 44.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
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
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.padding(end = 2.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
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
                                    val current = limitText.toDoubleOrNull() ?: 0.0
                                    val next = current + 25.0
                                    limitText = "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Limit",
                                        tint = MaterialTheme.colorScheme.primary
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
                                        val currentVal = limitText.toDoubleOrNull() ?: 0.0
                                        limitText = "%.2f".format(currentVal + preset)
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

                    // 2. Category Field (Taps to open CategorySelectionModal)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Select Expense Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expense Category") },
                            trailingIcon = { Icon(getCategoryIcon(selectedCategory), contentDescription = null) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    expandedDropdown = true
                                }
                        )
                    }

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
                                text = "Note: This monthly limit applies to ALL bills and expenses in this category, not just a single item.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

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
                                Text("Roll Over Unused Budget", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Carries forward previous month's balance.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            }
                        }
                        Switch(checked = rolloverEnabled, onCheckedChange = { rolloverEnabled = it })
                    }

                    if (categoryRecurringMonthly > 0.0) {
                        AssistChip(
                            onClick = {
                                limitText = "%.2f".format(categoryRecurringMonthly)
                            },
                            label = {
                                Text(
                                    text = "Set to bill floor ($currencySymbol%.2f/mo)".format(categoryRecurringMonthly),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        )
                    }

                    // Voice Assistant Entry Button
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
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
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(
                                            RecognizerIntent.EXTRA_PROMPT,
                                            "e.g. 'Groceries 500 dollars'"
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

                    } // end isEditMode form content

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons Layout:
                    // Row 1: [ Cancel/Close ] | [ Save Budget / Save Budget Changes / Edit ] on the same line
                    // Row 2: [ Delete Category Budget ] stacked directly below
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Text(if (isEditing && !isEditMode) "Close" else "Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                             Button(
                                onClick = {
                                    if (isEditing && !isEditMode) {
                                        enterEditMode()
                                    } else {
                                        saveBudget()
                                    }
                                },
                                enabled = if (isEditing && !isEditMode) true else canSave,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isEditing && !isEditMode) "Edit Category Budget" else if (isEditing) "Save Changes" else "Save Budget",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        if (initialCategoryId != null && onDeleteBudget != null) {
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    showDeleteConfirmationModal = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, ExpenseRed.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                            ) {
                                Text("Delete Category Budget", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }
    }

    // Delete Confirmation Modal
    if (showDeleteConfirmationModal) {
        Dialog(
            onDismissRequest = { showDeleteConfirmationModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
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

                    Text(
                        text = "Delete Category Budget?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Are you sure you want to delete the monthly budget limit for \"${selectedCategory?.name ?: "this category"}\"? This action will remove the budget allocation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmationModal = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showDeleteConfirmationModal = false
                                selectedCategory?.id?.let { catId -> onDeleteBudget?.invoke(catId) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showNewCategoryDialog) {
        AddCustomCategoryDialog(
            initialType = TransactionType.EXPENSE,
            onDismiss = { showNewCategoryDialog = false },
            onConfirm = { newCat ->
                onAddCustomCategory?.invoke(newCat)
                selectedCategory = newCat
                showNewCategoryDialog = false
            }
        )
    }

    if (expandedDropdown) {
        CategorySelectionModal(
            categories = expenseCategories,
            selectedCategory = selectedCategory,
            transactionType = TransactionType.EXPENSE,
            onDismiss = { expandedDropdown = false },
            onSelectCategory = { cat ->
                selectedCategory = cat
                expandedDropdown = false
            },
            onAddCustomCategory = {
                expandedDropdown = false
                showNewCategoryDialog = true
            },
            onArchiveCategory = onAddCustomCategory?.let { { cat -> onAddCustomCategory.invoke(cat.copy(isArchived = true)) } }
        )
    }
}

@Composable
private fun BudgetViewModeSummary(
    currencySymbol: String,
    limitText: String,
    categoryName: String?,
    rolloverEnabled: Boolean,
    categoryRecurringMonthly: Double
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
                text = "MONTHLY BUDGET LIMIT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currencySymbol${limitText.ifBlank { "0.00" }}",
                style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            )
        }

        BudgetInfoRow(icon = Icons.Default.Category, label = "Expense Category", value = categoryName ?: "—")
        BudgetInfoRow(
            icon = Icons.Default.Autorenew,
            label = "Roll Over Unused Budget",
            value = if (rolloverEnabled) "On" else "Off",
            valueColor = if (rolloverEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        if (categoryRecurringMonthly > 0.0) {
            BudgetInfoRow(
                icon = Icons.Default.AutoAwesome,
                label = "Recurring Bills In This Category",
                value = "$currencySymbol%.2f/mo".format(categoryRecurringMonthly),
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun BudgetInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
