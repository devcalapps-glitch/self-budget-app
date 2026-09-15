package com.selfbudget.app.feature.budget

import android.app.Activity
import java.util.Locale
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.util.VoiceParser
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Switch

import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.getWarningColor
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Calculate

private data class BudgetEditSnapshot(
    val categoryId: String?,
    val limitText: String,
    val rolloverEnabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetBudgetDialog(
    categories: List<CategoryEntity>,
    initialCategoryId: String? = null,
    initialLimit: Double? = null,
    initialRolloverEnabled: Boolean = false,
    currencySymbol: String = "$",
    recurringList: List<RecurringTransactionEntity> = emptyList(),
    transactions: List<TransactionEntity> = emptyList(),
    budgetUiModel: CategoryBudgetUiModel? = null,
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
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.background else Color(0xFFFAFAFA)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularBackButton(onClick = onDismiss)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!isEditing) "Set category budget" else if (isEditMode) "Edit category budget" else "Category budget details",
                                style = com.selfbudget.app.ui.theme.SelfBudgetType.title,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Save/Edit lives in the footer only — never duplicated in the header (spec §14/§19).
                    }
                }

                // Scrollable Form Content with clean radial glow on off-white background
                val isDark = isSystemInDarkTheme()
                val glowColorCenter = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                val glowColorMid = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (!isDark) {
                                Modifier.drawWithContent {
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                glowColorCenter,
                                                glowColorMid,
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width / 2f, size.height * 0.18f),
                                            radius = maxOf(size.width, size.height) * 0.75f
                                        )
                                    )
                                    drawContent()
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (isEditing && !isEditMode) {
                            val activeCatId = selectedCategory?.id
                            val currentCategoryTransactions = remember(transactions, activeCatId) {
                                if (activeCatId == null) emptyList()
                                else transactions.filter { it.categoryId == activeCatId }
                            }
                            val currentCategoryRecurring = remember(recurringList, activeCatId) {
                                if (activeCatId == null) emptyList()
                                else recurringList.filter { it.categoryId == activeCatId && !it.isArchived }
                            }

                            BudgetViewModeSummary(
                                currencySymbol = currencySymbol,
                                limitText = limitText,
                                category = selectedCategory,
                                rolloverEnabled = rolloverEnabled,
                                categoryRecurringMonthly = categoryRecurringMonthly,
                                budgetUiModel = budgetUiModel,
                                categoryTransactions = currentCategoryTransactions,
                                categoryRecurring = currentCategoryRecurring,
                                onEditClick = { enterEditMode() },
                                onDeleteClick = if (initialCategoryId != null && onDeleteBudget != null) {
                                    {
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        showDeleteConfirmationModal = true
                                    }
                                } else null,
                                onClose = onDismiss
                            )
                        } else {

                        // 1. Hero Amount Card — shared component (spec §16): every amount-entry
                        // card in the app uses this one implementation, not a per-screen copy.
                        com.selfbudget.app.core.ui.components.TransactionAmountHero(
                            type = com.selfbudget.app.core.ui.components.EntryType.Income,
                            amountText = limitText,
                            onAmountChange = { limitText = it },
                            currencySymbol = currencySymbol,
                            badgeText = "MONTHLY BUDGET LIMIT",
                            ramp = com.selfbudget.app.ui.theme.Ramp.Teal
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        com.selfbudget.app.core.ui.components.QuickAmountChips(
                            presets = listOf(50, 100, 250, 500, 1000),
                            currencySymbol = currencySymbol,
                            onPick = { preset ->
                                val currentVal = limitText.toDoubleOrNull() ?: 0.0
                                limitText = "%.2f".format(currentVal + preset)
                            }
                        )

                        // 2. Grouped Budget Details Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "BUDGET DETAILS",
                                style = SelfBudgetType.eyebrow,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            val formRamp = selectedCategory?.let { com.selfbudget.app.ui.theme.sectionRamp(it.name) } ?: Ramp.Teal
                            Surface(
                                shape = com.selfbudget.app.ui.theme.ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Category Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                focusManager.clearFocus(force = true)
                                                keyboardController?.hide()
                                                expandedDropdown = true
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            com.selfbudget.app.core.ui.components.RampIconTile(
                                                icon = getCategoryIcon(selectedCategory),
                                                ramp = formRamp,
                                                size = 38.dp,
                                                iconSize = 20.dp
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = "Expense category",
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = selectedCategory?.name ?: "Select Expense Category",
                                                    style = SelfBudgetType.rowTitle,
                                                    color = if (selectedCategory != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = "Select Category",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 70.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    )

                                    // Rollover Switch Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            com.selfbudget.app.core.ui.components.GrayIconTile(
                                                icon = Icons.Default.Autorenew,
                                                size = 38.dp,
                                                iconSize = 20.dp
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text("Roll over unused budget", style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                                                Text(
                                                    text = "Carries forward previous month's balance.",
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = rolloverEnabled,
                                            onCheckedChange = { rolloverEnabled = it },
                                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = com.selfbudget.app.ui.theme.getAccentColor(),
                                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    if (categoryRecurringMonthly > 0.0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 70.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    limitText = "%.2f".format(categoryRecurringMonthly)
                                                }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                com.selfbudget.app.core.ui.components.RampIconTile(
                                                    icon = Icons.Default.AutoAwesome,
                                                    ramp = Ramp.Purple,
                                                    size = 38.dp,
                                                    iconSize = 20.dp
                                                )
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column {
                                                    Text(
                                                        text = "Recurring bills floor",
                                                        style = SelfBudgetType.meta,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "Set to $currencySymbol%.2f/mo".format(categoryRecurringMonthly),
                                                        style = SelfBudgetType.rowTitle,
                                                        color = Ramp.Purple.secondaryText(isAppInDarkTheme())
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Apply Floor",
                                                tint = Ramp.Purple.secondaryText(isAppInDarkTheme())
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Category Budget Scope Information Banner
                        Surface(
                            color = Ramp.Teal.tintFill(isAppInDarkTheme()),
                            shape = com.selfbudget.app.ui.theme.ShapeCard,
                            border = BorderStroke(1.dp, Ramp.Teal.containerBorder(isAppInDarkTheme())),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.selfbudget.app.core.ui.components.RampIconTile(
                                    icon = Icons.Default.Info,
                                    ramp = Ramp.Teal,
                                    size = 36.dp,
                                    iconSize = 18.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Note: This monthly limit applies to all bills and expenses logged under this category.",
                                    style = SelfBudgetType.meta,
                                    color = Ramp.Teal.titleText(isAppInDarkTheme())
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons Layout
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                com.selfbudget.app.core.ui.components.SecondaryPillButton(
                                    text = "Cancel",
                                    ramp = com.selfbudget.app.ui.theme.Ramp.Gray,
                                    onClick = {
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        if (isEditing) {
                                            editBaseline?.let { baseline ->
                                                selectedCategory = expenseCategories.firstOrNull { it.id == baseline.categoryId }
                                                limitText = baseline.limitText
                                                rolloverEnabled = baseline.rolloverEnabled
                                            }
                                            isEditMode = false
                                        } else {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                )

                                com.selfbudget.app.core.ui.components.PrimaryPillButton(
                                    text = if (isEditing) "Save changes" else "Save budget",
                                    ramp = com.selfbudget.app.ui.theme.Ramp.Teal,
                                    enabled = canSave,
                                    onClick = { saveBudget() },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(54.dp)
                                )
                            }

                            if (isEditing && initialCategoryId != null && onDeleteBudget != null) {
                                com.selfbudget.app.core.ui.components.DestructivePillButton(
                                    text = "Delete category budget",
                                    onClick = {
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        showDeleteConfirmationModal = true
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
        }
    }

    // Delete Confirmation Modal
    if (showDeleteConfirmationModal) {
        Dialog(
            onDismissRequest = { showDeleteConfirmationModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = com.selfbudget.app.ui.theme.ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    com.selfbudget.app.core.ui.components.RampIconTile(
                        icon = Icons.Default.Delete,
                        ramp = com.selfbudget.app.ui.theme.Ramp.Red,
                        size = 64.dp,
                        iconSize = 32.dp
                    )

                    Text(
                        text = "Delete category budget?",
                        style = com.selfbudget.app.ui.theme.SelfBudgetType.title,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Are you sure you want to delete the monthly budget limit for \"${selectedCategory?.name ?: "this category"}\"? This action will remove the budget allocation.",
                        style = com.selfbudget.app.ui.theme.SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        com.selfbudget.app.core.ui.components.SecondaryPillButton(
                            text = "Cancel",
                            onClick = { showDeleteConfirmationModal = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        com.selfbudget.app.core.ui.components.PrimaryPillButton(
                            text = "Delete",
                            onClick = {
                                showDeleteConfirmationModal = false
                                selectedCategory?.id?.let { catId -> onDeleteBudget?.invoke(catId) }
                            },
                            ramp = com.selfbudget.app.ui.theme.Ramp.Red,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
                    }
                }
            }
        }
    }

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
    category: CategoryEntity?,
    rolloverEnabled: Boolean,
    categoryRecurringMonthly: Double,
    budgetUiModel: CategoryBudgetUiModel?,
    categoryTransactions: List<TransactionEntity>,
    categoryRecurring: List<RecurringTransactionEntity>,
    onEditClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    onClose: () -> Unit
) {
    val themeColor = MaterialTheme.colorScheme.primary
    val effectiveLimit = budgetUiModel?.budgetLimit ?: (limitText.toDoubleOrNull() ?: 0.0)
    val spentAmount = budgetUiModel?.spentAmount ?: categoryTransactions.sumOf { it.amount }
    val pendingBills = budgetUiModel?.pendingUpcomingAmount ?: (categoryRecurringMonthly - spentAmount).coerceAtLeast(0.0)
    val safeToSpend = budgetUiModel?.safeToSpendAmount ?: (effectiveLimit - spentAmount - pendingBills).coerceAtLeast(0.0)
    val isOver = budgetUiModel?.isOverBudget ?: ((spentAmount + pendingBills) > effectiveLimit + 0.005)
    val isWarning = budgetUiModel?.isWarning ?: false
    val status = when {
        isOver -> com.selfbudget.app.ui.theme.BudgetStatus.Over
        isWarning -> com.selfbudget.app.ui.theme.BudgetStatus.Watch
        else -> com.selfbudget.app.ui.theme.BudgetStatus.Safe
    }
    val statusColor = if (isOver) getExpenseColor() else if (isWarning) getWarningColor() else getIncomeColor()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Card: Category & Status Banner
        val catRamp = category?.let { com.selfbudget.app.ui.theme.sectionRamp(it.name) } ?: Ramp.Teal
        val isDark = isAppInDarkTheme()
        Surface(
            shape = com.selfbudget.app.ui.theme.ShapeCard,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                com.selfbudget.app.core.ui.components.RampIconTile(
                    icon = getCategoryIcon(category),
                    ramp = catRamp,
                    size = 52.dp,
                    iconSize = 26.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = category?.name ?: "Category Budget",
                    style = SelfBudgetType.title,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                com.selfbudget.app.core.ui.components.StatusBadge(
                    text = if (isOver) "Over budget" else if (isWarning) "Approaching limit" else "On track",
                    status = status
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                val progressVal = if (effectiveLimit > 0.0) ((spentAmount + pendingBills) / effectiveLimit).toFloat().coerceIn(0f, 1f) else 0f
                com.selfbudget.app.core.ui.components.StatusProgressBar(
                    progress = progressVal,
                    status = status,
                    height = 8.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$currencySymbol%.2f allocated".format(spentAmount + pendingBills),
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "of $currencySymbol%.2f limit".format(effectiveLimit),
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 2. Budget Math Equation Breakdown Card
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "CALCULATION BREAKDOWN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            Surface(
                shape = com.selfbudget.app.ui.theme.ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    BudgetViewModeInfoItem(
                        icon = Icons.Default.Calculate,
                        label = "Monthly Base Limit",
                        value = "$currencySymbol%.2f".format(budgetUiModel?.ownLimit ?: effectiveLimit),
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 70.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    BudgetViewModeInfoItem(
                        icon = Icons.Default.Autorenew,
                        label = "Rollover Adjustment",
                        value = if (rolloverEnabled) {
                            val diff = effectiveLimit - (budgetUiModel?.ownLimit ?: effectiveLimit)
                            if (diff >= 0) "+$currencySymbol%.2f".format(diff) else "-$currencySymbol%.2f".format(kotlin.math.abs(diff))
                        } else "Off ($currencySymbol 0.00)",
                        valueColor = if (rolloverEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 70.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    BudgetViewModeInfoItem(
                        icon = Icons.Default.ReceiptLong,
                        label = "Actual Spent (This Month)",
                        value = "-$currencySymbol%.2f".format(spentAmount),
                        valueColor = if (spentAmount > 0) getExpenseColor() else MaterialTheme.colorScheme.onSurface,
                        iconTint = getExpenseColor()
                    )
                    if (pendingBills > 0.0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 70.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                        BudgetViewModeInfoItem(
                            icon = Icons.Default.Schedule,
                            label = "Upcoming Committed Bills",
                            value = "-$currencySymbol%.2f".format(pendingBills),
                            valueColor = getWarningColor(),
                            iconTint = getWarningColor()
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                    BudgetViewModeInfoItem(
                        icon = Icons.Default.AutoAwesome,
                        label = "Safe to Spend (Discretionary)",
                        value = "$currencySymbol%.2f".format(safeToSpend),
                        valueColor = statusColor,
                        iconTint = statusColor
                    )
                }
            }
        }

        // 3. Recurring Bills in this Category
        if (categoryRecurring.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "RECURRING BILLS IN CATEGORY (${categoryRecurring.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Surface(
                    shape = com.selfbudget.app.ui.theme.ShapeCard,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        categoryRecurring.forEachIndexed { index, rec ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 70.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )
                            }
                            val monthlyEquivalent = RecurringFrequencyNormalizer.toMonthlyAmount(rec.amount, rec.frequency)
                            val freqText = rec.frequency.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
                            BudgetViewModeInfoItem(
                                icon = Icons.Default.Autorenew,
                                label = "${rec.title} ($freqText)",
                                value = "$currencySymbol%.2f/mo".format(monthlyEquivalent),
                                valueColor = MaterialTheme.colorScheme.primary,
                                iconTint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 4. Recent Transactions in this Category
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "LOGGED TRANSACTIONS (${categoryTransactions.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            Surface(
                shape = com.selfbudget.app.ui.theme.ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (categoryTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses logged in this category this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        val displayTransactions: List<TransactionEntity> = categoryTransactions.sortedByDescending { it.timestamp }.take(8)
                        displayTransactions.forEachIndexed { index, tx ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 70.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )
                            }
                            val dateStr = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp))
                            BudgetViewModeInfoItem(
                                icon = Icons.Default.ReceiptLong,
                                label = "${tx.title.ifBlank { "Expense" }} • $dateStr",
                                value = "-$currencySymbol%.2f".format(tx.amount),
                                valueColor = getExpenseColor(),
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (categoryTransactions.size > 8) {
                            Text(
                                text = "+ ${categoryTransactions.size - 8} more transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Action Buttons Layout
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.selfbudget.app.core.ui.components.SecondaryPillButton(
                    text = "Close",
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                )

                com.selfbudget.app.core.ui.components.PrimaryPillButton(
                    text = "Edit budget",
                    onClick = onEditClick,
                    ramp = Ramp.Teal,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(54.dp)
                )
            }

            if (onDeleteClick != null) {
                com.selfbudget.app.core.ui.components.DestructivePillButton(
                    text = "Delete category budget",
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )
            }
        }
    }
}

@Composable
private fun BudgetViewModeInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            com.selfbudget.app.core.ui.components.GrayIconTile(
                icon = icon,
                size = 36.dp,
                iconSize = 18.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = SelfBudgetType.rowTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = SelfBudgetType.rowTitle,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}
