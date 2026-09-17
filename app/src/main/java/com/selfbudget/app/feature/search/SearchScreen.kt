package com.selfbudget.app.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.selfbudget.app.core.ui.getCategoryIcon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.DestructivePillButton
import com.selfbudget.app.core.ui.components.GrayIconTile
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.getExpenseCategoryGroup
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.ActivityAction
import com.selfbudget.app.data.model.ActivityEntityType
import com.selfbudget.app.data.model.ActivityLogEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.CardSurfaceDark
import com.selfbudget.app.ui.theme.DividerDark
import com.selfbudget.app.ui.theme.PageBackgroundDark
import com.selfbudget.app.ui.theme.PageBackgroundLight
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.TextPrimaryDark
import com.selfbudget.app.ui.theme.TextSecondaryDark
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.sectionRamp
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateRangeFilter(val label: String) {
    ALL("All time"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    LAST_30_DAYS("Last 30 days"),
    THIS_YEAR("This year")
}

enum class SortOption(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    HIGHEST_AMOUNT("Highest amount"),
    LOWEST_AMOUNT("Lowest amount")
}

private fun matchesDateRange(timestamp: Long, filter: DateRangeFilter): Boolean {
    if (filter == DateRangeFilter.ALL) return true
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()
    return when (filter) {
        DateRangeFilter.ALL -> true
        DateRangeFilter.THIS_MONTH -> cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        DateRangeFilter.LAST_MONTH -> {
            val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            cal.get(Calendar.YEAR) == lastMonthCal.get(Calendar.YEAR) && cal.get(Calendar.MONTH) == lastMonthCal.get(Calendar.MONTH)
        }
        DateRangeFilter.LAST_30_DAYS -> timestamp >= System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        DateRangeFilter.THIS_YEAR -> cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    }
}

private fun activityLogLabel(entry: ActivityLogEntity): String {
    val subject = when (entry.entityType) {
        ActivityEntityType.TRANSACTION -> "Transaction"
        ActivityEntityType.GOAL -> "Goal"
        ActivityEntityType.RECURRING -> "Recurring"
        ActivityEntityType.ACCOUNT -> "Account"
        ActivityEntityType.CATEGORY -> "Category"
    }
    return when (entry.action) {
        ActivityAction.EDITED -> "$subject edited"
        ActivityAction.DELETED -> "$subject deleted"
        ActivityAction.ARCHIVED -> "$subject archived"
        ActivityAction.RESTORED -> "$subject restored"
        ActivityAction.CONTRIBUTED -> "Contributed to goal"
    }
}

private fun activityLogIcon(action: ActivityAction): ImageVector = when (action) {
    ActivityAction.DELETED -> Icons.Default.Delete
    ActivityAction.EDITED -> Icons.Default.Edit
    ActivityAction.ARCHIVED -> Icons.Default.Archive
    ActivityAction.RESTORED -> Icons.Default.Unarchive
    ActivityAction.CONTRIBUTED -> Icons.Default.Savings
}

private fun activityLogRamp(action: ActivityAction): Ramp = when (action) {
    ActivityAction.DELETED -> Ramp.Red
    ActivityAction.CONTRIBUTED, ActivityAction.RESTORED -> Ramp.Teal
    ActivityAction.EDITED, ActivityAction.ARCHIVED -> Ramp.Gray
}

/** A row in the activity feed: a transaction, a goal/recurring/account/category creation event,
 *  or a logged edit/delete/archive/contribution against one of those entities. */
private sealed class ActivityEntry {
    abstract val id: String
    abstract val timestamp: Long
    abstract val sortAmount: Double

    data class Tx(val transaction: TransactionEntity) : ActivityEntry() {
        override val id: String get() = "tx_${transaction.id}"
        override val timestamp get() = transaction.timestamp
        override val sortAmount get() = transaction.amount
    }

    data class GoalCreated(val goal: GoalEntity) : ActivityEntry() {
        override val id: String get() = "goal_${goal.id}"
        override val timestamp get() = goal.createdAt
        override val sortAmount get() = goal.targetAmount
    }

    data class RecurringAdded(val recurring: RecurringTransactionEntity) : ActivityEntry() {
        override val id: String get() = "rec_${recurring.id}"
        override val timestamp get() = recurring.createdAt
        override val sortAmount get() = recurring.amount
    }

    data class AccountAdded(val account: AccountEntity) : ActivityEntry() {
        override val id: String get() = "acc_${account.id}"
        override val timestamp get() = account.createdAt
        override val sortAmount get() = account.initialBalance
    }

    data class CategoryAdded(val category: CategoryEntity) : ActivityEntry() {
        override val id: String get() = "cat_${category.id}"
        override val timestamp get() = category.createdAt
        override val sortAmount get() = 0.0
    }

    data class LogEvent(val entry: ActivityLogEntity) : ActivityEntry() {
        override val id: String get() = "log_${entry.id}"
        override val timestamp get() = entry.timestamp
        override val sortAmount get() = entry.amount ?: 0.0
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    goals: List<GoalEntity> = emptyList(),
    recurringTransactions: List<RecurringTransactionEntity> = emptyList(),
    activityLog: List<ActivityLogEntity> = emptyList(),
    currencySymbol: String = "$",
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: ((TransactionEntity) -> Unit)? = null,
    onAddClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) } // null = All
    var selectedCategoryId by remember { mutableStateOf<String?>(null) } // null = All Categories
    var selectedAccountId by remember { mutableStateOf<String?>(null) } // null = All Accounts
    var selectedDateRange by remember { mutableStateOf(DateRangeFilter.ALL) }
    var selectedSortOption by remember { mutableStateOf(SortOption.NEWEST) }

    var showFilterModal by remember { mutableStateOf(false) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }

    val activeFilterCount = remember(
        selectedTypeFilter,
        selectedCategoryId,
        selectedAccountId,
        selectedDateRange,
        selectedSortOption
    ) {
        var count = 0
        if (selectedTypeFilter != null) count++
        if (selectedCategoryId != null) count++
        if (selectedAccountId != null) count++
        if (selectedDateRange != DateRangeFilter.ALL) count++
        if (selectedSortOption != SortOption.NEWEST) count++
        count
    }

    val filteredTransactions = remember(
        transactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryId,
        selectedAccountId,
        selectedDateRange
    ) {
        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    (tx.note?.contains(searchQuery, ignoreCase = true) == true) ||
                    (accountMap[tx.accountId]?.name?.contains(searchQuery, ignoreCase = true) == true)

            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val matchesCategory = selectedCategoryId == null || tx.categoryId == selectedCategoryId
            val matchesAccount = selectedAccountId == null || tx.accountId == selectedAccountId || tx.transferAccountId == selectedAccountId
            val matchesDate = matchesDateRange(tx.timestamp, selectedDateRange)

            matchesQuery && matchesType && matchesCategory && matchesAccount && matchesDate
        }
    }

    // Goal-creation events only show up in the unfiltered "all types" view — a type filter
    // (expense/income/transfer) implies the user only wants transactions.
    val filteredGoals = remember(goals, searchQuery, selectedTypeFilter, selectedAccountId, selectedCategoryId, selectedDateRange) {
        if (selectedTypeFilter != null || selectedAccountId != null || selectedCategoryId != null) {
            emptyList()
        } else {
            goals.filter { goal ->
                val matchesQuery = searchQuery.isBlank() || goal.name.contains(searchQuery, ignoreCase = true)
                matchesQuery && matchesDateRange(goal.createdAt, selectedDateRange)
            }
        }
    }

    // Recurring items have a real type/category/account, so "added" events respect those
    // filters the same way ordinary transactions do. createdAt == 0 means this row predates the
    // column (backfilled by the schema migration, not a real creation time) — skip it rather than
    // show a nonsense "added Dec 31, 1969" event.
    val filteredRecurring = remember(recurringTransactions, searchQuery, selectedTypeFilter, selectedCategoryId, selectedAccountId, selectedDateRange) {
        recurringTransactions.filter { rec ->
            val matchesQuery = searchQuery.isBlank() ||
                    rec.title.contains(searchQuery, ignoreCase = true) ||
                    (rec.note?.contains(searchQuery, ignoreCase = true) == true)
            val matchesType = selectedTypeFilter == null || rec.type == selectedTypeFilter
            val matchesCategory = selectedCategoryId == null || rec.categoryId == selectedCategoryId
            val matchesAccount = selectedAccountId == null || rec.accountId == selectedAccountId || rec.transferAccountId == selectedAccountId
            rec.createdAt > 0 && matchesQuery && matchesType && matchesCategory && matchesAccount && matchesDateRange(rec.createdAt, selectedDateRange)
        }
    }

    // Account-creation events have no transaction type/category, same reasoning as goals.
    // Same createdAt == 0 backfill guard as recurring items above.
    val filteredAccounts = remember(accounts, searchQuery, selectedTypeFilter, selectedCategoryId, selectedAccountId, selectedDateRange) {
        if (selectedTypeFilter != null || selectedCategoryId != null) {
            emptyList()
        } else {
            accounts.filter { acc ->
                val matchesQuery = searchQuery.isBlank() || acc.name.contains(searchQuery, ignoreCase = true)
                val matchesAccount = selectedAccountId == null || acc.id == selectedAccountId
                acc.createdAt > 0 && matchesQuery && matchesAccount && matchesDateRange(acc.createdAt, selectedDateRange)
            }
        }
    }

    // Only user-created categories are activity-worthy — the built-in defaults are silently
    // re-seeded on every app open (see DatabaseModule.onOpen) and would otherwise flood the feed.
    // Same createdAt == 0 backfill guard as recurring items/accounts above.
    val filteredCategories = remember(categories, searchQuery, selectedTypeFilter, selectedCategoryId, selectedAccountId, selectedDateRange) {
        if (selectedAccountId != null) {
            emptyList()
        } else {
            categories.filter { cat ->
                val matchesQuery = searchQuery.isBlank() || cat.name.contains(searchQuery, ignoreCase = true)
                val matchesType = selectedTypeFilter == null || cat.type == selectedTypeFilter
                val matchesCategory = selectedCategoryId == null || cat.id == selectedCategoryId
                !cat.isDefault && cat.createdAt > 0 && matchesQuery && matchesType && matchesCategory && matchesDateRange(cat.createdAt, selectedDateRange)
            }
        }
    }

    // Log entries are just a title/timestamp/amount snapshot — no category or account link is
    // kept, so (like goals/accounts) they only surface in the unfiltered "all types" view.
    val filteredLog = remember(activityLog, searchQuery, selectedTypeFilter, selectedCategoryId, selectedAccountId, selectedDateRange) {
        if (selectedTypeFilter != null || selectedCategoryId != null || selectedAccountId != null) {
            emptyList()
        } else {
            activityLog.filter { entry ->
                val matchesQuery = searchQuery.isBlank() || entry.title.contains(searchQuery, ignoreCase = true)
                matchesQuery && matchesDateRange(entry.timestamp, selectedDateRange)
            }
        }
    }

    val sortedEntries = remember(filteredTransactions, filteredGoals, filteredRecurring, filteredAccounts, filteredCategories, filteredLog, selectedSortOption) {
        val entries: List<ActivityEntry> = filteredTransactions.map { ActivityEntry.Tx(it) } +
                filteredGoals.map { ActivityEntry.GoalCreated(it) } +
                filteredRecurring.map { ActivityEntry.RecurringAdded(it) } +
                filteredAccounts.map { ActivityEntry.AccountAdded(it) } +
                filteredCategories.map { ActivityEntry.CategoryAdded(it) } +
                filteredLog.map { ActivityEntry.LogEvent(it) }
        when (selectedSortOption) {
            SortOption.NEWEST -> entries.sortedByDescending { it.timestamp }
            SortOption.OLDEST -> entries.sortedBy { it.timestamp }
            SortOption.HIGHEST_AMOUNT -> entries.sortedByDescending { it.sortAmount }
            SortOption.LOWEST_AMOUNT -> entries.sortedBy { it.sortAmount }
        }
    }

    val totalFilteredExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val totalFilteredIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    val isDark = isAppInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input Field & Filter Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.selfbudget.app.core.ui.AppSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search activity",
                modifier = Modifier.weight(1f)
            )

            // Filter pill button (spec §5 pill component)
            Surface(
                shape = ShapePill,
                color = if (activeFilterCount > 0) Ramp.Teal.tintFill(isDark) else Ramp.Gray.tintFill(isDark),
                modifier = Modifier
                    .height(52.dp)
                    .clickable { showFilterModal = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (activeFilterCount > 0) Ramp.Teal.titleText(isDark) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (activeFilterCount > 0) "Filter ($activeFilterCount)" else "Filter",
                        style = SelfBudgetType.rowTitle,
                        color = if (activeFilterCount > 0) Ramp.Teal.titleText(isDark) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active Filter Badges Bar
        if (activeFilterCount > 0 || searchQuery.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (searchQuery.isNotBlank()) {
                    item {
                        ActiveFilterPill(label = "Search: \"$searchQuery\"", onClear = { searchQuery = "" })
                    }
                }
                if (selectedDateRange != DateRangeFilter.ALL) {
                    item {
                        ActiveFilterPill(label = selectedDateRange.label, onClear = { selectedDateRange = DateRangeFilter.ALL })
                    }
                }
                if (selectedTypeFilter != null) {
                    item {
                        val typeLabel = when (selectedTypeFilter) {
                            TransactionType.EXPENSE -> "Expenses"
                            TransactionType.INCOME -> "Income"
                            TransactionType.TRANSFER -> "Transfers"
                            else -> ""
                        }
                        ActiveFilterPill(label = typeLabel, onClear = { selectedTypeFilter = null })
                    }
                }
                if (selectedAccountId != null) {
                    item {
                        val accName = accountMap[selectedAccountId]?.name ?: "Account"
                        ActiveFilterPill(label = accName, onClear = { selectedAccountId = null })
                    }
                }
                if (selectedCategoryId != null) {
                    item {
                        val catName = categoryMap[selectedCategoryId]?.name ?: "Category"
                        ActiveFilterPill(label = catName, onClear = { selectedCategoryId = null })
                    }
                }
                if (selectedSortOption != SortOption.NEWEST) {
                    item {
                        ActiveFilterPill(label = selectedSortOption.label, onClear = { selectedSortOption = SortOption.NEWEST })
                    }
                }
                item {
                    // Quiet text action — never red, this isn't destructive (spec §24).
                    Text(
                        text = "Reset",
                        style = SelfBudgetType.body,
                        color = Ramp.Teal.secondaryText(isDark),
                        modifier = Modifier
                            .clip(ShapePill)
                            .clickable {
                                searchQuery = ""
                                selectedTypeFilter = null
                                selectedCategoryId = null
                                selectedAccountId = null
                                selectedDateRange = DateRangeFilter.ALL
                                selectedSortOption = SortOption.NEWEST
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = ShapeCard,
            color = if (isDark) PageBackgroundDark else PageBackgroundLight,
            border = BorderStroke(0.5.dp, if (isDark) DividerDark else Ramp.Purple.containerBorder(isDark)),
        ) {
            Column(modifier = Modifier.clip(ShapeCard)) {
                // "Recent activity" section identity is Purple (design system §"Section identity colors").
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) CardSurfaceDark else Ramp.Purple.tintFill(isDark))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = if (isDark) Ramp.Gray.c400 else Ramp.Purple.secondaryText(isDark),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Activity log",
                        style = SelfBudgetType.section,
                        color = if (isDark) TextPrimaryDark else Ramp.Purple.titleText(isDark),
                        modifier = Modifier.weight(1f),
                    )
                    val trailingText = when {
                        totalFilteredIncome > 0 && totalFilteredExpense == 0.0 -> "+$currencySymbol%.2f".format(totalFilteredIncome)
                        totalFilteredExpense > 0 && totalFilteredIncome == 0.0 -> "-$currencySymbol%.2f".format(totalFilteredExpense)
                        totalFilteredIncome > 0 -> "+$currencySymbol%.2f / -$currencySymbol%.2f".format(totalFilteredIncome, totalFilteredExpense)
                        else -> "${sortedEntries.size} record${if (sortedEntries.size != 1) "s" else ""}"
                    }
                    Text(
                        trailingText,
                        style = SelfBudgetType.meta,
                        color = if (isDark) TextSecondaryDark else Ramp.Purple.secondaryText(isDark),
                    )
                }

                if (sortedEntries.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (activeFilterCount > 0 || searchQuery.isNotBlank()) "No records match your active filters." else "No activity logged yet.",
                            style = SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 150.dp)
                    ) {
                        itemsIndexed(
                            items = sortedEntries,
                            key = { _, entry -> entry.id }
                        ) { index, entry ->
                            ActivityRowItem(
                                entry = entry,
                                categoryMap = categoryMap,
                                accountMap = accountMap,
                                currencySymbol = currencySymbol,
                                dateFormat = dateFormat,
                                isDark = isDark,
                                onEditTransaction = onEditTransaction
                            )

                            if (index < sortedEntries.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Unified Full-Screen Modal: Filter Options
    if (showFilterModal) {
        Dialog(
            onDismissRequest = { showFilterModal = false },
            properties = DialogProperties(
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Header Row: close · title · quiet reset (spec §24)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showFilterModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Filter activity",
                                style = SelfBudgetType.heading,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (activeFilterCount > 0) {
                            TextButton(
                                onClick = {
                                    selectedTypeFilter = null
                                    selectedCategoryId = null
                                    selectedAccountId = null
                                    selectedDateRange = DateRangeFilter.ALL
                                    selectedSortOption = SortOption.NEWEST
                                }
                            ) {
                                Text("Reset", style = SelfBudgetType.body, color = Ramp.Teal.secondaryText(isDark))
                            }
                        }
                    }

                    FilterChipGroup(
                        title = "Timeframe",
                        options = DateRangeFilter.entries,
                        optionLabel = { it.label },
                        isSelected = { it == selectedDateRange },
                        onSelect = { selectedDateRange = it }
                    )

                    // Section 2: Transaction Type — 8px leading dot instead of emoji (spec §24)
                    Column {
                        Text("Transaction type", style = SelfBudgetType.heading, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TypeFilterChip(
                                label = "All types",
                                dotColor = null,
                                selected = selectedTypeFilter == null,
                                onClick = { selectedTypeFilter = null }
                            )
                            TypeFilterChip(
                                label = "Expenses",
                                dotColor = Ramp.Red.c400,
                                selected = selectedTypeFilter == TransactionType.EXPENSE,
                                onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE }
                            )
                            TypeFilterChip(
                                label = "Income",
                                dotColor = Ramp.Teal.c400,
                                selected = selectedTypeFilter == TransactionType.INCOME,
                                onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME }
                            )
                            TypeFilterChip(
                                label = "Transfers",
                                dotColor = Ramp.Gray.c400,
                                selected = selectedTypeFilter == TransactionType.TRANSFER,
                                onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.TRANSFER) null else TransactionType.TRANSFER }
                            )
                        }
                    }

                    // Section 3: Bank Accounts
                    if (accounts.isNotEmpty()) {
                        FilterChipGroup(
                            title = "Bank account / wallet",
                            options = listOf<AccountEntity?>(null) + accounts,
                            optionLabel = { it?.name ?: "All accounts" },
                            isSelected = { it?.id == selectedAccountId },
                            onSelect = { selectedAccountId = it?.id }
                        )
                    }

                    FilterChipGroup(
                        title = "Sort order",
                        options = SortOption.entries,
                        optionLabel = { it.label },
                        isSelected = { it == selectedSortOption },
                        onSelect = { selectedSortOption = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer: Cancel (secondary) + one primary stating the result (spec §24)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = { showFilterModal = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )

                        PrimaryPillButton(
                            text = "Show ${sortedEntries.size} record${if (sortedEntries.size != 1) "s" else ""}",
                            onClick = { showFilterModal = false },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }
    }

    // Delete Confirmation Modal Dialog
    if (pendingDeleteTx != null) {
        val txToDelete = pendingDeleteTx!!
        val isIncome = txToDelete.type == TransactionType.INCOME

        Dialog(
            onDismissRequest = { pendingDeleteTx = null },
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
                    RampIconTile(icon = Icons.Default.Delete, ramp = Ramp.Red, size = 64.dp, iconSize = 32.dp, modifier = Modifier)

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

                    Surface(
                        shape = ShapeCard,
                        color = Ramp.Gray.tintFill(isDark),
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
                                text = txToDelete.title,
                                style = SelfBudgetType.rowTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${if (isIncome) "+$currencySymbol" else "-$currencySymbol"}%.2f".format(txToDelete.amount),
                                style = SelfBudgetType.rowTitle,
                                color = if (isIncome) Ramp.Teal.secondaryText(isDark) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = { pendingDeleteTx = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        // Confirm-dialog destructive action may be solid (spec §14).
                        androidx.compose.material3.Button(
                            onClick = {
                                onDeleteTransaction(txToDelete)
                                pendingDeleteTx = null
                            },
                            shape = ShapePill,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Ramp.Red.c400,
                                contentColor = androidx.compose.ui.graphics.Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Delete", style = SelfBudgetType.rowTitle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveFilterPill(
    label: String,
    onClear: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    Surface(
        shape = ShapePill,
        color = Ramp.Teal.tintFill(isDark),
        border = BorderStroke(0.5.dp, Ramp.Teal.c600)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = SelfBudgetType.badge,
                color = Ramp.Teal.titleText(isDark)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = Ramp.Teal.titleText(isDark),
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onClear() }
            )
        }
    }
}

/** A single-select chip group (spec §20/§24): "All" (or first option) is the default selection. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> FilterChipGroup(
    title: String,
    options: List<T>,
    optionLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit
) {
    val isDark = isAppInDarkTheme()
    Column {
        Text(title, style = SelfBudgetType.heading, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val selected = isSelected(option)
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(option) },
                    label = { Text(optionLabel(option), style = SelfBudgetType.badge) },
                    shape = ShapePill,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Ramp.Gray.tintFill(isDark),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = Ramp.Teal.solidFill(isDark),
                        selectedLabelColor = Ramp.Teal.onSolidFill(isDark)
                    )
                )
            }
        }
    }
}

/** A transaction-type filter chip with an 8px leading dot instead of an emoji (spec §24). */
@Composable
fun TypeFilterChip(
    label: String,
    dotColor: androidx.compose.ui.graphics.Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = SelfBudgetType.badge) },
        leadingIcon = if (dotColor != null) {
            {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        } else null,
        shape = ShapePill,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Ramp.Gray.tintFill(isDark),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = Ramp.Teal.solidFill(isDark),
            selectedLabelColor = Ramp.Teal.onSolidFill(isDark)
        )
    )
}

@Composable
private fun ActivityRowItem(
    entry: ActivityEntry,
    categoryMap: Map<String, CategoryEntity>,
    accountMap: Map<String, AccountEntity>,
    currencySymbol: String,
    dateFormat: SimpleDateFormat,
    isDark: Boolean,
    onEditTransaction: ((TransactionEntity) -> Unit)?
) {
    when (entry) {
        is ActivityEntry.Tx -> {
            val tx = entry.transaction
            val category = categoryMap[tx.categoryId]
            val account = accountMap[tx.accountId]
            val isIncome = tx.type == TransactionType.INCOME
            val isTransfer = tx.type == TransactionType.TRANSFER
            val sym = if (account?.currencyCode?.isNotBlank() == true) Currencies.symbolFor(account.currencyCode) else currencySymbol

            // Activity rows are colored by category identity, never by transaction sign (spec §10).
            val rowRamp = when {
                isTransfer -> Ramp.Gray
                isIncome -> Ramp.Teal
                category != null -> sectionRamp(getExpenseCategoryGroup(category))
                else -> Ramp.Gray
            }
            val icon = when {
                isTransfer -> Icons.Default.SwapHoriz
                category != null -> getCategoryIcon(category)
                isIncome -> Icons.Default.ArrowDownward
                else -> Icons.Default.ArrowUpward
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onEditTransaction != null) Modifier.clickable { onEditTransaction(tx) } else Modifier)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RampIconTile(icon = icon, ramp = rowRamp, size = 36.dp, iconSize = 18.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tx.title,
                            style = SelfBudgetType.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${category?.name ?: "General"}${if (account != null) " · ${account.name}" else ""} · ${dateFormat.format(Date(tx.timestamp))}",
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!tx.note.isNullOrBlank()) {
                            Text(
                                text = "Note: ${tx.note}",
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Income reads Teal; transfers and ordinary expenses read neutral —
                // red is reserved for over-limit, not ordinary spending (spec §10/§13).
                val amountPrefix = if (isIncome) "+$sym" else if (isTransfer) sym else "-$sym"
                val amountColor = if (isIncome) Ramp.Teal.secondaryText(isDark) else MaterialTheme.colorScheme.onSurface
                Text(
                    text = "$amountPrefix%.2f".format(tx.amount),
                    style = SelfBudgetType.rowTitle,
                    color = amountColor
                )
            }
        }

        is ActivityEntry.GoalCreated -> {
            val goal = entry.goal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RampIconTile(icon = Icons.Default.Savings, ramp = Ramp.Purple, size = 36.dp, iconSize = 18.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Goal created: ${goal.name}",
                            style = SelfBudgetType.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Target $currencySymbol%.2f · ${dateFormat.format(Date(goal.createdAt))}".format(goal.targetAmount),
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is ActivityEntry.RecurringAdded -> {
            val rec = entry.recurring
            val category = categoryMap[rec.categoryId]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RampIconTile(icon = Icons.Default.Repeat, ramp = Ramp.Purple, size = 36.dp, iconSize = 18.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Recurring added: ${rec.title}",
                            style = SelfBudgetType.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${category?.name ?: "General"} · ${rec.frequency.name.lowercase().replaceFirstChar { it.uppercase() }} · ${dateFormat.format(Date(rec.createdAt))}",
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "$currencySymbol%.2f".format(rec.amount),
                    style = SelfBudgetType.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        is ActivityEntry.AccountAdded -> {
            val account = entry.account
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RampIconTile(icon = Icons.Default.AccountBalanceWallet, ramp = Ramp.Purple, size = 36.dp, iconSize = 18.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Account added: ${account.name}",
                            style = SelfBudgetType.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${account.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${dateFormat.format(Date(account.createdAt))}",
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is ActivityEntry.CategoryAdded -> {
            val category = entry.category
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RampIconTile(icon = getCategoryIcon(category), ramp = sectionRamp(getExpenseCategoryGroup(category)), size = 36.dp, iconSize = 18.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Category added: ${category.name}",
                            style = SelfBudgetType.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${category.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${dateFormat.format(Date(category.createdAt))}",
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is ActivityEntry.LogEvent -> {
            val logEntry = entry.entry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RampIconTile(icon = activityLogIcon(logEntry.action), ramp = activityLogRamp(logEntry.action), size = 36.dp, iconSize = 18.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${activityLogLabel(logEntry)}: ${logEntry.title}",
                            style = SelfBudgetType.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateFormat.format(Date(logEntry.timestamp)),
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (logEntry.amount != null) {
                    val isContribution = logEntry.action == ActivityAction.CONTRIBUTED
                    Text(
                        text = "${if (isContribution) "+$currencySymbol" else currencySymbol}%.2f".format(logEntry.amount),
                        style = SelfBudgetType.rowTitle,
                        color = if (isContribution) Ramp.Teal.secondaryText(isDark) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
