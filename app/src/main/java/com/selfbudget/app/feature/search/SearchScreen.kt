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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.sectionRamp
import com.selfbudget.app.ui.theme.solidFill
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
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
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)

        val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastMonthYear = lastMonthCal.get(Calendar.YEAR)
        val lastMonthMonth = lastMonthCal.get(Calendar.MONTH)

        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    (tx.note?.contains(searchQuery, ignoreCase = true) == true) ||
                    (accountMap[tx.accountId]?.name?.contains(searchQuery, ignoreCase = true) == true)

            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val matchesCategory = selectedCategoryId == null || tx.categoryId == selectedCategoryId
            val matchesAccount = selectedAccountId == null || tx.accountId == selectedAccountId || tx.transferAccountId == selectedAccountId

            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val matchesDate = when (selectedDateRange) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.THIS_MONTH -> txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth
                DateRangeFilter.LAST_MONTH -> txCal.get(Calendar.YEAR) == lastMonthYear && txCal.get(Calendar.MONTH) == lastMonthMonth
                DateRangeFilter.LAST_30_DAYS -> tx.timestamp >= thirtyDaysAgo
                DateRangeFilter.THIS_YEAR -> txCal.get(Calendar.YEAR) == currentYear
            }

            matchesQuery && matchesType && matchesCategory && matchesAccount && matchesDate
        }
    }

    val sortedTransactions = remember(filteredTransactions, selectedSortOption) {
        when (selectedSortOption) {
            SortOption.NEWEST -> filteredTransactions.sortedByDescending { it.timestamp }
            SortOption.OLDEST -> filteredTransactions.sortedBy { it.timestamp }
            SortOption.HIGHEST_AMOUNT -> filteredTransactions.sortedByDescending { it.amount }
            SortOption.LOWEST_AMOUNT -> filteredTransactions.sortedBy { it.amount }
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

        LazyColumn(
            contentPadding = PaddingValues(bottom = 150.dp)
        ) {
            item {
                // "Recent activity" section identity is Purple (design system §"Section identity colors").
                SectionHeaderBand(
                    title = "Activity log",
                    ramp = Ramp.Purple,
                    icon = Icons.Default.History,
                    trailingText = when {
                        totalFilteredIncome > 0 && totalFilteredExpense == 0.0 -> "+$currencySymbol%.2f".format(totalFilteredIncome)
                        totalFilteredExpense > 0 && totalFilteredIncome == 0.0 -> "-$currencySymbol%.2f".format(totalFilteredExpense)
                        totalFilteredIncome > 0 -> "+$currencySymbol%.2f / -$currencySymbol%.2f".format(totalFilteredIncome, totalFilteredExpense)
                        else -> "${sortedTransactions.size} record${if (sortedTransactions.size != 1) "s" else ""}"
                    }
                ) {
                    if (sortedTransactions.isEmpty()) {
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
                        sortedTransactions.forEachIndexed { index, tx ->
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

                            if (index < sortedTransactions.lastIndex) {
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
                            text = "Show ${sortedTransactions.size} transaction${if (sortedTransactions.size != 1) "s" else ""}",
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
                        selectedLabelColor = Ramp.Teal.let { if (isDark) it.c900 else it.c50 }
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
            selectedLabelColor = Ramp.Teal.let { if (isDark) it.c900 else it.c50 }
        )
    )
}
