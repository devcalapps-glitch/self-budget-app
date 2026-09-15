package com.selfbudget.app.feature.recurring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.selfbudget.app.core.ui.getCategoryIcon
import com.selfbudget.app.core.ui.GoalSelectionModal
import com.selfbudget.app.core.ui.MonthYearHeader
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.core.util.RecurringCycleCalculator.getCyclePaymentSummary
import com.selfbudget.app.core.util.RecurringCyclePaymentSummary
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.getAccentColor
import com.selfbudget.app.ui.theme.getWarningColor
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.containerBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.selfbudget.app.core.ui.AccountSelectionModal
import com.selfbudget.app.core.ui.AddCustomAccountDialog
import com.selfbudget.app.core.ui.AddCustomCategoryDialog
import com.selfbudget.app.core.ui.CategorySelectionModal
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.util.toWordTitleCase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.core.ui.RecurringBillsModal
import com.selfbudget.app.core.ui.RecurringIncomeModal
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.CardSurfaceDark
import com.selfbudget.app.ui.theme.DividerDark
import com.selfbudget.app.ui.theme.PageBackgroundDark
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.TextPrimaryDark
import com.selfbudget.app.ui.theme.TextSecondaryDark
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.getProgressBarColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization

private data class RecurringEditSnapshot(
    val title: String,
    val amountText: String,
    val frequency: RecurringFrequency,
    val categoryId: String?,
    val isArchived: Boolean,
    val nextDueDate: Long,
    val hasLimitedOccurrences: Boolean,
    val occurrencesText: String,
    val transferAccountId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringScreen(
    recurringList: List<RecurringTransactionEntity>,
    categories: List<CategoryEntity>,
    allTransactions: List<TransactionEntity> = emptyList(),
    selectedMonthYear: String = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date()),
    onPreviousMonth: (() -> Unit)? = null,
    onNextMonth: (() -> Unit)? = null,
    onSelectMonthYear: ((String) -> Unit)? = null,
    currencySymbol: String = "$",
    accounts: List<com.selfbudget.app.data.model.AccountEntity> = emptyList(),
    accountBalances: Map<String, Double> = emptyMap(),
    goals: List<GoalEntity> = emptyList(),
    onAddRecurring: (title: String, amount: Double, type: TransactionType, categoryId: String, frequency: RecurringFrequency, remainingOccurrences: Int?, nextDueDate: Long?, transferAccountId: String?) -> Unit,
    onDeleteRecurring: (RecurringTransactionEntity) -> Unit,
    onPostTransaction: (RecurringTransactionEntity, Double, String?) -> Unit = { _, _, _ -> },
    onUpdateRecurring: (RecurringTransactionEntity) -> Unit = {},
    onAddCustomCategory: ((CategoryEntity) -> Unit)? = null,
    onAddCustomAccount: ((AccountEntity) -> Unit)? = null,
    // Lets the single global "+" (owned by HomeScreen) open this screen's "new recurring" dialog
    // from anywhere in the app, instead of this screen needing its own floating add button.
    requestNewRecurring: Boolean = false,
    requestNewRecurringType: TransactionType = TransactionType.EXPENSE,
    onNewRecurringRequestHandled: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedFilterType by remember { mutableStateOf<TransactionType?>(null) }
    var selectedRecurringForDetails by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var pendingDuplicateItem by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var pendingPostItem by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var duplicateMatchDate by remember { mutableStateOf<String?>(null) }
    var recentlyPostedId by remember { mutableStateOf<String?>(null) }
    var postedBannerMessage by remember { mutableStateOf<String?>(null) }
    var showRecurringIncomeModal by remember { mutableStateOf(false) }
    var showRecurringBillsModal by remember { mutableStateOf(false) }
    val monthNameFormatter = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(requestNewRecurring) {
        if (requestNewRecurring) {
            addDialogInitialType = requestNewRecurringType
            showAddDialog = true
            onNewRecurringRequestHandled()
        }
    }

    val executePost: (RecurringTransactionEntity, Double, String?) -> Unit = { itemToPost, postAmount, targetGoalId ->
        onPostTransaction(itemToPost, postAmount, targetGoalId)
        val bannerToken = itemToPost.id
        postedBannerMessage = "Posted \"${itemToPost.title}\" ($currencySymbol%.2f)".format(postAmount)
        recentlyPostedId = itemToPost.id
        coroutineScope.launch {
            delay(2500L)
            if (recentlyPostedId == bannerToken) {
                recentlyPostedId = null
            }
            if (postedBannerMessage != null) {
                postedBannerMessage = null
            }
        }
    }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Archived/finished items no longer count toward totals - they're done, not active.
    val activeList = remember(recurringList) { recurringList.filter { !it.isArchived } }
    val activeIncomeCount = remember(activeList) { activeList.count { it.type == TransactionType.INCOME } }
    val activeExpenseCount = remember(activeList) { activeList.count { it.type == TransactionType.EXPENSE } }

    val totalRecurringExpense = remember(activeList) {
        Money.sum(activeList.filter { it.type == TransactionType.EXPENSE }.map { rec ->
            com.selfbudget.app.core.util.RecurringFrequencyNormalizer.toMonthlyAmount(rec.amount, rec.frequency)
        })
    }
    val totalRecurringIncome = remember(activeList) {
        Money.sum(activeList.filter { it.type == TransactionType.INCOME }.map { rec ->
            com.selfbudget.app.core.util.RecurringFrequencyNormalizer.toMonthlyAmount(rec.amount, rec.frequency)
        })
    }
    val netRecurringMonthly = remember(totalRecurringIncome, totalRecurringExpense) {
        totalRecurringIncome - totalRecurringExpense
    }
    val billsRatio = remember(totalRecurringIncome, totalRecurringExpense) {
        if (totalRecurringIncome > 0.0) {
            (totalRecurringExpense / totalRecurringIncome).toFloat().coerceIn(0f, 1f)
        } else if (totalRecurringExpense > 0.0) {
            1f
        } else {
            0f
        }
    }

    val now = remember { System.currentTimeMillis() }
    val sevenDaysLater = remember { now + 7L * 24 * 60 * 60 * 1000 }
    val upcomingDueExpenses = remember(activeList) {
        activeList.filter { it.type == TransactionType.EXPENSE && it.nextDueDate in now..sevenDaysLater }
    }
    val upcomingDueTotal = remember(upcomingDueExpenses) {
        Money.sum(upcomingDueExpenses.map { it.amount })
    }

    val filteredList = remember(recurringList, selectedFilterType) {
        val list = if (selectedFilterType == null) recurringList
        else recurringList.filter { it.type == selectedFilterType }
        list.sortedBy { it.isArchived }
    }

    val currentMonthName = remember(selectedMonthYear) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val monthSdf = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
        try {
            monthSdf.format(sdf.parse(selectedMonthYear) ?: java.util.Date())
        } catch (e: Exception) {
            selectedMonthYear
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Reimagined Committed Cash Flow Hero Card
            val isDarkHero = isAppInDarkTheme()
            Surface(
                shape = ShapeHero,
                color = if (isDarkHero) CardSurfaceDark else MaterialTheme.colorScheme.surface,
                border = if (isDarkHero) BorderStroke(0.5.dp, DividerDark) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row: Badge Icon + Eyebrow / Status + Count Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val flowRamp = if (netRecurringMonthly >= 0) Ramp.Teal else Ramp.Red
                            com.selfbudget.app.core.ui.components.RampIconTile(
                                icon = Icons.Default.Autorenew,
                                ramp = flowRamp,
                                size = 40.dp,
                                iconSize = 22.dp
                            )

                            Column {
                                Text(
                                    text = "COMMITTED CASH FLOW",
                                    style = SelfBudgetType.eyebrow,
                                    color = if (isDarkHero) flowRamp.c400 else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = when {
                                        activeList.isEmpty() -> "No recurring items configured"
                                        netRecurringMonthly >= 0 -> "Positive recurring flow"
                                        else -> "Bills exceed recurring income"
                                    },
                                    style = SelfBudgetType.title,
                                    color = if (isDarkHero) TextPrimaryDark else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = ShapePill,
                            color = if (isDarkHero) DividerDark else getAccentColor().copy(alpha = 0.12f),
                            border = if (isDarkHero) null else BorderStroke(1.dp, getAccentColor().copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "${activeList.size} Active",
                                style = SelfBudgetType.badge,
                                color = if (isDarkHero) TextSecondaryDark else getAccentColor(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Main Metric: Net Monthly Recurring
                    Column {
                        val absNet = kotlin.math.abs(netRecurringMonthly)
                        Text(
                            text = "$currencySymbol%,.2f".format(absNet),
                            style = SelfBudgetType.display,
                            color = if (netRecurringMonthly >= 0) {
                                getIncomeColor()
                            } else {
                                getExpenseColor()
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (netRecurringMonthly >= 0) "estimated monthly net recurring flow" else "monthly recurring deficit",
                            style = SelfBudgetType.meta,
                            color = if (isDarkHero) TextSecondaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Dual Progress Bar (Committed vs Free)
                    if (totalRecurringIncome > 0.0) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { billsRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(ShapeChip),
                                color = getProgressBarColor(if (billsRatio > 0.9f) getExpenseColor() else if (billsRatio > 0.6f) getWarningColor() else getAccentColor()),
                                trackColor = if (isDarkHero) DividerDark else MaterialTheme.colorScheme.surfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(billsRatio * 100).toInt()}% committed to bills",
                                    style = SelfBudgetType.meta,
                                    color = if (isDarkHero) TextSecondaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currencySymbol%,.0f free / mo".format(netRecurringMonthly.coerceAtLeast(0.0)),
                                    style = SelfBudgetType.meta,
                                    color = if (netRecurringMonthly >= 0) getIncomeColor() else getExpenseColor()
                                )
                            }
                        }
                    }

                    // Side-by-Side Metric Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Paychecks Summary Tile
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showRecurringIncomeModal = true },
                            shape = ShapeChip,
                            color = if (isDarkHero) PageBackgroundDark else getIncomeColor().copy(alpha = 0.08f),
                            border = if (isDarkHero) BorderStroke(0.5.dp, DividerDark) else BorderStroke(1.dp, getIncomeColor().copy(alpha = 0.22f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDarkHero) Ramp.Teal.tintFill(isDarkHero) else getIncomeColor().copy(alpha = 0.18f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = getIncomeColor(),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Paychecks",
                                        style = SelfBudgetType.meta,
                                        color = if (isDarkHero) TextSecondaryDark else getIncomeColor()
                                    )
                                }
                                Text(
                                    text = "$currencySymbol%,.2f".format(totalRecurringIncome),
                                    style = SelfBudgetType.heading,
                                    color = if (isDarkHero) TextPrimaryDark else getIncomeColor()
                                )
                                Text(
                                    text = "$activeIncomeCount active stream${if (activeIncomeCount == 1) "" else "s"}",
                                    style = SelfBudgetType.meta,
                                    color = if (isDarkHero) TextSecondaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Bills & Subscriptions Summary Tile
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showRecurringBillsModal = true },
                            shape = ShapeChip,
                            color = if (isDarkHero) PageBackgroundDark else getExpenseColor().copy(alpha = 0.08f),
                            border = if (isDarkHero) BorderStroke(0.5.dp, DividerDark) else BorderStroke(1.dp, getExpenseColor().copy(alpha = 0.22f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDarkHero) Ramp.Red.c900 else getExpenseColor().copy(alpha = 0.18f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (isDarkHero) Ramp.Red.c200 else getExpenseColor(),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Bills & Subs",
                                        style = SelfBudgetType.meta,
                                        color = if (isDarkHero) TextSecondaryDark else getExpenseColor()
                                    )
                                }
                                Text(
                                    text = "$currencySymbol%,.2f".format(totalRecurringExpense),
                                    style = SelfBudgetType.heading,
                                    color = if (isDarkHero) Ramp.Red.c200 else getExpenseColor()
                                )
                                Text(
                                    text = "$activeExpenseCount active bill${if (activeExpenseCount == 1) "" else "s"}",
                                    style = SelfBudgetType.meta,
                                    color = if (isDarkHero) TextSecondaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Upcoming Due Soon Mini-Banner (if any due in next 7 days)
                    if (upcomingDueExpenses.isNotEmpty()) {
                        Surface(
                            shape = ShapeChip,
                            color = if (isDarkHero) PageBackgroundDark else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isDarkHero) BorderStroke(0.5.dp, DividerDark) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isDarkHero) Ramp.Amber.c200 else getWarningColor(),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${upcomingDueExpenses.size} bill${if (upcomingDueExpenses.size == 1) "" else "s"} due in next 7 days ($currencySymbol%,.2f)".format(upcomingDueTotal),
                                    style = SelfBudgetType.meta,
                                    color = if (isDarkHero) TextPrimaryDark else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Pills (matching BudgetScreen pill style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val filters = listOf(
                    Triple("All (${recurringList.size})", null, null),
                    Triple("Bills", TransactionType.EXPENSE, getExpenseColor()),
                    Triple("Paychecks", TransactionType.INCOME, getIncomeColor())
                )

                val isDarkFilters = isAppInDarkTheme()
                filters.forEach { (label, type, dotColor) ->
                    val selected = selectedFilterType == type
                    Surface(
                        shape = ShapePill,
                        color = if (selected) Ramp.Teal.solidFill(isDarkFilters) else Ramp.Gray.tintFill(isDarkFilters),
                        border = if (selected) null else BorderStroke(0.5.dp, Ramp.Gray.containerBorder(isDarkFilters)),
                        modifier = Modifier
                            .height(42.dp)
                            .clickable { selectedFilterType = type }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (dotColor != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (selected) Color.White.copy(alpha = 0.9f) else dotColor)
                                    )
                                }
                                Text(
                                    text = label,
                                    style = SelfBudgetType.rowTitle,
                                    color = if (selected) Ramp.Teal.onSolidFill(isDarkFilters) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = com.selfbudget.app.ui.theme.ShapeCard,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        com.selfbudget.app.core.ui.components.RampIconTile(
                            icon = Icons.Default.Repeat,
                            ramp = com.selfbudget.app.ui.theme.Ramp.Teal,
                            size = 64.dp,
                            iconSize = 32.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "No monthly commitments",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.heading,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Track your recurring paychecks, rent, subscriptions, and utilities with automatic next-due reminders.",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        com.selfbudget.app.core.ui.components.PrimaryPillButton(
                            text = when (selectedFilterType) {
                                TransactionType.INCOME -> "Add recurring income"
                                TransactionType.EXPENSE -> "Add recurring expense"
                                else -> "Add recurring item"
                            },
                            onClick = {
                                addDialogInitialType = selectedFilterType ?: TransactionType.EXPENSE
                                showAddDialog = true
                            }
                        )
                    }
                }
            } else {
                val (incomeItems, expenseItems) = filteredList.partition { it.type == TransactionType.INCOME }

                // Shared row body so "Paychecks" and "Bills & subscriptions" bands render
                // identical rows without duplicating this closure over outer screen state.
                val recurringRow: @Composable (RecurringTransactionEntity, com.selfbudget.app.ui.theme.Ramp) -> Unit = { item, groupRamp ->
                    val category = categoryMap[item.categoryId]
                    val isIncome = item.type == TransactionType.INCOME
                    val freqText = when (item.frequency) {
                        RecurringFrequency.WEEKLY -> "Weekly"
                        RecurringFrequency.BI_WEEKLY -> "Bi-weekly"
                        RecurringFrequency.SEMI_MONTHLY -> "Semi-monthly (2x/mo)"
                        RecurringFrequency.MONTHLY -> "Monthly"
                        RecurringFrequency.YEARLY -> "Annual"
                    }
                    val cycleSummary = remember(item, allTransactions, selectedMonthYear) {
                        getCyclePaymentSummary(item, allTransactions, selectedMonthYear)
                    }
                    val isFullyPostedThisCycle = cycleSummary.isFullyPaid
                    val isPartiallyPaidThisCycle = cycleSummary.isPartiallyPaid
                    val rowRamp = if (item.isArchived) com.selfbudget.app.ui.theme.Ramp.Gray else groupRamp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRecurringForDetails = item }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                com.selfbudget.app.core.ui.components.RampIconTile(
                                    icon = category?.let { getCategoryIcon(it) } ?: Icons.Default.Repeat,
                                    ramp = rowRamp,
                                    size = 36.dp,
                                    iconSize = 18.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                        color = if (item.isArchived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${category?.name ?: "General"} · $freqText",
                                        style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                // Ordinary amounts are neutral; only income reads Teal (spec §10 money colors).
                                Text(
                                    text = "${if (isIncome) "+" else "-"}$currencySymbol%.2f".format(item.amount),
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                    color = if (item.isArchived) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else if (isIncome) {
                                        com.selfbudget.app.ui.theme.getIncomeColor()
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (item.frequency == RecurringFrequency.SEMI_MONTHLY || item.frequency == RecurringFrequency.BI_WEEKLY || item.frequency == RecurringFrequency.WEEKLY) {
                                    val monthlyNorm = RecurringFrequencyNormalizer.toMonthlyAmount(item.amount, item.frequency)
                                    Text(
                                        text = "$currencySymbol%.2f/mo".format(monthlyNorm),
                                        style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (item.isArchived) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    com.selfbudget.app.core.ui.components.NeutralBadge(
                                        text = if (item.remainingOccurrences == 0) "Completed" else "Archived"
                                    )
                                } else if (isFullyPostedThisCycle) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val statusText = if (cycleSummary.expectedOccurrences > 1) {
                                        "Posted (${cycleSummary.postedOccurrences} of ${cycleSummary.expectedOccurrences})"
                                    } else {
                                        "Posted for ${monthNameFormatter.format(cycleSummary.matchingTransactions.firstOrNull()?.timestamp?.let { Date(it) } ?: Date())}"
                                    }
                                    com.selfbudget.app.core.ui.components.StatusBadge(
                                        text = statusText,
                                        status = com.selfbudget.app.ui.theme.BudgetStatus.Safe
                                    )
                                } else if (isPartiallyPaidThisCycle) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val statusText = if (cycleSummary.expectedOccurrences > 1) {
                                        "${cycleSummary.postedOccurrences} of ${cycleSummary.expectedOccurrences} posted ($currencySymbol%.2f) · $currencySymbol%.2f left".format(cycleSummary.totalPaid, cycleSummary.remainingAmount)
                                    } else {
                                        "Paid $currencySymbol%.2f · $currencySymbol%.2f left".format(cycleSummary.totalPaid, cycleSummary.remainingAmount)
                                    }
                                    com.selfbudget.app.core.ui.components.StatusBadge(
                                        text = statusText,
                                        status = com.selfbudget.app.ui.theme.BudgetStatus.Watch
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Next: ${dateFormatter.format(Date(item.nextDueDate))}",
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val isPosted = recentlyPostedId == item.id || isFullyPostedThisCycle
                            val isArchived = item.isArchived
                            val accentColor = com.selfbudget.app.ui.theme.getAccentColor()
                            val postButtonColor = if (isArchived) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            } else if (isPosted) {
                                getIncomeColor()
                            } else if (isPartiallyPaidThisCycle) {
                                getWarningColor()
                            } else {
                                accentColor
                            }
                            val postButtonLabel = if (isArchived) {
                                "Archived"
                            } else if (isPosted) {
                                "Posted"
                            } else if (cycleSummary.expectedOccurrences > 1 && cycleSummary.postedOccurrences > 0) {
                                "Post ${cycleSummary.postedOccurrences + 1}${if (cycleSummary.postedOccurrences + 1 == 2) "nd" else if (cycleSummary.postedOccurrences + 1 == 3) "rd" else "th"} ($currencySymbol%.2f)".format(item.amount)
                            } else if (isPartiallyPaidThisCycle) {
                                "Pay $currencySymbol%.2f".format(cycleSummary.remainingAmount)
                            } else {
                                "Post now"
                            }
                            Button(
                                onClick = {
                                    if (isPosted) {
                                        duplicateMatchDate = cycleSummary.matchingTransactions.firstOrNull()?.timestamp?.let { dateFormatter.format(Date(it)) } ?: dateFormatter.format(Date())
                                        pendingDuplicateItem = item
                                    } else {
                                        pendingPostItem = item
                                    }
                                },
                                enabled = !isArchived,
                                shape = com.selfbudget.app.ui.theme.ShapePill,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = postButtonColor,
                                    disabledContainerColor = Color.Transparent,
                                    disabledContentColor = postButtonColor
                                ),
                                border = BorderStroke(0.5.dp, postButtonColor.copy(alpha = if (isArchived) 0.4f else 0.6f)),
                                elevation = null
                            ) {
                                Icon(
                                    imageVector = if (isArchived) Icons.Default.Archive else if (isPosted) Icons.Default.Verified else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = postButtonLabel,
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.badge
                                )
                            }
                        }

                        item.remainingOccurrences?.let { remaining ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (remaining == 1) "Last payment remaining" else "$remaining payments remaining",
                                style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (incomeItems.isNotEmpty()) {
                        com.selfbudget.app.core.ui.components.SectionHeaderBand(
                            title = "Paychecks",
                            ramp = com.selfbudget.app.ui.theme.Ramp.Teal,
                            icon = Icons.Default.TrendingUp
                        ) {
                            incomeItems.forEachIndexed { index, item ->
                                if (index > 0) com.selfbudget.app.core.ui.components.SectionRowDivider()
                                recurringRow(item, com.selfbudget.app.ui.theme.Ramp.Teal)
                            }
                        }
                    }
                    if (expenseItems.isNotEmpty()) {
                        com.selfbudget.app.core.ui.components.SectionHeaderBand(
                            title = "Bills & subscriptions",
                            ramp = com.selfbudget.app.ui.theme.Ramp.Coral,
                            icon = Icons.Default.Repeat
                        ) {
                            expenseItems.forEachIndexed { index, item ->
                                if (index > 0) com.selfbudget.app.core.ui.components.SectionRowDivider()
                                recurringRow(item, com.selfbudget.app.ui.theme.Ramp.Coral)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        if (showAddDialog) {
            SetRecurringDialog(
                categories = categories,
                currencySymbol = currencySymbol,
                accounts = accounts,
                accountBalances = accountBalances,
                initialType = addDialogInitialType,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, amount, type, categoryId, frequency, remainingOccurrences, nextDueDate, transferAccountId ->
                    onAddRecurring(title, amount, type, categoryId, frequency, remainingOccurrences, nextDueDate, transferAccountId)
                    showAddDialog = false
                },
                onAddCustomCategory = onAddCustomCategory,
                onAddCustomAccount = onAddCustomAccount
            )
        }

        // Full-page confirmation before any "Post Now" actually posts: lets the user see the
        // bill's details and pick which account the money actually comes from / goes to for this
        // occurrence, instead of silently reusing whatever account the item was created with.
        pendingPostItem?.let { postItem ->
            val cycleSummary = remember(postItem, allTransactions, selectedMonthYear) {
                getCyclePaymentSummary(postItem, allTransactions, selectedMonthYear)
            }
            PostRecurringConfirmModal(
                item = postItem,
                cycleSummary = cycleSummary,
                accounts = accounts,
                accountBalances = accountBalances,
                goals = goals,
                categoryName = categoryMap[postItem.categoryId]?.name,
                currencySymbol = currencySymbol,
                onDismiss = { pendingPostItem = null },
                onConfirm = { accountId, amount, goalId ->
                    executePost(postItem.copy(accountId = accountId), amount, goalId)
                    pendingPostItem = null
                },
                onDelete = {
                    pendingDeleteItem = postItem
                    pendingPostItem = null
                },
                onAddCustomAccount = onAddCustomAccount
            )
        }

        // Potential Duplicate Entry Warning Modal
        if (pendingDuplicateItem != null) {
            val itemToPost = pendingDuplicateItem!!
            val isIncome = itemToPost.type == TransactionType.INCOME
            val isDarkDup = com.selfbudget.app.ui.theme.isAppInDarkTheme()

            Dialog(
                onDismissRequest = { pendingDuplicateItem = null },
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
                        com.selfbudget.app.core.ui.components.RampIconTile(
                            icon = Icons.Default.Warning,
                            ramp = com.selfbudget.app.ui.theme.Ramp.Amber,
                            size = 64.dp,
                            iconSize = 32.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Potential duplicate entry",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "An entry matching this recurring transaction was already logged recently.",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Highlighted Transaction Details Card
                        Surface(
                            shape = ShapeCard,
                            color = com.selfbudget.app.ui.theme.Ramp.Gray.tintFill(isDarkDup),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = itemToPost.title,
                                        style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${if (isIncome) "+" else "-"}$currencySymbol%.2f".format(itemToPost.amount),
                                        style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                        color = if (isIncome) com.selfbudget.app.ui.theme.getIncomeColor() else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                com.selfbudget.app.core.ui.components.SectionRowDivider()
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Previously logged:",
                                        style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = duplicateMatchDate ?: "Today",
                                        style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.selfbudget.app.core.ui.components.SecondaryPillButton(
                                text = "Cancel",
                                onClick = { pendingDuplicateItem = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )

                            com.selfbudget.app.core.ui.components.PrimaryPillButton(
                                text = "Post anyway",
                                onClick = {
                                    executePost(itemToPost, itemToPost.amount, null)
                                    pendingDuplicateItem = null
                                },
                                ramp = com.selfbudget.app.ui.theme.Ramp.Amber,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )
                        }
                    }
                }
            }
        }

        // Delete Recurring Confirmation Modal
        if (pendingDeleteItem != null) {
            val itemToDelete = pendingDeleteItem!!
            val isIncome = itemToDelete.type == TransactionType.INCOME
            val isDarkDel = com.selfbudget.app.ui.theme.isAppInDarkTheme()

            Dialog(
                onDismissRequest = { pendingDeleteItem = null },
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
                        com.selfbudget.app.core.ui.components.RampIconTile(
                            icon = Icons.Default.Delete,
                            ramp = com.selfbudget.app.ui.theme.Ramp.Red,
                            size = 64.dp,
                            iconSize = 32.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Delete recurring entry?",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Are you sure you want to remove this recurring commitment? This cannot be undone.",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Highlighted Item Card
                        Surface(
                            shape = ShapeCard,
                            color = com.selfbudget.app.ui.theme.Ramp.Gray.tintFill(isDarkDel),
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
                                    text = itemToDelete.title,
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${if (isIncome) "+" else "-"}$currencySymbol%.2f".format(itemToDelete.amount),
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                    color = if (isIncome) com.selfbudget.app.ui.theme.getIncomeColor() else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.selfbudget.app.core.ui.components.SecondaryPillButton(
                                text = "Cancel",
                                onClick = { pendingDeleteItem = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )

                            com.selfbudget.app.core.ui.components.PrimaryPillButton(
                                text = "Delete",
                                onClick = {
                                    onDeleteRecurring(itemToDelete)
                                    pendingDeleteItem = null
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

        // Edit Recurring - same full-screen modal pattern as the rest of the app (persistent top
        // bar with Close + inline Save), instead of the old compact read-only details dialog.
        // Post Now and Delete act on the last-saved item and live in the sticky bottom bar; field
        // edits (title, amount, frequency, category, end date, archive) are only committed by Save.
        selectedRecurringForDetails?.let { item ->
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusAnchor = remember { FocusRequester() }

            val isIncome = item.type == TransactionType.INCOME
            val cycleSummary = remember(item, allTransactions, selectedMonthYear) {
                getCyclePaymentSummary(item, allTransactions, selectedMonthYear)
            }
            val isFullyPostedThisCycle = cycleSummary.isFullyPaid
            val isJustPosted = recentlyPostedId == item.id || isFullyPostedThisCycle

            var title by remember(item.id) { mutableStateOf(item.title) }
            var amountText by remember(item.id) { mutableStateOf("%.2f".format(item.amount)) }
            var selectedFrequency by remember(item.id) { mutableStateOf(item.frequency) }
            var selectedCategory by remember(item.id) { mutableStateOf(categoryMap[item.categoryId]) }
            var isArchived by remember(item.id) { mutableStateOf(item.isArchived) }
            var selectedNextDueDate by remember(item.id) { mutableStateOf(item.nextDueDate) }
            var showDatePickerModal by remember { mutableStateOf(false) }
            var hasLimitedOccurrences by remember(item.id) { mutableStateOf(item.remainingOccurrences != null) }
            var occurrencesText by remember(item.id) { mutableStateOf(item.remainingOccurrences?.toString() ?: "") }
            var expandedCategory by remember { mutableStateOf(false) }
            var showNewCategoryDialog by remember { mutableStateOf(false) }

            var selectedTargetDebtAccount by remember(item.id) {
                mutableStateOf(accounts.firstOrNull { it.id == item.transferAccountId })
            }
            var showTargetDebtAccountModal by remember { mutableStateOf(false) }
            var showNewAccountDialog by remember { mutableStateOf(false) }

            val availableTargetAccounts = remember(accounts) {
                accounts.filter {
                    com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(it.type) ||
                    it.type == AccountType.INVESTMENT ||
                    it.type == AccountType.RETIREMENT ||
                    it.type == AccountType.SAVINGS
                }
            }

            val amount = amountText.toDoubleOrNull() ?: 0.0
            val isValid = title.isNotBlank() && amount > 0.0

            LaunchedEffect(expandedCategory, showTargetDebtAccountModal) {
                runCatching { focusAnchor.requestFocus() }
                keyboardController?.hide()
            }

            // Dialog opens read-only; tapping "Edit" is the deliberate action that unlocks the
            // form. Post Now / Delete stay live regardless of mode since they act on the
            // last-saved item, not the in-progress edit draft.
            var isEditMode by remember(item.id) { mutableStateOf(false) }
            var editBaseline by remember(item.id) { mutableStateOf<RecurringEditSnapshot?>(null) }

            fun captureEditSnapshot() = RecurringEditSnapshot(
                title = title,
                amountText = amountText,
                frequency = selectedFrequency,
                categoryId = selectedCategory?.id,
                isArchived = isArchived,
                nextDueDate = selectedNextDueDate,
                hasLimitedOccurrences = hasLimitedOccurrences,
                occurrencesText = occurrencesText,
                transferAccountId = selectedTargetDebtAccount?.id
            )

            val isDirty = editBaseline != null && editBaseline != captureEditSnapshot()

            fun enterEditMode() {
                editBaseline = captureEditSnapshot()
                isEditMode = true
            }

            fun save() {
                if (!isValid) return
                val remainingOccurrences = if (hasLimitedOccurrences) occurrencesText.toIntOrNull() else null
                onUpdateRecurring(
                    item.copy(
                        title = title.trim(),
                        amount = amount,
                        categoryId = selectedCategory?.id ?: item.categoryId,
                        frequency = selectedFrequency,
                        nextDueDate = selectedNextDueDate,
                        remainingOccurrences = remainingOccurrences,
                        isArchived = isArchived,
                        transferAccountId = selectedTargetDebtAccount?.id
                    )
                )
                selectedRecurringForDetails = null
            }

            fun postNow() {
                if (isArchived) return
                selectedRecurringForDetails = null
                if (!isJustPosted) {
                    pendingPostItem = item
                } else {
                    duplicateMatchDate = cycleSummary.matchingTransactions.firstOrNull()?.timestamp?.let { dateFormatter.format(Date(it)) } ?: dateFormatter.format(Date())
                    pendingDuplicateItem = item
                }
            }

            Dialog(
                onDismissRequest = { selectedRecurringForDetails = null },
                properties = DialogProperties(
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
                            // No header Edit/Save action (spec §14/§16): the header holds only
                            // close + title. Edit is triggered from the view-mode footer; Save
                            // lives in the edit-mode footer below.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularBackButton(onClick = { selectedRecurringForDetails = null })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEditMode) "Edit Recurring" else "Recurring Details",
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.title,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Scrollable Form Content with clean radial glow on off-white background
                        val isDark = isSystemInDarkTheme()
                        val glowColorCenter = if (isIncome) getIncomeColor().copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        val glowColorMid = if (isIncome) getIncomeColor().copy(alpha = 0.05f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
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
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Inert focus anchor
                                Box(
                                    modifier = Modifier
                                        .size(0.dp)
                                        .focusRequester(focusAnchor)
                                        .focusable()
                                )

                                val themeColor = if (isIncome) com.selfbudget.app.ui.theme.getIncomeColor() else com.selfbudget.app.ui.theme.getExpenseColor()

                            if (!isEditMode) {
                                RecurringViewModeSummary(
                                    isIncome = isIncome,
                                    themeColor = themeColor,
                                    currencySymbol = currencySymbol,
                                    amountText = amountText,
                                    title = title,
                                    frequency = selectedFrequency,
                                    categoryName = selectedCategory?.name,
                                    nextDueDate = selectedNextDueDate,
                                    dateFormatter = dateFormatter,
                                    hasLimitedOccurrences = hasLimitedOccurrences,
                                    occurrencesText = occurrencesText,
                                    isArchived = isArchived,
                                    isJustPosted = isJustPosted,
                                    cycleSummary = cycleSummary,
                                    transferAccountName = selectedTargetDebtAccount?.name ?: item.transferAccountId?.let { accountMap[it]?.name },
                                    onPostNow = { postNow() },
                                    onEditClick = { enterEditMode() },
                                    onDeleteClick = {
                                        pendingDeleteItem = item
                                        selectedRecurringForDetails = null
                                    },
                                    onClose = { selectedRecurringForDetails = null }
                                )
                            } else {

                                // 1. Amount Hero + Quick-Add Chips
                                com.selfbudget.app.core.ui.components.TransactionAmountHero(
                                    type = if (isIncome) com.selfbudget.app.core.ui.components.EntryType.Income else com.selfbudget.app.core.ui.components.EntryType.Expense,
                                    amountText = amountText,
                                    onAmountChange = { amountText = it },
                                    currencySymbol = currencySymbol,
                                    badgeText = if (isIncome) "RECURRING INCOME AMOUNT" else "RECURRING EXPENSE AMOUNT",
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

                                // 2. Grouped Commitment Details Section
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "COMMITMENT DETAILS",
                                        style = com.selfbudget.app.ui.theme.SelfBudgetType.eyebrow,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )

                                    Surface(
                                        shape = ShapeCard,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // Title Row (free text — kept inline, not a picker)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                com.selfbudget.app.core.ui.components.GrayIconTile(icon = Icons.Default.Info, size = 36.dp, iconSize = 18.dp)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                OutlinedTextField(
                                                    value = title,
                                                    onValueChange = { title = it.toWordTitleCase() },
                                                    placeholder = {
                                                        Text(
                                                            if (isIncome) "Income title (e.g. Salary, Freelance)" else "Bill title (e.g. Netflix, Rent)",
                                                            style = com.selfbudget.app.ui.theme.SelfBudgetType.body,
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
                                                    textStyle = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle.copy(color = MaterialTheme.colorScheme.onSurface),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color.Transparent,
                                                        unfocusedBorderColor = Color.Transparent,
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))

                                            // Category Row
                                            com.selfbudget.app.core.ui.components.FieldRow(
                                                icon = getCategoryIcon(selectedCategory),
                                                label = "Category",
                                                value = selectedCategory?.name ?: "Select category",
                                                isPlaceholder = selectedCategory == null,
                                                showChevron = true,
                                                onClick = {
                                                    focusManager.clearFocus(force = true)
                                                    keyboardController?.hide()
                                                    expandedCategory = true
                                                }
                                            )

                                            com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))

                                            // Start / Next Due Date Row
                                            com.selfbudget.app.core.ui.components.FieldRow(
                                                icon = Icons.Default.CalendarToday,
                                                label = "Start / next due date",
                                                value = dateFormatter.format(Date(selectedNextDueDate)),
                                                showChevron = true,
                                                onClick = {
                                                    focusManager.clearFocus(force = true)
                                                    keyboardController?.hide()
                                                    showDatePickerModal = true
                                                }
                                            )
                                        }
                                    }
                                }

                                // 3. Repeat Frequency & Schedule Section
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "FREQUENCY & SCHEDULE",
                                        style = com.selfbudget.app.ui.theme.SelfBudgetType.eyebrow,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )

                                    Surface(
                                        shape = ShapeCard,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize()
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
                                                    color = com.selfbudget.app.ui.theme.Ramp.Teal.tintFill(isDark),
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
                                                        color = com.selfbudget.app.ui.theme.Ramp.Teal.secondaryText(isDark),
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 4. Optional Target Debt / Linked Account
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
                                val shouldShowTargetAccountField = !isIncome &&
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
                                        "Deposit / contribute toward account"
                                    } else {
                                        "Apply payment toward debt"
                                    }

                                    val iconVector = if (selectedTargetDebtAccount?.type == AccountType.INVESTMENT || selectedTargetDebtAccount?.type == AccountType.RETIREMENT || recurringCategoryName.contains("invest") || recurringCategoryName.contains("retire") || recurringCategoryName.contains("stock") || recurringCategoryName.contains("401k") || recurringCategoryName.contains("ira")) {
                                        Icons.Default.TrendingUp
                                    } else if (selectedTargetDebtAccount?.type == AccountType.SAVINGS || recurringCategoryName.contains("saving")) {
                                        Icons.Default.Savings
                                    } else {
                                        Icons.Default.CreditCard
                                    }

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

                                    Surface(
                                        shape = ShapeCard,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        com.selfbudget.app.core.ui.components.FieldRow(
                                            icon = iconVector,
                                            label = labelText,
                                            value = targetText,
                                            isPlaceholder = selectedTargetDebtAccount == null,
                                            showChevron = true,
                                            onClick = {
                                                focusManager.clearFocus(force = true)
                                                keyboardController?.hide()
                                                showTargetDebtAccountModal = true
                                            }
                                        )
                                    }
                                }

                                // 5. Finite Lifespan Toggle
                                Surface(
                                    shape = ShapeCard,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
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

                                // 6. Archive / Pause Toggle
                                Surface(
                                    shape = ShapeCard,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    com.selfbudget.app.core.ui.components.ToggleRow(
                                        icon = Icons.Default.Archive,
                                        title = "Archived / paused",
                                        description = "Stops reminders and excludes it from budget totals",
                                        checked = isArchived,
                                        onCheckedChange = { isArchived = it }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // 7. Action Buttons (spec §14: Cancel + Save pair, destructive isolated below)
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
                                            onClick = {
                                                editBaseline?.let { baseline ->
                                                    title = baseline.title
                                                    amountText = baseline.amountText
                                                    selectedFrequency = baseline.frequency
                                                    selectedCategory = categoryMap[baseline.categoryId]
                                                    isArchived = baseline.isArchived
                                                    selectedNextDueDate = baseline.nextDueDate
                                                    hasLimitedOccurrences = baseline.hasLimitedOccurrences
                                                    occurrencesText = baseline.occurrencesText
                                                    selectedTargetDebtAccount = accounts.firstOrNull { it.id == baseline.transferAccountId }
                                                }
                                                isEditMode = false
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                        )

                                        com.selfbudget.app.core.ui.components.PrimaryPillButton(
                                            text = "Save changes",
                                            onClick = { save() },
                                            enabled = isDirty && isValid,
                                            ramp = com.selfbudget.app.ui.theme.Ramp.Teal,
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .height(54.dp)
                                        )
                                    }

                                    com.selfbudget.app.core.ui.components.DestructivePillButton(
                                        text = "Delete recurring item",
                                        onClick = {
                                            pendingDeleteItem = item
                                            selectedRecurringForDetails = null
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                    )
                                }

                            } // end isEditMode form fields

                                Spacer(modifier = Modifier.height(150.dp))
                            }
                        }
                    }
                }
            }

            if (showNewCategoryDialog) {
                AddCustomCategoryDialog(
                    initialType = item.type,
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
                    transactionType = item.type,
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

            if (showDatePickerModal) {
                val datePickerState = androidx.compose.material3.rememberDatePickerState(
                    initialSelectedDateMillis = com.selfbudget.app.core.util.DateUtils.localDateToUtcMillis(selectedNextDueDate)
                )

                androidx.compose.material3.DatePickerDialog(
                    onDismissRequest = { showDatePickerModal = false },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    selectedNextDueDate = com.selfbudget.app.core.util.DateUtils.utcMillisToLocalDate(millis, selectedNextDueDate)
                                }
                                showDatePickerModal = false
                            }
                        ) {
                            Text("OK", fontWeight = FontWeight.Medium)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { showDatePickerModal = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                ) {
                    androidx.compose.material3.DatePicker(state = datePickerState)
                }
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
        }

        // Post confirmation banner (spec §9-style status surface, not a native Toast).
        AnimatedVisibility(
            visible = postedBannerMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            val isDarkBanner = com.selfbudget.app.ui.theme.isAppInDarkTheme()
            Surface(
                shape = com.selfbudget.app.ui.theme.ShapePill,
                color = com.selfbudget.app.ui.theme.Ramp.Teal.solidFill(isDarkBanner),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = com.selfbudget.app.ui.theme.Ramp.Teal.onSolidFill(isDarkBanner),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = postedBannerMessage.orEmpty(),
                        style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                        color = com.selfbudget.app.ui.theme.Ramp.Teal.onSolidFill(isDarkBanner)
                    )
                }
            }
        }

        if (showRecurringIncomeModal) {
            RecurringIncomeModal(
                recurringList = recurringList,
                categories = categories,
                accounts = accounts,
                currencySymbol = currencySymbol,
                onDismiss = { showRecurringIncomeModal = false }
            )
        }

        if (showRecurringBillsModal) {
            RecurringBillsModal(
                recurringList = recurringList,
                categories = categories,
                accounts = accounts,
                currencySymbol = currencySymbol,
                onDismiss = { showRecurringBillsModal = false }
            )
        }
    }
}

// Full-screen "review before you post" step, same modal pattern as the rest of the app
// (persistent top bar with Close, scrollable content, sticky bottom action). Exists because
// posting used to silently reuse whatever account the recurring item was created with - usually
// posting used to silently reuse whatever account the recurring item was created with - usually
@Composable
private fun PostRecurringConfirmModal(
    item: RecurringTransactionEntity,
    cycleSummary: RecurringCyclePaymentSummary?,
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double>,
    goals: List<GoalEntity> = emptyList(),
    categoryName: String?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (accountId: String, amount: Double, goalId: String?) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddCustomAccount: ((AccountEntity) -> Unit)? = null
) {
    val isIncome = item.type == TransactionType.INCOME
    val heroRamp = if (isIncome) com.selfbudget.app.ui.theme.Ramp.Teal else com.selfbudget.app.ui.theme.Ramp.Red
    var selectedAccount by remember(item.id) {
        mutableStateOf(accounts.firstOrNull { it.id == item.accountId } ?: accounts.firstOrNull())
    }
    var selectedGoal by remember(item.id) {
        mutableStateOf<GoalEntity?>(
            goals.firstOrNull { it.linkedAccountId != null && it.linkedAccountId == item.transferAccountId }
        )
    }
    var pickingAccount by remember { mutableStateOf(false) }
    var pickingGoal by remember { mutableStateOf(false) }
    var showNewAccountDialog by remember { mutableStateOf(false) }

    val defaultAmount = if (cycleSummary != null && cycleSummary.isPartiallyPaid && cycleSummary.remainingAmount > 0.005) {
        cycleSummary.remainingAmount
    } else {
        item.amount
    }
    var amountText by remember(item.id, defaultAmount) { mutableStateOf("%.2f".format(defaultAmount)) }
    val enteredAmount = amountText.toDoubleOrNull()
    val isFullScheduledAmount = enteredAmount != null && kotlin.math.abs(enteredAmount - item.amount) < 0.005
    val isPartial = enteredAmount != null && enteredAmount < (item.amount - 0.005)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularBackButton(onClick = onDismiss)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm & Post", style = com.selfbudget.app.ui.theme.SelfBudgetType.title, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val isDark = com.selfbudget.app.ui.theme.isAppInDarkTheme()

                    // Hero Amount Card — shared component (spec §16): every amount-entry
                    // card in the app uses this one implementation, not a per-screen copy.
                    com.selfbudget.app.core.ui.components.TransactionAmountHero(
                        type = if (isIncome) com.selfbudget.app.core.ui.components.EntryType.Income else com.selfbudget.app.core.ui.components.EntryType.Expense,
                        amountText = amountText,
                        onAmountChange = { amountText = it },
                        currencySymbol = currencySymbol,
                        badgeText = if (isIncome) "POST RECURRING INCOME" else "POST RECURRING EXPENSE",
                        stepAmount = 5.0
                    )

                    // Cycle status pill / banner
                    if (cycleSummary != null && cycleSummary.isPartiallyPaid) {
                        Surface(
                            shape = com.selfbudget.app.ui.theme.ShapeChip,
                            color = com.selfbudget.app.ui.theme.Ramp.Amber.tintFill(isDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isDark) Color.White else com.selfbudget.app.ui.theme.Ramp.Amber.titleText(isDark),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Paid $currencySymbol%.2f · $currencySymbol%.2f left of $currencySymbol%.2f scheduled".format(
                                        cycleSummary.totalPaid,
                                        cycleSummary.remainingAmount,
                                        item.amount
                                    ),
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                    color = if (isDark) Color.White else com.selfbudget.app.ui.theme.Ramp.Amber.titleText(isDark)
                                )
                            }
                        }
                    } else if (isPartial) {
                        Text(
                            text = "Partial payment · full scheduled amount is $currencySymbol%.2f".format(item.amount),
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { amountText = "%.2f".format(item.amount) }
                        )
                    }

                    // Quick preset chips row
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (cycleSummary != null && cycleSummary.isPartiallyPaid && cycleSummary.remainingAmount > 0.005) {
                            Surface(
                                onClick = { amountText = "%.2f".format(cycleSummary.remainingAmount) },
                                shape = com.selfbudget.app.ui.theme.ShapePill,
                                color = com.selfbudget.app.ui.theme.Ramp.Teal.tintFill(isDark)
                            ) {
                                Text(
                                    text = "Remaining: $currencySymbol%.2f".format(cycleSummary.remainingAmount),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.badge,
                                    color = com.selfbudget.app.ui.theme.Ramp.Teal.titleText(isDark)
                                )
                            }
                        }
                        if (!isFullScheduledAmount) {
                            Surface(
                                onClick = { amountText = "%.2f".format(item.amount) },
                                shape = com.selfbudget.app.ui.theme.ShapePill,
                                color = com.selfbudget.app.ui.theme.Ramp.Gray.tintFill(isDark)
                            ) {
                                Text(
                                    text = "Full: $currencySymbol%.2f".format(item.amount),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.badge,
                                    color = com.selfbudget.app.ui.theme.Ramp.Gray.titleText(isDark)
                                )
                            }
                        }
                        listOf(10.0, 25.0, 50.0, 100.0).forEach { inc ->
                            Surface(
                                onClick = {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = "%.2f".format(current + inc)
                                },
                                shape = com.selfbudget.app.ui.theme.ShapePill,
                                color = com.selfbudget.app.ui.theme.Ramp.Gray.tintFill(isDark)
                            ) {
                                Text(
                                    text = "+$currencySymbol${inc.toInt()}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = com.selfbudget.app.ui.theme.SelfBudgetType.badge,
                                    color = com.selfbudget.app.ui.theme.Ramp.Gray.titleText(isDark)
                                )
                            }
                        }
                    }

                    // Grouped Details Card
                    Surface(
                        shape = ShapeCard,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            RecurringViewModeInfoItem(
                                icon = Icons.Default.Info,
                                label = if (isIncome) "Income Title" else "Bill Title",
                                value = item.title
                            )
                            com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                            RecurringViewModeInfoItem(
                                icon = Icons.Default.Repeat,
                                label = "Repeat Frequency",
                                value = item.frequency.name.lowercase().replace('_', '-').replaceFirstChar { it.uppercase() }
                            )
                            com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                            RecurringViewModeInfoItem(
                                icon = Icons.Default.Category,
                                label = "Category",
                                value = categoryName ?: "—"
                            )

                            if (item.transferAccountId != null) {
                                val targetAcc = accounts.firstOrNull { it.id == item.transferAccountId }
                                val targetAccName = targetAcc?.name ?: "Linked account"
                                val isLiability = targetAcc?.let { com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(it.type) } ?: true
                                com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                                RecurringViewModeInfoItem(
                                    icon = if (isLiability) Icons.Default.CreditCard else Icons.Default.TrendingUp,
                                    label = if (isLiability) "Pays Down Debt" else "Deposits To Account",
                                    value = targetAccName,
                                    valueColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Payment Account Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isIncome) "DEPOSIT TO" else "PAY FROM",
                            style = com.selfbudget.app.ui.theme.SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pickingAccount = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    com.selfbudget.app.core.ui.components.GrayIconTile(icon = Icons.Default.AccountBalance, size = 36.dp, iconSize = 18.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = selectedAccount?.name ?: "Select an account",
                                            style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        selectedAccount?.let { acc ->
                                            val rawBal = accountBalances[acc.id] ?: acc.initialBalance
                                            val isLiab = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(acc.type)
                                            val dispBal = if (isLiab) kotlin.math.abs(rawBal) else rawBal
                                            val accSym = com.selfbudget.app.core.util.Currencies.symbolFor(acc.currencyCode).ifBlank { currencySymbol }
                                            Text(
                                                text = "Balance: $accSym%.2f".format(dispBal),
                                                style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Select account",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    // Savings Goal Picker (Optional)
                    if (goals.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CREDIT SAVINGS GOAL (OPTIONAL)",
                                style = com.selfbudget.app.ui.theme.SelfBudgetType.eyebrow,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pickingGoal = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        com.selfbudget.app.core.ui.components.GrayIconTile(
                                            icon = Icons.Default.Savings,
                                            size = 36.dp,
                                            iconSize = 18.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = selectedGoal?.name ?: "None (Standard recurring payment)",
                                                style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = selectedGoal?.let { "Contributes $currencySymbol${amountText.ifBlank { "0.00" }} toward goal" }
                                                    ?: "Tap to allocate to a goal",
                                                style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Select goal",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons (spec §14: Cancel + Confirm pair, destructive isolated below)
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
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            )

                            com.selfbudget.app.core.ui.components.PrimaryPillButton(
                                text = "Confirm & post",
                                onClick = { selectedAccount?.let { acc -> enteredAmount?.let { onConfirm(acc.id, it, selectedGoal?.id) } } },
                                enabled = selectedAccount != null && enteredAmount != null && enteredAmount > 0.0,
                                ramp = com.selfbudget.app.ui.theme.Ramp.Teal,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(54.dp)
                            )
                        }

                        if (onDelete != null) {
                            com.selfbudget.app.core.ui.components.DestructivePillButton(
                                text = "Delete recurring item",
                                onClick = onDelete,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            )
                        }
                    }

                    // Standardized 150.dp bottom scroll spacing for effortless scrolling
                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }
    }

    if (pickingGoal) {
        GoalSelectionModal(
            goals = goals,
            selectedGoalId = selectedGoal?.id,
            accounts = accounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            allowNone = true,
            onDismiss = { pickingGoal = false },
            onSelectGoal = { goal ->
                selectedGoal = goal
                pickingGoal = false
            }
        )
    }

    if (pickingAccount) {
        AccountSelectionModal(
            accounts = accounts,
            selectedAccount = selectedAccount,
            currencySymbol = currencySymbol,
            accountBalances = accountBalances,
            onDismiss = { pickingAccount = false },
            onSelectAccount = { acc ->
                selectedAccount = acc
                pickingAccount = false
            },
            onAddCustomAccount = {
                pickingAccount = false
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
                selectedAccount = newAcc
                showNewAccountDialog = false
            }
        )
    }
}

@Composable
private fun RecurringViewModeSummary(
    isIncome: Boolean,
    themeColor: Color,
    currencySymbol: String,
    amountText: String,
    title: String,
    frequency: RecurringFrequency,
    categoryName: String?,
    nextDueDate: Long,
    dateFormatter: SimpleDateFormat,
    hasLimitedOccurrences: Boolean,
    occurrencesText: String,
    isArchived: Boolean,
    isJustPosted: Boolean = false,
    cycleSummary: RecurringCyclePaymentSummary? = null,
    transferAccountName: String? = null,
    onPostNow: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClose: () -> Unit
) {
    val isDark = com.selfbudget.app.ui.theme.isAppInDarkTheme()
    val heroRamp = if (isIncome) com.selfbudget.app.ui.theme.Ramp.Teal else com.selfbudget.app.ui.theme.Ramp.Red

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Amount Card — neutral display number, the type badge carries the color (spec §14)
        Surface(
            shape = ShapeHero,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = com.selfbudget.app.ui.theme.ShapePill,
                    color = heroRamp.tintFill(isDark)
                ) {
                    Text(
                        text = if (isIncome) "RECURRING INCOME" else "RECURRING EXPENSE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = com.selfbudget.app.ui.theme.SelfBudgetType.eyebrow,
                        color = heroRamp.titleText(isDark)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${if (isIncome) "+" else "-"}$currencySymbol${amountText.ifBlank { "0.00" }}",
                    style = com.selfbudget.app.ui.theme.SelfBudgetType.display,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Post Action Card — explainer-card pattern (spec §21): icon tile + headline + body, tinted by state
        val postRamp = when {
            isArchived -> com.selfbudget.app.ui.theme.Ramp.Gray
            isJustPosted -> com.selfbudget.app.ui.theme.Ramp.Teal
            cycleSummary != null && cycleSummary.isPartiallyPaid -> com.selfbudget.app.ui.theme.Ramp.Amber
            else -> com.selfbudget.app.ui.theme.Ramp.Teal
        }
        Surface(
            shape = ShapeCard,
            color = postRamp.tintFill(isDark),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isArchived) { onPostNow() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.selfbudget.app.core.ui.components.RampIconTile(
                    icon = if (isArchived) Icons.Default.Archive else if (isJustPosted) Icons.Default.Verified else Icons.Default.Publish,
                    ramp = postRamp,
                    size = 36.dp,
                    iconSize = 20.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArchived) {
                            "Bill Archived"
                        } else if (isJustPosted) {
                            "Posted for Current Cycle"
                        } else if (cycleSummary != null && cycleSummary.isPartiallyPaid) {
                            "Partially Paid ($currencySymbol%.2f remaining)".format(cycleSummary.remainingAmount)
                        } else {
                            "Post Now"
                        },
                        style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
                        color = if (isDark) Color.White else postRamp.titleText(isDark)
                    )
                    Text(
                        text = if (isArchived) {
                            "Posting is disabled while archived."
                        } else if (isJustPosted) {
                            "Transaction logged for this cycle. Tap to post again."
                        } else if (cycleSummary != null && cycleSummary.isPartiallyPaid) {
                            "Paid $currencySymbol%.2f of scheduled total. Tap to post remaining balance.".format(cycleSummary.totalPaid)
                        } else {
                            "Tap to post current cycle transaction immediately."
                        },
                        style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                        color = if (isDark) postRamp.c100 else postRamp.secondaryText(isDark)
                    )
                }

                if (!isArchived) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = if (isDark) postRamp.c100 else postRamp.secondaryText(isDark),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Grouped Details Card
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Recurring details",
                style = com.selfbudget.app.ui.theme.SelfBudgetType.section,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    RecurringViewModeInfoItem(
                        icon = Icons.Default.Info,
                        label = if (isIncome) "Income Title" else "Bill Title",
                        value = title
                    )
                    com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                    RecurringViewModeInfoItem(
                        icon = Icons.Default.Repeat,
                        label = "Repeat Frequency",
                        value = frequency.name.lowercase().replace('_', '-').replaceFirstChar { it.uppercase() }
                    )
                    com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                    RecurringViewModeInfoItem(
                        icon = Icons.Default.Category,
                        label = "Category",
                        value = categoryName ?: "—"
                    )
                    com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                    RecurringViewModeInfoItem(
                        icon = Icons.Default.CalendarToday,
                        label = "Start / Next Due Date",
                        value = dateFormatter.format(Date(nextDueDate))
                    )

                    if (transferAccountName != null) {
                        com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                        RecurringViewModeInfoItem(
                            icon = Icons.Default.CreditCard,
                            label = "Pays Down Debt",
                            value = transferAccountName,
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (hasLimitedOccurrences) {
                        com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                        RecurringViewModeInfoItem(
                            icon = Icons.Default.Schedule,
                            label = "Payments Remaining",
                            value = occurrencesText.ifBlank { "—" }
                        )
                    }

                    if (isArchived) {
                        com.selfbudget.app.core.ui.components.SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                        RecurringViewModeInfoItem(
                            icon = Icons.Default.Archive,
                            label = "Status",
                            value = "Archived / Paused"
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
                com.selfbudget.app.core.ui.components.SecondaryPillButton(
                    text = "Close",
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                )

                com.selfbudget.app.core.ui.components.PrimaryPillButton(
                    text = "Edit Recurring",
                    onClick = onEditClick,
                    ramp = com.selfbudget.app.ui.theme.Ramp.Teal,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(54.dp)
                )
            }

            com.selfbudget.app.core.ui.components.DestructivePillButton(
                text = "Delete Recurring Item",
                onClick = onDeleteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )
        }
    }
}

/** Detail-row icon tiles are Gray (spec §14): they label a field TYPE, not a category. */
@Composable
private fun RecurringViewModeInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
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
            com.selfbudget.app.core.ui.components.GrayIconTile(icon = icon, size = 36.dp, iconSize = 18.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = com.selfbudget.app.ui.theme.SelfBudgetType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = com.selfbudget.app.ui.theme.SelfBudgetType.rowTitle,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}

