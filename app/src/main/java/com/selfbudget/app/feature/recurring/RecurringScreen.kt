package com.selfbudget.app.feature.recurring

import android.widget.Toast
import com.selfbudget.app.core.ui.getCategoryIcon
import com.selfbudget.app.core.ui.MonthYearHeader
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.selfbudget.app.core.ui.AddCustomCategoryDialog
import com.selfbudget.app.core.ui.CategorySelectionModal
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
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
    onAddRecurring: (title: String, amount: Double, type: TransactionType, categoryId: String, frequency: RecurringFrequency, remainingOccurrences: Int?, nextDueDate: Long?) -> Unit,
    onDeleteRecurring: (RecurringTransactionEntity) -> Unit,
    onPostTransaction: (RecurringTransactionEntity) -> Unit,
    onUpdateRecurring: (RecurringTransactionEntity) -> Unit = {},
    onAddCustomCategory: ((CategoryEntity) -> Unit)? = null,
    // Lets the single global "+" (owned by HomeScreen) open this screen's "new recurring" dialog
    // from anywhere in the app, instead of this screen needing its own floating add button.
    requestNewRecurring: Boolean = false,
    onNewRecurringRequestHandled: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilterType by remember { mutableStateOf<TransactionType?>(null) }
    var selectedRecurringForDetails by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var pendingDuplicateItem by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var duplicateMatchDate by remember { mutableStateOf<String?>(null) }
    var recentlyPostedId by remember { mutableStateOf<String?>(null) }
    val monthNameFormatter = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(requestNewRecurring) {
        if (requestNewRecurring) {
            showAddDialog = true
            onNewRecurringRequestHandled()
        }
    }

    val executePost: (RecurringTransactionEntity) -> Unit = { itemToPost ->
        onPostTransaction(itemToPost)
        Toast.makeText(
            context,
            "Posted \"${itemToPost.title}\" ($currencySymbol%.2f)!".format(itemToPost.amount),
            Toast.LENGTH_SHORT
        ).show()
        recentlyPostedId = itemToPost.id
        coroutineScope.launch {
            delay(2000L)
            if (recentlyPostedId == itemToPost.id) {
                recentlyPostedId = null
            }
        }
    }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Archived/finished items no longer count toward totals - they're done, not active.
    val activeList = remember(recurringList) { recurringList.filter { !it.isArchived } }
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

    val filteredList = remember(recurringList, selectedFilterType) {
        if (selectedFilterType == null) recurringList
        else recurringList.filter { it.type == selectedFilterType }
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
            if (onPreviousMonth != null && onNextMonth != null && onSelectMonthYear != null) {
                MonthYearHeader(
                    currentMonthYear = selectedMonthYear,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectMonthYear = onSelectMonthYear
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Recurring Overview Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly Commitments ($currentMonthName)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Income Summary Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = IncomeGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Paychecks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+$currencySymbol%.2f".format(totalRecurringIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = IncomeGreen
                                )
                            }
                        }

                        // Expense Summary Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = ExpenseRed.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Bills & Subscriptions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    // Shown as a plain positive figure, not "-$X" - this is a
                                    // planned monthly bill total, not money already lost.
                                    text = "$currencySymbol%.2f".format(totalRecurringExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips (All vs Expenses vs Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilterType == null,
                    onClick = { selectedFilterType = null },
                    label = { Text("All (${recurringList.size})") }
                )
                FilterChip(
                    selected = selectedFilterType == TransactionType.EXPENSE,
                    onClick = { selectedFilterType = TransactionType.EXPENSE },
                    label = { Text("Bills 🔴") }
                )
                FilterChip(
                    selected = selectedFilterType == TransactionType.INCOME,
                    onClick = { selectedFilterType = TransactionType.INCOME },
                    label = { Text("Paychecks 🟢") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "No Monthly Commitments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Track your recurring paychecks, rent, subscriptions, and utilities with automatic next-due reminders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Add Recurring Bill / Salary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredList.forEach { item ->
                        val category = categoryMap[item.categoryId]
                        val isIncome = item.type == TransactionType.INCOME
                        val freqText = when (item.frequency) {
                            RecurringFrequency.WEEKLY -> "Weekly 📅"
                            RecurringFrequency.BI_WEEKLY -> "Bi-Weekly 🗓️"
                            RecurringFrequency.MONTHLY -> "Monthly 🗓️"
                            RecurringFrequency.YEARLY -> "Annual 🎆"
                        }
                        val postedTransactionForThisCycle = remember(item, allTransactions, selectedMonthYear) {
                            getPostedTransactionForCurrentCycle(item, allTransactions, selectedMonthYear)
                        }
                        val isPostedThisCycle = postedTransactionForThisCycle != null

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRecurringForDetails = item },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isArchived) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (item.isArchived) 0.dp else 2.dp
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (item.isArchived) {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (item.isArchived) {
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                                    } else if (isIncome) {
                                                        IncomeGreen
                                                    } else {
                                                        ExpenseRed
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = if (item.isArchived) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (item.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = category?.name ?: "General",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        color = if (item.isArchived) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "•  $freqText",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (item.isArchived) 0.6f else 1f)
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isIncome) "+" else "-"}$currencySymbol%.2f".format(item.amount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = if (item.isArchived) {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            } else if (isIncome) {
                                                IncomeGreen
                                            } else {
                                                ExpenseRed
                                            }
                                        )

                                        if (item.isArchived) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                            ) {
                                                Text(
                                                    text = if (item.remainingOccurrences == 0) "Completed ✅" else "Archived 📦",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (isPostedThisCycle) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = IncomeGreen.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Posted for ${monthNameFormatter.format(postedTransactionForThisCycle?.timestamp?.let { Date(it) } ?: Date())} ✅",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IncomeGreen,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Next: ${dateFormatter.format(Date(item.nextDueDate))}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val isPosted = recentlyPostedId == item.id || isPostedThisCycle
                                    val isArchived = item.isArchived
                                    Button(
                                        onClick = {
                                            if (!isPosted) {
                                                executePost(item)
                                            } else {
                                                duplicateMatchDate = postedTransactionForThisCycle?.timestamp?.let { dateFormatter.format(Date(it)) } ?: dateFormatter.format(Date())
                                                pendingDuplicateItem = item
                                            }
                                        },
                                        enabled = !isArchived,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else if (isPosted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f) else if (isIncome) IncomeGreen else MaterialTheme.colorScheme.primary,
                                            contentColor = if (isArchived) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else if (isPosted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else Color.White,
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isArchived) Icons.Default.Archive else if (isPosted) Icons.Default.Verified else Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isArchived) "Archived" else if (isPosted) "Posted" else "Post Now",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                item.remainingOccurrences?.let { remaining ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (remaining == 1) "Last payment remaining" else "$remaining payments remaining",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(200.dp))
        }

        if (showAddDialog) {
            SetRecurringDialog(
                categories = categories,
                currencySymbol = currencySymbol,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, amount, type, categoryId, frequency, remainingOccurrences, nextDueDate ->
                    onAddRecurring(title, amount, type, categoryId, frequency, remainingOccurrences, nextDueDate)
                    showAddDialog = false
                },
                onAddCustomCategory = onAddCustomCategory
            )
        }

        // Potential Duplicate Entry Warning Modal (Custom Styled M3 Dialog)
        if (pendingDuplicateItem != null) {
            val itemToPost = pendingDuplicateItem!!
            val isIncome = itemToPost.type == TransactionType.INCOME

            Dialog(
                onDismissRequest = { pendingDuplicateItem = null },
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
                        // Glowing Warning Icon Badge
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Potential Duplicate Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "An entry matching this recurring transaction was already logged recently.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Highlighted Transaction Details Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${if (isIncome) "+" else "-"}$currencySymbol%.2f".format(itemToPost.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncome) IncomeGreen else ExpenseRed
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Previously Logged:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = duplicateMatchDate ?: "Today",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pendingDuplicateItem = null },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    executePost(itemToPost)
                                    pendingDuplicateItem = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Post Anyway", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Delete Recurring Confirmation Modal
        if (pendingDeleteItem != null) {
            val itemToDelete = pendingDeleteItem!!
            val isIncome = itemToDelete.type == TransactionType.INCOME

            Dialog(
                onDismissRequest = { pendingDeleteItem = null },
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
                            text = "Delete Recurring Entry?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Are you sure you want to remove this recurring commitment? This cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Highlighted Item Card
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
                                    text = itemToDelete.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${if (isIncome) "+" else "-"}$currencySymbol%.2f".format(itemToDelete.amount),
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
                                onClick = { pendingDeleteItem = null },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onDeleteRecurring(itemToDelete)
                                    pendingDeleteItem = null
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

        // Edit Recurring - same full-screen modal pattern as the rest of the app (persistent top
        // bar with Close + inline Save), instead of the old compact read-only details dialog.
        // Post Now and Delete act on the last-saved item and live in the sticky bottom bar; field
        // edits (title, amount, frequency, category, end date, archive) are only committed by Save.
        selectedRecurringForDetails?.let { item ->
            val isIncome = item.type == TransactionType.INCOME
            val postedTransactionForThisCycle = remember(item, allTransactions, selectedMonthYear) {
                getPostedTransactionForCurrentCycle(item, allTransactions, selectedMonthYear)
            }
            val isPostedThisCycle = postedTransactionForThisCycle != null
            val isJustPosted = recentlyPostedId == item.id || isPostedThisCycle

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

            val amount = amountText.toDoubleOrNull() ?: 0.0
            val isValid = title.isNotBlank() && amount > 0.0

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
                        isArchived = isArchived
                    )
                )
                selectedRecurringForDetails = null
            }

            fun postNow() {
                if (isArchived) return
                selectedRecurringForDetails = null
                if (!isJustPosted) {
                    executePost(item)
                } else {
                    duplicateMatchDate = postedTransactionForThisCycle?.timestamp?.let { dateFormatter.format(Date(it)) } ?: dateFormatter.format(Date())
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
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { selectedRecurringForDetails = null }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Edit Recurring",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { save() },
                                    enabled = isValid,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        // Scrollable Form Content with clean radial glow on off-white background
                        val isDark = isSystemInDarkTheme()
                        val glowColorCenter = if (isIncome) IncomeGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        val glowColorMid = if (isIncome) IncomeGreen.copy(alpha = 0.05f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
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
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                val themeColor = if (isIncome) IncomeGreen else ExpenseRed

                            // 1. Top Amount Entry Stepper (- $0.00 +) in 44.sp ExtraBold font
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isIncome) "RECURRING INCOME AMOUNT ($currencySymbol)" else "RECURRING BILL AMOUNT ($currencySymbol)",
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
                                    val presets = listOf(50, 100, 250, 500, 1000)
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

                            // 2. Verified Status / Post Action Card (Elevated for Light Mode Contrast)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isArchived) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                } else if (isJustPosted) {
                                    if (isDark) IncomeGreen.copy(alpha = 0.15f) else Color(0xFFE8F5E9)
                                } else {
                                    if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isArchived) {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    } else if (isJustPosted) {
                                        if (isDark) IncomeGreen.copy(alpha = 0.4f) else IncomeGreen.copy(alpha = 0.6f)
                                    } else {
                                        if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    }
                                ),
                                shadowElevation = if (!isDark && !isArchived) 2.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isArchived) { postNow() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else if (isJustPosted) IncomeGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isArchived) Icons.Default.Archive else if (isJustPosted) Icons.Default.Verified else Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (isArchived) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else if (isJustPosted) IncomeGreen else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isArchived) "Bill Archived" else if (isJustPosted) "Posted for ${monthNameFormatter.format(postedTransactionForThisCycle?.timestamp?.let { Date(it) } ?: Date())}" else "Mark Done / Post Now",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isArchived) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else if (isJustPosted) IncomeGreen else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isJustPosted && !isArchived) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified Tick Mark",
                                                    tint = IncomeGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isArchived) "Posting is disabled while archived. Unarchive to post." else if (isJustPosted) "Transaction logged for this cycle. Tap to post again." else "Tap to log current cycle transaction immediately.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // 3. Title Field
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text(if (isIncome) "Income Title" else "Bill Title") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Repeat Frequency
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Repeat Frequency",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    val frequencies = listOf(
                                        RecurringFrequency.WEEKLY to "Weekly",
                                        RecurringFrequency.BI_WEEKLY to "Bi-Weekly",
                                        RecurringFrequency.MONTHLY to "Monthly",
                                        RecurringFrequency.YEARLY to "Yearly"
                                    )
                                    frequencies.forEach { (freq, label) ->
                                        FilterChip(
                                            selected = selectedFrequency == freq,
                                            onClick = { selectedFrequency = freq },
                                            label = { Text(label, fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
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
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { expandedCategory = true }
                                )
                            }

                            // Start / Next Due Date
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = dateFormatter.format(Date(selectedNextDueDate)),
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    label = { Text("Start / Next Due Date") },
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showDatePickerModal = true }
                                )
                            }

                            // Finite Lifespan Toggle
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("This Has an End Date", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "e.g. a car loan or installment plan.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                        )
                                    }
                                    Switch(checked = hasLimitedOccurrences, onCheckedChange = { hasLimitedOccurrences = it })
                                }
                                if (hasLimitedOccurrences) {
                                    OutlinedTextField(
                                        value = occurrencesText,
                                        onValueChange = { input -> occurrencesText = input.filter { ch -> ch.isDigit() } },
                                        label = { Text("Payments Remaining") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Archive / Pause Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Archived / Paused", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "Stops reminders and excludes it from budget totals.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                }
                                Switch(checked = isArchived, onCheckedChange = { isArchived = it })
                            }

                            Spacer(modifier = Modifier.height(48.dp))
                        }
                        }

                        // Sticky Bottom Action Bar
                        Surface(
                            shadowElevation = 12.dp,
                            tonalElevation = 6.dp,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding()
                                .navigationBarsPadding()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        pendingDeleteItem = item
                                        selectedRecurringForDetails = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, ExpenseRed.copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                                ) {
                                    Text("Delete Recurring Item", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (showNewCategoryDialog) {
                AddCustomCategoryDialog(
                    initialType = item.type,
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
                    }
                )
            }

            if (showDatePickerModal) {
                val datePickerState = androidx.compose.material3.rememberDatePickerState(
                    initialSelectedDateMillis = selectedNextDueDate
                )

                androidx.compose.material3.DatePickerDialog(
                    onDismissRequest = { showDatePickerModal = false },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    selectedNextDueDate = millis
                                }
                                showDatePickerModal = false
                            }
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold)
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
        }
    }
}

private fun getPostedTransactionForCurrentCycle(
    item: RecurringTransactionEntity,
    allTransactions: List<TransactionEntity>,
    selectedMonthYear: String
): TransactionEntity? {
    val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
    val selectedCal = java.util.Calendar.getInstance()
    try {
        val date = sdf.parse(selectedMonthYear)
        if (date != null) {
            selectedCal.time = date
        }
    } catch (e: Exception) {
        // fallback
    }

    val currentYear = selectedCal.get(java.util.Calendar.YEAR)
    val currentMonth = selectedCal.get(java.util.Calendar.MONTH)

    val now = System.currentTimeMillis()
    val isCurrentMonthSelected = sdf.format(java.util.Date()) == selectedMonthYear
    val nowForWeekly = if (isCurrentMonthSelected) now else {
        val tempCal = selectedCal.clone() as java.util.Calendar
        tempCal.set(java.util.Calendar.DAY_OF_MONTH, tempCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        tempCal.timeInMillis
    }

    return allTransactions.firstOrNull { tx ->
        tx.type == item.type &&
        (tx.title.trim().equals(item.title.trim(), ignoreCase = true) || (tx.categoryId == item.categoryId && Math.abs(tx.amount - item.amount) < 0.01)) &&
        when (item.frequency) {
            RecurringFrequency.WEEKLY -> tx.timestamp >= nowForWeekly - (7 * 24 * 60 * 60 * 1000L) && tx.timestamp <= nowForWeekly
            RecurringFrequency.BI_WEEKLY -> tx.timestamp >= nowForWeekly - (14 * 24 * 60 * 60 * 1000L) && tx.timestamp <= nowForWeekly
            RecurringFrequency.MONTHLY -> {
                val txCal = java.util.Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                txCal.get(java.util.Calendar.YEAR) == currentYear && txCal.get(java.util.Calendar.MONTH) == currentMonth
            }
            RecurringFrequency.YEARLY -> {
                val txCal = java.util.Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                txCal.get(java.util.Calendar.YEAR) == currentYear
            }
        }
    }
}

